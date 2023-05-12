package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.bmsanbar.GlobalParameters.cameraScanning;
import static az.inci.bmsanbar.GlobalParameters.jwt;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.ShipTrx;
import az.inci.bmsanbar.model.v2.CustomResponse;
import az.inci.bmsanbar.model.v2.ShipDocInfo;
import az.inci.bmsanbar.model.v3.ShipmentRequest;
import az.inci.bmsanbar.model.v3.ShipmentRequestItem;
import okhttp3.ResponseBody;

public class ShipTrxActivity extends ScannerSupportActivity
{

    static final int SCAN_DRIVER_CODE = 0;
    static final int SCAN_VEHICLE_CODE = 1;
    static final int SCAN_NEW_DOC = 2;
    int mode;
    String driverCode = "";

    String driverName;
    String vehicleCode;
    String barcode;
    ListView trxListView;
    Button scanDriverCode;
    Button scanVehicleCode;
    Button scanNewDoc;
    EditText driverCodeEditText;
    TextView driverNameText;
    EditText vehicleCodeEditText;
    ImageButton send;
    CheckBox checkMode;
    CheckBox toCentralCheck;
    List<ShipTrx> trxList;
    boolean docCreated = false;
    boolean checkModeOn;
    boolean toCentral;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ship_trx_layout);
        mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);

        driverCodeEditText = findViewById(R.id.driver);
        driverNameText = findViewById(R.id.driver_name);
        vehicleCodeEditText = findViewById(R.id.vehicle);
        scanDriverCode = findViewById(R.id.scan_driver_code);
        scanVehicleCode = findViewById(R.id.scan_vehicle_code);
        scanNewDoc = findViewById(R.id.scan_new_doc);
        trxListView = findViewById(R.id.ship_trx_list_view);
        send = findViewById(R.id.send);
        checkMode = findViewById(R.id.check_mode);
        toCentralCheck = findViewById(R.id.to_central_check);

        checkModeOn = checkMode.isChecked();

        if (cameraScanning)
        {
            scanDriverCode.setVisibility(View.VISIBLE);
            scanVehicleCode.setVisibility(View.VISIBLE);
            scanNewDoc.setVisibility(View.VISIBLE);

            scanDriverCode.setEnabled(!checkModeOn);
            scanVehicleCode.setEnabled(!checkModeOn);
        }

        checkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkModeOn = isChecked;
            scanDriverCode.setEnabled(!checkModeOn);
            scanVehicleCode.setEnabled(!checkModeOn);
        });

        toCentralCheck.setOnCheckedChangeListener((buttonView, isChecked) -> toCentral = isChecked);

        send.setOnClickListener(v -> {
            if (trxList.size() > 0 && !checkModeOn && docCreated)
                createShipment();
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(R.string.want_to_delete)
                         .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                             ShipTrx trx = (ShipTrx) parent.getItemAtPosition(position);
                             if (checkModeOn)
                                 trxList.remove(trx);
                             else
                                 dbHelper.deleteShipTrxBySrc(trx.getSrcTrxNo());
                             loadData();
                         })
                         .setNegativeButton(R.string.cancel, null);
            dialogBuilder.show();
            return true;
        });

        if (mode == AppConfig.VIEW_MODE)
        {
            docCreated = true;
            driverCode = getIntent().getStringExtra("driverCode");
            driverName = getIntent().getStringExtra("driverName");
            vehicleCode = getIntent().getStringExtra("vehicleCode");
            driverCodeEditText.setText(driverCode);
            driverNameText.setText(driverName);
            vehicleCodeEditText.setText(vehicleCode);
            scanDriverCode.setVisibility(View.GONE);
            scanVehicleCode.setVisibility(View.GONE);

            loadData();
        }

        Intent intent = new Intent(this, BarcodeScannerCamera.class);
        scanDriverCode.setOnClickListener(v -> startActivityForResult(intent, SCAN_DRIVER_CODE));

        scanVehicleCode.setOnClickListener(v -> startActivityForResult(intent, SCAN_VEHICLE_CODE));

        scanNewDoc.setOnClickListener(v -> {
            if ((driverCode == null || vehicleCode == null) && !checkModeOn)
            {
                showMessageDialog(getString(R.string.info),
                                  getString(R.string.driver_or_vehicle_not_defined),
                                  ic_dialog_info);
                return;
            }
            startActivityForResult(intent, SCAN_NEW_DOC);
        });

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        this.barcode = barcode;

        if (docCreated || checkModeOn)
            validateShipping(this.barcode);
        else
        {
            if (this.barcode.startsWith("PER"))
                setDriverCode(this.barcode);
            else
                setVehicleCode(this.barcode);

            if (driverCode != null && !driverCode.isEmpty() && vehicleCode != null &&
                !vehicleCode.isEmpty())
                docCreated = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        barcode = data.getStringExtra("barcode");

        if (resultCode == 1 && barcode != null)
            switch (requestCode)
            {
                case SCAN_DRIVER_CODE:
                    setDriverCode(barcode);
                    break;
                case SCAN_VEHICLE_CODE:
                    setVehicleCode(barcode);
                    break;
                case SCAN_NEW_DOC:
                    validateShipping(barcode);
                    break;
            }
    }

    public void setDriverCode(String driverCode)
    {
        if (driverCode.startsWith("PER"))
        {
            this.driverCode = driverCode;
            showProgressDialog(true);
            new Thread(() -> {
                String url = url("personnel", "get-name");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("per-code", driverCode);
                url = addRequestParameters(url, parameters);
                driverName = getSimpleObject(url, "GET", null, String.class);
                if (driverName != null)
                    runOnUiThread(() -> {
                        if (!driverName.isEmpty())
                        {
                            this.driverCode = driverCode;
                            driverCodeEditText.setText(driverCode);
                            driverNameText.setText(driverName);
                            playSound(SOUND_SUCCESS);
                        }
                        else
                        {
                            showMessageDialog(getString(R.string.error),
                                              getString(R.string.driver_code_incorrect),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        }
                    });
            }).start();
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                              ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void setVehicleCode(String vehicleCode)
    {
        if (!vehicleCode.startsWith("PER"))
        {
            if (vehicleCode.startsWith("VHC"))
                vehicleCode = vehicleCode.substring(3);
            this.vehicleCode = vehicleCode;
            vehicleCodeEditText.setText(vehicleCode);
            playSound(SOUND_SUCCESS);
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.vehicle_code_incorrect),
                              ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void validateShipping(String trxNo)
    {
        if (!checkModeOn && dbHelper.isShipped(trxNo))
        {
            showMessageDialog(getString(R.string.error), getString(R.string.doc_already_loaded),
                              ic_dialog_alert);
            playSound(SOUND_FAIL);
            return;
        }


        checkShipment(trxNo);
    }

    public void addDoc(String trxNo, boolean taxed)
    {
        ShipTrx trx = new ShipTrx();
        trx.setSrcTrxNo(trxNo);
        trx.setDriverCode(driverCode);
        trx.setDriverName(driverName);
        trx.setVehicleCode(vehicleCode);
        trx.setRegionCode("SHR0000001");
        trx.setUserId(config().getUser().getId());
        trx.setTaxed(taxed);

        if (checkModeOn)
        {
            trxList = new ArrayList<>();
            trxList.add(trx);
        }
        else
            dbHelper.addShipTrx(trx);
        scanVehicleCode.setVisibility(View.GONE);
        scanDriverCode.setVisibility(View.GONE);
        loadData();
    }

    public void loadData()
    {
        if (!checkModeOn)
            trxList = dbHelper.getShipTrx(driverCode);

        if (trxList == null)
            trxList = new ArrayList<>();

        ArrayAdapter<ShipTrx> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout,
                                                           trxList);
        trxListView.setAdapter(adapter);
    }

    private void checkShipment(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("shipment", "check-shipment");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            try
            {
                okhttp3.Response httpResponse = sendRequest(new URL(url), "GET", null);
                if (httpResponse.code() == 403)
                {
                    jwt = jwtResolver.resolve();
                    preferences.edit().putString("jwt", jwt).apply();
                    httpResponse = sendRequest(new URL(url), "GET", null);
                }
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = new Gson().fromJson(responseBody.string(),
                                                              new TypeToken<CustomResponse>()
                                                              {}.getType());
                if (response.getStatusCode() == 0)
                {
                    ShipDocInfo docInfo = gson.fromJson(gson.toJson(response.getData()),
                                                        new TypeToken<ShipDocInfo>()
                                                        {}.getType());

                    runOnUiThread(() -> {
                        if (checkModeOn || docInfo == null)
                        {
                            if (trxNo.startsWith("DLV") || trxNo.startsWith("SIN"))
                                checkTaxed(trxNo);
                            else
                                addDoc(trxNo, false);
                            playSound(SOUND_SUCCESS);
                        }
                        else
                        {
                            showMessageDialog(getString(R.string.error),
                                              "Bu sənəd yüklənib: " + docInfo.getDriverCode() +
                                              " - " + docInfo.getDriverName(),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        }
                    });
                }
                else if (response.getStatusCode() == 2)
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error), response.getDeveloperMessage(),
                                          ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                else
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error),
                                          response.getDeveloperMessage() + ": " +
                                          response.getSystemMessage(),
                                          ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
            }
            catch (IOException e)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                                      getString(R.string.internal_error) + ": " + e.getMessage(),
                                      ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            }
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void checkTaxed(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("doc", "taxed");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            Boolean result = getSimpleObject(url, "GET", null, Boolean.class);
            if (result != null)
                runOnUiThread(() -> addDoc(trxNo, result));
        }).start();
    }

    public void createShipment()
    {
        showProgressDialog(true);
        List<ShipmentRequest> requestList = new ArrayList<>();
        String shipStatus = toCentral ? "MG" : "AC";
        String url = url("shipment", "create-shipment");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("user-id", config().getUser().getId());
        url = addRequestParameters(url, parameters);
        ShipmentRequest request = ShipmentRequest.builder()
                                                 .regionCode(trxList.get(0).getRegionCode())
                                                 .driverCode(trxList.get(0).getDriverCode())
                                                 .vehicleCode(trxList.get(0).getVehicleCode())
                                                 .build();
        List<ShipmentRequestItem> requestItems = new ArrayList<>();
        for (ShipTrx trx : trxList)
        {
            ShipmentRequestItem requestItem = ShipmentRequestItem.builder()
                                                                 .srcTrxNo(trx.getSrcTrxNo())
                                                                 .shipStatus(shipStatus)
                                                                 .build();
            requestItems.add(requestItem);
        }
        request.setRequestItems(requestItems);

        executeUpdate(url, requestList, message -> {
            showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());

            if (message.getStatusCode() == 0)
            {
                dbHelper.deleteShipTrxByDriver(driverCode);
                clearFields();
            }
        });
    }


    private void clearFields()
    {
        driverCode = "";
        driverName = "";
        vehicleCode = "";
        driverCodeEditText.setText("");
        driverNameText.setText("");
        vehicleCodeEditText.setText("");
        toCentralCheck.setChecked(false);
        toCentral = false;
        docCreated = false;
        trxList.clear();
        loadData();
    }
}
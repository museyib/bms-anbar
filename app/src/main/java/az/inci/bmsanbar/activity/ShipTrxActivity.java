package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.GlobalParameters.cameraScanning;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.ShipTrx;
import az.inci.bmsanbar.model.v3.CheckShipmentResponse;
import az.inci.bmsanbar.model.v3.ShipmentRequest;
import az.inci.bmsanbar.model.v3.ShipmentRequestItem;

public class ShipTrxActivity extends ScannerSupportActivity
{
    private String driverCode;
    private String driverName;
    private String vehicleCode;
    private ListView trxListView;
    private EditText driverCodeEditText;
    private TextView driverNameText;
    private EditText vehicleCodeEditText;
    private CheckBox toCentralCheck;
    private List<ShipTrx> trxList;
    private boolean docCreated;
    private boolean checkModeOn;
    private boolean toCentral;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ship_trx_layout);
        int mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);

        driverCodeEditText = findViewById(R.id.driver);
        driverNameText = findViewById(R.id.driver_name);
        vehicleCodeEditText = findViewById(R.id.vehicle);
        trxListView = findViewById(R.id.ship_trx_list_view);
        toCentralCheck = findViewById(R.id.to_central_check);

        Button scanCam = findViewById(R.id.scan_cam);
        ImageButton send = findViewById(R.id.send);
        CheckBox checkMode = findViewById(R.id.check_mode);

        checkModeOn = checkMode.isChecked();

        checkMode.setOnCheckedChangeListener((buttonView, isChecked) -> checkModeOn = isChecked);

        toCentralCheck.setOnCheckedChangeListener((buttonView, isChecked) -> toCentral = isChecked);

        send.setOnClickListener(v -> {
            if(!trxList.isEmpty() && !checkModeOn && docCreated)
                createShipment();
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(R.string.want_to_delete)
                         .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                             ShipTrx trx = (ShipTrx) parent.getItemAtPosition(position);
                             if(checkModeOn)
                                 trxList.remove(trx);
                             else
                                 dbHelper.deleteShipTrxBySrc(trx.getSrcTrxNo());
                             loadData();
                         })
                         .setNegativeButton(R.string.cancel, null);
            dialogBuilder.show();
            return true;
        });

        if(mode == AppConfig.VIEW_MODE)
        {
            docCreated = true;
            driverCode = getIntent().getStringExtra("driverCode");
            driverName = getIntent().getStringExtra("driverName");
            vehicleCode = getIntent().getStringExtra("vehicleCode");
            driverCodeEditText.setText(driverCode);
            driverNameText.setText(driverName);
            vehicleCodeEditText.setText(vehicleCode);

            loadData();
        }

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode)
    {

        if(docCreated || checkModeOn)
            validateShipping(barcode);
        else
        {
            if(barcode.startsWith("PER"))
                setDriverCode(barcode);
            else
                setVehicleCode(barcode);

            if(!isEmpty(driverCode) && !isEmpty(vehicleCode))
                docCreated = true;
        }
    }

    public void setDriverCode(String driverCode)
    {
        if(driverCode.startsWith("PER"))
        {
            this.driverCode = driverCode;
            showProgressDialog(true);
            new Thread(() -> {
                String url = url("personnel", "get-name");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("per-code", driverCode);
                url = addRequestParameters(url, parameters);
                driverName = getSimpleObject(url, "GET", null, String.class);
                if(driverName != null)
                    runOnUiThread(() -> {
                        if(!driverName.isEmpty())
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
        if(!vehicleCode.startsWith("PER"))
        {
            if(vehicleCode.startsWith("VHC"))
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
        if(!checkModeOn && dbHelper.isShipped(trxNo))
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

        if(checkModeOn)
        {
            trxList = new ArrayList<>();
            trxList.add(trx);
        }
        else
            dbHelper.addShipTrx(trx);
        loadData();
    }

    public void loadData()
    {
        if(!checkModeOn)
            trxList = dbHelper.getShipTrx(driverCode);

        if(trxList == null)
            trxList = new ArrayList<>();

        ArrayAdapter<ShipTrx> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item,
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
            parameters.put("driver-code", driverCode);
            url = addRequestParameters(url, parameters);
            CheckShipmentResponse shipmentResponse = getSimpleObject(url, "GET", null,
                                                                     CheckShipmentResponse.class);

            if(shipmentResponse != null)
                runOnUiThread(() -> {
                    if(checkModeOn || !shipmentResponse.isShipped())
                    {
                        if(trxNo.startsWith("DLV") || trxNo.startsWith("SIN"))
                            checkTaxed(trxNo);
                        else
                            addDoc(trxNo, false);
                        playSound(SOUND_SUCCESS);
                    }
                    else
                    {
                        showMessageDialog(getString(R.string.error),
                                          "Bu sənəd yüklənib: " + shipmentResponse.getDriverCode() +
                                          " - " + shipmentResponse.getDriverName(),
                                          ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    }
                });
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
            if(result != null)
                runOnUiThread(() -> addDoc(trxNo, result));
        }).start();
    }

    public void createShipment()
    {
        showProgressDialog(true);
        String shipStatus = toCentral ? "MG" : "AC";
        String url = url("shipment", "create-shipment");
        ShipmentRequest request = ShipmentRequest.builder()
                                                 .regionCode(trxList.get(0).getRegionCode())
                                                 .driverCode(trxList.get(0).getDriverCode())
                                                 .vehicleCode(trxList.get(0).getVehicleCode())
                                                 .userId(config().getUser().getId())
                                                 .build();
        List<ShipmentRequestItem> requestItems = new ArrayList<>();
        for(ShipTrx trx : trxList)
        {
            ShipmentRequestItem requestItem = ShipmentRequestItem.builder()
                                                                 .srcTrxNo(trx.getSrcTrxNo())
                                                                 .shipStatus(shipStatus)
                                                                 .build();
            requestItems.add(requestItem);
        }
        request.setRequestItems(requestItems);

        executeUpdate(url, request, message -> {
            showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());

            if(message.getStatusCode() == 0)
            {
                dbHelper.deleteShipTrxByDriver(driverCode);
                clearFields();
            }
            else
                playSound(SOUND_FAIL);
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
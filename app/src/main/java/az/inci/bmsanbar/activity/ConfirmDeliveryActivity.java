package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.v2.ShipDocInfo;
import az.inci.bmsanbar.model.v2.UpdateDeliveryRequest;

public class ConfirmDeliveryActivity extends ScannerSupportActivity
{

    static final int SCAN_DRIVER_CODE = 0;
    static final int SCAN_NEW_DOC = 1;
    String driverCode;
    String barcode;
    ListView docListView;
    Button scanDriverCode;
    Button scanNewDoc;
    Button cancel;
    EditText driverCodeEditText;
    ImageButton send;
    List<String> docList;
    boolean docCreated = false;
    private String note;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_delivery_layout);

        driverCodeEditText = findViewById(R.id.driver);
        scanDriverCode = findViewById(R.id.scan_driver_code);
        scanNewDoc = findViewById(R.id.scan_new_doc);
        docListView = findViewById(R.id.ship_trx_list_view);
        send = findViewById(R.id.send);
        cancel = findViewById(R.id.cancel_button);

        if (config().isCameraScanning())
        {
            scanDriverCode.setVisibility(View.VISIBLE);
            scanNewDoc.setVisibility(View.VISIBLE);
        }

        docList = new ArrayList<>();

        send.setOnClickListener(v -> {
            if (docList.size() > 0)
            {
                changeDocStatus();
            }
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.want_to_delete)
                                                              .setPositiveButton(R.string.delete,
                                                                                 (dialogInterface, i) -> {
                                                                                     String trxNo = (String) parent.getItemAtPosition(
                                                                                             position);
                                                                                     docList.remove(
                                                                                             trxNo);
                                                                                     loadData();
                                                                                 })
                                                              .setNegativeButton(R.string.cancel,
                                                                                 null)
                                                              .create();
            dialog.show();
            return true;
        });

        scanDriverCode.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmDeliveryActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_DRIVER_CODE);
        });

        scanNewDoc.setOnClickListener(v -> {
            if (!docCreated)
            {
                showMessageDialog(getString(R.string.info), getString(R.string.driver_not_defined),
                                  android.R.drawable.ic_dialog_info);
                return;
            }
            Intent intent = new Intent(ConfirmDeliveryActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_NEW_DOC);
        });

        cancel.setOnClickListener(v -> clearFields());

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        this.barcode = barcode;

        if (docCreated)
        {
            getShipDetails(barcode);
        }
        else
        {
            setDriverCode(barcode);

            docCreated = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        barcode = data.getStringExtra("barcode");

        if (resultCode == 1 && barcode != null)
        {
            switch (requestCode)
            {
                case SCAN_DRIVER_CODE:
                    setDriverCode(barcode);
                    break;
                case SCAN_NEW_DOC:
                    getShipDetails(barcode);
                    break;
            }
        }
    }

    public void setDriverCode(String driverCode)
    {
        if (driverCode.startsWith("PER"))
        {
            showProgressDialog(true);
            new Thread(() -> {
                String url = url("personnel", "get-name");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("per-code", driverCode);
                url = addRequestParameters(url, parameters);
                Log.e("URL", url);
                String perName = getSimpleObject(url, "GET", null, String.class);
                if (perName != null)
                {
                    runOnUiThread(() -> {
                        if (!perName.isEmpty())
                        {
                            this.driverCode = driverCode;
                            driverCodeEditText.setText(driverCode);
                            ((TextView) findViewById(R.id.driver_name)).setText(perName);
                            playSound(SOUND_SUCCESS);
                        }
                        else
                        {
                            showMessageDialog(getString(R.string.error),
                                              getString(R.string.driver_code_incorrect),
                                              android.R.drawable.ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        }
                    });
                }
            }).start();
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                              android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void loadData()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, docList);
        docListView.setAdapter(adapter);
    }

    private void getShipDetails(String trxNo)
    {
        if (docList.contains(trxNo))
            return;

        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "doc-info-for-confirm");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);
            runOnUiThread(() -> addDoc(trxNo, docInfo));
        }).start();
    }

    private void addDoc(String trxNo, ShipDocInfo docInfo)
    {
        if (docInfo != null)
        {
            if (!driverCode.equals(docInfo.getDriverCode()))
            {
                showMessageDialog(getString(R.string.info),
                                  getString(R.string.not_shipped_for_current_driver) +
                                  "\n\nYükləndiyi sürücü  və N/V nömrəsi:\n" +
                                  docInfo.getDriverName() + " - " + docInfo.getVehicleCode() +
                                  "\n" + docInfo.getDeliverNotes(),
                                  android.R.drawable.ic_dialog_info);
                playSound(SOUND_FAIL);
                return;
            }

//            String status = docInfo.getShipStatus();
//
//            if (status.equals("AC"))
//            {
//                showMessageDialog(getString(R.string.info),
//                        "DİQQƏT!!! Bu sənəd qapıdan çıxışa vurulmayıb!",
//                        android.R.drawable.ic_dialog_info);
//                playSound(SOUND_FAIL);
//            }

            playSound(SOUND_SUCCESS);
            docList.add(trxNo);
            scanDriverCode.setVisibility(View.GONE);
            loadData();
        }
    }

    private void changeDocStatus()
    {
        showProgressDialog(true);
        new Thread(() -> {
            List<UpdateDeliveryRequest> requestList = new ArrayList<>();
            note = "İstifadəçi: " + config().getUser().getId();
            String url = url("logistics", "confirm-shipment");
            for (String trxNo : docList)
            {
                UpdateDeliveryRequest request = new UpdateDeliveryRequest();
                request.setTrxNo(trxNo);
                request.setNote(note);
                request.setDeliverPerson("");
                request.setDriverCode(driverCode);
                requestList.add(request);
            }
            executeUpdate(url, requestList, message -> {
                {
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());

                    if (message.getStatusCode() == 0)
                    {
                        clearFields();
                    }
                }
            });
        }).start();
    }

    private void clearFields()
    {
        driverCode = "";
        driverCodeEditText.setText("");
        ((TextView) findViewById(R.id.driver_name)).setText("");
        docCreated = false;
        docList.clear();
        scanDriverCode.setVisibility(View.VISIBLE);
        loadData();
    }
}
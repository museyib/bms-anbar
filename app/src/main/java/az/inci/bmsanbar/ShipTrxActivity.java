package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.util.List;

public class ShipTrxActivity extends ScannerSupportActivity {

    static final int SCAN_DRIVER_CODE=0;
    static final int SCAN_VEHICLE_CODE=1;
    static final int SCAN_NEW_DOC=2;

    private boolean docCreated=false;

    int mode;
    String driverCode;
    String vehicleCode;
    String barcode;

    ListView trxListView;
    Button scanDriverCode;
    Button scanVehicleCode;
    Button scanNewDoc;
    EditText driverCodeEditText;
    EditText vehicleCodeEditText;
    ImageButton send;

    List<ShipTrx> trxList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ship_trx_layout);
        mode=getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);

        driverCodeEditText=findViewById(R.id.driver);
        vehicleCodeEditText=findViewById(R.id.vehicle);
        scanDriverCode=findViewById(R.id.scan_driver_code);
        scanVehicleCode=findViewById(R.id.scan_vehicle_code);
        scanNewDoc =findViewById(R.id.scan_new_doc);
        trxListView=findViewById(R.id.ship_trx_list_view);
        send=findViewById(R.id.send);

        if (model.equals("C4000_6582"))
        {
            scanDriverCode.setVisibility(View.GONE);
            scanVehicleCode.setVisibility(View.GONE);
            scanNewDoc.setVisibility(View.GONE);
        }

        send.setOnClickListener(v -> new SendSipping(ShipTrxActivity.this).execute());

        trxListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog dialog=new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage(R.string.want_to_delete)
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                        ShipTrx trx= (ShipTrx) parent.getItemAtPosition(position);
                        dbHelper.deleteShipTrxBySrc(trx.getSrcTrxNo());
                        loadTrx();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
            return true;
        });

        if (mode==AppConfig.VIEW_MODE)
        {
            docCreated=true;
            driverCode=getIntent().getStringExtra("driverCode");
            vehicleCode=getIntent().getStringExtra("vehicleCode");
            driverCodeEditText.setText(driverCode);
            vehicleCodeEditText.setText(vehicleCode);
            scanDriverCode.setVisibility(View.GONE);
            scanVehicleCode.setVisibility(View.GONE);

            loadTrx();
        }

        scanDriverCode.setOnClickListener(v -> {
            Intent intent=new Intent(ShipTrxActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_DRIVER_CODE);
        });

        scanVehicleCode.setOnClickListener(v -> {
            Intent intent=new Intent(ShipTrxActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_VEHICLE_CODE);
        });

        scanNewDoc.setOnClickListener(v -> {
            if (driverCode==null || vehicleCode==null)
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.driver_or_vehicle_not_defined),
                        android.R.drawable.ic_dialog_info);
                return;
            }
            Intent intent=new Intent(ShipTrxActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_NEW_DOC);
        });

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode) {
        busy=false;
        this.barcode=barcode;
        if (docCreated)
        {
            checkShipping(this.barcode);
        }
        else
        {
            if (barcode.startsWith("ITO") || barcode.startsWith("DLV")) {
                showMessageDialog(getString(R.string.info), getString(R.string.driver_or_vehicle_not_defined),
                        android.R.drawable.ic_dialog_info);
                playSound(SOUND_FAIL);
                return;
            }

            if (this.barcode.startsWith("PER"))
            {
                setDriverCode(this.barcode);
            }
            else
            {
                setVehicleCode(this.barcode);
            }

            if (driverCode!=null && vehicleCode!=null)
                docCreated=true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        barcode = data.getStringExtra("barcode");

        if (resultCode==1 && barcode!=null)
        {
            switch (requestCode)
            {
                case SCAN_DRIVER_CODE:
                    setDriverCode(barcode);
                    break;
                case SCAN_VEHICLE_CODE:
                    setVehicleCode(barcode);
                    break;
                case SCAN_NEW_DOC:
                    checkShipping(barcode);
                    break;
            }
        }
    }

    public void setDriverCode(String driverCode) {
        if (driverCode.startsWith("PER")) {
            this.driverCode = driverCode;
            driverCodeEditText.setText(driverCode);
            playSound(SOUND_SUCCESS);
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                    android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void setVehicleCode(String vehicleCode) {
        if (!vehicleCode.startsWith("PER")) {
            this.vehicleCode = vehicleCode;
            vehicleCodeEditText.setText(vehicleCode);
            playSound(SOUND_SUCCESS);
        }
        else {
            showMessageDialog(getString(R.string.error), getString(R.string.vehicle_code_incorrect),
                    android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void checkShipping(String trxNo)
    {
        if (!trxNo.startsWith("ITO") && !trxNo.startsWith("DLV")) {
            showMessageDialog(getString(R.string.info), getString(R.string.not_valid_doc_for_shipping),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
            return;
        }

        if (dbHelper.isShipped(trxNo))
        {
            showMessageDialog(getString(R.string.error), getString(R.string.doc_already_loaded),
                    android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
            return;
        }
        String url=url("trx", "shipped", trxNo);
        new ShippingCheck(this).execute(url);
    }

    public void addDoc(String trxNo)
    {
        ShipTrx trx=new ShipTrx();
        trx.setSrcTrxNo(trxNo);
        trx.setDriverCode(driverCode);
        trx.setVehicleCode(vehicleCode);
        trx.setRegionCode("SHR0000001");
        trx.setUserId(config().getUser().getId());
        dbHelper.addShipTrx(trx);
        scanVehicleCode.setVisibility(View.GONE);
        scanDriverCode.setVisibility(View.GONE);
        loadTrx();
    }

    void loadTrx()
    {
        trxList=dbHelper.getShipTrx(driverCode);
        ArrayAdapter<ShipTrx> adapter=new ArrayAdapter<>(this, R.layout.ship_trx_item_layout, trxList);
        trxListView.setAdapter(adapter);
    }

    private static class ShippingCheck extends AsyncTask<String, Void, Boolean>
    {
        WeakReference<ShipTrxActivity> reference;

        public ShippingCheck(ShipTrxActivity activity)
        {
            reference=new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(String... url) {
            RestTemplate template=new RestTemplate();
            template.getMessageConverters().add(new StringHttpMessageConverter());
            boolean result;
            try {
                result = template.getForObject(url[0], Boolean.class);
            }
            catch (ResourceAccessException e)
            {
                e.printStackTrace();
                return false;
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
                return false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            ShipTrxActivity activity=reference.get();
            if (aBoolean)
            {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.doc_already_loaded),
                        android.R.drawable.ic_dialog_alert);
            }
            else
                activity.addDoc(activity.barcode);
        }
    }

    private static class SendSipping extends AsyncTask<Void, Void, Boolean>
    {
        WeakReference<ShipTrxActivity> reference;
        List<ShipTrx> trxList;

        public SendSipping(ShipTrxActivity activity)
        {
            reference=new WeakReference<>(activity);
            trxList=reference.get().trxList;
        }

        @Override
        protected void onPreExecute() {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected Boolean doInBackground(Void... aVoid) {
            ShipTrxActivity activity=reference.get();
            RestTemplate template=new RestTemplate();
            template.getMessageConverters().add(new StringHttpMessageConverter());
            ((SimpleClientHttpRequestFactory)template.getRequestFactory()).setConnectTimeout(activity.config().getConnectionTimeout()*1000);
            boolean result = false;
            for (Object item : trxList)
            {
                ShipTrx trx=(ShipTrx)item;
                String url=activity.url("trx","ship",
                        trx.getRegionCode(),
                        trx.getDriverCode(),
                        trx.getSrcTrxNo(),
                        trx.getVehicleCode(),
                        trx.getUserId());
                try {
                    result = template.postForObject(url, null, Boolean.class);
                }
                catch (ResourceAccessException ex)
                {
                    ex.printStackTrace();
                    result=false;
                }
                catch (RuntimeException ex) {
                    ex.printStackTrace();
                    result=false;
                }
            }

            if (result)
            {
                String url=activity.url("trx","ship", activity.config().getUser().getId());
                try {
                    result = template.postForObject(url, null, Boolean.class);
                }
                catch (ResourceAccessException ex)
                {
                    ex.printStackTrace();
                    result=false;
                }
                catch (RuntimeException ex) {
                    ex.printStackTrace();
                    result=false;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean)
        {
            ShipTrxActivity activity=reference.get();
            if (aBoolean)
                activity.dbHelper.deleteShipTrxByDriver(activity.driverCode);
            activity.showProgressDialog(false);
            activity.finish();
        }
    }
}
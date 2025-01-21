package az.inci.bmsanbar.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.GlobalParameters.cameraScanning;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.model.Uom;
import az.inci.bmsanbar.model.v2.InvInfo;

public class EditBarcodesActivity extends ScannerSupportActivity
{
    public static DecimalFormat decimalFormat = new DecimalFormat();
    ListView barcodeListView;
    String invCode;
    String invName;
    String defaultUomCode;
    List<InvBarcode> barcodeList;
    List<Uom> uomList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_barcodes);
        barcodeListView = findViewById(R.id.barcode_list);

        invCode = getIntent().getStringExtra("invCode");
        invName = getIntent().getStringExtra("invName");
        defaultUomCode = getIntent().getStringExtra("defaultUomCode");
        setTitle(invName);

        decimalFormat.setGroupingUsed(false);

        Button scanCam = findViewById(R.id.scan);
        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(1));
        loadData();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        InvBarcode invBarcode = new InvBarcode();
        invBarcode.setBarcode(barcode);
        invBarcode.setUomFactor(1);
        View dialog = LayoutInflater.from(this)
                                    .inflate(R.layout.edit_barcode_dialog,
                                             findViewById(android.R.id.content), false);
        for(InvBarcode existingBarcode : barcodeList)
        {
            if(existingBarcode.getBarcode().equals(barcode))
            {
                invBarcode = existingBarcode;
            }
        }
        if(barcodeList.contains(invBarcode))
        {
            showEditBarcodeDialog(invBarcode, dialog);
        }
        else
        {
            checkBarcode(barcode);
        }
    }

    private void addNewBarcode(String barcode)
    {
        InvBarcode invBarcode = new InvBarcode();
        invBarcode.setInvCode(invCode);
        invBarcode.setBarcode(barcode);
        invBarcode.setUomFactor(1);
        invBarcode.setUom(defaultUomCode);
        View dialog = LayoutInflater.from(this)
                                    .inflate(R.layout.edit_barcode_dialog,
                                             findViewById(android.R.id.content), false);
        showAddBarcodeDialog(invBarcode, dialog);
    }

    public void loadData()
    {
        showProgressDialog(true);
        new Thread(() -> {
            barcodeList = getBarcodeList();
            uomList = getUomList();
            if(barcodeList != null && uomList != null) runOnUiThread(this::updatePage);
        }).start();
    }

    public void updatePage()
    {
        if(barcodeList.size() > 0)
        {
            findViewById(R.id.save).setOnClickListener(v -> updateBarcodes());
        }
        BarcodeAdapter adapter = new BarcodeAdapter(this, barcodeList);
        barcodeListView.setAdapter(adapter);
        barcodeListView.setOnItemClickListener((parent, view, position, id) -> {
            View dialog = LayoutInflater.from(this)
                                        .inflate(R.layout.edit_barcode_dialog, parent, false);
            InvBarcode barcode = barcodeList.get(position);
            showEditBarcodeDialog(barcode, dialog);
        });
    }

    private void showAddBarcodeDialog(InvBarcode barcode, View dialog)
    {
        EditText barcodeStringEdit = dialog.findViewById(R.id.barcode_string);
        barcodeStringEdit.setEnabled(false);
        barcodeStringEdit.setText(barcode.getBarcode());

        EditText uomFactorEdit = dialog.findViewById(R.id.uom_factor);
        uomFactorEdit.setText(decimalFormat.format(barcode.getUomFactor()));

        Spinner uomListSpinner = dialog.findViewById(R.id.uom);
        ArrayAdapter<Uom> adapter = new ArrayAdapter<>(this,
                                                       R.layout.support_simple_spinner_dropdown_item,
                                                       uomList);
        uomListSpinner.setAdapter(adapter);
        uomListSpinner.setSelection(uomList.indexOf(new Uom(barcode.getUom())));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialog).setPositiveButton("OK", (dialog1, which) -> {
            double uomFactor = Double.parseDouble(uomFactorEdit.getText().toString());
            String uomCode = ((Uom) uomListSpinner.getSelectedItem()).getUomCode();
            barcode.setUomFactor(uomFactor);
            barcode.setUom(uomCode);
            barcodeList.add(barcode);
            updatePage();
        });
        dialogBuilder.show();
    }

    private void showEditBarcodeDialog(InvBarcode barcode, View dialog)
    {
        EditText barcodeStringEdit = dialog.findViewById(R.id.barcode_string);
        barcodeStringEdit.setEnabled(false);
        barcodeStringEdit.setText(barcode.getBarcode());

        Spinner uomListSpinner = dialog.findViewById(R.id.uom);
        ArrayAdapter<Uom> adapter = new ArrayAdapter<>(this,
                                                       R.layout.support_simple_spinner_dropdown_item,
                                                       uomList);
        uomListSpinner.setAdapter(adapter);
        uomListSpinner.setSelection(uomList.indexOf(new Uom(barcode.getUom())));

        EditText uomFactorEdit = dialog.findViewById(R.id.uom_factor);
        uomFactorEdit.setText(decimalFormat.format(barcode.getUomFactor()));
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialog).setPositiveButton("OK", (dialog1, which) -> {
            double uomFactor = Double.parseDouble(uomFactorEdit.getText().toString());
            String uomCode = ((Uom) uomListSpinner.getSelectedItem()).getUomCode();
            barcode.setUomFactor(uomFactor);
            barcode.setUom(uomCode);
            updatePage();
        });
        dialogBuilder.show();
    }

    private List<InvBarcode> getBarcodeList()
    {
        String url = url("inv", "barcode-list");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("inv-code", invCode);
        url = addRequestParameters(url, parameters);
        return getListData(url, "GET", null, InvBarcode[].class);
    }

    private List<Uom> getUomList()
    {

        String url = url("uom", "all");
        Map<String, String> parameters = new HashMap<>();
        url = addRequestParameters(url, parameters);
        return getListData(url, "GET", null, Uom[].class);
    }

    private void updateBarcodes()
    {
        showProgressDialog(true);
        String url = url("inv", "update-barcodes");
        executeUpdate(url, barcodeList,
                      message -> showMessageDialog(message.getTitle(), message.getBody(),
                                                   message.getIconId()));
    }

    private void checkBarcode(String barcode)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv", "info-by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            InvInfo invInfo = getSimpleObject(url, "GET", null, InvInfo.class);

            if(invInfo != null)
            {
                runOnUiThread(() -> {
                    if(invInfo.getInvCode() == null)
                    {
                        addNewBarcode(barcode);
                        playSound(SOUND_SUCCESS);
                    }
                    else
                    {
                        showMessageDialog(getString(R.string.error),
                                          getString(R.string.barcode_already_assigned),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    }
                });
            }
        }).start();
    }

    private static class BarcodeAdapter extends ArrayAdapter<InvBarcode>
    {
        Context context;
        List<InvBarcode> barcodeList;

        public BarcodeAdapter(@NonNull Context context, @NonNull List<InvBarcode> objects)
        {
            super(context, 0, objects);
            this.context = context;
            barcodeList = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            InvBarcode barcode = barcodeList.get(position);
            if(convertView == null)
            {
                convertView = LayoutInflater.from(context)
                                            .inflate(R.layout.barcode_item, parent, false);
            }

            ViewHolder holder = new ViewHolder();
            holder.barcodeStringText = convertView.findViewById(R.id.barcode_string);
            holder.uomFactorText = convertView.findViewById(R.id.uom_factor);
            holder.uomText = convertView.findViewById(R.id.uom);
            holder.barcodeStringText.setText(barcode.getBarcode());
            holder.uomFactorText.setText(decimalFormat.format(barcode.getUomFactor()));
            holder.uomText.setText(barcode.getUom());

            return convertView;
        }

        private static class ViewHolder
        {
            TextView barcodeStringText;
            TextView uomFactorText;
            TextView uomText;
        }
    }
}
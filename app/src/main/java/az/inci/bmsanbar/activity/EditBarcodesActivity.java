package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.model.Uom;

public class EditBarcodesActivity extends ScannerSupportActivity
{

    ListView barcodeListView;
    String invCode;
    String invName;
    String defaultUomCode;
    String result;
    List<InvBarcode> barcodeList;
    List<Uom> uomList;
    private static DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_barcodes);
        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        barcodeListView = findViewById(R.id.barcode_list);

        invCode = getIntent().getStringExtra("invCode");
        invName = getIntent().getStringExtra("invName");
        defaultUomCode = getIntent().getStringExtra("defaultUomCode");
        setTitle(invName);

        Button scanBtn = findViewById(R.id.scan);

        if (config().isCameraScanning())
            scanBtn.setVisibility(View.VISIBLE);

        scanBtn.setOnClickListener(v -> {
            Intent barcodeIntent = new Intent(this, BarcodeScannerCamera.class);
            startActivityForResult(barcodeIntent, 1);
        });
        getData();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        InvBarcode invBarcode=new InvBarcode();
        invBarcode.setBarcode(barcode);
        invBarcode.setUomFactor(1);
        View dialog = LayoutInflater.from(this)
                .inflate(R.layout.edit_barcode_dialog, null, false);
        for (InvBarcode existingBarcode: barcodeList)
        {
            if (existingBarcode.getBarcode().equals(barcode))
                invBarcode=existingBarcode;
        }
        if (barcodeList.contains(invBarcode))
            showEditBarcodeDialog(invBarcode, dialog);
        else
            checkBarcode(barcode);
    }

    private void addNewBarcode(String barcode)
    {
        InvBarcode invBarcode=new InvBarcode();
        invBarcode.setInvCode(invCode);
        invBarcode.setBarcode(barcode);
        invBarcode.setUomFactor(1);
        invBarcode.setUom(defaultUomCode);
        View dialog = LayoutInflater.from(this)
                .inflate(R.layout.edit_barcode_dialog, null, false);
        showAddBarcodeDialog(invBarcode, dialog);
    }

    private void getData()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            barcodeList = getBarcodeList();
            uomList = getUomList();
            runOnUiThread(this::loadData);
        }).start();
    }

    private void loadData()
    {
        showProgressDialog(false);
        if (barcodeList.size() > 0)
        {
            findViewById(R.id.save).setOnClickListener(v -> updateBarcodes());
        }
        BarcodeAdapter adapter = new BarcodeAdapter(this, barcodeList);
        barcodeListView.setAdapter(adapter);
        barcodeListView.setOnItemClickListener((parent, view, position, id) ->
        {
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
        ArrayAdapter<Uom> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, uomList);
        uomListSpinner.setAdapter(adapter);
        Log.e("UOM-1", barcode.getUom());
        uomListSpinner.setSelection(uomList.indexOf(new Uom(barcode.getUom())));

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialog)
                .setPositiveButton("OK", (dialog1, which) ->
                {
                    double uomFactor=Double.parseDouble(uomFactorEdit.getText().toString());
                    String uomCode = ((Uom) uomListSpinner.getSelectedItem()).getUomCode();
                    barcode.setUomFactor(uomFactor);
                    barcode.setUom(uomCode);
                    barcodeList.add(barcode);
                    loadData();
                })
                .create();
        alertDialog.show();
    }

    private void showEditBarcodeDialog(InvBarcode barcode, View dialog)
    {
        EditText barcodeStringEdit = dialog.findViewById(R.id.barcode_string);
        barcodeStringEdit.setEnabled(false);
        barcodeStringEdit.setText(barcode.getBarcode());

        Spinner uomListSpinner = dialog.findViewById(R.id.uom);
        ArrayAdapter<Uom> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, uomList);
        uomListSpinner.setAdapter(adapter);
        uomListSpinner.setSelection(uomList.indexOf(new Uom(barcode.getUom())));

        EditText uomFactorEdit = dialog.findViewById(R.id.uom_factor);
        uomFactorEdit.setText(decimalFormat.format(barcode.getUomFactor()));
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialog)
                .setPositiveButton("OK", (dialog1, which) ->
                {
                    double uomFactor=Double.parseDouble(uomFactorEdit.getText().toString());
                    String uomCode = ((Uom) uomListSpinner.getSelectedItem()).getUomCode();
                    barcode.setUomFactor(uomFactor);
                    barcode.setUom(uomCode);
                    loadData();
                })
                .create();
        alertDialog.show();
    }

    private List<InvBarcode> getBarcodeList()
    {
        List<InvBarcode> result;

        String url = url("inv", "barcode-list");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("inv-code", invCode);
        url = addRequestParameters(url, parameters);
        RestTemplate template = new RestTemplate();
        ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                .setConnectTimeout(config().getConnectionTimeout() * 1000);
        template.getMessageConverters().add(new StringHttpMessageConverter());
        try
        {
            String content = template.getForObject(url, String.class);
            Gson gson = new Gson();
            result = new ArrayList<>(gson.fromJson(content, new TypeToken<List<InvBarcode>>()
            {
            }.getType()));
        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
            return new ArrayList<>();
        }
        return result;
    }

    private List<Uom> getUomList()
    {
        List<Uom> result;

        String url = url("uom", "all");
        Map<String, String> parameters = new HashMap<>();
        url = addRequestParameters(url, parameters);
        RestTemplate template = new RestTemplate();
        ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                .setConnectTimeout(config().getConnectionTimeout() * 1000);
        template.getMessageConverters().add(new StringHttpMessageConverter());
        try
        {
            String content = template.getForObject(url, String.class);
            Gson gson = new Gson();
            result = new ArrayList<>(gson.fromJson(content, new TypeToken<List<Uom>>()
            {
            }.getType()));
        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
            return new ArrayList<>();
        }
        return result;
    }

    private void updateBarcodes()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "update-barcodes");
            JSONArray jsonArray = new JSONArray();
            try
            {
                for (InvBarcode barcode : barcodeList)
                {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("invCode", barcode.getInvCode());
                    jsonObject.put("barcode", barcode.getBarcode());
                    jsonObject.put("uom", barcode.getUom());
                    jsonObject.put("uomFactor", barcode.getUomFactor());
                    jsonObject.put("defined", barcode.isDefined());
                    jsonArray.put(jsonObject);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

            HttpEntity<String> entity = new HttpEntity<>(jsonArray.toString(), headers);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            try
            {
                template.postForObject(url, entity, Boolean.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                finish();
            });
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
            if (convertView == null)
            {
                convertView = LayoutInflater.from(context).inflate(R.layout.barcode_item, parent, false);
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


    private void checkBarcode(String barcode)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "info-by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addRequestParameters(url, parameters);

            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                result = template.getForObject(url, String.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (result == null)
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
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != -1 && data != null)
        {
            String barcode = data.getStringExtra("barcode");
            if (barcode != null)
            {
                onScanComplete(barcode);
            }
        }
    }
}
package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Inventory;

public class InventoryInfoActivity extends ScannerSupportActivity
{

    TextView infoText;
    String invCode;
    String invName;
    EditText keywordEdit;
    String keyword;
    String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventar_info);
        infoText = findViewById(R.id.good_info);
        keywordEdit = findViewById(R.id.keyword_edit);

        if (config().isCameraScanning())
        {
            findViewById(R.id.camera_scanner).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (invCode != null)
            getDataByInvCode(invCode);
    }

    @Override
    public void onScanComplete(String barcode)
    {
        getDataByBarcode(barcode);
    }

    public void viewImage(View view)
    {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("invCode", invCode);
        startActivity(intent);
    }

    public void searchKeyword(View view)
    {
        keyword = keywordEdit.getText().toString();

        if (keyword.isEmpty())
        {
            showMessageDialog(getString(R.string.info), getString(R.string.keyword_not_entered), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        }
        else
        {
            searchForKeyword(keyword);
        }
    }

    private void showResultListDialog(List<Inventory> list)
    {
        View view = LayoutInflater.from(this).inflate(R.layout.result_list_dialog,
                findViewById(android.R.id.content), false);

        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, list);
        listView.setAdapter(adapter);
        SearchView searchView = view.findViewById(R.id.search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Axtarışın nəticəsi")
                .setView(view)
                .create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) ->
        {
            Inventory inventory = (Inventory) adapterView.getItemAtPosition(i);
            invCode = inventory.getInvCode();
            getDataByInvCode(invCode);

            dialog.dismiss();
        });
    }

    private void printInfo(String info)
    {
        int firstIndex = info.indexOf(';');
        invCode = info.substring(10, firstIndex);
        invName = info.substring(firstIndex + invCode.length() + 4,
                info.indexOf(';', firstIndex + invCode.length() + 4));
        info = info.replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        infoText.setText(info);
    }

    public void scanWithCamera(View view)
    {
        Intent barcodeIntent = new Intent(this, BarcodeScannerCamera.class);
        startActivityForResult(barcodeIntent, 1);
    }

    public void editAttributes(View view)
    {
        if (invCode != null)
        {
            Intent intent = new Intent(this, EditAttributesActivity.class);
            intent.putExtra("invCode", invCode);
            intent.putExtra("invName", invName);
            startActivity(intent);
        }
    }

    private void getDataByInvCode(String invCode)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "info-by-inv-code");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
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
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.good_not_found),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    printInfo(result);
                    playSound(SOUND_SUCCESS);
                }
            });
        }).start();
    }

    private void getDataByBarcode(String barcode)
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
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.good_not_found),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    printInfo(result);
                    playSound(SOUND_SUCCESS);
                }
            });
        }).start();
    }

    private void searchForKeyword(String keyword)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            List<Inventory> inventoryList = null;
            String url = url("inv", "search");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("keyword", keyword);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Inventory>>()
                {
                }.getType();
                inventoryList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (ResourceAccessException ex)
            {
                ex.printStackTrace();
            }
            List<Inventory> finalInventoryList = inventoryList;
            runOnUiThread(() ->
            {
                showProgressDialog(false);

                showResultListDialog(finalInventoryList);
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
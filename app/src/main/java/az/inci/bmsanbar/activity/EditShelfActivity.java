package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Inventory;
import az.inci.bmsanbar.model.Response;

public class EditShelfActivity extends ScannerSupportActivity {

    EditText shelfBarcodeEdit;
    Button scanBtn;
    ImageButton sendBtn;
    ImageButton clearBtn;
    ListView invListView;

    String shelfBarcode = "";
    Inventory result;
    List<Inventory> inventoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_shelf);
        loadFooter();

        shelfBarcodeEdit = findViewById(R.id.shelf_barcode);
        scanBtn = findViewById(R.id.scan_shelf_barcode);
        sendBtn = findViewById(R.id.send);
        clearBtn = findViewById(R.id.clear);
        invListView = findViewById(R.id.inv_list_view);

        if (config().isCameraScanning())
            scanBtn.setVisibility(View.VISIBLE);

        ActivityResultLauncher<String> barcode = registerForActivityResult(
                new ActivityResultContract<String, String>() {
                    @NonNull
                    @Override
                    public Intent createIntent(@NonNull Context context, String input) {
                        return new Intent(EditShelfActivity.this, BarcodeScannerCamera.class);
                    }

                    @Override
                    public String parseResult(int resultCode, @Nullable Intent intent) {
                        String scanResult = "";
                        if (intent != null) {
                            scanResult = intent.getStringExtra("barcode");
                        }

                        return scanResult;
                    }
                },
                this::onScanComplete);

        scanBtn.setOnClickListener(
                v -> barcode.launch(""));

        sendBtn.setOnClickListener(v -> {
            if (inventoryList.size() > 0)
                uploadData();
        });

        clearBtn.setOnClickListener(v -> clearAndRefreshList());

        invListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Inventory item = (Inventory) parent.getItemAtPosition(position);
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage("Silmək istəyirsinizmi? " + item)
                    .setPositiveButton("Bəli", (dialog, which) -> deleteAndRefreshList(item))
                    .setNegativeButton("Xeyr", null)
                    .create();

            alertDialog.show();
            return true;
        });

        invListView.setOnItemClickListener((parent, view, position, id) -> {
            Inventory item = (Inventory) parent.getItemAtPosition(position);
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage("Barkod: " + item.getBarcode())
                    .setNeutralButton("OK", null)
                    .create();

            alertDialog.show();
        });
    }

    @Override
    public void onScanComplete(String barcode) {
        if (barcode !=null) {
            if (shelfBarcode.isEmpty()) {
                if (!barcode.startsWith("#"))
                {
                    showMessageDialog(getString(R.string.info),
                            "Vitrin barkodu daxil etməlisiniz!",
                            android.R.drawable.ic_dialog_alert);
                    return;
                }
                shelfBarcode = barcode;
                shelfBarcodeEdit.setText(barcode);
            } else
            {
                if (barcode.startsWith("#"))
                {
                    showMessageDialog(getString(R.string.info),
                            "Mal barkodu daxil etməlisiniz!",
                            android.R.drawable.ic_dialog_alert);
                    return;
                }
                getDataByBarcode(barcode);
            }
        }
    }

    private void addAndRefreshList(Inventory inventory)
    {
        if (!inventoryList.contains(inventory))
            inventoryList.add(inventory);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, inventoryList);
        invListView.setAdapter(adapter);
    }

    private void deleteAndRefreshList(Inventory inventory)
    {
        inventoryList.remove(inventory);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, inventoryList);
        invListView.setAdapter(adapter);
    }

    private void clearAndRefreshList()
    {
        shelfBarcodeEdit.setText("");
        inventoryList.clear();
        shelfBarcode = "";
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, inventoryList);
        invListView.setAdapter(adapter);
    }

    private void getDataByBarcode(String barcode)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addRequestParameters(url, parameters);

            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                result = template.getForObject(url, Inventory.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                            "Server xətası: " + ex.getMessage(),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return;
            }
            finally {
                showProgressDialog(false);
            }
            runOnUiThread(() ->
            {
                if (result.getInvCode() == null)
                {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.good_not_found),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    result.setBarcode(barcode);
                    addAndRefreshList(result);
                    playSound(SOUND_SUCCESS);
                }
            });
        }).start();
    }

    private void uploadData()
    {
        showProgressDialog(true);

        new Thread(() -> {
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());

            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);
            headers.setContentType(mediaType);

            String url = url("inv", "update-shelf-barcode");
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("shelf-barcode", shelfBarcode)
                    .queryParam("whs-code", config().getUser().getWhsCode());
            List<String> barcodeList = new ArrayList<>();
            for (Inventory inventory : inventoryList)
                barcodeList.add(inventory.getBarcode());
            HttpEntity<List<String>> httpEntity = new HttpEntity<>(barcodeList, headers);
            try {
                HttpEntity<Response> responseEntity = template.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        httpEntity,
                        Response.class);

                runOnUiThread(() -> {
                    String message = "Əməliyyat tamamlandı.";
                    int icon = android.R.drawable.ic_dialog_info;

                    if (responseEntity.getBody().getStatus() == -1) {
                        message += "\n\nQeyd:" + responseEntity.getBody().getMessage();
                        icon = android.R.drawable.ic_dialog_alert;
                    }

                    showMessageDialog(getString(R.string.info), message, icon);
                });
            }
            catch (RuntimeException e)
            {
                runOnUiThread(() ->
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert));
            }
            finally {
                showProgressDialog(false);
            }
        }).start();
    }
}
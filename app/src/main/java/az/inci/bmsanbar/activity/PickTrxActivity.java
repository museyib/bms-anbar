package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.Objects;

import az.inci.bmsanbar.DBHelper;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.model.Trx;

public class PickTrxActivity extends ScannerSupportActivity implements SearchView.OnQueryTextListener
{

    List<Trx> trxList;
    ListView trxListView;
    ImageButton sendButton;
    ImageButton barcodeButton;
    ImageButton equateAll;
    ImageButton reload;
    ImageButton reset;
    SearchView searchView;
    CheckBox continuousCheck;
    CheckBox readyCheck;

    String trxNo;
    String prevTrxNo;
    String pickGroup;
    String pickArea;
    int focusPosition;
    boolean onFocus;
    boolean pickedAll;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_trx_layout);
        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        sendButton = findViewById(R.id.send);
        barcodeButton = findViewById(R.id.barcode);
        trxListView = findViewById(R.id.trx_list);
        equateAll = findViewById(R.id.equate_all);
        reload = findViewById(R.id.reload);
        reset = findViewById(R.id.reset);
        continuousCheck = findViewById(R.id.continuous_check);
        readyCheck = findViewById(R.id.readyToSend);

        trxListView.setItemsCanFocus(true);
        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(getResources().getColor(R.color.colorZeroQty));

        continuousCheck.setOnCheckedChangeListener((compoundButton, b) -> isContinuous = b);

        readyCheck.setOnCheckedChangeListener((compoundButton, b) ->
                {
                    sendButton.setEnabled(b);
                    sendButton.setBackgroundColor(b ? Color.GREEN : getResources().getColor(R.color.colorZeroQty));
                }
        );

        if (config().isCameraScanning())
            barcodeButton.setVisibility(View.VISIBLE);

        loadFooter();

        Intent intent = getIntent();
        trxNo = intent.getStringExtra("trxNo");
        prevTrxNo = intent.getStringExtra("prevTrxNo");
        pickGroup = intent.getStringExtra("pickGroup");
        pickArea = intent.getStringExtra("pickArea");
        setTitle(prevTrxNo + " - " + pickArea);

        trxListView.setOnItemClickListener((parent, view, position, id) ->
        {
            onFocus = true;
            Trx trx = (Trx) view.getTag();
            showEditPickedQtyDialog(trx);
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            Trx trx = (Trx) view.getTag();
            showInfoDialog(trx);
            return true;
        });

        sendButton.setOnClickListener(v ->
        {
            if (trxList.size() > 0)
            {
                if (pickedAll)
                    sendTrx();
                else
                {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Mallar tam yığılmayıb. Göndərmək istəyirsiniz?")
                            .setNegativeButton("Bəli", (dialogInterface, i) -> sendTrx())
                            .setPositiveButton("Xeyr", null)
                            .create();

                    dialog.show();
                    playSound(SOUND_FAIL);
                }
            }
        });

        barcodeButton.setOnClickListener(v ->
        {
            Intent barcodeIntent = new Intent(PickTrxActivity.this, BarcodeScannerCamera.class);
            barcodeIntent.putExtra("serialScan", isContinuous);
            barcodeIntent.putExtra("trxNo", trxNo);
            barcodeIntent.putExtra("trxType", "pick");
            startActivityForResult(barcodeIntent, 1);
        });

        equateAll.setOnClickListener(v ->
        {
            playSound(SOUND_FAIL);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Sayları eyniləşdirmək istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) ->
                    {
                        for (Trx trx : trxList)
                        {
                            trx.setPickedQty(trx.getQty());
                            dbHelper.updatePickTrx(trx);
                        }

                        loadTrx();
                    })
                    .setPositiveButton("Xeyr", null)
                    .create();
            dialog.show();
        });

        reload.setOnClickListener(view ->
        {
            playSound(SOUND_FAIL);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Sayları sıfırlamaq istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) ->
                    {
                        for (Trx trx : trxList)
                        {
                            trx.setPickedQty(0);
                            dbHelper.updatePickTrx(trx);
                        }

                        loadTrx();
                    })
                    .setPositiveButton("Xeyr", null)
                    .create();
            dialog.show();
        });

        reset.setOnClickListener(v -> {

            playSound(SOUND_FAIL);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Sənədi geri göndərmək istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) ->
                    {
                        boolean modified = false;
                        for (Trx trx : trxList)
                        {
                            if (trx.getPickedQty()>0)
                            {
                                modified = true;
                                break;
                            }
                        }
                        if (modified)
                            showMessageDialog(getString(R.string.info),
                                    "Sənəddə dəyişiklik edilib, geri göndərilə bilməz!",
                                    android.R.drawable.ic_dialog_info);
                        else
                            reset();
                    })
                    .setPositiveButton("Xeyr", null)
                    .create();
            dialog.show();
        });

        loadTrx();
    }

    private void showInfoDialog(Trx trx)
    {
        String info = trx.getNotes().replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        info += "\n\nÖlçü vahidi: " + trx.getUom();
        info += "\n\nBrend: " + trx.getInvBrand();
        info += "\n\nBarkodlar:" + dbHelper.barcodeList(trx.getInvCode(), DBHelper.PICK_TRX);
        AlertDialog.Builder builder = new AlertDialog.Builder(PickTrxActivity.this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) ->
        {
            Intent photoIntent = new Intent(PickTrxActivity.this, PhotoActivity.class);
            photoIntent.putExtra("invCode", trx.getInvCode());
            photoIntent.putExtra("notes", trx.getNotes());
            startActivity(photoIntent);
        });
        builder.setNeutralButton("Say", (dialog, which) ->
        {
            String url = url("inv", "qty");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("whs-code", trx.getWhsCode());
            parameters.put("inv-code", trx.getInvCode());
            url = addRequestParameters(url, parameters);
            new ShowQuantity(PickTrxActivity.this).execute(url);
        });
        builder.show();
    }

    private void checkInvBarcode(InvBarcode invBarcode)
    {
        Trx trx = dbHelper.getPickTrxByInvCode(invBarcode.getInvCode(), trxNo);
        if (trx == null)
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        }
        else
        {
            trx.setUomFactor(invBarcode.getUomFactor());
            goToScannedItem(trx);
        }
    }

    private void goToScannedItem(Trx trx)
    {

        onFocus = true;
        focusPosition = trxList.indexOf(trx);
        if (trx.getPickedQty() >= trx.getQty())
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.already_picked),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        }
        else
        {
            if (!isContinuous)
            {
                showEditPickedQtyDialog(trx);
            }
            else
            {
                double qty=trx.getPickedQty() + trx.getUomFactor();
                if (qty>trx.getQty())
                    qty=trx.getQty();
                trx.setPickedQty(qty);
                dbHelper.updatePickTrx(trx);
            }
            playSound(SOUND_SUCCESS);
        }
    }

    @Override
    public void onScanComplete(String barcode)
    {
        Trx trx = dbHelper.getPickTrxByBarcode(barcode, trxNo);
        if (trx != null)
        {
            goToScannedItem(trx);
        }
        else
        {
            getInvBarcodeFromServer(barcode, this::checkInvBarcode);
        }

        loadTrx();
    }

    public void loadTrx()
    {
        pickedAll = true;
        trxList = dbHelper.getPickTrx(trxNo);
        TrxAdapter trxAdapter = new TrxAdapter(this, R.layout.pick_trx_item_layout, trxList);
        trxListView.setAdapter(trxAdapter);
        trxListView.setSelection(focusPosition);
        trxListView.requestFocus();
    }

    public void showEditPickedQtyDialog(Trx trx)
    {
        focusPosition = trxList.indexOf(trx);
        View view = getLayoutInflater()
                .inflate(R.layout.edit_picked_qty_dialog_layout,
                        findViewById(android.R.id.content), false);


        TextView invCodeView = view.findViewById(R.id.inv_code);
        TextView invNameView = view.findViewById(R.id.inv_name);
        EditText qtyEdit = view.findViewById(R.id.qty);
        EditText pickedQtyEdit = view.findViewById(R.id.picked_qty);

        invCodeView.setText(trx.getInvCode());
        invNameView.setText(trx.getInvName());

        qtyEdit.setText(decimalFormat.format(trx.getQty()));
        qtyEdit.setEnabled(false);

        pickedQtyEdit.setText(decimalFormat.format(trx.getPickedQty()));
        pickedQtyEdit.selectAll();

        invNameView.setOnClickListener(view1 -> showInfoDialog(trx));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog1, which) ->
                {
                    double pickedQty;
                    if (!pickedQtyEdit.getText().toString().isEmpty())
                        pickedQty = Double.parseDouble(pickedQtyEdit.getText().toString());
                    else
                        pickedQty = -1;

                    if (pickedQty < 0 || pickedQty > trx.getQty())
                    {
                        Toast.makeText(this, R.string.quantity_not_correct, Toast.LENGTH_LONG).show();
                        showEditPickedQtyDialog(trx);
                    }
                    else
                    {
                        trx.setPickedQty(pickedQty);
                        dbHelper.updatePickTrx(trx);
                        loadTrx();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                .setNeutralButton(R.string.equate, (dialog1, which) ->
                {
                    trx.setPickedQty(trx.getQty());
                    dbHelper.updatePickTrx(trx);
                    loadTrx();
                })
                .create();

        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setActivated(true);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        TrxAdapter adapter = (TrxAdapter) trxListView.getAdapter();
        if (adapter != null)
            adapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (!searchView.isIconified())
            searchView.setIconified(true);
        else
            super.onBackPressed();
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

    @Override
    protected void onResume()
    {
        super.onResume();
        loadTrx();
    }

    static class TrxAdapter extends ArrayAdapter<Trx> implements Filterable
    {
        PickTrxActivity activity;
        List<Trx> list;

        TrxAdapter(@NonNull Context context, int resourceId, @NonNull List<Trx> objects)
        {
            super(context, resourceId, objects);
            list = objects;
            activity = (PickTrxActivity) context;
        }

        @Override
        public int getCount()
        {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            Trx trx = list.get(position);
            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.pick_trx_item_layout, parent, false);
            }

            if (position == activity.focusPosition && activity.onFocus)
            {
                convertView.setBackgroundColor(Color.LTGRAY);
            }
            else
            {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            if (trx.getPickedQty() == 0)
                convertView.setBackgroundColor(activity.getResources().getColor(R.color.colorZeroQty));
            else if (trx.getPickedQty() < trx.getQty())
                convertView.setBackgroundColor(Color.YELLOW);

            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView invBrand = convertView.findViewById(R.id.inv_brand);
            TextView qty = convertView.findViewById(R.id.qty);
            TextView pickedQty = convertView.findViewById(R.id.picked);

            invCode.setText(trx.getInvCode());
            invName.setText(trx.getInvName());
            invBrand.setText(trx.getInvBrand());
            qty.setText(activity.decimalFormat.format(trx.getQty()));
            pickedQty.setText(activity.decimalFormat.format(trx.getPickedQty()));
            convertView.setTag(trx);

            if (trx.getPickedQty() < trx.getQty())
                activity.pickedAll = false;

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter()
        {
            return new Filter()
            {
                @Override
                protected FilterResults performFiltering(CharSequence constraint)
                {
                    FilterResults results = new FilterResults();
                    List<Trx> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Trx trx : activity.trxList)
                    {
                        if (trx.getInvCode().concat(trx.getInvName()).concat(trx.getBarcode())
                                .toLowerCase().contains(constraint))
                        {
                            filteredArrayData.add(trx);
                        }
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results)
                {
                    list = (List<Trx>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private void sendTrx()
    {
        showProgressDialog(true);
        new Thread(() -> {

            JSONArray jsonArray = new JSONArray();
            try
            {
                for (Trx trx : trxList)
                {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("trxId", trx.getTrxId());
                    jsonObject.put("qty", trx.getPickedQty());
                    jsonObject.put("pickStatus", "A");
                    jsonArray.put(jsonObject);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);
            headers.setContentType(mediaType);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
            HttpEntity<String> entity = new HttpEntity<>(jsonArray.toString(), headers);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
            boolean result;
            String url = url("trx", "collect");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", null);
            url = addRequestParameters(url, parameters);
            try
            {
                result = template.postForObject(url, entity, Boolean.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                result=false;
            }
            boolean finalResult = result;
            runOnUiThread(() -> {
                if (!finalResult)
                {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    dbHelper.deletePickTrx(trxNo);
                    finish();
                }
                showProgressDialog(false);
            });
        }).start();
    }

    private void reset()
    {
        new Thread(()->{
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
            boolean result;
            String url = url("doc", "pick", "reset");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            try
            {
                result = template.postForObject(url, null, Boolean.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return;
            }
            boolean finalResult = result;
            runOnUiThread(() -> {
                if (!finalResult)
                {
                    showMessageDialog(getString(R.string.warning),
                            "Yığım sənədini geri qaytarmaq səlahiyyətiniz yoxdur!",
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    dbHelper.deletePickTrx(trxNo);
                    finish();
                }
                showProgressDialog(false);
            });
        }).start();
    }
}

package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

public class PackTrxActivity extends ScannerSupportActivity
        implements SearchView.OnQueryTextListener
{

    List<Trx> trxList;
    ListView trxListView;
    ImageButton sendButton;
    ImageButton barcodeButton;
    ImageButton equateAll;
    ImageButton reload;
    SearchView searchView;
    CheckBox continuousCheck;
    CheckBox readyCheck;

    String trxNo;
    String orderTrxNo;
    String bpName;
    String notes;
    private boolean onFocus;
    private int focusPosition;
    private boolean packedAll;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pack_trx_layout);
        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        sendButton = findViewById(R.id.send);
        barcodeButton = findViewById(R.id.barcode);
        trxListView = findViewById(R.id.trx_list);
        equateAll = findViewById(R.id.equate_all);
        reload = findViewById(R.id.reload);
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
        orderTrxNo = intent.getStringExtra("orderTrxNo");
        bpName = intent.getStringExtra("bpName");
        notes = intent.getStringExtra("notes");
        setTitle(orderTrxNo + ": " + bpName);

        trxListView.setOnItemClickListener((parent, view, position, id) ->
        {
            onFocus = true;
            Trx trx = (Trx) view.getTag();
            showEditPackedQtyDialog(trx);
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            Trx trx = (Trx) view.getTag();
            showInfoDialog(trx);
            return true;
        });

        sendButton.setOnClickListener(v ->
        {
            if (trxList.size() > 0 && packedAll)
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
        });

        barcodeButton.setOnClickListener(v ->
        {
            Intent barcodeIntent = new Intent(PackTrxActivity.this, BarcodeScannerCamera.class);
            barcodeIntent.putExtra("serialScan", isContinuous);
            barcodeIntent.putExtra("trxNo", trxNo);
            barcodeIntent.putExtra("trxType", "pack");
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
                            trx.setPackedQty(trx.getPickedQty());
                            dbHelper.updatePackTrx(trx);
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
                            trx.setPackedQty(0);
                            dbHelper.updatePackTrx(trx);
                        }

                        loadTrx();
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
        info += "\n\nYığan: " + trx.getPickUser();
        info += "\nKöməkçi yığan: " + notes;
        info += "\n\nBarkodlar: " + dbHelper.barcodeList(trx.getInvCode(), DBHelper.PACK_TRX);
        AlertDialog.Builder builder = new AlertDialog.Builder(PackTrxActivity.this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) ->
        {
            Intent photoIntent = new Intent(PackTrxActivity.this, PhotoActivity.class);
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
            new ShowQuantity(PackTrxActivity.this).execute(url);
        });
        builder.show();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        Trx trx = dbHelper.getPackTrxByBarcode(barcode, trxNo);
        if (trx != null)
        {
            onFocus = true;
            focusPosition = trxList.indexOf(trx);
            if (trx.getPackedQty() >= trx.getQty())
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.already_packed),
                        android.R.drawable.ic_dialog_info);
                playSound(SOUND_FAIL);
            }
            else
            {
                if (!isContinuous)
                {
                    showEditPackedQtyDialog(trx);
                }
                else
                {
                    trx.setPackedQty(trx.getPackedQty() + 1);
                    dbHelper.updatePackTrx(trx);
                }
                playSound(SOUND_SUCCESS);
            }
        }
        else
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        }

        loadTrx();
    }

    public void loadTrx()
    {
        packedAll = true;
        trxList = dbHelper.getPackTrxByApproveUser(trxNo);
        TrxAdapter trxAdapter = new TrxAdapter(this, R.layout.pack_trx_item_layout, trxList);
        trxListView.setAdapter(trxAdapter);
        trxListView.setSelection(focusPosition);
        trxListView.requestFocus();
    }

    public void showEditPackedQtyDialog(Trx trx)
    {
        focusPosition = trxList.indexOf(trx);
        View view = getLayoutInflater()
                .inflate(R.layout.edit_packed_qty_dialog_layout,
                        findViewById(android.R.id.content), false);


        TextView invCodeView = view.findViewById(R.id.inv_code);
        TextView invNameView = view.findViewById(R.id.inv_name);
        EditText qtyEdit = view.findViewById(R.id.qty);
        EditText pickedQtyEdit = view.findViewById(R.id.picked_qty);
        EditText packedQtyEdit = view.findViewById(R.id.packed_qty);

        invCodeView.setText(trx.getInvCode());
        invNameView.setText(trx.getInvName());

        qtyEdit.setText(decimalFormat.format(trx.getQty()));
        qtyEdit.setEnabled(false);

        pickedQtyEdit.setText(decimalFormat.format(trx.getPickedQty()));
        pickedQtyEdit.setEnabled(false);

        packedQtyEdit.setText(decimalFormat.format(trx.getPackedQty()));
        packedQtyEdit.selectAll();

        invNameView.setOnClickListener(view1 -> showInfoDialog(trx));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog1, which) ->
                {
                    double packedQty;
                    if (!packedQtyEdit.getText().toString().isEmpty())
                        packedQty = Double.parseDouble(packedQtyEdit.getText().toString());
                    else
                        packedQty = -1;

                    if (packedQty < 0 || packedQty > trx.getQty())
                    {
                        showToastMessage(getString(R.string.quantity_not_correct));
                        showEditPackedQtyDialog(trx);
                    }
                    else
                    {
                        trx.setPackedQty(packedQty);
                        dbHelper.updatePackTrx(trx);
                        loadTrx();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                .setNeutralButton(R.string.equate, (dialog1, which) ->
                {
                    trx.setPackedQty(trx.getPickedQty());
                    dbHelper.updatePackTrx(trx);
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
        return false;
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
        PackTrxActivity activity;
        List<Trx> list;

        TrxAdapter(@NonNull Context context, int resourceId, @NonNull List<Trx> objects)
        {
            super(context, resourceId, objects);
            list = objects;
            activity = (PackTrxActivity) context;
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
                        .inflate(R.layout.pack_trx_item_layout, parent, false);
            }

            if (position == activity.focusPosition && activity.onFocus)
            {
                convertView.setBackgroundColor(Color.LTGRAY);
            }
            else
            {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }

            if (trx.getPackedQty() == 0)
                convertView.setBackgroundColor(activity.getResources().getColor(R.color.colorZeroQty));
            else if (trx.getPackedQty() < trx.getQty())
                convertView.setBackgroundColor(Color.YELLOW);

            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView invBrandView = convertView.findViewById(R.id.inv_brand);
            TextView qty = convertView.findViewById(R.id.qty);
            TextView pickedQty = convertView.findViewById(R.id.picked);
            TextView packedQty = convertView.findViewById(R.id.packed);

            invCode.setText(trx.getInvCode());
            invName.setText(trx.getInvName());
            invBrandView.setText(trx.getInvBrand());
            qty.setText(activity.decimalFormat.format(trx.getQty()));
            pickedQty.setText(activity.decimalFormat.format(trx.getPickedQty()));
            packedQty.setText(activity.decimalFormat.format(trx.getPackedQty()));
            convertView.setTag(trx);

            if (trx.getPackedQty() < trx.getQty())
                activity.packedAll = false;

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
                    jsonObject.put("qty", trx.getPackedQty());
                    jsonObject.put("pickStatus", "null");
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
            parameters.put("trx-no", trxNo);
            Log.e("TRXNO", trxNo+" test");
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
                    dbHelper.deletePackTrx(trxNo);
                    finish();
                }
                showProgressDialog(false);
            });
        }).start();
    }
}
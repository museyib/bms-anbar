package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.DBHelper;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Inventory;
import az.inci.bmsanbar.model.Response;
import az.inci.bmsanbar.model.Trx;
import az.inci.bmsanbar.model.Whs;

public class InternalUseTrxActivity extends ScannerSupportActivity
{
    ImageButton selectInvBtn;
    ImageButton uploadBtn;
    ImageButton printBtn;
    ImageButton cameraScanner;
    RecyclerView trxListView;
    Spinner whsListSpinner;
    SearchView searchView;
    TextView srcTxt;

    List<Trx> trxList;
    List<Whs> whsList;
    List<Inventory> invList;

    int mode;
    boolean docCreated;
    int trxTypeId = 73;
    double amount;
    String trxNo;
    String whsCode;
    String whsName;
    Whs whs;
    private String notes;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.internal_use_trx_layout);
        selectInvBtn = findViewById(R.id.inv_list);
        uploadBtn = findViewById(R.id.send);
        printBtn = findViewById(R.id.print);
        cameraScanner = findViewById(R.id.camera_scanner);
        trxListView = findViewById(R.id.trx_list_view);
        whsListSpinner = findViewById(R.id.trg_whs_list);
        srcTxt = findViewById(R.id.src_text);

        mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);
        if (mode == AppConfig.VIEW_MODE)
        {
            docCreated = true;
            trxNo = getIntent().getStringExtra("trxNo");
            notes = getIntent().getStringExtra("notes");
            trxTypeId = getIntent().getIntExtra("trxTypeId", 73);
            whsCode = getIntent().getStringExtra("whsCode");
            whsName = getIntent().getStringExtra("whsName");
            amount = getIntent().getDoubleExtra("amount", 0);
            whsListSpinner.setVisibility(View.INVISIBLE);

            loadWhsSumList();
        }
        else
        {
            trxNo = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(new Date());
            whsCode = "05";
            whsName = "İstehsalat anbarı";

            loadWhsList();
        }

        whsListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                whs = (Whs) parent.getItemAtPosition(position);
                whsCode = whs.getWhsCode();
                whsName = whs.getWhsName();

                if (docCreated)
                    updateDocSrc();
                else
                    createNewDoc();

                srcTxt.setText(whs.toString());
                loadWhsSumList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        selectInvBtn.setOnClickListener(v ->
                showInvList());

        uploadBtn.setOnClickListener(v ->
        {
            AlertDialog dialog = new AlertDialog.Builder(InternalUseTrxActivity.this)
                    .setMessage("Göndərmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog1, which) ->
                            uploadDoc())
                    .setNegativeButton("Xeyr", null)
                    .create();
            dialog.show();
        });

        if (config().isCameraScanning())
            cameraScanner.setVisibility(View.VISIBLE);

        cameraScanner.setOnClickListener(v ->
        {
            Intent barcodeIntent = new Intent(this, BarcodeScannerCamera.class);
            startActivityForResult(barcodeIntent, 1);
        });

        loadData();

        loadFooter();
    }

    @Override
    public void onBackPressed()
    {
        if (!searchView.isIconified())
            searchView.setIconified(true);
        else
        {
            if (trxList.size() == 0)
            {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage("Mal daxil edilməyib. Sənəd silinsin?")
                        .setPositiveButton("Bəli", (dialog1, which) ->
                        {
                            dbHelper.deleteInternalUseDoc(trxNo);
                            finish();
                        })
                        .setNegativeButton("Xeyr", (dialog12, which) -> finish())
                        .create();
                dialog.show();
            }
            else
                finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);

        menu.findItem(R.id.inv_attributes).setVisible(false);
        menu.findItem(R.id.pick_report).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
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
                ((TrxAdapter) trxListView.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });
        return true;
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

    private void createNewDoc()
    {
        Doc doc = new Doc();
        doc.setTrxNo(trxNo);
        doc.setTrxTypeId(trxTypeId);
        doc.setTrxDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        doc.setWhsCode(whs.getWhsCode());

        doc.setWhsCode(whsCode);
        doc.setWhsName(whsName);
        doc.setAmount(amount);
        dbHelper.addInternalUseDoc(doc);
        docCreated = true;
        setTitle(trxNo);
    }

    private void updateDocAmount()
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.AMOUNT, amount);
        dbHelper.updateInternalUseDoc(trxNo, values);
    }

    private void updateDocSrc()
    {
        ContentValues values = new ContentValues();
        whsCode = whs.getWhsCode();
        whsName = whs.getWhsName();
        values.put(DBHelper.WHS_CODE, whs.getWhsCode());
        values.put(DBHelper.WHS_NAME, whs.getWhsName());
        dbHelper.updateInternalUseDoc(trxNo, values);
    }

    private void loadData()
    {
        whs = new Whs();
        whs.setWhsCode(whsCode);
        whs.setWhsName(whsName);
        setTitle(trxNo);
        srcTxt.setText(whs.toString());
        trxList = dbHelper.getInternalUseTrxList(trxNo);
        TrxAdapter adapter = new TrxAdapter(this, trxList);
        trxListView.setLayoutManager(new LinearLayoutManager(this));
        trxListView.setAdapter(adapter);

        if (trxList.size() == 0)
            findViewById(R.id.trx_list_scroll).setVisibility(View.GONE);
        else
            findViewById(R.id.trx_list_scroll).setVisibility(View.VISIBLE);
    }

    private void loadWhsList()
    {
        new Thread(() ->
        {
            String url = url("src", "whs", "target");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                String result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Whs>>()
                {
                }.getType();
                whsList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                whsList = new ArrayList<>();
                whsList.add(whs);
            }
            finally
            {
                runOnUiThread(this::publishWhsList);
            }
        }).start();
    }

    private void publishWhsList()
    {
        ArrayAdapter<Whs> adapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, whsList);
        whsListSpinner.setAdapter(adapter);
        whsListSpinner.setSelection(whsList.indexOf(whs));
    }

    @Override
    public void onScanComplete(String barcode)
    {
        getInvFromServer(barcode);
    }

    private void getInvFromServer(String barcode)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            Inventory currentInv = null;
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
                currentInv = template.postForObject(url, null, Inventory.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }

            Inventory finalCurrentInv = currentInv;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalCurrentInv == null)
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
                else
                    showAddInvDialog(finalCurrentInv);
            });
        }).start();
    }

    private void loadWhsSumList()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "whs-sum");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", config().getUser().getId());
            parameters.put("whs-code", whsCode);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                String result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Inventory>>()
                {
                }.getType();
                invList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }

            List<Inventory> finalInventoryList = invList;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalInventoryList == null)
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
            });
        }).start();
    }

    private void showInvList()
    {
        showProgressDialog(false);
        View view = LayoutInflater.from(this).inflate(R.layout.inv_list_dialog,
                findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        InventoryAdapter adapter = new InventoryAdapter(this, invList);
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
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view1, position, id) ->
                showAddInvDialog((Inventory) view1.getTag()));
        listView.setOnItemLongClickListener((parent, view1, position, id) ->
        {
            showInfoDialog((Inventory) view1.getTag());
            return true;
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Mallar")
                .setView(view)
                .create();
        dialog.show();
    }

    private void showAddInvDialog(Inventory inventory)
    {
        if (inventory.getInvCode() == null)
        {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
        }
        else
        {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.selectAll();
            qtyEdit.requestFocus();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(inventory.getInvCode())
                    .setMessage(inventory.getInvName() + "\nMaksimum limit: " + inventory.getWhsQty())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) ->
                    {
                        try
                        {
                            double qty = Double.parseDouble(qtyEdit.getText().toString());
                            Trx containingTrx = containingTrx(inventory.getInvCode());
                            if (qty + containingTrx.getQty() > inventory.getWhsQty())
                            {
                                showMessageDialog("Miqdar aşması",
                                        "Anbar qalığı kifayət qədər deyil.\nMaksimum limit: "
                                                + inventory.getWhsQty(),
                                        android.R.drawable.ic_dialog_info);
                                return;
                            }
                            if (containingTrx.getTrxId() > 0)
                                updateInternalUseTrxQty(containingTrx,
                                        qty + containingTrx.getQty());
                            else
                            {
                                Trx trx = Trx.parseFromInv(inventory);
                                trx.setQty(qty);
                                trx.setTrxNo(trxNo);
                                trx.setAmount(trx.getPrice() * qty);
                                addInternalUseTrx(trx);
                            }
                            loadData();
                        }
                        catch (NumberFormatException e)
                        {
                            showAddInvDialog(inventory);
                        }
                    })
                    .setNegativeButton("Ləğv et", null)
                    .create();

            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void showEditInvDialog(Trx trx)
    {
        if (trx == null)
        {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
        }
        else
        {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.setText(String.format(Locale.getDefault(), "%.0f", trx.getQty()));
            qtyEdit.selectAll();
            qtyEdit.requestFocus();
            Inventory inventory = invList.get(invList.indexOf(Inventory.parseFromTrx(trx)));
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(trx.getInvCode())
                    .setMessage(trx.getInvName() + "\nMaksimum limit: " + inventory.getWhsQty())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) ->
                    {
                        try
                        {
                            double qty = Double.parseDouble(qtyEdit.getText().toString());
                            if (qty > inventory.getWhsQty())
                            {
                                showMessageDialog("Miqdar aşması",
                                        "Anbar qalığı kifayət qədər deyil.\nMaksimum limit: "
                                                + inventory.getWhsQty(),
                                        android.R.drawable.ic_dialog_info);
                                return;
                            }
                            updateInternalUseTrxQty(trx, qty);
                            loadData();
                        }
                        catch (NumberFormatException e)
                        {
                            showEditInvDialog(trx);
                        }
                    })
                    .setNegativeButton("Ləğv et", null)
                    .create();

            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void addInternalUseTrx(Trx trx)
    {
        dbHelper.addInternalUseTrx(trx);
        amount += trx.getAmount();
        if (docCreated)
        {
            updateDocAmount();
        }
        else
        {
            createNewDoc();
        }
        loadData();
    }

    private void updateInternalUseTrxQty(Trx trx, double qty)
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.QTY, qty);
        dbHelper.updateInternalUseTrx(String.valueOf(trx.getTrxId()), values);
        amount += (qty - trx.getQty()) * trx.getPrice();
        updateDocAmount();
    }

    private Trx containingTrx(String invCode)
    {
        for (Trx trx : trxList)
        {
            if (trx.getInvCode().equals(invCode))
                return trx;
        }

        return new Trx();
    }

    private void uploadDoc()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("trx", "create-internal-use");

            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = new MediaType("application", "json",
                    StandardCharsets.UTF_8);
            headers.setContentType(mediaType);

            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("user-id", config().getUser().getId())
                    .queryParam("whs-code", whsCode)
                    .queryParam("notes", notes);
            HttpEntity<List<Trx>> httpEntity = new HttpEntity<>(trxList, headers);
            try
            {
                HttpEntity<Response> responseEntity = template.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        httpEntity,
                        Response.class);

                runOnUiThread(() -> {
                    if (responseEntity.getBody().getStatus() == 0) {
                        dbHelper.deleteInternalUseDoc(trxNo);
                        finish();
                    }
                    else {
                        String message = responseEntity.getBody().getMessage();
                        int icon = android.R.drawable.ic_dialog_alert;

                        showMessageDialog(getString(R.string.error), message, icon);
                    }
                });
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            finally {
                runOnUiThread(() ->
                        showProgressDialog(false));
            }
        }).start();
    }

    private void showInfoDialog(Inventory inventory)
    {
        String info = inventory.getInvName();
        info += "\n\nBrend: " + inventory.getInvBrand();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) ->
        {
            Intent photoIntent = new Intent(this, PhotoActivity.class);
            photoIntent.putExtra("invCode", inventory.getInvCode());
            startActivity(photoIntent);
        });
        builder.show();
    }

    @SuppressWarnings("unchecked")
    private static class InventoryAdapter extends ArrayAdapter<Inventory> implements Filterable
    {
        List<Inventory> list;
        InternalUseTrxActivity activity;

        public InventoryAdapter(@NonNull Context context, @NonNull List<Inventory> objects)
        {
            super(context, 0, 0, objects);
            list = objects;
            activity = (InternalUseTrxActivity) context;
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
            Inventory inventory = list.get(position);
            if (convertView == null)
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.inv_list_item, parent, false);
            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView barcode = convertView.findViewById(R.id.barcode);
            TextView invBrand = convertView.findViewById(R.id.inv_brand);

            invCode.setText(inventory.getInvCode());
            invName.setText(inventory.getInvName());
            barcode.setText(inventory.getBarcode());
            invBrand.setText(inventory.getInvBrand());
            convertView.setTag(inventory);
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
                    List<Inventory> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Inventory inventory : activity.invList)
                    {
                        if (inventory.getBarcode().concat(inventory.getInvCode()).concat(inventory.getInvName())
                                .concat(inventory.getInvBrand()).toLowerCase().contains(constraint))
                        {
                            filteredArrayData.add(inventory);
                        }
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results)
                {
                    list = (List<Inventory>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private class TrxAdapter extends RecyclerView.Adapter<TrxAdapter.Holder> implements Filterable
    {
        private final InternalUseTrxActivity activity;
        List<Trx> trxList;
        View itemView;

        public TrxAdapter(Context context, List<Trx> trxList)
        {
            this.trxList = trxList;
            activity = (InternalUseTrxActivity) context;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.approve_trx_item_layout, parent, false);
            return new Holder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position)
        {
            Trx trx = trxList.get(position);
            itemView.setOnLongClickListener(view ->
            {
                Trx selectedTrx = trxList.get(position);
                if (!selectedTrx.isReturned())
                {
                    AlertDialog dialog = new AlertDialog.Builder(InternalUseTrxActivity.this)
                            .setTitle(selectedTrx.getInvName())
                            .setMessage("Silmək istəyirsiniz?")
                            .setPositiveButton("Bəli", (dialog1, which) ->
                            {
                                dbHelper.deleteInternalUseTrx(String.valueOf(selectedTrx.getTrxId()));
                                amount -= selectedTrx.getQty() * selectedTrx.getPrice();
                                updateDocAmount();
                                loadData();
                            })
                            .setNegativeButton("Xeyr", null)
                            .create();
                    dialog.show();
                }
                return true;
            });
            itemView.setOnClickListener(view ->
            {
                Trx selectedTrx = trxList.get(position);
                if (!selectedTrx.isReturned())
                    showEditInvDialog(selectedTrx);
            });
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.invBrand.setText(String.valueOf(trx.getInvBrand()));
            holder.notes.setVisibility(View.GONE);
            itemView.setTag(trx);
        }

        @Override
        public int getItemCount()
        {
            return trxList.size();
        }

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
                        if (trx.getInvCode().concat(trx.getInvName())
                                .concat(trx.getInvBrand()).toLowerCase().contains(constraint))
                        {
                            filteredArrayData.add(trx);
                        }
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results)
                {
                    trxList = (List<Trx>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        private class Holder extends RecyclerView.ViewHolder
        {
            TextView invCode;
            TextView invName;
            TextView qty;
            TextView invBrand;
            TextView notes;

            public Holder(@NonNull View itemView)
            {
                super(itemView);

                invCode = itemView.findViewById(R.id.inv_code);
                invName = itemView.findViewById(R.id.inv_name);
                qty = itemView.findViewById(R.id.qty);
                invBrand = itemView.findViewById(R.id.inv_brand);
                notes = itemView.findViewById(R.id.notes);
            }
        }
    }
}
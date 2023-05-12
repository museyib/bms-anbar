package az.inci.bmsanbar.activity;

import static az.inci.bmsanbar.GlobalParameters.cameraScanning;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import az.inci.bmsanbar.model.Trx;
import az.inci.bmsanbar.model.v2.ProductApproveRequest;
import az.inci.bmsanbar.model.v2.ProductApproveRequestItem;

public class ProductApproveTrxActivity extends ScannerSupportActivity
{

    ImageButton selectInvBtn;
    ImageButton uploadBtn;
    ImageButton printBtn;
    ImageButton cameraScanner;
    RecyclerView trxListView;
    SearchView searchView;
    EditText notesEdit;

    List<Trx> trxList;
    List<Inventory> invList;

    int mode;
    boolean docCreated;
    String trxNo;
    private String notes = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_approve_trx_layout);
        selectInvBtn = findViewById(R.id.inv_list);
        uploadBtn = findViewById(R.id.send);
        printBtn = findViewById(R.id.print);
        cameraScanner = findViewById(R.id.camera_scanner);
        trxListView = findViewById(R.id.trx_list_view);
        notesEdit = findViewById(R.id.doc_description);

        mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);
        if (mode == AppConfig.VIEW_MODE)
        {
            docCreated = true;
            trxNo = getIntent().getStringExtra("trxNo");
            notes = getIntent().getStringExtra("notes");
            notesEdit.setText(notes);
        }
        else
            trxNo = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(new Date());

        printBtn.setOnClickListener(v -> {
            if (trxList.size() > 0)
            {
                String report = getPrintForm();
                showProgressDialog(true);
                print(report);
            }
        });

        selectInvBtn.setOnClickListener(v -> {
            if (invList == null) getInvList();
            else showInvList();
        });

        uploadBtn.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    ProductApproveTrxActivity.this);
            dialogBuilder.setMessage("Göndərmək istəyirsiniz?")
                         .setPositiveButton("Bəli", (dialog1, which) -> {
                             int status = config().getUser().isApprovePrdFlag() ? 0 : 2;
                             uploadDoc(status);
                         })
                         .setNegativeButton("Xeyr", null);
            dialogBuilder.show();
        });

        if (cameraScanning) cameraScanner.setVisibility(View.VISIBLE);

        cameraScanner.setOnClickListener(v -> {
            Intent barcodeIntent = new Intent(this, BarcodeScannerCamera.class);
            startActivityForResult(barcodeIntent, 1);
        });

        notesEdit.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                notes = s.toString();
                ContentValues values = new ContentValues();
                values.put(DBHelper.NOTES, notes);
                dbHelper.updateApproveDoc(trxNo, values);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        loadData();

        loadFooter();
    }

    @Override
    public void onBackPressed()
    {
        if (!searchView.isIconified()) searchView.setIconified(true);
        else finish();
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
                ((TrxAdapter) Objects.requireNonNull(trxListView.getAdapter())).getFilter()
                                                                               .filter(newText);
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
            if (barcode != null) onScanComplete(barcode);
        }
    }

    private void createNewDoc()
    {
        Doc doc = new Doc();
        doc.setTrxNo(trxNo);
        doc.setTrxDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        dbHelper.addProductApproveDoc(doc);
        docCreated = true;
        setTitle(trxNo);
    }

    public void loadData()
    {
        setTitle(trxNo);
        trxList = dbHelper.getApproveTrxList(trxNo);
        TrxAdapter adapter = new TrxAdapter(this, trxList);
        trxListView.setLayoutManager(new LinearLayoutManager(this));
        trxListView.setAdapter(adapter);

        if (trxList.size() == 0) findViewById(R.id.trx_list_scroll).setVisibility(View.GONE);
        else findViewById(R.id.trx_list_scroll).setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanComplete(String barcode)
    {
        getInvFromServer(barcode);
    }

    private void getInvFromServer(String barcode)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv", "by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addRequestParameters(url, parameters);
            Inventory currentInv = getSimpleObject(url, "GET", null, Inventory.class);

            if (currentInv != null) runOnUiThread(() -> showAddInvDialog(currentInv));
        }).start();
    }

    private void getInvList()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv", "by-user-producer-list");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            invList = getListData(url, "GET", null, Inventory[].class);

            runOnUiThread(() -> {
                if (invList != null) showInvList();
            });
        }).start();
    }

    private void showInvList()
    {
        View view = LayoutInflater.from(this)
                                  .inflate(R.layout.inv_list_dialog,
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
        listView.setOnItemClickListener(
                (parent, view1, position, id) -> showAddInvDialog((Inventory) view1.getTag()));
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            showInfoDialog((Inventory) view1.getTag());
            return true;
        });
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Mallar")
                                                          .setView(view)
                                                          .create();
        dialog.show();
    }

    private void showAddInvDialog(Inventory inventory)
    {
        if (inventory.getInvCode() == null)
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                              android.R.drawable.ic_dialog_info);
        else
        {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.requestFocus();
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(inventory.getInvCode())
                         .setMessage(inventory.getInvName())
                         .setCancelable(false)
                         .setView(qtyEdit)
                         .setPositiveButton("OK", (dialog1, which) -> {
                             try
                             {
                                 double qty = Double.parseDouble(qtyEdit.getText().toString());
                                 Trx containingTrx = containingTrx(inventory.getInvCode());
                                 if (containingTrx.getTrxId() > 0)
                                     updateApproveTrxQty(containingTrx,
                                                         qty + containingTrx.getQty());
                                 else
                                 {
                                     Trx trx = Trx.parseFromInv(inventory);
                                     trx.setQty(qty);
                                     trx.setTrxNo(trxNo);
                                     addApproveTrx(trx);
                                 }
                                 loadData();
                             }
                             catch (NumberFormatException e)
                             {
                                 showAddInvDialog(inventory);
                             }
                         })
                         .setNegativeButton("Ləğv et", null);
            AlertDialog dialog = dialogBuilder.create();

            Objects.requireNonNull(dialog.getWindow())
                   .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void showEditInvDialog(Trx trx)
    {
        if (trx == null)
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                              android.R.drawable.ic_dialog_info);
        else
        {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.setText(String.format(Locale.getDefault(), "%.0f", trx.getQty()));
            qtyEdit.selectAll();
            qtyEdit.requestFocus();
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(trx.getInvCode())
                         .setMessage(trx.getInvName())
                         .setCancelable(false)
                         .setView(qtyEdit)
                         .setPositiveButton("OK", (dialog1, which) -> {
                             try
                             {
                                 double qty = Double.parseDouble(qtyEdit.getText().toString());
                                 updateApproveTrxQty(trx, qty);
                                 loadData();
                             }
                             catch (NumberFormatException e)
                             {
                                 showEditInvDialog(trx);
                             }
                         })
                         .setNegativeButton("Ləğv et", null);
            AlertDialog dialog = dialogBuilder.create();

            Objects.requireNonNull(dialog.getWindow())
                   .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void addApproveTrx(Trx trx)
    {
        dbHelper.addApproveTrx(trx);
        if (!docCreated) createNewDoc();
        loadData();
    }

    private void updateApproveTrxQty(Trx trx, double qty)
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.QTY, qty);
        dbHelper.updateApproveTrx(String.valueOf(trx.getTrxId()), values);
    }

    private Trx containingTrx(String invCode)
    {
        for (Trx trx : trxList)
        {
            if (trx.getInvCode().equals(invCode) && !trx.isReturned())
                return trx;
        }

        return new Trx();
    }

    private void uploadDoc(int status)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv-move", "approve-prd", "insert");
            ProductApproveRequest request = new ProductApproveRequest();
            request.setTrxNo(trxNo);
            request.setTrxDate(new java.sql.Date(System.currentTimeMillis()).toString());
            request.setStatus(status);
            request.setNotes(notes);
            request.setUserId(config().getUser().getId());
            List<ProductApproveRequestItem> requestItems = new ArrayList<>();
            for (Trx trx : trxList)
            {
                ProductApproveRequestItem requestItem = new ProductApproveRequestItem();
                requestItem.setInvCode(trx.getInvCode());
                requestItem.setInvName(trx.getInvName());
                requestItem.setInvBrand(trx.getInvBrand());
                requestItem.setBarcode(trx.getBarcode());
                requestItem.setQty(trx.getQty());
                requestItems.add(requestItem);
            }
            request.setRequestItems(requestItems);

            executeUpdate(url, request, message -> {
                if (message.getStatusCode() == 0)
                {
                    dbHelper.deleteApproveDoc(trxNo);
                    finish();
                }
            });
        }).start();
    }

    private void print(String html)
    {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient()
        {

            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                showProgressDialog(false);
                createWebPrintJob(view);
            }
        });

        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private void createWebPrintJob(WebView webView)
    {
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        String jobName = getString(R.string.app_name) + " Document";

        PrintDocumentAdapter printAdapter;
        PrintAttributes.Builder builder = new PrintAttributes.Builder().setMediaSize(
                PrintAttributes.MediaSize.ISO_A4);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            printAdapter = webView.createPrintDocumentAdapter(jobName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_LONG_EDGE);
            printManager.print(jobName, printAdapter, builder.build());
        }
        else
            try
            {

                Class<?> printDocumentAdapterClass = Class.forName(
                        "android.print.PrintDocumentAdapter");
                Method createPrintDocumentAdapterMethod = webView.getClass()
                                                                 .getMethod(
                                                                         "createPrintDocumentAdapter");
                Object printAdapterObject = createPrintDocumentAdapterMethod.invoke(webView);

                Class<?> printAttributesBuilderClass = Class.forName(
                        "android.print.PrintAttributes$Builder");
                Constructor<?> constructor = printAttributesBuilderClass.getConstructor();
                Object printAttributes = constructor.newInstance();
                Method buildMethod = printAttributes.getClass().getMethod("build");
                Object printAttributesBuild = buildMethod.invoke(printAttributes);

                Method printMethod = printManager.getClass()
                                                 .getMethod("print", String.class,
                                                            printDocumentAdapterClass,
                                                            printAttributesBuild.getClass());
                printMethod.invoke(printManager, jobName, printAdapterObject, builder.build());
            }
            catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                   IllegalAccessException | InstantiationException e)
            {
                e.printStackTrace();
            }
    }

    private String getPrintForm()
    {
        String html = "<html><head><style>*{margin:0px; padding:0px}" +
                      "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                      "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                      "</head><body>";
        html = html.concat("<h3 style='text-align: center'>İstehsaldan mal qəbulu</h3></br>");
        html = html.concat("<h4>" + notes + "</h4></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Barkod</th>");
        html = html.concat("<th>Brend</th>");
        html = html.concat("<th>İç sayı</th>");
        html = html.concat("<th>Miqdar</th>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Collections.sort(trxList, Comparator.comparing(Trx::getInvCode));
        for (Trx trx : trxList)
        {

            html = html.concat("<tr><td>" + trx.getInvCode() + "</td>");
            html = html.concat("<td>" + trx.getInvName() + "</td>");
            html = html.concat("<td>" + trx.getBarcode() + "</td>");
            html = html.concat("<td>" + trx.getInvBrand() + "</td>");
            html = html.concat("<td>" + trx.getNotes() + "</td>");
            html = html.concat("<td style='text-align: right'>" + trx.getQty() + "</td></tr>");
        }

        html = html.concat("</table></body></head>");

        return html;
    }

    private void showInfoDialog(Inventory inventory)
    {
        String info = inventory.getInvName();
        info += "\n\nBrend: " + inventory.getInvBrand();
        info += "\n\nİç sayı: " + inventory.getInternalCount();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) -> {
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
        ProductApproveTrxActivity activity;

        public InventoryAdapter(@NonNull Context context, @NonNull List<Inventory> objects)
        {
            super(context, 0, 0, objects);
            list = objects;
            activity = (ProductApproveTrxActivity) context;
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
                        if (inventory.getBarcode()
                                     .concat(inventory.getInvCode())
                                     .concat(inventory.getInvName())
                                     .concat(inventory.getInvBrand())
                                     .toLowerCase()
                                     .contains(constraint))
                            filteredArrayData.add(inventory);
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
        private final ProductApproveTrxActivity activity;
        List<Trx> trxList;
        View itemView;

        public TrxAdapter(Context context, List<Trx> trxList)
        {
            this.trxList = trxList;
            activity = (ProductApproveTrxActivity) context;
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
            itemView.setOnLongClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        ProductApproveTrxActivity.this);
                dialogBuilder.setTitle(selectedTrx.getInvName())
                             .setMessage("Silmək istəyirsiniz?")
                             .setPositiveButton("Bəli", (dialog1, which) -> {
                                 dbHelper.deleteApproveTrx(String.valueOf(selectedTrx.getTrxId()));
                                 loadData();
                             })
                             .setNegativeButton("Xeyr", null);
                dialogBuilder.show();
                return true;
            });
            itemView.setOnClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                showEditInvDialog(selectedTrx);
            });
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.invBrand.setText(String.valueOf(trx.getInvBrand()));
            holder.notes.setText(String.valueOf(trx.getNotes()));
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
                        if (trx.getInvCode()
                               .concat(trx.getInvName())
                               .concat(trx.getInvBrand())
                               .toLowerCase()
                               .contains(constraint))
                            filteredArrayData.add(trx);
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
package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ApproveTrxActivity extends ScannerSupportActivity
{

    RadioButton fromCustomerBtn;
    RadioButton fromWhsBtn;
    ImageButton selectInvBtn;
    ImageButton uploadBtn;
    ImageButton printBtn;
    Button selectSrcBtn;
    RecyclerView trxListView;
    TextView srcTxt;
    Spinner trgWhsListSpinner;

    List<Trx> trxList;
    List<Whs> trgWhsList;
    List<Inventory> invList;

    int mode;
    boolean docCreated;
    boolean isReady;
    int trxTypeId = 27;
    double amount;
    String trxNo;
    String trgWhsCode;
    String srcWhsCode;
    String srcWhsName;
    String bpCode;
    String bpName;
    String sbeCode;
    String sbeName;
    Customer customer;
    Sbe sbe;
    Whs srcWhs;
    Whs trgWhs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.approve_trx_layout);
        selectInvBtn = findViewById(R.id.inv_list);
        uploadBtn = findViewById(R.id.send);
        printBtn = findViewById(R.id.print);
        selectSrcBtn = findViewById(R.id.select_src);
        trxListView = findViewById(R.id.trx_list_view);
        fromCustomerBtn = findViewById(R.id.customer_btn);
        fromWhsBtn = findViewById(R.id.src_whs_btn);
        trgWhsListSpinner = findViewById(R.id.trg_whs_list);
        srcTxt = findViewById(R.id.src_text);

        mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);
        if (mode == AppConfig.VIEW_MODE)
        {
            docCreated = true;
            trxNo = getIntent().getStringExtra("trxNo");
            trxTypeId = getIntent().getIntExtra("trxTypeId", 27);
            trgWhsCode = getIntent().getStringExtra("whsCode");
            srcWhsCode = getIntent().getStringExtra("srcWhsCode");
            srcWhsName = getIntent().getStringExtra("srcWhsName");
            bpCode = getIntent().getStringExtra("bpCode");
            bpName = getIntent().getStringExtra("bpName");
            sbeCode = getIntent().getStringExtra("sbeCode");
            sbeName = getIntent().getStringExtra("sbeName");
            amount = getIntent().getDoubleExtra("amount", 0);

            switch (trxTypeId)
            {
                case 27:
                    fromCustomerBtn.setChecked(true);
                    break;
                case 53:
                    fromWhsBtn.setChecked(true);
                    break;
            }

            isReady = !srcTxt.getText().toString().isEmpty();
        }
        else
        {
            trxNo = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(new Date());
        }

        fromCustomerBtn.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            trxTypeId = isChecked ? 27 : 53;
            isReady = false;
            srcTxt.setText("");
        });

        fromWhsBtn.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            trxTypeId = isChecked ? 53 : 27;
            isReady = false;
            srcTxt.setText("");
        });

        printBtn.setOnClickListener(v ->
        {
            if (isReady)
            {
                String report;
                switch (trxTypeId)
                {
                    case 27:
                        report = getReturnForm();
                        break;
                    case 53:
                        report = getTransferForm();
                        break;
                    default:
                        report = "";
                }
                showProgressDialog(true);
                print(report);
            }
            else
            {
                showMessageDialog(getString(R.string.info),
                        "Mənbə seçilməyib",
                        android.R.drawable.ic_dialog_info);
            }
        });

        selectSrcBtn.setOnClickListener(v ->
        {
            switch (trxTypeId)
            {
                case 27:
                    getSbeList();
                    break;
                case 53:
                    getWhsList();
                    break;
            }
        });

        trgWhsListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                trgWhs = (Whs) parent.getItemAtPosition(position);
                trgWhsCode = trgWhs.getWhsCode();
                if (docCreated)
                    updateDocTrg();
                else
                    createNewDoc();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        selectInvBtn.setOnClickListener(v ->
        {
            if (invList == null)
                getInvList();
            else
                showInvList();
        });

        uploadBtn.setOnClickListener(v ->
        {
            switch (trxTypeId)
            {
                case 27:
                    break;
                case 53:
                    uploadTransfer();
                    break;
            }
        });

        loadTrgWhsList();

        loadData();

        loadFooter();
    }

    @Override
    public void onBackPressed()
    {
        if (trxList.size() == 0)
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Mal daxil edilməyib. Sənəd silinsin?")
                    .setPositiveButton("Bəli", (dialog1, which) ->
                    {
                        dbHelper.deleteApproveDoc(trxNo);
                        finish();
                    })
                    .setNegativeButton("Xeyr", (dialog12, which) -> finish())
                    .create();
            dialog.show();
        }
        else
            finish();
    }

    private void createNewDoc()
    {
        Doc doc = new Doc();
        doc.setTrxNo(trxNo);
        doc.setTrxTypeId(trxTypeId);
        doc.setTrxDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        doc.setWhsCode(trgWhs.getWhsCode());
        switch (trxTypeId)
        {
            case 27:
                doc.setBpCode(bpCode);
                doc.setBpName(bpName);
                doc.setSbeCode(sbeCode);
                doc.setSbeName(sbeName);
                break;
            case 53:
                doc.setSrcWhsCode(srcWhsCode);
                doc.setSrcWhsName(srcWhsName);
        }
        doc.setAmount(amount);
        dbHelper.addApproveDoc(doc);
        docCreated = true;
        setTitle(trxNo);
    }

    private void updateDocAmount()
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.AMOUNT, amount);
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    private void updateDocTrg()
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.TRG_WHS, trgWhs.getWhsCode());
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    private void updateDocSrc()
    {
        ContentValues values = new ContentValues();
        switch (trxTypeId)
        {
            case 27:
                bpCode = customer.getBpCode();
                bpName = customer.getBpName();
                sbeCode = customer.getSbeCode();
                sbeName = customer.getSbeName();
                values.put(DBHelper.TRX_TYPE_ID, trxTypeId);
                values.put(DBHelper.BP_CODE, customer.getBpCode());
                values.put(DBHelper.BP_NAME, customer.getBpName());
                values.put(DBHelper.SBE_CODE, customer.getSbeCode());
                values.put(DBHelper.SBE_NAME, customer.getSbeName());
                values.put(DBHelper.SRC_WHS_CODE, "");
                values.put(DBHelper.SRC_WHS_NAME, "");
                break;
            case 53:
                srcWhsCode = srcWhs.getWhsCode();
                srcWhsName = srcWhs.getWhsName();
                values.put(DBHelper.TRX_TYPE_ID, trxTypeId);
                values.put(DBHelper.BP_CODE, "");
                values.put(DBHelper.BP_NAME, "");
                values.put(DBHelper.SBE_CODE, "");
                values.put(DBHelper.SBE_NAME, "");
                values.put(DBHelper.SRC_WHS_CODE, srcWhs.getWhsCode());
                values.put(DBHelper.SRC_WHS_NAME, srcWhs.getWhsName());
                break;
        }
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    private void loadData()
    {
        customer = new Customer();
        customer.setBpCode(bpCode);
        customer.setBpName(bpName);
        customer.setSbeCode(sbeCode);
        customer.setSbeName(sbeName);
        sbe = new Sbe();
        sbe.setSbeCode(sbeCode);
        sbe.setSbeName(sbeName);
        trgWhs = new Whs();
        trgWhs.setWhsCode(trgWhsCode);
        srcWhs = new Whs();
        srcWhs.setWhsCode(srcWhsCode);
        srcWhs.setWhsName(srcWhsName);
        srcTxt.setText(trxTypeId == 27 ? customer.toString() : srcWhs.toString());
        setTitle(trxNo);
        trxList = dbHelper.getApproveTrxList(trxNo);
        TrxAdapter adapter = new TrxAdapter(trxList);
        trxListView.setLayoutManager(new LinearLayoutManager(this));
        trxListView.setAdapter(adapter);

        updateButtonsStatus();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        getInvFromServer(barcode);
    }

    private void updateButtonsStatus()
    {
        if (trxTypeId == 27 && trxList.size() > 0)
        {
            fromWhsBtn.setEnabled(!isReady);
            selectSrcBtn.setEnabled(!isReady);
        }
        else
        {
            fromWhsBtn.setEnabled(true);
            selectSrcBtn.setEnabled(true);
        }
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

    private void getSbeList()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            List<Sbe> sbeList = null;
            String url = url("src", "sbe");
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                String result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Sbe>>()
                {
                }.getType();
                sbeList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }

            List<Sbe> finalSbeList = sbeList;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalSbeList == null)
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
                else
                    showSbeList(finalSbeList);
            });
        }).start();
    }

    private void getCustomerList(String sbeCode)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            List<Customer> customerList = null;
            String url = url("src", "customer");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("sbe-code", sbeCode);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                String result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Customer>>()
                {
                }.getType();
                customerList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }

            List<Customer> finalCustomerList = customerList;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalCustomerList == null)
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
                else
                    showCustomerList(finalCustomerList);
            });
        }).start();
    }

    private void getWhsList()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            List<Whs> whsList = null;
            String url = url("src", "whs");
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
            }

            List<Whs> finalWhsList = whsList;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalWhsList == null)
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
                else
                    showSrcWhsList(finalWhsList);
            });
        }).start();
    }

    private void loadTrgWhsList()
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
                trgWhsList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                trgWhsList = new ArrayList<>();
                trgWhsList.add(trgWhs);
            }
            finally
            {
                runOnUiThread(this::publishTrgWhsList);
            }
        }).start();
    }

    private void getInvList()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv");
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
                else
                    showInvList();
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Mallar")
                .setView(view)
                .create();
        dialog.show();
    }

    private void publishTrgWhsList()
    {
        ArrayAdapter<Whs> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, trgWhsList);
        trgWhsListSpinner.setAdapter(adapter);
        trgWhsListSpinner.setSelection(trgWhsList.indexOf(trgWhs));
    }

    private void showSbeList(List<Sbe> sbeList)
    {
        View view = LayoutInflater.from(this).inflate(R.layout.result_list_dialog,
                findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Sbe> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, sbeList);
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Təmsilçilər")
                .setView(view)
                .create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) ->
        {
            dialog.dismiss();
            sbe = (Sbe) adapterView.getItemAtPosition(i);
            getCustomerList(sbe.getSbeCode());
        });
    }

    private void showCustomerList(List<Customer> customerList)
    {
        View view = LayoutInflater.from(this).inflate(R.layout.result_list_dialog,
                findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Customer> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, customerList);
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Müştərilər")
                .setView(view)
                .create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) ->
        {
            dialog.dismiss();
            customer = (Customer) adapterView.getItemAtPosition(i);
            srcTxt.setText(customer.toString());
            updateDocSrc();
            if (trxList.size() > 0)
            {
                fromWhsBtn.setEnabled(false);
                selectSrcBtn.setEnabled(false);
                for (Trx trx : trxList)
                {
                    dbHelper.deleteApproveTrx(String.valueOf(trx.getTrxId()));
                    amount -= trx.getAmount();
                    splitTrx(trx);
                }
            }
        });
    }

    private void showSrcWhsList(List<Whs> whsList)
    {
        View view = LayoutInflater.from(this).inflate(R.layout.result_list_dialog,
                findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Whs> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, whsList);
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Təmsilçilər")
                .setView(view)
                .create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) ->
        {
            dialog.dismiss();
            srcWhs = (Whs) adapterView.getItemAtPosition(i);
            srcTxt.setText(srcWhs.toString());
            if (docCreated)
                updateDocSrc();
            else
                createNewDoc();
        });
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
            qtyEdit.setText("0");
            qtyEdit.selectAll();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(inventory.getInvCode())
                    .setMessage(inventory.getInvName())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) ->
                    {
                        try
                        {
                            double qty = Double.parseDouble(qtyEdit.getText().toString());
                            Trx containingTrx = containingTrx(inventory.getInvCode());
                            if (containingTrx.getTrxId() > 0)
                                updateApproveTrxQty(containingTrx, qty + containingTrx.getQty());
                            else
                            {
                                Trx trx = Trx.parseFromInv(inventory);
                                trx.setQty(qty);
                                trx.setTrxNo(trxNo);
                                trx.setAmount(trx.getPrice() * qty);
                                if (trxTypeId == 27 && !isReturned(trx.getInvCode()))
                                    splitTrx(trx);
                                else
                                    addApproveTrx(trx);
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
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(trx.getInvCode())
                    .setMessage(trx.getInvName())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) ->
                    {
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
                    .setNegativeButton("Ləğv et", null)
                    .create();

            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void addApproveTrx(Trx trx)
    {
        dbHelper.addApproveTrx(trx);
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

    private void updateApproveTrxQty(Trx trx, double qty)
    {
        ContentValues values = new ContentValues();
        values.put(DBHelper.QTY, qty);
        dbHelper.updateApproveTrx(String.valueOf(trx.getTrxId()), values);
        amount += (qty - trx.getQty()) * trx.getPrice();
        updateDocAmount();
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

    private boolean isReturned(String invCode)
    {
        for (Trx trx : trxList)
        {
            if (trx.getInvCode().equals(invCode) && trx.isReturned())
                return true;
        }

        return false;
    }

    private void splitTrx(Trx trx)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            ArrayList<Trx> splitTrxList = null;
            String url = url("trx", "split-trx");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("bp-code", bpCode);
            parameters.put("inv-code", trx.getInvCode());
            parameters.put("qty", String.valueOf(trx.getQty()));
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                String result = template.getForObject(url, String.class);
                Gson gson = new Gson();
                Type type = new TypeToken<List<Trx>>()
                {
                }.getType();
                splitTrxList = new ArrayList<>(gson.fromJson(result, type));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                splitTrxList = new ArrayList<>();
            }
            finally
            {
                ArrayList<Trx> finalSplitTrxList = splitTrxList;
                runOnUiThread(() ->
                {
                    showProgressDialog(false);
                    if (finalSplitTrxList.size() > 0)
                        addSplitTrx(finalSplitTrxList, trx);
                    else
                        addApproveTrx(trx);
                });
            }
        }).start();
    }

    private void addSplitTrx(List<Trx> splitTrxList, Trx baseTrx)
    {
        double qtySum = baseTrx.getQty();

        for (int i = 0; i < splitTrxList.size(); i++)
        {
            Trx trx = splitTrxList.get(i);
            trx.setTrxNo(baseTrx.getTrxNo());
            trx.setTrxDate(baseTrx.getTrxDate());
            trx.setInvCode(baseTrx.getInvCode());
            trx.setInvBrand(baseTrx.getInvBrand());
            trx.setInvName(baseTrx.getInvName());
            trx.setDiscount(trx.getQty() * trx.getPrice() * trx.getDiscountRatio() / 100);
            trx.setAmount(trx.getPrice() * trx.getQty());
            if (qtySum < trx.getQty())
            {
                trx.setQty(qtySum);
                trx.setDiscount(trx.getQty() * trx.getPrice() * trx.getDiscountRatio() / 100);
                trx.setAmount(trx.getPrice() * trx.getQty());
            }
            amount += trx.getAmount();
            dbHelper.addApproveTrx(trx);
            qtySum -= trx.getQty();
        }

        if (qtySum > 0)
        {
            baseTrx.setQty(qtySum);
            baseTrx.setAmount(qtySum * baseTrx.getPrice());
            dbHelper.addApproveTrx(baseTrx);
        }
        loadData();
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
        PrintManager printManager = (PrintManager) this
                .getSystemService(Context.PRINT_SERVICE);

        String jobName = getString(R.string.app_name) + " Document";

        PrintDocumentAdapter printAdapter;
        PrintAttributes.Builder builder = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
            printAdapter = webView.createPrintDocumentAdapter(jobName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_LONG_EDGE);
            printManager.print(jobName, printAdapter, builder.build());
        }
        else
        {
            try
            {

                Class<?> printDocumentAdapterClass = Class.forName("android.print.PrintDocumentAdapter");
                Method createPrintDocumentAdapterMethod = webView.getClass().getMethod("createPrintDocumentAdapter");
                Object printAdapterObject = createPrintDocumentAdapterMethod.invoke(webView);

                Class<?> printAttributesBuilderClass = Class.forName("android.print.PrintAttributes$Builder");
                Constructor<?> constructor = printAttributesBuilderClass.getConstructor();
                Object printAttributes = constructor.newInstance();
                Method buildMethod = printAttributes.getClass().getMethod("build");
                Object printAttributesBuild = buildMethod.invoke(printAttributes);

                Method printMethod = printManager.getClass().getMethod("print", String.class, printDocumentAdapterClass, printAttributesBuild.getClass());
                printMethod.invoke(printManager, jobName, printAdapterObject, builder.build());
            }
            catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e)
            {
                e.printStackTrace();
            }
        }
    }

    private String getTransferForm()
    {
        String html = "<html><head><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>Anbardan anbara transfer</h3></br>");
        html = html.concat("<div style='font-size: 14px'><b>Çıxış anbarı: " + srcWhs.toString() + "</br>");
        html = html.concat("Giriş anbarı: " + trgWhs.toString() + "</b></div></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Miqdar</th>");
        html = html.concat("<th>Qiymət</th>");
        html = html.concat("<th>Məbləğ</th>");
        trxList.remove(trxList.size() - 1);
        Collections.sort(trxList, (good, t1) ->
                good.getPrevTrxNo().compareTo(t1.getPrevTrxNo()));
        for (Trx trx : trxList)
        {

            html = html.concat("<tr><td>" + trx.getInvCode() + "</td><td>" + trx.getInvName() + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getQty()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getPrice()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getQty() * trx.getPrice()) + "</td>");
        }

        html = html.concat("<tr><td colspan='4' style='text-align: right'><b>Cəmi</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amount) + "</b></td></tr>");
        html = html.concat("</table></body></head>");

        return html;
    }

    private String getReturnForm()
    {
        String html = "<html><head><meta charset=\"UTF-8\"><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>Satışdan geri qaytarma</h3></br>");
        html = html.concat("<div style='font-size: 14px'><b>Müştəri: " + customer.toString() + "</br>");
        html = html.concat("Təmsilçi: " + sbe.toString() + "</br>");
        html = html.concat("Anbar: " + trgWhs.toString() + "</b></div></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Miqdar</th>");
        html = html.concat("<th>Qiymət</th>");
        html = html.concat("<th>Endirim (%)</th>");
        html = html.concat("<th>Məbləğ</th>");
        html = html.concat("<th>Satış qaiməsi</th></tr>");
        double invAmount;
        double discountAmount = 0;
        double amountWithoutDiscount = 0;
        if (trxTypeId == 27)
            Collections.sort(trxList, (trx1, trx2) ->
                    trx1.getPrevTrxNo().compareTo(trx2.getPrevTrxNo()));
        else
            Collections.sort(trxList, (trx1, trx2) ->
                    trx1.getTrxId() - trx2.getTrxId());
        for (Trx trx : trxList)
        {
            invAmount = trx.getAmount() - trx.getDiscount();
            discountAmount += trx.getDiscount();
            amountWithoutDiscount += invAmount;

            html = html.concat("<tr><td>" + trx.getInvCode() + "</td><td>" + trx.getInvName() + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getQty()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getPrice()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getDiscountRatio()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(invAmount) + "</td>");
            html = html.concat("<td>" + trx.getPrevTrxNo() + "</td></tr>");
        }

        html = html.concat("<tr><td colspan='5' style='text-align: right'><b>Cəmi</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amount) + "</b></td></tr>");
        html = html.concat("<tr><td colspan='5' style='text-align: right'><b>Endirim</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(discountAmount) + "</b></td></tr>");
        html = html.concat("<tr><td colspan='5' style='text-align: right'><b>Cəmi (Endirimli)</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amountWithoutDiscount) + "</b></td></tr>");
        html = html.concat("</table></body></html>");

        return html;
    }

    private String format(double value)
    {
        return String.format(Locale.getDefault(), "%.03f", value);
    }

    private void uploadTransfer()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("trx", "create-transfer");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("src-whs", srcWhs.getWhsCode());
            parameters.put("trg-whs", trgWhs.getWhsCode());
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            JSONArray jsonArray = new JSONArray();
            try
            {
                for (Trx trx : trxList)
                {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("invCode", trx.getInvCode());
                    jsonObject.put("qty", trx.getQty());
                    jsonArray.put(jsonObject);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonArray.toString(), headers);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            Boolean result;
            try
            {
                result = template.postForObject(url, entity, Boolean.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                result = false;
            }
            Boolean finalResult = result;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalResult)
                {
                    dbHelper.deleteApproveDoc(trxNo);
                    finish();
                }
                else
                {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_info);
                }
            });
        }).start();
    }

    @SuppressWarnings("unchecked")
    private static class InventoryAdapter extends ArrayAdapter<Inventory> implements Filterable
    {
        List<Inventory> list;
        ApproveTrxActivity activity;

        public InventoryAdapter(@NonNull Context context, @NonNull List<Inventory> objects)
        {
            super(context, 0, 0, objects);
            list = objects;
            activity = (ApproveTrxActivity) context;
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

    private class TrxAdapter extends RecyclerView.Adapter<TrxAdapter.Holder>
    {
        List<Trx> trxList;
        View itemView;

        public TrxAdapter(List<Trx> trxList)
        {
            this.trxList = trxList;
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
                if (!trx.isReturned())
                {
                    AlertDialog dialog = new AlertDialog.Builder(ApproveTrxActivity.this)
                            .setTitle(trx.getInvName())
                            .setMessage("Silmək istəyirsiniz?")
                            .setPositiveButton("Bəli", (dialog1, which) ->
                            {
                                dbHelper.deleteApproveTrx(String.valueOf(trx.getTrxId()));
                                amount -= trx.getQty() * trx.getPrice();
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
                if (!trx.isReturned())
                    showEditInvDialog(trx);
            });
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.invBrand.setText(String.valueOf(trx.getInvBrand()));
        }

        @Override
        public int getItemCount()
        {
            return trxList.size();
        }

        private class Holder extends RecyclerView.ViewHolder
        {
            TextView invCode;
            TextView invName;
            TextView qty;
            TextView invBrand;

            public Holder(@NonNull View itemView)
            {
                super(itemView);

                invCode = itemView.findViewById(R.id.inv_code);
                invName = itemView.findViewById(R.id.inv_name);
                qty = itemView.findViewById(R.id.qty);
                invBrand = itemView.findViewById(R.id.inv_brand);
            }
        }

    }
}
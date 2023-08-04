package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Trx;

public class ProductApproveDocActivity extends AppBaseActivity
{

    ListView docListView;
    ImageButton add;
    ImageButton download;
    List<Doc> docList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_approve_doc_layout);
        setTitle("Mal qəbulu (İstehsalat)");

        docListView = findViewById(R.id.doc_list);
        add = findViewById(R.id.add);
        download = findViewById(R.id.download);

        if(config().getUser().isApproveFlag()) download.setVisibility(View.VISIBLE);

        add.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductApproveTrxActivity.class);
            intent.putExtra("mode", AppConfig.NEW_MODE);
            startActivity(intent);
        });

        download.setOnClickListener(view -> loadDocsFromServer());

        docListView.setOnItemClickListener((parent, view, position, id) -> {
            Doc doc = (Doc) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, ProductApproveTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("notes", doc.getNotes());
            startActivity(intent);
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Silmək istəyirsiniz?")
                         .setPositiveButton("Bəli", (dialog1, which) -> {
                             Doc doc = (Doc) parent.getItemAtPosition(position);
                             dbHelper.deleteApproveDoc(doc.getTrxNo());
                             loadData();
                         })
                         .setNegativeButton("Xeyr", null);
            dialogBuilder.show();
            return true;
        });

        loadData();

        loadFooter();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadData();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);
        MenuItem attributes = menu.findItem(R.id.inv_attributes);
        attributes.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        attributes.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        menu.findItem(R.id.pick_report).setVisible(false);
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        return true;
    }

    public void loadData()
    {
        docList = dbHelper.getProductApproveDocList();
        if(docList.size() == 0) findViewById(R.id.doc_list_scroll).setVisibility(View.INVISIBLE);
        else
        {
            findViewById(R.id.doc_list_scroll).setVisibility(View.VISIBLE);
            DocAdapter adapter = new DocAdapter(this, R.layout.product_approve_doc_item_layout,
                                                docList);
            docListView.setAdapter(adapter);
        }
    }

    private void loadDocsFromServer()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv-move", "approve-prd", "trx-list");
            List<Trx> trxList = getListData(url, "GET", null, Trx[].class);

            if(trxList != null)
                if(trxList.size() > 0)
                    loadDocListFromServer(trxList);
                else
                    runOnUiThread(() -> showMessageDialog(getString(R.string.info),
                                                          getString(R.string.no_data),
                                                          android.R.drawable.ic_dialog_info));
        }).start();
    }

    private void loadDocListFromServer(List<Trx> trxList)
    {

        String url = url("inv-move", "approve-prd", "doc-list");
        List<Doc> docList = getListData(url, "GET", null, Doc[].class);
        runOnUiThread(() -> {
            for(Doc doc : docList)
            {
                doc.setTrxTypeId(4);
                dbHelper.addApproveDoc(doc);
            }

            for(Trx trx : trxList)
            {
                dbHelper.addApproveTrx(trx);
            }
            loadData();
        });
    }

    static class DocAdapter extends ArrayAdapter<Doc>
    {
        ProductApproveDocActivity activity;
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects)
        {
            super(context, resourceId, objects);
            list = objects;
            activity = (ProductApproveDocActivity) context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            Doc doc = list.get(position);

            if(convertView == null)
                convertView = LayoutInflater.from(getContext())
                                            .inflate(R.layout.product_approve_doc_item_layout,
                                                     parent, false);

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView trxDate = convertView.findViewById(R.id.trx_date);
            TextView trxNotes = convertView.findViewById(R.id.trx_notes);

            trxNo.setText(doc.getTrxNo());
            trxDate.setText(doc.getTrxDate());
            trxNotes.setText(doc.getNotes());
            convertView.setTag(doc);

            return convertView;
        }
    }
}
package az.inci.bmsanbar.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;

public class ApproveDocActivity extends AppBaseActivity
{

    ListView docListView;
    ImageButton add;

    List<Doc> docList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.approve_doc_layout);
        setTitle("Mal qəbulu");

        docListView = findViewById(R.id.doc_list);
        add = findViewById(R.id.add);

        add.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, ApproveTrxActivity.class);
            intent.putExtra("mode", AppConfig.NEW_MODE);
            startActivity(intent);
        });

        docListView.setOnItemClickListener((parent, view, position, id) ->
        {
            Doc doc = (Doc) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, ApproveTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("trxTypeId", doc.getTrxTypeId());
            intent.putExtra("trgWhsCode", doc.getWhsCode());
            intent.putExtra("trgWhsName", doc.getWhsName());
            intent.putExtra("srcWhsCode", doc.getSrcWhsCode());
            intent.putExtra("srcWhsName", doc.getSrcWhsName());
            intent.putExtra("bpCode", doc.getBpCode());
            intent.putExtra("bpName", doc.getBpName());
            intent.putExtra("sbeCode", doc.getSbeCode());
            intent.putExtra("sbeName", doc.getSbeName());
            intent.putExtra("amount", doc.getAmount());
            startActivity(intent);
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Silmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog1, which) ->
                    {
                        Doc doc = (Doc) parent.getItemAtPosition(position);
                        dbHelper.deleteApproveDoc(doc.getTrxNo());
                        loadDocs();
                    })
                    .setNegativeButton("Xeyr", null)
                    .create();
            dialog.show();
            return true;
        });

        loadDocs();

        loadFooter();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadDocs();
    }

    private void loadDocs()
    {
        docList = dbHelper.getApproveDocList();
        if (docList.size() == 0)
        {
            findViewById(R.id.doc_list_scroll).setVisibility(View.INVISIBLE);
        }
        else
        {
            findViewById(R.id.doc_list_scroll).setVisibility(View.VISIBLE);
            DocAdapter adapter = new DocAdapter(this, R.layout.approve_doc_item_layout, docList);
            docListView.setAdapter(adapter);
        }
    }


    static class DocAdapter extends ArrayAdapter<Doc>
    {

        ApproveDocActivity activity;
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects)
        {
            super(context, resourceId, objects);
            list = objects;
            activity = (ApproveDocActivity) context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            Doc doc = list.get(position);

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.approve_doc_item_layout, parent, false);
            }

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView trgWhs = convertView.findViewById(R.id.trg_whs);
            TextView srcWhsName = convertView.findViewById(R.id.src_whs);
            TextView bpName = convertView.findViewById(R.id.bp_name);
            TextView sbeName = convertView.findViewById(R.id.sbe_name);
            TextView amount = convertView.findViewById(R.id.amount);

            trxNo.setText(doc.getTrxNo());
            trgWhs.setText(doc.getWhsCode());
            srcWhsName.setText(doc.getSrcWhsName());
            bpName.setText(doc.getBpName());
            sbeName.setText(doc.getSbeName());
            amount.setText(String.format(Locale.getDefault(), "%.3f", doc.getAmount()));
            convertView.setTag(doc);

            return convertView;
        }
    }
}
package az.inci.bmsanbar;

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

public class ProductApproveDocActivity extends AppBaseActivity
{

    ListView docListView;
    ImageButton add;

    List<Doc> docList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_approve_doc_layout);
        setTitle("Mal qəbulu (İstehsalat)");

        docListView = findViewById(R.id.doc_list);
        add = findViewById(R.id.add);

        add.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, ProductApproveTrxActivity.class);
            intent.putExtra("mode", AppConfig.NEW_MODE);
            startActivity(intent);
        });

        docListView.setOnItemClickListener((parent, view, position, id) ->
        {
            Doc doc = (Doc) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, ProductApproveTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("notes", doc.getNotes());
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
        docList = dbHelper.getProductApproveDocList();
        if (docList.size() == 0)
        {
            findViewById(R.id.doc_list_scroll).setVisibility(View.INVISIBLE);
        }
        else
        {
            findViewById(R.id.doc_list_scroll).setVisibility(View.VISIBLE);
            DocAdapter adapter = new DocAdapter(this, R.layout.product_approve_doc_item_layout, docList);
            docListView.setAdapter(adapter);
        }
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

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.product_approve_doc_item_layout, parent, false);
            }

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
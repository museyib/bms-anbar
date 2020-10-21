package az.inci.bmsanbar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingPackDocActivity extends AppBaseActivity
{

    List<Doc> docList;
    ListView docListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_pack_doc);

        docListView = findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((adapterView, view, i, l) ->
        {
            Doc doc = (Doc) view.getTag();
            Intent intent=new Intent(this, WaitingPackTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("orderTrxNo", doc.getPrevTrxNo());
            intent.putExtra("bpName", doc.getBpName());
            startActivity(intent);
        });
        loadFooter();
    }

    public void loadDocs()
    {
        DocAdapter docAdapter = new DocAdapter(this, R.layout.pack_doc_item_layout, docList);
        docListView.setAdapter(docAdapter);
        if (docList.size() == 0)
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    public void getNewDocs()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("doc", "pack", "all");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                docList = Arrays.asList(template.getForObject(url, Doc[].class));
                runOnUiThread(this::loadDocs);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                showProgressDialog(false);
            }
        }).start();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getNewDocs();
    }

    static class DocAdapter extends ArrayAdapter<Doc>
    {
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects)
        {
            super(context, resourceId, objects);
            list = objects;
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
            Doc doc = list.get(position);

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.pack_doc_item_layout, parent, false);
            }

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView itemCount = convertView.findViewById(R.id.item_count);
            TextView docDesc = convertView.findViewById(R.id.doc_description);
            TextView pickedItemCount = convertView.findViewById(R.id.picked_item_count);
            TextView bpName = convertView.findViewById(R.id.bp_name);
            TextView sbeName = convertView.findViewById(R.id.sbe_name);

            assert doc != null;
            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            docDesc.setText((!doc.getDescription().equals("null")) ? doc.getDescription() : "");
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            bpName.setText(doc.getBpName());
            sbeName.setText(doc.getSbeName());
            convertView.setTag(doc);

            return convertView;
        }
    }
}
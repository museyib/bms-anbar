package az.inci.bmsanbar.activity;

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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Trx;

public class PickDocActivity extends AppBaseActivity
{

    List<Doc> docList;
    ListView docListView;
    ImageButton newDocs;
    ImageButton newDocsIncomplete;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_doc_layout);

        docListView = findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((parent, view, position, id1) ->
        {
            Intent intent = new Intent(this, PickTrxActivity.class);
            Doc doc = (Doc) parent.getItemAtPosition(position);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("prevTrxNo", doc.getPrevTrxNo());
            intent.putExtra("pickGroup", doc.getPickGroup());
            intent.putExtra("pickArea", doc.getPickArea());
            startActivity(intent);
        });

        loadFooter();
        loadDocs();

        newDocs = findViewById(R.id.newDocs);
        newDocs.setOnClickListener(v ->
                loadTrxFromServer(1));

        newDocsIncomplete = findViewById(R.id.newDocsIncomplete);
        newDocsIncomplete.setOnClickListener(v ->
        {
            List<String> list=dbHelper.getIncompletePickDocList(config().getUser().getId());
            if (list.size()==0)
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.no_incomplete_doc),
                        android.R.drawable.ic_dialog_alert);
                playSound(SOUND_FAIL);
            }
            else
            {
                for (String trxNo : list)
                    loadDocFromServer(trxNo);
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadDocs();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    public void loadDocs()
    {
        docList = dbHelper.getPickDocsByPickUser(config().getUser().getId());
        DocAdapter docAdapter = new DocAdapter(this, docList);
        docListView.setAdapter(docAdapter);
        if (docList.size() == 0)
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);
        MenuItem attributes = menu.findItem(R.id.inv_attributes);
        attributes.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        attributes.setOnMenuItemClickListener(item1 ->
        {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        MenuItem report = menu.findItem(R.id.pick_report);
        report.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        report.setOnMenuItemClickListener(item1 ->
        {
            showPickDateDialog("pick-report");
            return true;
        });

        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        return true;
    }

    static class DocAdapter extends ArrayAdapter<Doc>
    {

        DocAdapter(@NonNull Context context, @NonNull List<Doc> objects)
        {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            Doc doc = getItem(position);

            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.pick_doc_item_layout, parent, false);
            }

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView itemCount = convertView.findViewById(R.id.item_count);
            TextView pickedItemCount = convertView.findViewById(R.id.picked_item_count);
            TextView pickArea = convertView.findViewById(R.id.pick_area);
            TextView docDesc = convertView.findViewById(R.id.doc_description);
            TextView whsCode = convertView.findViewById(R.id.whs_code);

            assert doc != null;
            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            pickArea.setText(doc.getPickArea());
            docDesc.setText((!doc.getDescription().equals("null")) ? doc.getDescription() : "");
            whsCode.setText(doc.getWhsCode());

            return convertView;
        }
    }

    private void loadDocFromServer(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("doc", "pick");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            parameters.put("pick-user", config().getUser().getId());
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result = null;
            try
            {
                result = template.getForObject(url, String.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                runOnUiThread(() ->
                {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            }
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
            String finalResult = result;
            runOnUiThread(() -> {
                if (finalResult!=null)
                {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Doc>()
                    {
                    }.getType();
                    Doc doc = gson.fromJson(finalResult, type);
                    dbHelper.addPickDoc(doc);
                    dbHelper.updatePickTrxStatus(doc.getTrxNo(), 1);
                    loadDocs();
                }
            });
        }).start();
    }

    private void loadTrxFromServer(int mode)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("trx", "pick");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("pick-user", config().getUser().getId());
            parameters.put("mode", String.valueOf(mode));
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try
            {
                result=template.getForObject(url, String.class);
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
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
            String finalResult = result;
            runOnUiThread(() -> {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Trx>>()
                {
                }.getType();
                List<Trx> trxList = new ArrayList<>(gson.fromJson(finalResult, type));
                Set<String> trxSet = new HashSet<>();
                if (trxList.isEmpty())
                {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.no_data), android.R.drawable.ic_dialog_info);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    for (Trx trx : trxList)
                    {
                        dbHelper.addPickTrx(trx);
                        trxSet.add(trx.getTrxNo());
                    }

                    for (String trxNo : trxSet)
                    {
                        loadDocFromServer(trxNo);
                    }
                }
            });
        }).start();
    }

}
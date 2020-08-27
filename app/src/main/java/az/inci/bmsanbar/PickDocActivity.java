package az.inci.bmsanbar;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PickDocActivity extends AppBaseActivity {

    List<Doc> docList;
    ListView docListView;
    ImageButton newDocs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_doc_layout);

        docListView= findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((parent, view, position, id1) -> {
            Intent intent=new Intent(this, PickTrxActivity.class);
            Doc doc= (Doc) parent.getItemAtPosition(position);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("prevTrxNo", doc.getPrevTrxNo());
            intent.putExtra("pickGroup", doc.getPickGroup());
            intent.putExtra("pickArea", doc.getPickArea());
            startActivity(intent);
        });

        loadFooter();
        loadDocs();

        newDocs = findViewById(R.id.newDocs);
        newDocs.setOnClickListener(v -> {
            getNewDocs(config().getUser().getId());
            loadDocs();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocs();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void loadDocs()
    {
        docList=dbHelper.getPickDocsByPickUser(config().getUser().getId());
        DocAdapter docAdapter = new DocAdapter(this, docList);
        docListView.setAdapter(docAdapter);
        if (docList.size()==0)
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    public void getNewDocs(String pickUser)
    {
        String url=url("trx", "pick");
        Map<String, String> parameters=new HashMap<>();
        parameters.put("pick-user", pickUser);
        url=addRequestParameters(url, parameters);
        new TrxLoader(this).execute(url);
    }

    static class DocAdapter extends ArrayAdapter<Doc>
    {

        DocAdapter(@NonNull Context context, @NonNull List<Doc> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc=getItem(position);

            if (convertView==null)
            {
                convertView= LayoutInflater.from(getContext())
                        .inflate(R.layout.pick_doc_item_layout, parent, false);
            }

            TextView trxNo=convertView.findViewById(R.id.trx_no);
            TextView itemCount=convertView.findViewById(R.id.item_count);
            TextView pickedItemCount=convertView.findViewById(R.id.picked_item_count);
            TextView pickArea=convertView.findViewById(R.id.pick_area);
            TextView docDesc=convertView.findViewById(R.id.doc_description);

            assert doc != null;
            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            pickArea.setText(doc.getPickArea());
            docDesc.setText((!doc.getDescription().equals("null")) ? doc.getDescription() : "");

            return convertView;
        }
    }

    private static class DocLoader extends AsyncTask<String, Boolean, String> {

        private WeakReference<PickDocActivity> reference;

        DocLoader(PickDocActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            publishProgress(true);
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity=reference.get();
            ((SimpleClientHttpRequestFactory)template.getRequestFactory()).setConnectTimeout(activity.config().getConnectionTimeout()*1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.getForObject(url[0], String.class);
            }
            catch (ResourceAccessException ex) {
                ex.printStackTrace();
                return null;
            }
            catch (RuntimeException ex) {
                ex.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Boolean... b) {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected void onPostExecute(String result) {
            PickDocActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            } else {
                Gson gson = new Gson();
                Type type = new TypeToken<Doc>() {
                }.getType();
                Doc doc = gson.fromJson(result, type);
                activity.dbHelper.addPickDoc(doc);
                activity.loadDocs();
            }
            activity.showProgressDialog(false);
        }
    }

    private static class TrxLoader extends AsyncTask<String, Boolean, String>
    {
        private WeakReference<PickDocActivity> reference;

        TrxLoader(PickDocActivity activity)
        {
            reference=new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            publishProgress(true);
            RestTemplate template=new RestTemplate();
            AppBaseActivity activity=reference.get();
            ((SimpleClientHttpRequestFactory)template.getRequestFactory()).setConnectTimeout(activity.config().getConnectionTimeout()*1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.getForObject(url[0], String.class);
            }
            catch (ResourceAccessException ex)
            {
                ex.printStackTrace();
                return null;
            }
            catch (RuntimeException ex) {
                ex.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Boolean... b) {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected void onPostExecute(String result) {
            PickDocActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            }
            else {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Trx>>() {
                }.getType();
                List<Trx> trxList = new ArrayList<>(gson.fromJson(result, type));
                if (trxList.isEmpty())
                {
                    activity.showMessageDialog(activity.getString(R.string.info),
                            activity.getString(R.string.no_data), android.R.drawable.ic_dialog_info);
                    activity.playSound(SOUND_FAIL);
                }
                else
                {
                    Set<String> trxSet = new HashSet<>();
                    for (Trx trx: trxList)
                    {
                        activity.dbHelper.addPickTrx(trx);
                        trxSet.add(trx.getTrxNo());
                    }

                    for (String trxNo: trxSet)
                    {
                        String url=activity.url("doc", "pick");
                        Map<String, String> parameters=new HashMap<>();
                        parameters.put("trx-no", trxNo);
                        parameters.put("pick-user", activity.config().getUser().getId());
                        url=activity.addRequestParameters(url, parameters);
                        new DocLoader(activity).execute(url);
                    }
                }
            }
            activity.showProgressDialog(false);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);
        MenuItem attributes = menu.findItem(R.id.inv_attributes);
        attributes.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        attributes.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        MenuItem report=menu.findItem(R.id.pick_report);
        report.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        report.setOnMenuItemClickListener(item1 -> {
            showPickDateDialog("pick-report");
            return true;
        });

        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        return true;
    }
}

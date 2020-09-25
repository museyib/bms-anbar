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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackDocActivity extends AppBaseActivity implements SearchView.OnQueryTextListener {

    List<Doc> docList;
    ListView docListView;
    ImageButton newDocs;
    private SearchView searchView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pack_dock_layout);

        docListView = findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((parent, view, position, id1) -> {
            Intent intent = new Intent(this, PackTrxActivity.class);
            Doc doc = (Doc) view.getTag();
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("orderTrxNo", doc.getPrevTrxNo());
            intent.putExtra("bpName", doc.getBpName());
            intent.putExtra("notes", doc.getNotes());
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

    public void loadDocs() {
        docList = dbHelper.getPackDocsByApproveUser(config().getUser().getId());
        DocAdapter docAdapter = new DocAdapter(this, R.layout.pack_doc_item_layout, docList);
        docListView.setAdapter(docAdapter);
        if (docList.size() == 0)
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    public void getNewDocs(String approveUser) {
        String url = url("trx", "pack");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("approve-user", approveUser);
        url = addRequestParameters(url, parameters);
        new TrxLoader(this).execute(url);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        DocAdapter adapter = (DocAdapter) docListView.getAdapter();
        if (adapter != null)
            adapter.getFilter().filter(s);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified())
            searchView.setIconified(true);
        else
            super.onBackPressed();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);

        MenuItem item = menu.findItem(R.id.inv_attributes);
        item.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        MenuItem report = menu.findItem(R.id.pick_report);
        report.setOnMenuItemClickListener(item1 -> {
            showPickDateDialog("pack-report");
            return true;
        });

        MenuItem docList = menu.findItem(R.id.doc_list);
        docList.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, OpenPackDocActivity.class));
            return true;
        });

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setActivated(true);
        return true;
    }

    static class DocAdapter extends ArrayAdapter<Doc> implements Filterable {

        PackDocActivity activity;
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (PackDocActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc = list.get(position);

            if (convertView == null) {
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

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Doc> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Doc doc : activity.docList) {
                        if (doc.getTrxNo().concat(doc.getPrevTrxNo())
                                .concat(doc.getBpName()).concat(doc.getSbeName()
                                        .concat(doc.getDescription()))
                                .toLowerCase().contains(constraint)) {
                            filteredArrayData.add(doc);
                        }
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Doc>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private static class DocLoader extends AsyncTask<String, Boolean, String> {

        private WeakReference<PackDocActivity> reference;

        DocLoader(PackDocActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            publishProgress(true);
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity = reference.get();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.getForObject(url[0], String.class);
            } catch (RuntimeException ex) {
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
            PackDocActivity activity = reference.get();
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
                activity.dbHelper.addPackDoc(doc);
                activity.loadDocs();
            }
            activity.showProgressDialog(false);
        }
    }

    private static class TrxLoader extends AsyncTask<String, Boolean, String> {
        private WeakReference<PackDocActivity> reference;

        TrxLoader(PackDocActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            publishProgress(true);
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity = reference.get();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.getForObject(url[0], String.class);
            } catch (RuntimeException ex) {
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
            PackDocActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            } else {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Trx>>() {
                }.getType();
                List<Trx> trxList = new ArrayList<>(gson.fromJson(result, type));
                Set<String> trxSet = new HashSet<>();
                if (trxList.isEmpty()) {
                    activity.showMessageDialog(activity.getString(R.string.info),
                            activity.getString(R.string.no_data), android.R.drawable.ic_dialog_info);
                    activity.playSound(SOUND_FAIL);
                } else {
                    for (Trx trx : trxList) {
                        activity.dbHelper.addPackTrx(trx);
                        trxSet.add(trx.getTrxNo());
                    }

                    for (String trxNo : trxSet) {
                        String url = activity.url("doc", "pack");
                        Map<String, String> parameters = new HashMap<>();
                        parameters.put("trx-no", trxNo);
                        url = activity.addRequestParameters(url, parameters);
                        new DocLoader(activity).execute(url);
                    }
                }
            }
            activity.showProgressDialog(false);
        }
    }

}
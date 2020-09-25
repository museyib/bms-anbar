package az.inci.bmsanbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryInfoActivity extends ScannerSupportActivity {

    TextView infoText;
    String invCode;
    EditText keywordEdit;
    String keyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventar_info);
        infoText = findViewById(R.id.good_info);
        keywordEdit = findViewById(R.id.keyword_edit);
    }

    @Override
    public void onScanComplete(String barcode) {
        String url = url("inv", "info-by-barcode");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("barcode", barcode);
        url = addRequestParameters(url, parameters);
        new ShowInvAttributes(InventoryInfoActivity.this).execute(url);
    }

    public void viewImage(View view) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("invCode", invCode);
        startActivity(intent);
    }

    public void searchKeyword(View view) {
        keyword = keywordEdit.getText().toString();

        if (keyword.isEmpty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.keyword_not_entered), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            String url = url("inv", "search");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("keyword", keyword);
            url = addRequestParameters(url, parameters);
            new SearchForKeyword(this).execute(url);
        }
    }

    private void showResultListDialog(List<JSONObject> list) {
        View view = LayoutInflater.from(this).inflate(R.layout.result_list_dialog,
                findViewById(android.R.id.content), false);

        ListView listView = view.findViewById(R.id.result_list);
        listView.setAdapter(new ResultListAdapter(this, R.layout.result_list_item, list));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Axtarışın nəticəsi")
                .setView(view)
                .create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            JSONObject jsonObject = list.get(i);
            try {
                invCode = jsonObject.getString("invCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String url = url("inv", "info-by-inv-code");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
            url = addRequestParameters(url, parameters);
            new ShowInvAttributes(this).execute(url);

            dialog.dismiss();
        });
    }

    private void printInfo(String info) {
        info = info.replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        invCode = info.substring(10, 17);
        infoText.setText(info);
    }

    protected static class ShowInvAttributes extends AsyncTask<String, Void, String> {
        WeakReference<InventoryInfoActivity> reference;

        ShowInvAttributes(InventoryInfoActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
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
        protected void onPostExecute(String result) {
            InventoryInfoActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            } else {
                activity.printInfo(result);
                activity.playSound(SOUND_SUCCESS);
            }
        }
    }

    protected static class SearchForKeyword extends AsyncTask<String, Void, String> {
        WeakReference<InventoryInfoActivity> reference;

        SearchForKeyword(InventoryInfoActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            reference.get().showProgressDialog(true);
        }

        @Override
        protected String doInBackground(String... url) {
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity = reference.get();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.getForObject(url[0], String.class);
            } catch (ResourceAccessException ex) {
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            reference.get().showProgressDialog(false);
            InventoryInfoActivity activity = reference.get();
            JSONArray jsonArray;
            List<JSONObject> jsonObjectList = new ArrayList<>();
            try {
                jsonArray = new JSONArray(result);

                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonObjectList.add(jsonArray.getJSONObject(i));
                }
            } catch (RuntimeException | JSONException e) {
                e.printStackTrace();
            }

            activity.showResultListDialog(jsonObjectList);
        }
    }

    private static class ResultListAdapter extends ArrayAdapter<JSONObject> {
        List<JSONObject> list;
        Context context;

        public ResultListAdapter(@NonNull Context context, int resource, List<JSONObject> list) {
            super(context, resource, list);
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            JSONObject item = list.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.result_list_item,
                        parent, false);
            }

            TextView invCodeText = convertView.findViewById(R.id.inv_code);
            TextView invNameText = convertView.findViewById(R.id.inv_name);

            try {
                invCodeText.setText(item.getString("invCode"));
                invNameText.setText(item.getString("invName"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return convertView;
        }
    }
}
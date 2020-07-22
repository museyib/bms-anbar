package az.inci.bmsanbar;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;

public class InventoryInfoActivity extends ScannerSupportActivity {

    TextView infoText;
    String invCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventar_info);
        infoText=findViewById(R.id.good_info);
    }

    @Override
    public void onScanComplete(String barcode) {
        busy=false;
        String url=url("inv", "info", barcode);
        new ShowInvAttributes(InventoryInfoActivity.this).execute(url);
    }

    public void viewImage(View view) {
        Intent intent=new Intent(this, PhotoActivity.class);
        intent.putExtra("invCode", invCode);
        startActivity(intent);
    }

    protected static class ShowInvAttributes extends AsyncTask<String, Void, String>
    {
        WeakReference<InventoryInfoActivity> reference;

        ShowInvAttributes(InventoryInfoActivity activity)
        {
            reference=new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity=reference.get();
            ((SimpleClientHttpRequestFactory)template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout()*1000);
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
            InventoryInfoActivity activity=reference.get();
            if (result==null) {
                activity.showMessageDialog(activity.getString(R.string.info),
                        activity.getString(R.string.good_not_found),
                        android.R.drawable.ic_dialog_info);
                activity.playSound(SOUND_FAIL);
            }
            else {
                activity.printInfo(result);
                activity.playSound(SOUND_SUCCESS);
            }
        }
    }

    private void printInfo(String info)
    {
        info=info.replaceAll("; ", "\n");
        info=info.replaceAll("\\\\n", "\n");
        invCode=info.substring(10, 17);
        Log.e("INV", invCode);
        infoText.setText(info);
    }
}
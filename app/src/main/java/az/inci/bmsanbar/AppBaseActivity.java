package az.inci.bmsanbar;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.ref.WeakReference;
import java.sql.Date;

public class AppBaseActivity extends AppCompatActivity {

    protected static int SOUND_SUCCESS=R.raw.barcodebeep;
    protected static int SOUND_FAIL=R.raw.serror3;

    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;

    AlertDialog progressDialog;
    int mode;
    DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper=new DBHelper(this);
        dbHelper.open();

        soundPool =new SoundPool(10, 3, 5);
        audioManager=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public void loadFooter()
    {
        TextView userId=findViewById(R.id.user_info_id);
        userId.setText(config().getUser().getId());
        userId.append(" - ");
        userId.append(config().getUser().getName());
    }

    public void showProgressDialog(boolean b) {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout,
                findViewById(android.R.id.content), false);
        if (progressDialog == null) {
            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if (b) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    public String url(String... value)
    {
        StringBuilder sb=new StringBuilder();
        sb.append(config().getServerUrl());
        for (String s : value) {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public void loadUserInfo(User user, boolean newUser)
    {
        if (newUser) {
            dbHelper.addUser(user);
        }
        config().setUser(user);
    }

    public AppConfig config()
    {
        return ((App) getApplication()).getConfig();
    }

    protected void showToastMessage(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showMessageDialog(String title, String message, int icon)
    {
        new android.app.AlertDialog.Builder(this)
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message).show();
    }

    protected static class ShowQuantity extends AsyncTask<String, Void, String>
    {
        WeakReference<AppBaseActivity> reference;

        ShowQuantity(AppBaseActivity activity)
        {
            reference=new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            reference.get().showProgressDialog(true);
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
            AppBaseActivity activity=reference.get();
            String title="Anbarda say";
            int type=android.R.drawable.ic_dialog_info;
            if (result==null) {
                title=activity.getString(R.string.error);
                result = activity.getString(R.string.connection_error);
                type=android.R.drawable.ic_dialog_alert;
                activity.playSound(SOUND_FAIL);
            }
            activity.showMessageDialog(title, result, type);
            activity.showProgressDialog(false);
        }
    }

    protected static class GetPickReport extends AsyncTask<String, Boolean, String>
    {
        WeakReference<AppBaseActivity> reference;

        public GetPickReport(AppBaseActivity activity)
        {
            reference=new WeakReference<>(activity);
        }

        @Override
        protected void onProgressUpdate(Boolean... b) {
            reference.get().showProgressDialog(true);
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
        protected void onPostExecute(String result) {

            AppBaseActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            }
            else
            {
                activity.showMessageDialog(activity.getString(R.string.info),
                        "Yığım hesabatı: "+result, android.R.drawable.ic_dialog_info);
            }
            activity.showProgressDialog(false);
        }
    }

    protected void showPickDateDialog(String reportType)
    {
        View view = getLayoutInflater().inflate(R.layout.pick_date_dialog,
                findViewById(android.R.id.content), false);

        EditText fromText=view.findViewById(R.id.from_date);
        EditText toText=view.findViewById(R.id.to_date);

        fromText.setText(new Date(System.currentTimeMillis()).toString());
        toText.setText(new Date(System.currentTimeMillis()).toString());

        AlertDialog dialog=new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Tarix intervalı")
                .setPositiveButton("OK", (dialogInterface, i) -> {

                    String startDate=fromText.getText().toString();
                    String endDate=toText.getText().toString();
                    String url=url("inv", reportType, startDate, endDate, config().getUser().getId());
                    new GetPickReport(this).execute(url);
                })
                .create();
        dialog.show();
    }

    protected void playSound(int resourceId) {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener((soundPool1, i, i1) -> {
            soundPool.play(sound, volume, volume, 1, 0, 1);
        });
    }
}

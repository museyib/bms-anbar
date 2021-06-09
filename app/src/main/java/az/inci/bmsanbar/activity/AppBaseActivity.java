package az.inci.bmsanbar.activity;

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
import java.util.HashMap;
import java.util.Map;

import az.inci.bmsanbar.App;
import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.DBHelper;
import az.inci.bmsanbar.OnInvBarcodeFetched;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.model.User;

public class AppBaseActivity extends AppCompatActivity
{

    protected static int SOUND_SUCCESS = R.raw.barcodebeep;
    protected static int SOUND_FAIL = R.raw.serror3;

    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;

    AlertDialog progressDialog;
    int mode;
    DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        dbHelper.open();

        soundPool = new SoundPool(10, 3, 5);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        dbHelper = new DBHelper(this);
        dbHelper.open();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        dbHelper.close();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        dbHelper.close();
    }

    public void loadFooter()
    {
        TextView userId = findViewById(R.id.user_info_id);
        userId.setText(config().getUser().getId());
        userId.append(" - ");
        userId.append(config().getUser().getName());
    }

    public void showProgressDialog(boolean b)
    {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout,
                findViewById(android.R.id.content), false);
        if (progressDialog == null)
        {
            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if (b)
        {
            progressDialog.show();
        }
        else
        {
            progressDialog.dismiss();
        }
    }

    public String url(String... value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(config().getServerUrl());
        for (String s : value)
        {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public String addRequestParameters(String url, Map<String, String> requestParameters)
    {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for (Map.Entry<String, String> entry : requestParameters.entrySet())
        {
            builder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }

        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    public void loadUserInfo(User user, boolean newUser)
    {
        if (newUser)
        {
            dbHelper.addUser(user);
            dbHelper.addUserPermission(user);
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

    protected void showPickDateDialog(String reportType)
    {
        View view = getLayoutInflater().inflate(R.layout.pick_date_dialog,
                findViewById(android.R.id.content), false);

        EditText fromText = view.findViewById(R.id.from_date);
        EditText toText = view.findViewById(R.id.to_date);

        fromText.setText(new Date(System.currentTimeMillis()).toString());
        toText.setText(new Date(System.currentTimeMillis()).toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Tarix intervalı")
                .setPositiveButton("OK", (dialogInterface, i) ->
                {

                    String startDate = fromText.getText().toString();
                    String endDate = toText.getText().toString();
                    String url = url("inv", reportType);
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("start-date", startDate);
                    parameters.put("end-date", endDate);
                    parameters.put("user-id", config().getUser().getId());
                    url = addRequestParameters(url, parameters);
                    new GetPickReport(this).execute(url);
                })
                .create();
        dialog.show();
    }

    protected void playSound(int resourceId)
    {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener((soundPool1, i, i1) ->
                soundPool.play(sound, volume, volume, 1, 0, 1));
    }

    protected static class ShowQuantity extends AsyncTask<String, Void, String>
    {
        WeakReference<AppBaseActivity> reference;

        ShowQuantity(AppBaseActivity activity)
        {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute()
        {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected String doInBackground(String... url)
        {
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity = reference.get();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try
            {
                result = template.getForObject(url[0], String.class);
            }
            catch (ResourceAccessException ex)
            {
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result)
        {
            AppBaseActivity activity = reference.get();
            String title = "Anbarda say";
            int type = android.R.drawable.ic_dialog_info;
            if (result == null)
            {
                title = activity.getString(R.string.error);
                result = activity.getString(R.string.connection_error);
                type = android.R.drawable.ic_dialog_alert;
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
            reference = new WeakReference<>(activity);
        }

        @Override
        protected void onProgressUpdate(Boolean... b)
        {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected String doInBackground(String... url)
        {
            publishProgress(true);
            RestTemplate template = new RestTemplate();
            AppBaseActivity activity = reference.get();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory()).setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try
            {
                result = template.getForObject(url[0], String.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result)
        {

            AppBaseActivity activity = reference.get();
            if (result == null)
            {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            }
            else
            {
                activity.showMessageDialog(activity.getString(R.string.info),
                        "Yığım hesabatı: " + result, android.R.drawable.ic_dialog_info);
            }
            activity.showProgressDialog(false);
        }
    }

    protected void getInvBarcodeFromServer(String barcode, OnInvBarcodeFetched onInvBarcodeFetched)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("inv", "inv-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addRequestParameters(url, parameters);

            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            InvBarcode invBarcodeFromServer = null;
            try
            {
                invBarcodeFromServer = template.getForObject(url, InvBarcode.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            InvBarcode finalInvBarcodeFromServer = invBarcodeFromServer;
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (finalInvBarcodeFromServer == null
                        || finalInvBarcodeFromServer.getInvCode() == null)
                {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.good_not_found),
                            android.R.drawable.ic_dialog_info);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    onInvBarcodeFetched.invBarcodeFetched(finalInvBarcodeFromServer);
                }
            });
        }).start();
    }
}

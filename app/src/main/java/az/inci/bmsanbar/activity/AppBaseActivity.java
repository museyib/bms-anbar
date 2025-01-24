package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.GlobalParameters.apiVersion;
import static az.inci.bmsanbar.GlobalParameters.connectionTimeout;
import static az.inci.bmsanbar.GlobalParameters.jwt;
import static az.inci.bmsanbar.GlobalParameters.serviceUrl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import az.inci.bmsanbar.App;
import az.inci.bmsanbar.DBHelper;
import az.inci.bmsanbar.OnExecuteComplete;
import az.inci.bmsanbar.OnInvBarcodeFetched;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.model.User;
import az.inci.bmsanbar.model.v2.CustomResponse;
import az.inci.bmsanbar.model.v2.ResponseMessage;
import az.inci.bmsanbar.model.v3.LatestMovementItem;
import az.inci.bmsanbar.security.JwtResolver;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class AppBaseActivity extends AppCompatActivity
{
    protected static int SOUND_SUCCESS = R.raw.barcodebeep;
    protected static int SOUND_FAIL = R.raw.serror3;

    private boolean spinnerChanged;
    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;
    protected DecimalFormat decimalFormat;
    protected SharedPreferences preferences;
    protected JwtResolver jwtResolver;
    AlertDialog progressDialog;
    AlertDialog.Builder dialogBuilder;
    int mode;
    DBHelper dbHelper;
    Gson gson = new Gson();
    Type responseType = new TypeToken<CustomResponse>()
    {
    }.getType();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        jwtResolver = new JwtResolver(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        dbHelper = new DBHelper(this);
        dbHelper.open();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        jwt = preferences.getString("jwt", "");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        dbHelper = new DBHelper(this);
        dbHelper.open();
    }

    public void loadFooter()
    {
        TextView userId = findViewById(R.id.user_info_id);
        userId.setText(getUser().getId());
        userId.append(" - ");
        userId.append(getUser().getName());
    }

    public void showProgressDialog(boolean b)
    {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout,
                                                findViewById(android.R.id.content), false);
        if(progressDialog == null)
            progressDialog = new AlertDialog.Builder(this).setView(view)
                                                          .setCancelable(false)
                                                          .create();

        if(b) progressDialog.show();
        else progressDialog.dismiss();
    }

    public String url(String... value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl).append("/").append(apiVersion);
        for(String s : value)
        {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public String addRequestParameters(String url, Map<String, String> requestParameters)
    {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for(Map.Entry<String, String> entry : requestParameters.entrySet())
        {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    public void loadUserInfo(User user, boolean newUser)
    {
        if(newUser)
        {
            dbHelper.addUser(user);
            dbHelper.addUserPermission(user);
        }
        ((App) getApplication()).setUser(user);
    }
    
    public User getUser()
    {
        return ((App) getApplication()).getUser();
    }

    protected void showToastMessage(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showMessageDialog(String title, String message, int icon)
    {
        if(dialogBuilder == null) dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setIcon(icon).setTitle(title).setMessage(message).show();
    }

    protected void showPickDateDialog(String report)
    {
        View view = getLayoutInflater().inflate(R.layout.pick_date_dialog,
                                                findViewById(android.R.id.content), false);

        EditText fromText = view.findViewById(R.id.from_date);
        EditText toText = view.findViewById(R.id.to_date);
        CheckBox actualCheckBox = view.findViewById(R.id.actual);

        fromText.setText(new Date(System.currentTimeMillis()).toString());
        toText.setText(new Date(System.currentTimeMillis()).toString());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(view)
                     .setTitle("Tarix intervalı")
                     .setPositiveButton("OK", (dialogInterface, i) -> {
                         String startDate = fromText.getText().toString();
                         String endDate = toText.getText().toString();
                         String url = url(report, "report");
                         if(actualCheckBox.isChecked())
                         {
                             url = url(report, "report-actual");
                         }
                         Map<String, String> parameters = new HashMap<>();
                         parameters.put("start-date", startDate);
                         parameters.put("end-date", endDate);
                         parameters.put("user-id", getUser().getId());
                         url = addRequestParameters(url, parameters);
                         showStringData(url, "Yığım hesabatı");
                     });
        dialogBuilder.show();
    }

    protected void playSound(int resourceId)
    {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener(
                (soundPool1, i, i1) -> soundPool.play(sound, volume, volume, 1, 0, 1));
    }

    Response sendRequest(URL url, String method, @Nullable Object requestBodyData)
            throws IOException
    {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
        OkHttpClient httpClient = clientBuilder.build();

        Request request;

        if(method.equals("POST")) {
            RequestBody requestBody = RequestBody.create(MediaType.get("application/json;charset=UTF-8"),
                    new Gson().toJson(requestBodyData));
            request = new Request.Builder()
                    .post(requestBody)
                    .header("Authorization", "Bearer " + jwt)
                    .url(url)
                    .build();
        }
        else {
            request = new Request.Builder()
                    .get()
                    .header("Authorization", "Bearer " + jwt)
                    .url(url)
                    .build();
        }

        return httpClient.newCall(request).execute();
    }

    public void executeUpdate(String urlString, Object requestData,
                              OnExecuteComplete executeComplete)
    {
        new Thread(() -> {
            try
            {
                int statusCode;
                String title;
                String message;
                int iconId;

                Response httpResponse = sendRequest(new URL(urlString), "POST", requestData);
                if(httpResponse.code() == 403)
                {
                    jwt = jwtResolver.resolve();
                    preferences.edit().putString("jwt", jwt).apply();
                    httpResponse = sendRequest(new URL(urlString), "POST", requestData);
                }

                if(httpResponse.code() == 200)
                {
                    ResponseBody responseBody = httpResponse.body();
                    CustomResponse response = gson.fromJson(
                            Objects.requireNonNull(responseBody).string(), responseType);
                    statusCode = response.getStatusCode();

                    if(statusCode == 0)
                    {
                        title = getString(R.string.info);
                        message = response.getDeveloperMessage();
                        iconId = ic_dialog_info;
                    }
                    else
                        if(statusCode == 2)
                        {
                            title = getString(R.string.error);
                            message = response.getDeveloperMessage();
                            iconId = ic_dialog_alert;
                        }
                        else
                        {
                            title = getString(R.string.error);
                            message = response.getDeveloperMessage() + ": " + response.getSystemMessage();
                            iconId = ic_dialog_alert;
                        }
                }
                else
                {
                    statusCode = httpResponse.code();
                    title = getString(R.string.error);
                    message = httpResponse.toString();
                    iconId = ic_dialog_alert;
                }

                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setStatusCode(statusCode);
                responseMessage.setTitle(title);
                responseMessage.setBody(message);
                responseMessage.setIconId(iconId);

                runOnUiThread(() -> executeComplete.executeComplete(responseMessage));
            }
            catch(IOException e)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            }
            finally
            {
                runOnUiThread(() -> {
                    loadData();
                    showProgressDialog(false);
                });
            }
        }).start();
    }

    protected void loadData() {}

    protected void showStringData(String url, String title)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String result = getSimpleObject(url, "GET", null, String.class);
            if(result != null)
                runOnUiThread(() -> showMessageDialog(title, result, ic_dialog_info));
        }).start();
    }

    public  <T> T getSimpleObject(String url, String method, Object request, Class<T> tClass)
    {
        try
        {
            Response httpResponse = sendRequest(new URL(url), method, request);
            if(httpResponse.code() == 403)
            {
                jwt = jwtResolver.resolve();
                preferences.edit().putString("jwt", jwt).apply();
                httpResponse = sendRequest(new URL(url), method, request);
            }
            if(httpResponse.code() == 200)
            {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(
                        Objects.requireNonNull(responseBody).string(),
                        responseType);
                if(response.getStatusCode() == 0)
                    return gson.fromJson(gson.toJson(response.getData()), tClass);
                else
                    if(response.getStatusCode() == 2)
                    {
                        runOnUiThread(() -> {
                            showMessageDialog(getString(R.string.error),
                                              response.getDeveloperMessage(),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        });
                        return null;
                    }
                    else
                    {
                        runOnUiThread(() -> {
                            showMessageDialog(getString(R.string.error),
                                              response.getDeveloperMessage() + ": " +
                                              response.getSystemMessage(),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        });
                        return null;
                    }
            }
            else
            {
                String message = httpResponse.toString();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), message, ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        }
        catch(IOException e)
        {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                                  getString(R.string.internal_error) + ": " + e,
                                  ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        }
        finally
        {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }

    public  <T> List<T> getListData(String url, String method, Object request, Class<T[]> tClass)
    {
        try
        {
            Response httpResponse = sendRequest(new URL(url), method, request);
            if(httpResponse.code() == 403)
            {
                jwt = jwtResolver.resolve();
                preferences.edit().putString("jwt", jwt).apply();
                httpResponse = sendRequest(new URL(url), method, request);
            }
            if(httpResponse.code() == 200)
            {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(
                        Objects.requireNonNull(responseBody).string(),
                        responseType);
                if(response.getStatusCode() == 0)
                    return new ArrayList<>(
                            Arrays.asList(gson.fromJson(gson.toJson(response.getData()), tClass)));
                else
                    if(response.getStatusCode() == 2)
                    {
                        runOnUiThread(() -> {
                            showMessageDialog(getString(R.string.error),
                                              response.getDeveloperMessage(),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        });
                        return null;
                    }
                    else
                    {
                        runOnUiThread(() -> {
                            showMessageDialog(getString(R.string.error),
                                              response.getDeveloperMessage() + ": " +
                                              response.getSystemMessage(),
                                              ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        });
                        return null;
                    }
            }
            else
            {
                String message = httpResponse.toString();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), message, ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        }
        catch(Exception e)
        {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                                  getString(R.string.internal_error) + ": " + e,
                                  ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        }
        finally
        {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }

    protected void getInvBarcodeFromServer(String barcode, OnInvBarcodeFetched onInvBarcodeFetched)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv", "inv-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addRequestParameters(url, parameters);

            InvBarcode invBarcode = getSimpleObject(url, "GET", null, InvBarcode.class);

            if(invBarcode != null)
                runOnUiThread(() -> {
                    if(invBarcode.getBarcode() == null)
                    {
                        showMessageDialog(getString(R.string.info),
                                          getString(R.string.good_not_found),
                                          ic_dialog_info);
                        playSound(SOUND_FAIL);
                    }
                    else
                        onInvBarcodeFetched.invBarcodeFetched(invBarcode);
                });
        }).start();
    }

    @SuppressLint("HardwareIds")
    public String getDeviceIdString()
    {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    protected void getLatestMovements(String invCode, String whsCode)
    {
        spinnerChanged = false;
        View view = LayoutInflater.from(this)
                .inflate(R.layout.result_list_dialog,
                        findViewById(android.R.id.content), false);
        Spinner spinner = view.findViewById(R.id.top_spinner);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList(5, 10, 15, 20, 25, 30)));
        spinner.setVisibility(VISIBLE);

        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
        android.app.AlertDialog dialog = dialogBuilder.setTitle("Son hərəkət tarixçəsi").setView(view).create();
        dialog.show();

        ListView listView = view.findViewById(R.id.result_list);
        SearchView searchView = view.findViewById(R.id.search);
        searchView.setVisibility(GONE);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerChanged)
                    refreshLatestMovements(invCode, whsCode, (int) parent.getItemAtPosition(position), listView);
                else
                    spinnerChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshLatestMovements(invCode, whsCode, 5, listView);
    }

    private void refreshLatestMovements(String invCode, String whsCode, int top, ListView listView)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("inv", "latest-movements");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
            parameters.put("whs-code", whsCode);
            parameters.put("top", String.valueOf(top));
            url = addRequestParameters(url, parameters);
            List<LatestMovementItem> latestMovementItems = getListData(url, "GET", null, LatestMovementItem[].class);
            runOnUiThread(() -> {
                if(latestMovementItems != null)
                {
                    ArrayAdapter<LatestMovementItem> adapter = (ArrayAdapter<LatestMovementItem>) listView.getAdapter();
                    if (adapter == null) {
                        adapter = new ArrayAdapter<LatestMovementItem>(this, R.layout.simple_list_item, latestMovementItems) {
                            @NonNull
                            @Override
                            public View getView(int position, @Nullable @org.jetbrains.annotations.Nullable View convertView, @NonNull ViewGroup parent) {
                                if (convertView == null)
                                    convertView = LayoutInflater.from(getContext())
                                            .inflate(R.layout.latest_movements_layout_item, parent, false);
                                LatestMovementItem item = latestMovementItems.get(position);
                                ((TextView) convertView.findViewById(R.id.trx_no)).setText(item.getTrxNo());
                                ((TextView) convertView.findViewById(R.id.trx_date)).setText(item.getTrxDate());
                                ((TextView) convertView.findViewById(R.id.quantity)).setText(String.valueOf(item.getQuantity()));
                                return convertView;
                            }
                        };
                        listView.setAdapter(adapter);
                    }
                    else {
                        adapter.clear();
                        adapter.addAll(latestMovementItems);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }).start();
    }
}

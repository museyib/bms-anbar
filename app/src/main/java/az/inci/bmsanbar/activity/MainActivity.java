package az.inci.bmsanbar.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.BuildConfig;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.User;
import az.inci.bmsanbar.model.v2.LoginRequest;

public class MainActivity extends AppBaseActivity
{
    String id;
    String password;
    String serverUrl;
    String imageUrl;
    boolean cameraScanning;
    int connectionTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableStorageAccess();
        loadConfig();

        String[] lastLogin = dbHelper.getLastLogin();
        id = lastLogin[0];
        password = lastLogin[1];
    }

    private void loadConfig()
    {
        serverUrl = dbHelper.getParameter("serverUrl");
        if (serverUrl.isEmpty())
        {
            serverUrl = config().getServerUrl();
        }
        config().setServerUrl(serverUrl);

        imageUrl = dbHelper.getParameter("imageUrl");
        if (imageUrl.isEmpty())
        {
            imageUrl = config().getImageUrl();
        }
        config().setImageUrl(imageUrl);

        connectionTimeout = dbHelper.getParameter("connectionTimeout")
                                    .isEmpty() ? 0 : Integer.parseInt(
                dbHelper.getParameter("connectionTimeout"));
        if (connectionTimeout == 0)
        {
            connectionTimeout = config().getConnectionTimeout();
        }
        config().setConnectionTimeout(connectionTimeout);

        cameraScanning = Boolean.parseBoolean(dbHelper.getParameter("cameraScanning"));
        config().setCameraScanning(cameraScanning);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemSettings = menu.findItem(R.id.settings);
        itemSettings.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        });

        MenuItem itemUpdate = menu.findItem(R.id.update);
        itemUpdate.setOnMenuItemClickListener(item1 -> {
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
                                                                      "Proqram versiyasını yenilə")
                                                              .setMessage(
                                                                      "Dəyişiklikdən asılı olaraq məlumatlar silinə bilər. Yeniləmək istəyirsinizmi?")
                                                              .setNegativeButton("Bəli",
                                                                                 (dialogInterface, i) -> checkForNewVersion())
                                                              .setPositiveButton("Xeyr", null)
                                                              .create();

            dialog.show();
            return true;
        });
        MenuItem itemInfo = menu.findItem(R.id.inv_attributes);
        itemInfo.setOnMenuItemClickListener(item1 -> {
            showLoginDialog(AppConfig.INV_ATTRIBUTE_MODE);
            return true;
        });
        return true;
    }

    protected void enableStorageAccess()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void openPickingDocs(View view)
    {
        showLoginDialog(AppConfig.PICK_MODE);
    }

    public void openPackingDocs(View view)
    {
        showLoginDialog(AppConfig.PACK_MODE);
    }

    public void openShippingDocs(View view)
    {
        showLoginDialog(AppConfig.SHIP_MODE);
    }

    public void openInvApproving(View view)
    {
        showLoginDialog(AppConfig.APPROVE_MODE);
    }

    public void openProductApproving(View view)
    {
        showLoginDialog(AppConfig.PRODUCT_APPROVE_MODE);
    }

    public void openConfirmDelivery(View view)
    {
        showLoginDialog(AppConfig.CONFIRM_DELIVERY_MODE);
    }

    private void showLoginDialog(int mode)
    {
        this.mode = mode;
        View view = getLayoutInflater().inflate(R.layout.login_page,
                                                findViewById(android.R.id.content), false);

        EditText idEdit = view.findViewById(R.id.id_edit);
        EditText passwordEdit = view.findViewById(R.id.password_edit);
        CheckBox fromServerCheck = view.findViewById(R.id.from_server_check);

        AtomicBoolean loginViaServer = new AtomicBoolean(false);
        fromServerCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> loginViaServer.set(isChecked));

        idEdit.setText(id);
        idEdit.selectAll();
        passwordEdit.setText(password);

        AlertDialog loginDialog = new AlertDialog.Builder(this).setTitle(R.string.enter)
                                                               .setView(view)
                                                               .setPositiveButton(R.string.enter,
                                                                                  (dialog, which) -> {
                                                                                      id = idEdit.getText()
                                                                                                 .toString()
                                                                                                 .toUpperCase();
                                                                                      password = passwordEdit.getText()
                                                                                                             .toString();

                                                                                      if (id.isEmpty() ||
                                                                                          password.isEmpty())
                                                                                      {
                                                                                          showToastMessage(
                                                                                                  getString(
                                                                                                          R.string.username_or_password_not_entered));
                                                                                          showLoginDialog(
                                                                                                  mode);
                                                                                          playSound(
                                                                                                  SOUND_FAIL);
                                                                                      }
                                                                                      else
                                                                                      {
                                                                                          User user = dbHelper.getUser(
                                                                                                  id);
                                                                                          if (user ==
                                                                                              null ||
                                                                                              loginViaServer.get())
                                                                                          {
                                                                                              loginViaServer();
                                                                                          }
                                                                                          else
                                                                                          {
                                                                                              loadUserInfo(
                                                                                                      user,
                                                                                                      false);
                                                                                              attemptLogin(
                                                                                                      user);
                                                                                          }

                                                                                          dialog.dismiss();
                                                                                      }
                                                                                  })
                                                               .create();

        Objects.requireNonNull(loginDialog.getWindow())
               .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin(User user)
    {
        if (!user.getPassword().equals(password))
        {
            loginViaServer();
        }
        else
        {
            dbHelper.updateLastLogin(id, password);
            Class<?> aClass;
            switch (mode)
            {
                case AppConfig.PICK_MODE:
                    if (!user.isPick())
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = PickDocActivity.class;
                    break;
                case AppConfig.PACK_MODE:
                    if (!user.isPack())
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = PackDocActivity.class;
                    break;
                case AppConfig.SHIP_MODE:
                    if (!user.isLoading())
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = ShipDocActivity.class;
                    break;
                case AppConfig.APPROVE_MODE:
                    if (!user.isApprove())
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = InternalUseDocActivity.class;
                    break;
                case AppConfig.PRODUCT_APPROVE_MODE:
                    if (!(user.isApprove() || user.isApprovePrd()))
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = ProductApproveDocActivity.class;
                    break;
                case AppConfig.INV_ATTRIBUTE_MODE:
                    aClass = InventoryInfoActivity.class;
                    break;
                case AppConfig.CONFIRM_DELIVERY_MODE:
                    if (!user.isLoading())
                    {
                        showMessageDialog(getString(R.string.warning),
                                          getString(R.string.not_allowed),
                                          android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                        return;
                    }
                    aClass = ConfirmDeliveryActivity.class;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mode);
            }
            Intent intent = new Intent(MainActivity.this, aClass);
            startActivity(intent);
        }
    }

    private void loginViaServer()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("user", "login");
            LoginRequest request = new LoginRequest();
            request.setUserId(id);
            request.setPassword(password);
            User user = getSimpleObject(url, "POST", request, User.class);
            if (user != null)
            {
                runOnUiThread(() -> {
                    user.setId(user.getId().toUpperCase());
                    loadUserInfo(user, true);
                    attemptLogin(user);
                });
            }
        }).start();
    }

    private void checkForNewVersion()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("download");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("file-name", "BMSAnbar");
            url = addRequestParameters(url, parameters);
            try
            {
                String bytes = getSimpleObject(url, "GET", null, String.class);
                if (bytes != null)
                {
                    byte[] fileBytes = android.util.Base64.decode(bytes, Base64.DEFAULT);
                    runOnUiThread(() -> {
                        showProgressDialog(false);
                        updateVersion(fileBytes);
                    });
                }
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
        }).start();
    }

    private void updateVersion(byte[] bytes)
    {
        if (bytes != null)
        {
            File file = new File(
                    Environment.getExternalStorageDirectory().getPath() + "/BMSAnbar.apk");
            if (!file.exists())
            {
                try
                {
                    boolean newFile = file.createNewFile();
                    if (!newFile)
                    {
                        showMessageDialog(getString(R.string.info),
                                          getString(R.string.error_occurred),
                                          android.R.drawable.ic_dialog_info);
                        return;
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            try (FileOutputStream stream = new FileOutputStream(file))
            {
                stream.write(bytes);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            int version = 0;
            try
            {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            }
            catch (PackageManager.NameNotFoundException e)
            {
                showMessageDialog(getString(R.string.error), e.getMessage(),
                                  android.R.drawable.ic_dialog_alert);
            }

            if (info == null)
            {
                showMessageDialog(getString(R.string.error), new String(bytes),
                                  android.R.drawable.ic_dialog_alert);
                return;
            }

            if (file.length() > 0 && info.versionCode > version)
            {
                Intent installIntent;
                Uri uri;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                {
                    installIntent = new Intent(Intent.ACTION_VIEW);
                    uri = Uri.fromFile(file);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                }
                else
                {
                    installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider",
                                                     file);
                    installIntent.setData(uri);
                    installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivity(installIntent);
            }
            else
            {
                showMessageDialog(getString(R.string.info), getString(R.string.no_new_version),
                                  android.R.drawable.ic_dialog_info);
            }
        }
        else
        {
            showMessageDialog(getString(R.string.info), getString(R.string.no_new_version),
                              android.R.drawable.ic_dialog_info);
        }
    }
}

package az.inci.bmsanbar.activity;

import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static az.inci.bmsanbar.AppConfig.APPROVE_MODE;
import static az.inci.bmsanbar.AppConfig.CONFIRM_DELIVERY_MODE;
import static az.inci.bmsanbar.AppConfig.INV_ATTRIBUTE_MODE;
import static az.inci.bmsanbar.AppConfig.PACK_MODE;
import static az.inci.bmsanbar.AppConfig.PICK_MODE;
import static az.inci.bmsanbar.AppConfig.PRODUCT_APPROVE_MODE;
import static az.inci.bmsanbar.AppConfig.PURCHASE_ORDER_MODE;
import static az.inci.bmsanbar.AppConfig.SHIP_MODE;
import static az.inci.bmsanbar.GlobalParameters.cameraScanning;
import static az.inci.bmsanbar.GlobalParameters.connectionTimeout;
import static az.inci.bmsanbar.GlobalParameters.imageUrl;
import static az.inci.bmsanbar.GlobalParameters.serviceUrl;

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

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.User;
import az.inci.bmsanbar.model.v2.LoginRequest;

public class MainActivity extends AppBaseActivity
{
    String id;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableStorageAccess();

        id = preferences.getString("last_login_id", "");
        password = preferences.getString("last_login_password", "");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        loadConfig();
    }

    private void loadConfig()
    {
        serviceUrl = preferences.getString("service_url", "http://185.129.0.46:8022");
        imageUrl = preferences.getString("image_url", "http://185.129.0.46:8025");
        connectionTimeout = Integer.parseInt(preferences.getString("connection_timeout", "5"));
        cameraScanning = preferences.getBoolean("camera_scanning", false);
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
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.update_version)
                         .setMessage(R.string.want_to_update)
                         .setNegativeButton(R.string.yes,
                                            (dialogInterface, i) -> checkForNewVersion())
                         .setPositiveButton(R.string.no, null);
            AlertDialog dialog = dialogBuilder.create();

            dialog.show();
            return true;
        });
        MenuItem itemInfo = menu.findItem(R.id.inv_attributes);
        itemInfo.setOnMenuItemClickListener(item1 -> {
            showLoginDialog(INV_ATTRIBUTE_MODE);
            return true;
        });
        MenuItem deiceInfo = menu.findItem(R.id.device_info);
        deiceInfo.setOnMenuItemClickListener(item1 -> {
            showMessageDialog(getString(R.string.device_info), getDeviceIdString(), ic_dialog_info);
            return true;
        });
        return true;
    }

    protected void enableStorageAccess()
    {
        String[] permissions;
        if(SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            permissions = new String[]{
                    READ_MEDIA_AUDIO,
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO
            };
        }
        else
            permissions = new String[]{WRITE_EXTERNAL_STORAGE};
        if(!permissionsGranted(permissions))
        {
            ActivityCompat.requestPermissions(this, permissions, 1);
            Intent intent;
            if(SDK_INT >= Build.VERSION_CODES.R)
            {
                intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private boolean permissionsGranted(String[] permissions)
    {
        for(String permission : permissions)
        {
            if(ActivityCompat.checkSelfPermission(this, permission) == PERMISSION_DENIED)
                return false;
        }

        return true;
    }

    public void openPickingDocs(View view)
    {
        showLoginDialog(PICK_MODE);
    }

    public void openPackingDocs(View view)
    {
        showLoginDialog(PACK_MODE);
    }

    public void openShippingDocs(View view)
    {
        showLoginDialog(SHIP_MODE);
    }

    public void openInvApproving(View view)
    {
        showLoginDialog(APPROVE_MODE);
    }

    public void openProductApproving(View view)
    {
        showLoginDialog(PRODUCT_APPROVE_MODE);
    }

    public void openConfirmDelivery(View view)
    {
        showLoginDialog(CONFIRM_DELIVERY_MODE);
    }

    public void openPurchaseOrders(View view)
    {
        showLoginDialog(PURCHASE_ORDER_MODE);
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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle(R.string.enter)
                     .setView(view)
                     .setPositiveButton(R.string.enter, (dialog, which) -> {
                         id = idEdit.getText().toString().toUpperCase();
                         password = passwordEdit.getText().toString();

                         if(id.isEmpty() || password.isEmpty())
                         {
                             showToastMessage(getString(R.string.username_or_password_not_entered));
                             showLoginDialog(mode);
                             playSound(SOUND_FAIL);
                         }
                         else
                         {
                             loginViaServer();

                             dialog.dismiss();
                         }
                     });

        AlertDialog loginDialog = dialogBuilder.create();
        Objects.requireNonNull(loginDialog.getWindow())
               .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin(User user)
    {
        preferences.edit().putString("last_login_id", id).apply();
        preferences.edit().putString("last_login_password", password).apply();
        Class<?> aClass;
        switch(mode)
        {
            case PICK_MODE:
                if(!user.isPickFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PickDocActivity.class;
                break;
            case PACK_MODE:
                if(!user.isPackFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PackDocActivity.class;
                break;
            case SHIP_MODE:
                if(!user.isLoadingFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ShipDocActivity.class;
                break;
            case APPROVE_MODE:
                if(!user.isApproveFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = InternalUseDocActivity.class;
                break;
            case PRODUCT_APPROVE_MODE:
                if(!(user.isApproveFlag() || user.isApprovePrdFlag()))
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ProductApproveDocActivity.class;
                break;
            case INV_ATTRIBUTE_MODE:
                aClass = InventoryInfoActivity.class;
                break;
            case CONFIRM_DELIVERY_MODE:
                if(!user.isLoadingFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ConfirmDeliveryActivity.class;
                break;
            case PURCHASE_ORDER_MODE:
                if(!user.isPurchaseOrdersFlag())
                {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PurchaseOrdersActivity.class;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
        Intent intent = new Intent(MainActivity.this, aClass);
        startActivity(intent);
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
            if(user != null)
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
                if(bytes != null)
                {
                    byte[] fileBytes = android.util.Base64.decode(bytes, Base64.DEFAULT);
                    runOnUiThread(() -> {
                        showProgressDialog(false);
                        updateVersion(fileBytes);
                    });
                }
            }
            catch(RuntimeException e)
            {
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(),
                                                      ic_dialog_alert));
            }
        }).start();
    }

    private void updateVersion(byte[] bytes)
    {
        if(bytes != null)
        {
            File file = new File(
                    Environment.getExternalStorageDirectory().getPath() + "/BMSAnbar.apk");
            if(!file.exists())
            {
                try
                {
                    boolean newFile = file.createNewFile();
                    if(!newFile)
                    {
                        showMessageDialog(getString(R.string.info),
                                          getString(R.string.error_occurred),
                                          ic_dialog_info);
                        return;
                    }
                }
                catch(IOException e)
                {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                    return;
                }
            }

            try(FileOutputStream stream = new FileOutputStream(file))
            {
                stream.write(bytes);
            }
            catch(Exception e)
            {
                showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                return;
            }

            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            int version;
            try
            {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            }
            catch(PackageManager.NameNotFoundException e)
            {
                showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                return;
            }

            if(info == null)
            {
                showMessageDialog(getString(R.string.error), new String(bytes),
                                  ic_dialog_alert);
                return;
            }

            if(file.length() > 0 && info.versionCode > version)
            {
                Intent installIntent;
                Uri uri;
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                {
                    installIntent = new Intent(Intent.ACTION_VIEW);
                    uri = Uri.fromFile(file);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                }
                else
                {
                    installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    uri = FileProvider.getUriForFile(this, "az.inci.bmsanbar.provider",
                                                     file);
                    installIntent.setData(uri);
                    installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivity(installIntent);
            }
            else
            {
                showMessageDialog(getString(R.string.info), getString(R.string.no_new_version),
                                  ic_dialog_info);
            }
        }
        else
        {
            showMessageDialog(getString(R.string.info), getString(R.string.no_new_version),
                              ic_dialog_info);
        }
    }
}

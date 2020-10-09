package az.inci.bmsanbar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsActivity extends AppBaseActivity
{

    EditText serverUrlEdit;
    EditText imageUrlEdit;
    EditText connectionTimeoutEdit;
    CheckBox cameraScannerCheck;
    Button update;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadComponents();
    }

    private void loadComponents()
    {
        serverUrlEdit = findViewById(R.id.server_url);
        imageUrlEdit = findViewById(R.id.image_url);
        connectionTimeoutEdit = findViewById(R.id.connection_timeout);
        cameraScannerCheck = findViewById(R.id.camera_scanner_check);
        update = findViewById(R.id.update);

        serverUrlEdit.setText(config().getServerUrl());
        imageUrlEdit.setText(config().getImageUrl());
        connectionTimeoutEdit.setText(String.valueOf(config().getConnectionTimeout()));
        cameraScannerCheck.setChecked(config().isCameraScanning());

        update.setOnClickListener(v -> updateParameters());
    }

    private void updateParameters()
    {
        String serverUrl = serverUrlEdit.getText().toString();
        String imageUrl = imageUrlEdit.getText().toString();
        String connectionTimeout = connectionTimeoutEdit.getText().toString();
        boolean cameraScanning = cameraScannerCheck.isChecked();

        if (!serverUrl.isEmpty())
        {
            config().setServerUrl(serverUrl);
            dbHelper.updateParameter("serverUrl", serverUrl);
        }
        if (!imageUrl.isEmpty())
        {
            config().setImageUrl(imageUrl);
            dbHelper.updateParameter("imageUrl", imageUrl);
        }

        if (!connectionTimeout.isEmpty())
        {
            config().setConnectionTimeout(Integer.parseInt(connectionTimeout));
            dbHelper.updateParameter("connectionTimeout", connectionTimeout);
        }

        config().setCameraScanning(cameraScanning);
        dbHelper.updateParameter("cameraScanning", String.valueOf(cameraScanning));

        showToastMessage("Parametrlər yeniləndi");
        loadComponents();
    }
}
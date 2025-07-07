package az.inci.bmsanbar.activity;

import static android.text.TextUtils.isEmpty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.view.KeyEvent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public abstract class ScannerSupportActivity extends AppBaseActivity {
    protected String model;
    protected boolean isContinuous = true;
    protected ScanManager scanManager;
    protected boolean busy = false;
    private final BroadcastReceiver urovoScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            byte[] barcodeArray = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int length = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            String barcode = new String(barcodeArray, 0, length);
            onScanComplete(barcode);
            busy = false;
        }
    };
    protected boolean isUrovoOpen = false;
    ActivityResultLauncher<Integer> barcodeResultLauncher = barcodeResultLauncher();

    private void initUrovoScanner() {
        try {
            busy = false;
            isUrovoOpen = true;
            scanManager = new ScanManager();
            scanManager.openScanner();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanManager.ACTION_DECODE);
            ContextCompat.registerReceiver(this, urovoScanReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        } catch (RuntimeException e) {
            logger.logError(e.getMessage());
        }
    }

    private void toggleUrovoScanner() {
        if (!isUrovoOpen) {
            initUrovoScanner();
        }

        if (!busy) {
            scanManager.startDecode();
            busy = true;
        } else {
            scanManager.stopDecode();
            busy = false;
        }
    }

    private void stopScan() {
        isUrovoOpen = false;
        try {
            if (scanManager != null && scanManager.getScannerState()) {
                scanManager.closeScanner();
                unregisterReceiver(urovoScanReceiver);
            }
        } catch (RuntimeException e) {
            logger.logError(e.getMessage());
        }
    }

    public abstract void onScanComplete(String barcode);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 520 || keyCode == 521 || keyCode == 522) {
            toggleUrovoScanner();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    ActivityResultLauncher<Integer> barcodeResultLauncher() {
        return registerForActivityResult(new ActivityResultContract<Integer, String>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Integer input) {
                Class<?> cameraClass;

                cameraClass = BarcodeScannerCamera.class;
                Intent intent = new Intent(ScannerSupportActivity.this, cameraClass);
                intent.putExtra("scanTarget", input);

                return intent;
            }

            @Override
            public String parseResult(int resultCode, @Nullable Intent intent) {
                String scanResult = "";
                if (intent != null) scanResult = intent.getStringExtra("barcode");

                return scanResult;
            }
        }, barcode -> {
            if (!isEmpty(barcode)) onScanComplete(barcode);
        });
    }
}
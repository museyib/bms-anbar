package az.inci.bmsanbar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.reflect.Field;

public class BarcodeScannerCamera extends ScannerSupportActivity {

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGenerator;
    private SurfaceHolder surfaceHolder;
    Camera camera;
    BarcodeDetector barcodeDetector;
    Button add;
    TextView goodInfo;

    String trxType;
    boolean isContinuous;
    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner_camera);
        add=findViewById(R.id.increase);
        goodInfo=findViewById(R.id.good_info);

        isContinuous =getIntent().getBooleanExtra("serialScan", false);

        if (isContinuous) {
            trxNo=getIntent().getStringExtra("trxNo");
            trxType=getIntent().getStringExtra("trxType");
            soundPool =new SoundPool(10, 3, 5);
            audioManager=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
            add.setVisibility(View.VISIBLE);
            goodInfo.setVisibility(View.VISIBLE);
            add.setEnabled(false);
        }

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        surfaceHolder=surfaceView.getHolder();

        surfaceView.setOnClickListener(v -> {
            if (setCamera(cameraSource))
                camera.autoFocus(myAutoFocusCallback);
        });

        initialiseDetectorsAndSources();
    }

    @Override
    public void onScanComplete(String barcode) {
        if (!isContinuous)
        {
            getAndClose(barcode);
        }
        else
        {
            runOnUiThread(() -> {
                add.setEnabled(true);
                add.setOnClickListener(view -> getAndContinue(barcode));
            });
        }
    }

    private boolean setCamera(CameraSource cameraSource)
    {
        Field[] declaredFields=CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields)
        {
            if (field.getType() == Camera.class)
            {
                field.setAccessible(true);

                try {
                    camera=(Camera)field.get(cameraSource);
                    if (camera !=null)
                        return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    private void initialiseDetectorsAndSources() {

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();


        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(BarcodeScannerCamera.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(BarcodeScannerCamera.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String barcode = barcodes.valueAt(0).displayValue;
                    onScanComplete(barcode);
                }
            }
        });
    }

    private void getAndClose(String barcode)
    {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
        Intent intent = new Intent();
        intent.putExtra("barcode", barcode);
        setResult(1, intent);
        finish();
    }

    private void getAndContinue(String barcode)
    {
        Trx trx=dbHelper.getPickTrxByBarcode(barcode, trxNo);
        if (trx!=null) {
            switch (trxType) {
                case "pick":
                    if (trx.getPickedQty() < trx.getQty()) {
                        trx.setPickedQty(trx.getPickedQty() + 1);
                        dbHelper.updatePickTrx(trx);
                        playSound(R.raw.barcodebeep);
                    } else {
                        showMessageDialog(getString(R.string.info),
                                getString(R.string.already_picked),
                                android.R.drawable.ic_dialog_info);
                        playSound(R.raw.serror2);
                    }
                    goodInfo.setText(trx.getInvName()+": "+trx.getPickedQty());
                    break;
                case "pack":
                    if (trx.getPackedQty() < trx.getPickedQty()) {
                        trx.setPackedQty(trx.getPackedQty() + 1);
                        dbHelper.updatePackTrx(trx);
                        playSound(R.raw.barcodebeep);
                    } else {
                        showMessageDialog(getString(R.string.info),
                                getString(R.string.already_packed),
                                android.R.drawable.ic_dialog_info);
                        playSound(R.raw.serror2);
                    }
                    goodInfo.setText(trx.getInvName()+": "+trx.getPackedQty());
                    break;
            }
        }
        else {
            Toast.makeText(this, R.string.good_not_found, Toast.LENGTH_LONG).show();
            playSound(R.raw.serror2);
        }
        add.setEnabled(false);
    }

    @Override
    public void onBackPressed() {
        setResult(-1, new Intent());
        finish();
    }

    Camera.AutoFocusCallback myAutoFocusCallback = (arg0, arg1) -> {

    };
}
package com.sge.sqr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String SOAP_ACTION = "http://SQR.IT.Contracts.Service/IIdentifyCards/ReadCard";
    private static final String METHOD_NAME = "ReadCard";
    private static final String NAMESPACE = "http://SQR.IT.Contracts.Service";
    private static String URL = "";
    private static boolean CAN_READ = true;
    private static boolean DENIED_ACCESS = false;
    private static final String DENIED_CODE = "00000000-0000-0000-0000-000000000000";

    private CoordinatorLayout layout;
    private SurfaceView cameraView;
    private TextView barcodeInfo;
    private CameraSource cameraSource;
    private BarcodeDetector barcodeDetector;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        CAN_READ = true;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        URL = sharedPref.getString("webservice_preference", getString(R.string.default_webservice));
        layout = (CoordinatorLayout) findViewById(R.id.layout);
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeInfo = (TextView) findViewById(R.id.code_info);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    barcodeInfo.post(new Runnable() {
                        public void run() {
                            barcodeInfo.setText(R.string.escanea_el_codigo);
                        }
                    });
                    CAN_READ = true;
                    DENIED_ACCESS = false;
                } catch (Exception ex) {
                    Snackbar.make(layout, ex.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
        });

        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        if (!barcodeDetector.isOperational()) {
            Snackbar.make(layout, R.string.cannot_config_detector, Snackbar.LENGTH_LONG).show();
            return;
        }

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                //.setRequestedPreviewSize(1600, 1024)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Snackbar.make(layout, ie.getMessage(), Snackbar.LENGTH_LONG).show();
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

                if (CAN_READ && barcodes.size() != 0) {
                    AsyncCallWS task = new AsyncCallWS();
                    task.execute(!DENIED_ACCESS ? barcodes.valueAt(0).displayValue : DENIED_CODE);
                    CAN_READ = false;
                    Snackbar.make(layout, R.string.card_was_read, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                break;
            case R.id.denied_access:
                DENIED_ACCESS = true;
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        URL = sharedPref.getString("webservice_preference", getString(R.string.default_webservice));
    }

    private class AsyncCallWS extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String webResponse = "";

            if (params.length == 0)
                return null;

            try {
                SoapObject Request = new SoapObject(NAMESPACE, METHOD_NAME);
                Request.addProperty("code", params[0]);

                SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                soapEnvelope.dotNet = true;
                soapEnvelope.setOutputSoapObject(Request);

                HttpTransportSE transport = new HttpTransportSE(URL);

                transport.call(SOAP_ACTION, soapEnvelope);
                SoapPrimitive response = (SoapPrimitive) soapEnvelope.getResponse();
                webResponse = response.toString();

            } catch (Exception ex) {
                Snackbar.make(layout, getString(R.string.cannot_access_web_service) + ex.toString(), Snackbar.LENGTH_LONG).show();
            }

            return webResponse;
        }

        @Override
        protected void onPostExecute(final String s) {
            barcodeInfo.post(new Runnable() {
                public void run() {
                    barcodeInfo.setText(s);
                }
            });
        }
    }
}

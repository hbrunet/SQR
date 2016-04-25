package com.sge.sqr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final String SOAP_ACTION = "http://www.w3schools.com/xml/CelsiusToFahrenheit";
    private static final String METHOD_NAME = "CelsiusToFahrenheit";
    private static final String NAMESPACE = "http://www.w3schools.com/xml/";
    private static String URL = "http://www.w3schools.com/xml/tempconvert.asmx?op=CelsiusToFahrenheit";

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

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        URL = sharedPref.getString("webservice_preference", URL);

        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeInfo = (TextView) findViewById(R.id.code_info);

        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        if (!barcodeDetector.isOperational()) {
            Toast.makeText(getApplicationContext(), R.string.cannot_config_detector, Toast.LENGTH_SHORT).show();
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
                    Log.e("CAMERA SOURCE", ie.getMessage());
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
                    AsyncCallWS task = new AsyncCallWS();
                    //task.execute(barcodes.valueAt(0).displayValue);
                    task.execute("30");

                    /*barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            barcodeInfo.setText(    // Update the TextView
                                    barcodes.valueAt(0).displayValue
                            );
                        }
                    });*/
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
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeInfo.setText(R.string.escanea_el_codigo);
        URL = sharedPref.getString("webservice_preference", "");
    }

    private class AsyncCallWS extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String webResponse = "";

            if (params.length == 0)
                return null;

            try {
                SoapObject Request = new SoapObject(NAMESPACE, METHOD_NAME);
                Request.addProperty("Celsius", params[0]);

                SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                soapEnvelope.dotNet = true;
                soapEnvelope.setOutputSoapObject(Request);

                HttpTransportSE transport = new HttpTransportSE(URL);

                transport.call(SOAP_ACTION, soapEnvelope);
                SoapPrimitive response = (SoapPrimitive) soapEnvelope.getResponse();
                webResponse = response.toString();

            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(), getString(R.string.cannot_access_web_service) + ex.toString(), Toast.LENGTH_LONG).show();
            }

            return webResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            barcodeInfo.setText(s);
        }
    }

}

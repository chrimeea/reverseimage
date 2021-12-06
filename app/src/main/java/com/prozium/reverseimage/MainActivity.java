package com.prozium.reverseimage;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: count scenes / ranking ladder
public class MainActivity extends Activity {

    final static Pattern p = Pattern.compile(":\\&nbsp\\;\\<br\\>\\<a[^\\>]+\\>([^\\<]+)\\<\\/a\\>");
    final DecimalFormat df = new DecimalFormat("#.#");

    Camera mCamera;
    TextView mTextView;
    byte[] lastData;
    long upload, download, timestamp;
    int queries;
    String lastText = "";

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            synchronized (mCamera) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
    }

    boolean isSameImage(byte[] data1, byte[] data2) {
        //return Arrays.equals(data1, data2);
        if (data1 == null || data2 == null || data1.length != data2.length) {
            return false;
        }
        double d = 0.0;
        for (int i = 0; i < data1.length; i++) {
            d += Math.abs(data1[i] - data2[i]);
        }
        return d / data1.length < 45.0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera = Camera.open();
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                final Camera.PreviewCallback me = this;
                new AsyncTask<Void, Void, String>() {

                    @Override
                    protected String doInBackground(final Void... params) {
                        if (isSameImage(lastData, data)) {
                            return lastText;
                        }
                        lastData = data;
                        byte[] d = data;
                        Camera.Parameters parameters;
                        synchronized (camera) {
                            if (mCamera != null) {
                                parameters = camera.getParameters();
                            } else {
                                return null;
                            }
                        }
                        if (parameters.getPreviewFormat() == ImageFormat.NV21
                                || parameters.getPreviewFormat() == ImageFormat.YUY2) {
                            final Camera.Size s = parameters.getPreviewSize();
                            final YuvImage img = new YuvImage(data, parameters.getPreviewFormat(), s.width, s.height, null);
                            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                            img.compressToJpeg(new Rect(0, 0, s.width, s.height), 90, outStream);
                            d = outStream.toByteArray();
                        } else if (parameters.getPreviewFormat() != ImageFormat.JPEG) {
                            return null;
                        }
                        if (System.currentTimeMillis() - timestamp < 30000) {
                            queries++;
                            if (queries > 10) {
                                try {
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {}
                                timestamp = System.currentTimeMillis();
                                queries = 0;
                                return lastText;
                            }
                        } else {
                            timestamp = System.currentTimeMillis();
                            queries = 0;
                        }
                        try {
                            final MultipartUtility multipart = new MultipartUtility("https://www.google.com/searchbyimage/upload");
                            multipart.addFilePart("encoded_image", "picture.jpg", d);
                            final String s = multipart.finish(p);
                            upload += multipart.upload;
                            download += multipart.download;
                            return s;
                        } catch (IOException e) {
                            Log.d("", "", e);
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(final String s) {
                        if (s != null) {
                            mTextView.setText(s + "     [Net U:" + df.format(upload / 1000000.0) + "MB / D:" + df.format(download / 1000000.0) + "MB]");
                            lastText = s;
                            synchronized (camera) {
                                if (mCamera != null) {
                                    camera.setOneShotPreviewCallback(me);
                                }
                            }
                        }
                    }
                }.execute(null, null);
            }
        });
        final Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size bestSize = null;
        for (Camera.Size s: parameters.getSupportedPreviewSizes()) {
            if (bestSize == null || s.width * s.height < bestSize.width * bestSize.height) {
                bestSize = s;
            }
        }
        parameters.setPreviewSize(bestSize.width, bestSize.height);
        mCamera.setParameters(parameters);
        ((FrameLayout) findViewById(R.id.camera_preview)).addView(new CameraPreview(this, mCamera));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text);
        //MobileAds.initialize(getApplicationContext(), getResources().getString(R.string.ad_app_id));
        //((AdView) v.findViewById(R.id.adview)).loadAd(new AdRequest.Builder().build());
    }
}

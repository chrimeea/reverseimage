package com.prozium.reverseimage;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import java.io.IOException;

/**
 * Created by cristian on 03.08.2016.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    Camera mCamera;
    final ScaleGestureDetector pinch;

    public CameraPreview(final Context context, final Camera camera) {
        super(context);
        mCamera = camera;
        pinch = new ScaleGestureDetector(context, new PinchListener());
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int w, final int h) {
        final Camera.Size s = mCamera.getParameters().getPreviewSize();
        final double ratio = Math.min((double) w / s.width, (double) h / s.height);
        (((Activity) getContext()).findViewById(R.id.camera_preview)).setLayoutParams(
                new RelativeLayout.LayoutParams((int) (s.width * ratio), (int) (s.height * ratio)));
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        pinch.onTouchEvent(event);
        return true;
    }

    class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public void onScaleEnd(final ScaleGestureDetector detector) {
            final Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                int z = parameters.getZoom() + (detector.getScaleFactor() > 1 ? 1 : -1);
                z = Math.min(z, parameters.getMaxZoom());
                z = Math.max(z, 0);
                parameters.setZoom(z);
                mCamera.setParameters(parameters);
            }
        }
    }
}
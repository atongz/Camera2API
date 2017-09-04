package test.com.camera2api;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.LogRecord;


public class CameraIntentActivity extends AppCompatActivity {

    private static final int REQUEST_ID_READ_PERMISSION = 100;
    private static final int REQUEST_ID_WRITE_PERMISSION = 200;

    private final static int ACTIVITY_START_CAMERA_APP = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mState;
    private String mImageFileLocation = "";
    private String GALLERY_LOCATION = "image gallery";
    private File mGalleryFolder;
    private static LruCache<String, Bitmap> mMemoryCache;
    private RecyclerView mRecyclerView;
    private TextureView mTextureView;
    private Size mPreviewSize;
    private String mCameraId;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //Toast.makeText(getApplicationContext(), "Camera Opened", Toast.LENGTH_SHORT).show();
            createCameraSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mCaptureSessionCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result)
        {
            switch (mState)
            {
                case STATE_PREVIEW:
                    //Do nothing
                    break;
                case STATE_WAIT_LOCK:
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    {
                        unlockFocus();
                        Toast.makeText(getApplicationContext(), "Focus Lock Successful", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Toast.makeText(getApplicationContext(), "Focus Lock Unseccessful", Toast.LENGTH_SHORT).show();
        }
    };
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_intent);

        ////////////////////////////////////////////////////////////////////////////////////////
        // Permisson Request
        askPermission(REQUEST_ID_WRITE_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        askPermission(REQUEST_ID_WRITE_PERMISSION, Manifest.permission.CAMERA);
        ///////////////////////////////////////////////////////////////////////////////////////

        createImageGallery();

        Button photoButton = (Button) findViewById(R.id.photoButton);

        mRecyclerView = (RecyclerView)findViewById(R.id.galleryRecyclerView);
        //RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter imageAdapter = new ImageAdapter(sortFilesToLatest(mGalleryFolder));
        mRecyclerView.setAdapter(imageAdapter);

        final int maxMemorySize = (int) Runtime.getRuntime().maxMemory() / 1024;
        final int cacheSize = maxMemorySize / 10;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf (String key, Bitmap value)
            {
                return value.getByteCount() / 1024;
            }
        };

        mTextureView = (TextureView)findViewById(R.id.textureView);

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                //Toast.makeText(getApplicationContext(), "Button Pressed", Toast.LENGTH_SHORT).show();
                Intent callCameraApplicationIntent = new Intent();

                File photoFIle = null;
                try
                {
                    photoFIle = createImageFile();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFIle));
                startActivityForResult(callCameraApplicationIntent, ACTIVITY_START_CAMERA_APP);
                */
                lockFocus();
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        openBackgroundThread();
        if (mTextureView.isAvailable())
        {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            openCamera();
        }
        else
        {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }
    @Override
    public void onPause()
    {
        closeCamera();
        closeBackgroundThread();
        super.onPause();
    }

    public void takePhoto()
    {

    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK)
        {
            RecyclerView.Adapter newImageAdadpter = new ImageAdapter(sortFilesToLatest(mGalleryFolder));
            mRecyclerView.swapAdapter(newImageAdadpter, false);
        }
        else
        {
            File file = new File(mImageFileLocation);
            file.delete();
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private void createImageGallery()
    {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mGalleryFolder = new File(storageDirectory, GALLERY_LOCATION);

        if (!mGalleryFolder.exists())
        {
            mGalleryFolder.mkdirs();
        }
    }

    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "_";

        File image = File.createTempFile(imageFileName, ".jpg", mGalleryFolder);
        mImageFileLocation = image.getAbsolutePath();

        return image;
    }

    private File[] sortFilesToLatest (File fileImagesDIr)
    {
        File[] files = fileImagesDIr.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.valueOf((o2.lastModified())).compareTo(o1.lastModified());
            }
        });
        return files;
    }

    public static Bitmap getBitmapFromMemoryCache (String key)
    {
        return mMemoryCache.get(key);
    }
    public static void setBitmapToMemoryCache (String key, Bitmap bitmap)
    {
        if (getBitmapFromMemoryCache(key) == null)
        {
            mMemoryCache.put(key, bitmap);
        }
    }
    private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try
        {
            for (String cameraId : cameraManager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getPrefferedPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
                return;
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
    private Size getPrefferedPreviewSize(Size[] mapSizes, int width, int height)
    {
        List<Size> collectrSize = new ArrayList<>();
        for (Size option : mapSizes)
        {
            if (width > height)
            {
                if (option.getWidth() > width && option.getHeight() > height)
                {
                    collectrSize.add(option);
                }
            }
            else
            {
                if (option.getWidth() > height && option.getHeight() > width)
                {
                    collectrSize.add(option);
                }
            }
        }
        if (collectrSize.size() > 0)
        {
            return Collections.min(collectrSize, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return mapSizes[0];
    }
    private void openCamera()
    {
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            cameraManager.openCamera(mCameraId, mCameraDeviceCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
    private void closeCamera()
    {
        if (mCameraCaptureSession != null)
        {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    private void createCameraSession()
    {
        try
        {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null)
                    {
                        return;
                    }
                    try
                    {
                        mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mPreviewCaptureRequest, mCaptureSessionCallback, mBackgroundHandler);
                    }
                    catch (CameraAccessException e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Create camera session failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread()
    {
        mBackgroundThread = new HandlerThread("Camera2 background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void closeBackgroundThread()
    {
        mBackgroundThread.quitSafely();
        try
        {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    private void lockFocus()
    {
        try
        {
            mState = STATE_WAIT_LOCK;
            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.capture(mPreviewCaptureRequestBuilder.build(), mCaptureSessionCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }
    private void unlockFocus()
    {
        try
        {
            mState = STATE_PREVIEW;
            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            mCameraCaptureSession.capture(mPreviewCaptureRequestBuilder.build(), mCaptureSessionCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////
    // Permisson Request begin
    private boolean askPermission(int requestId, String permissionName) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            // Check if we have permission
            int permission = ActivityCompat.checkSelfPermission(this, permissionName);


            if (permission != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{permissionName},
                        requestId
                );
                return false;
            }
        }
        return true;
    }
    // When you have the request results
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        // Note: If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
            switch (requestCode) {
                case REQUEST_ID_READ_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //readFile();
                    }
                }
                case REQUEST_ID_WRITE_PERMISSION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //writeFile();
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Permission Cancelled!", Toast.LENGTH_SHORT).show();
        }
    }
    // Permission Request end
    ///////////////////////////////////////////////////
}
package test.com.camera2api;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;


public class CameraIntentActivity extends AppCompatActivity {

    private static final int REQUEST_ID_READ_PERMISSION = 100;
    private static final int REQUEST_ID_WRITE_PERMISSION = 200;

    private final static int ACTIVITY_START_CAMERA_APP = 0;
    private String mImageFileLocation = "";
    private String GALLERY_LOCATION = "image gallery";
    private File mGalleryFolder;
    private static LruCache<String, Bitmap> mMemoryCache;
    private RecyclerView mRecyclerView;

    @Override
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
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
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

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
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
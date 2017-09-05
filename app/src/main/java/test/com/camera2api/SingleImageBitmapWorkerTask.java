package test.com.camera2api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;


/**
 * Created by admin on 2017-08-14.
 */

public class SingleImageBitmapWorkerTask extends AsyncTask<File, Void, Bitmap> {

    WeakReference<ImageView> imageViewWeakReference;
    final int TARGET_IMAGE_VIEW_WIDTH;
    final int TARGET_IMAGE_VIEW_HEIGHT;
    private File mImageFile;
    private String filePath;

    public SingleImageBitmapWorkerTask(ImageView imageView, int width, int height)
    {
        imageViewWeakReference = new WeakReference<ImageView>(imageView);
        TARGET_IMAGE_VIEW_WIDTH = width;
        TARGET_IMAGE_VIEW_HEIGHT = height;
    }

    @Override
    protected Bitmap doInBackground(File... params) {
        //filePath = params[0].getAbsolutePath();
        //return BitmapFactory.decodeFile(params[0].getAbsolutePath());
        mImageFile = params[0];
        //return decodeBitmapFromFile(params[0]);
        Bitmap bitmap = decodeBitmapFromFile(mImageFile);
        //CameraIntentActivity.setBitmapToMemoryCache(mImageFile.getName(), bitmap);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap)
    {
        /*
        if (bitmap != null && imageViewWeakReference != null)
        {
            ImageView viewImage = imageViewWeakReference.get();
            if (viewImage != null)
            {
                //rotateImage(setReducedImageSize (), viewImage);
                rotateImage(bitmap, viewImage);
                //viewImage.setImageBitmap(bitmap);
            }
        }
        */
        if (bitmap != null && imageViewWeakReference != null)
        {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView != null)
            {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options bmOptions)
    {
        final int photoWidth = bmOptions.outWidth;
        final int photoHeight = bmOptions.outHeight;
        int scaleFactor = 1;

        if (photoWidth > TARGET_IMAGE_VIEW_WIDTH || photoHeight > TARGET_IMAGE_VIEW_HEIGHT)
        {
            final int halfPhotoWidth = photoWidth / 2;
            final int halfPhotoHeight = photoHeight / 2;
            while (halfPhotoWidth / scaleFactor > TARGET_IMAGE_VIEW_WIDTH || halfPhotoHeight / scaleFactor > TARGET_IMAGE_VIEW_HEIGHT)
            {
                scaleFactor *= 2;
            }
        }
        return scaleFactor;
    }

    private Bitmap decodeBitmapFromFile (File imageFile)
    {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
        bmOptions.inSampleSize = calculateInSampleSize(bmOptions);
        bmOptions.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bmOptions);
    }

    public File getImageFile()
    {
        return mImageFile;
    }

    private void rotateImage(Bitmap bitmap, ImageView imageView)
    {
        ExifInterface exifInterface = null;
        try
        {
            exifInterface = new ExifInterface(filePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation)
        {
            case  ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            default:
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        imageView.setImageBitmap(rotatedBitmap);
    }

    private Bitmap setReducedImageSize ()
    {
        //int targetImageViewWIdth = imageView.getWidth();
        //int targetImageViewHeight = imageView.getHeight();
        int targetImageViewWIdth = 450;
        int targetImageViewHeight = 800;

        BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
        bmpOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bmpOptions);

        int cameraImageWidth = bmpOptions.outWidth;
        int cameraImageHeight = bmpOptions.outHeight;

        int scaleFactor = Math.min(cameraImageWidth/targetImageViewWIdth, cameraImageHeight/targetImageViewHeight);

        bmpOptions.inSampleSize = scaleFactor;
        bmpOptions.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, bmpOptions);
    }
}

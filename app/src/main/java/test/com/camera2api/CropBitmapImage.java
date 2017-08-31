package test.com.camera2api;

import android.graphics.Bitmap;

/**
 * Created by admin on 2017-08-16.
 */

public class CropBitmapImage {
    public Bitmap cropImage(Bitmap bitmap, int w, int h)
    {
        if (bitmap == null)
        {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if ( width < w && height < h)
        {
            return bitmap;
        }
        int x = 0;
        int y = 0;
        if (width > w)
            x = (width - w) / 2;
        if (height > h)
            y = (height - h) / 2;

        int cw = w;
        int ch = h;

        if (w > width)
            cw  = width;
        if (h > height)
            ch = height;
        return Bitmap.createBitmap(bitmap, x, y, cw, ch);
    }
}

package test.com.camera2api;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by admin on 2017-08-11.
 */

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private Bitmap placeHolderBitmap;
    private File imagesFile;
    private File[] imageFiles;
    private static RecyclerViewClickPositionInterface mPositionInterface;

    public ImageAdapter(File[] folderFiles, RecyclerViewClickPositionInterface positionInterface)
    {
        imageFiles = folderFiles;
        mPositionInterface = positionInterface;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_images_relative_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        File imageFile = imageFiles[position];
        final String name = imageFile.getName();
        Bitmap bitmap = CameraIntentActivity.getBitmapFromMemoryCache(imageFile.getName());
        if (bitmap != null)
        {
            holder.getImageView().setImageBitmap(new CropBitmapImage().cropImage(bitmap, BitmapWorkerTask.TARGET_IMAGE_VIEW_WIDTH, BitmapWorkerTask.TARGET_IMAGE_VIEW_HEIGHT));
        }
        else if (checkBitmapWorkerTask(imageFile, holder.getImageView()))
        {
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(holder.getImageView());
            AsyncDrawble asyncDrawble = new AsyncDrawble(holder.getImageView().getResources(), placeHolderBitmap, bitmapWorkerTask);
            holder.getImageView().setImageDrawable(asyncDrawble);
            bitmapWorkerTask.execute(imageFile);
        }

        /*
        // Add onclicklistener on each imageView
        holder.getImageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(v.getContext(), name, Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    @Override
    public int getItemCount() {
        return imageFiles.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView imageview;

        public ViewHolder (View view)
        {
            super(view);

            view.setOnClickListener(this);
            imageview = (ImageView)view.findViewById(R.id.imageGalleryView);
        }

        public ImageView getImageView()
        {
            return imageview;
        }

        @Override
        public void onClick(View v) {
            mPositionInterface.getRecyclerViewAdapterPosition(this.getPosition());
        }
    }

    public static class AsyncDrawble extends BitmapDrawable
    {
        final WeakReference<BitmapWorkerTask> taskReference;
        public AsyncDrawble (Resources resources, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask)
        {
            super(resources, bitmap);
            taskReference = new WeakReference(bitmapWorkerTask);
        }
        public BitmapWorkerTask getBitmapWorkerTask ()
        {
            return taskReference.get();
        }
    }
    public static boolean checkBitmapWorkerTask (File imagesFile, ImageView imageView)
    {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null)
        {
            final File workerFIle = bitmapWorkerTask.getImageFile();
            if (workerFIle != null)
            {
                bitmapWorkerTask.cancel(true);
            }
            else
            {
                return false;
            }
        }
        return true;
    }
    public static BitmapWorkerTask getBitmapWorkerTask (ImageView imageView)
    {
        if (imageView == null)
            return null;
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof AsyncDrawble)
        {
            AsyncDrawble asyncDrawble = (AsyncDrawble) drawable;
            return asyncDrawble.getBitmapWorkerTask();
        }
        return null;
    }
}

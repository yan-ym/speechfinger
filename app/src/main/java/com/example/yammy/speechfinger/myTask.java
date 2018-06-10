package com.example.yammy.speechfinger;

import android.graphics.Bitmap;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class myTask extends AsyncTask {
    private MediaProjection mediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mediaProjectionManager;
    private ImageReader mImageReader;
    private String filePath;
    public Bitmap bitmap;
    private isLoaded isLoaded;

    public interface isLoaded {
        void loadComplete();
    }
    public void setIsLoaded(isLoaded Loaded){
        this.isLoaded=Loaded;
    }
    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Object doInBackground(Object[] objects) {
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_4444);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        image.close();
        if (bitmap != null)
        {
            try {
                // 图片文件路径
                filePath =  "/mnt/sdcard/screenshot4.png";
                Log.d("path1", filePath);
                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }



    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if(isLoaded!=null){
            isLoaded.loadComplete();
        }
    }


    public void  setmImageReader(ImageReader imageReader){
        this.mImageReader=imageReader;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
    }

    public void setmVirtualDisplay(VirtualDisplay mVirtualDisplay) {
        this.mVirtualDisplay = mVirtualDisplay;
    }
}


package com.example.yammy.speechfinger;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.Word;
import com.baidu.ocr.sdk.model.WordSimple;
import com.baidu.speech.asr.SpeechConstant;
import com.example.yammy.speechfinger.recog.IRecogListener;
import com.example.yammy.speechfinger.recog.MyRecognizer;
import com.example.yammy.speechfinger.recog.SimpleRecogListener;
import com.example.yammy.speechfinger.wakeup.IWakeupListener;
import com.example.yammy.speechfinger.wakeup.MyWakeup;
import com.example.yammy.speechfinger.wakeup.SimpleWakeupListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActivityWakeUp extends Activity implements IStatus {

    protected Button btn;
    private Handler handler;

    private static final String TAG = "ActivityWakeUp";
    protected MyWakeup myWakeup;
    protected MyRecognizer myRecognizer;
    private int status = STATUS_NONE;
    private int backTrackInMs = 0;//唤醒词说完后，中间有停顿，然后接句子。

    private ImageView imageView;
    private MediaProjection mediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mediaProjectionManager;
    private static final int REQUESTRESULT = 0x100;
    private int mWidth;
    private int mHeight;
    private int mScreenDensity;
    private ImageReader mImageReader;
    private myTask task;
    private volatile boolean isLoaded;
    private Notification.Builder builder;
    private NotificationManager notificationManager;
    private static MediaPlayer player;

    private String filePath;


    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化界面，设置监听事件等等。
        initView();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //身份验证与安全
                OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
                    @Override
                    public void onResult(AccessToken result) {
                        // 调用成功，返回AccessToken对象
                        String token = result.getAccessToken();
                        Log.d("token",token);
                    }
                    @Override
                    public void onError(OCRError error) {
                        // 调用失败，返回OCRError子类SDKError对象
                        Log.d("token","defeat");
                        error.printStackTrace();
                    }
                }, getApplicationContext());
                initData();
                initWakeup();
            }
        }).start();

        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        initPermission();
    }
    protected void handleMsg(Message msg) {
        if (msg.what == STATUS_WAKEUP_SUCCESS) {
            screenshot5();
            wakeUpAndUnlock();
            // 此处 开始正常识别流程
            if(myRecognizer==null){
                IRecogListener recogListener = new SimpleRecogListener(handler);
                // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
                myRecognizer = new MyRecognizer(this, recogListener);
            }
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
            // 如识别短句，不需要需要逗号，使用1536搜索模型。其它PID参数请看文档
            params.put(SpeechConstant.PID, 1536);
            if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);

            }
            myRecognizer.cancel();
            myRecognizer.start(params);
        }else if(msg.what == START_OCR){
            filePath = "/mnt/sdcard/screenshot4.png";
            final String reconresult=(String)msg.obj;
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager WM = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
            Display display = WM.getDefaultDisplay();
            display.getMetrics(metrics);
            int height = metrics.heightPixels; // 屏幕高
            int width = metrics.widthPixels; // 屏幕的宽
            Log.d("h", height+""+width);
            if(reconresult.equals("主界面")){
                execShellCmd("input keyevent 3");
            }else if(reconresult.equals("返回")){
                execShellCmd("input keyevent 4");
            }else if(reconresult.equals("向左滑")){
                execShellCmd( String.format("input swipe %d %d %d %d",width-100,height/2,100,height/2));
            }else if(reconresult.equals("向右滑")){
                execShellCmd( String.format("input swipe %d %d %d %d",0,height/2,width,height/2));
            }else if(reconresult.equals("向上滑")){
                execShellCmd( String.format("input swipe %d %d %d %d",width/2,height/10*9,width/2,100));
            }else if (reconresult.equals("向下滑")){
                execShellCmd( String.format("input swipe %d %d %d %d",width/2,0,width/2,height));
            }else if (reconresult.equals("唤醒")){
              playRing();
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long stt=System.nanoTime();
                        Log.d("stt",stt/100000000+"");
                        while (true){
                            if (isLoaded){
                                long con1=System.nanoTime()-stt;
                                Log.d("whiletime",con1/100000000+"");
                                getWordAndLocation(filePath,reconresult);
                                long con2=System.nanoTime()-stt;
                                Log.d("whiletime",con2/100000000+"");
                                break;
                            }
                           // Log.d("isloaded",isLoaded+"");
                        }
                    }
                }).start();
            }
        }
    }
    private void execShellCmd(String cmd) {

        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    private void getWordAndLocation(String filePath, final String reconresult){
        // 通用文字识别参数设置
        GeneralParams param = new GeneralParams();
        param.setDetectDirection(true);

        param.setImageFile(new File(filePath));
        //定位单字符位置
        //param.setRecognizeGranularity("small");

        Log.d("path",filePath);
        // 调用通用文字识别服务（含位置信息版）
        OCR.getInstance().recognizeGeneral(param,new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) { // 调用成功，返回GeneralResult对象，通过getJsonRes方法获取API返回字符串
                for (WordSimple wordSimple : result.getWordList()) {
                    Word word = (Word) wordSimple; // word包含位置
                    //判断一个文字识别结果中包含语音识别结果字符串
                    if(word.getWords().indexOf(reconresult)!=-1){
                        Log.d("word",word.getLocation().getTop()+" "+word.getLocation().getLeft()+" "+word.getLocation().getHeight()+" "+word.getLocation().getWidth()+" ");
                        //产生点击事件
                        if(word.getLocation().getHeight()!=-1&&word.getLocation().getWidth()!=-1&&word.getLocation().getLeft()!=-1&&word.getLocation().getTop()!=-1){
                            float x=word.getLocation().getLeft()+word.getLocation().getWidth()/2;
                            float y=word.getLocation().getTop()-word.getLocation().getHeight()/2;
                            execShellCmd( String.format("input tap %.2f %.2f",x,y));
                        }
                        break;
                    }
                }
            }
            @Override
            public void onError(OCRError error) {
                // 调用失败，返回OCRError对象
                Log.d("res","defeat");
                error.printStackTrace();
            }
        });
    }
    private void screenshot5(){
        isLoaded=false;
        task = new myTask();
        task.setmImageReader(mImageReader);
        task.setMediaProjection(mediaProjection);
        task.setmVirtualDisplay(mVirtualDisplay);
        task.setMediaProjectionManager(mediaProjectionManager);
        final long st=System.nanoTime();
        Log.d("st",st/100000000+"");
        task.setIsLoaded(new myTask.isLoaded() {
            @Override
            public void loadComplete() {
                    long co = System.nanoTime() - st;
                    Log.d("time", co / 1000000 + "");
                    isLoaded = true;
            }
        });
        task.execute();
    }
    private void screenShot4(){
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        image.close();
        if (bitmap != null)
        {
            try {
                // 图片文件路径
                filePath =  "/mnt/sdcard/screenshot4.png";
                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);//quality为压缩率，100不压缩
                os.flush();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private String screenShot()
    {   // 获取屏幕(主视图)
        View dView = getWindow().getDecorView();
        //允许生成对当前view的一个bitmap形式的复制
        dView.setDrawingCacheEnabled(true);
        //生成对当前view的一个bitmap形式的复制
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        //保存路径
        String filePath=null;
        if (bmp != null)
        {
            try {
                // 获取内置SD卡路径
                String sdCardPath = Environment.getExternalStorageDirectory().getPath();
                // 图片文件路径
                filePath = sdCardPath + File.separator + "screenshot.jpeg";
                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                //将位图通过输出流写到SD卡
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//返回文件路径
        return filePath;
    }
    private String screenShot3()
    {   // 获取屏幕
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager WM = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        Display display = WM.getDefaultDisplay();
        display.getMetrics(metrics);
        int height = metrics.heightPixels; // 屏幕高
        int width = metrics.widthPixels; // 屏幕的宽
        // 获取显示方式
        int pixelformat = display.getPixelFormat();
        PixelFormat localPixelFormat1 = new PixelFormat();
        PixelFormat.getPixelFormatInfo(pixelformat, localPixelFormat1);
        int deepth = localPixelFormat1.bytesPerPixel;// 位深
        byte[] piex = new byte[height * width * deepth];
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // 获取fb0数据输入流
            InputStream stream = new FileInputStream(new File(
                    "/dev/graphics/fb0"));
            DataInputStream dStream = new DataInputStream(stream);
            dStream.readFully(piex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 保存图片
        int[] colors = new int[height * width];
        for (int m = 0; m < colors.length; m++) {
            int r = (piex[m * 4] & 0xFF);
            int g = (piex[m * 4 + 1] & 0xFF);
            int b = (piex[m * 4 + 2] & 0xFF);
            int a = (piex[m * 4 + 3] & 0xFF);
            colors[m] = (a << 24) + (r << 16) + (g << 8) + b;

        }
        String filePath3=null;
        Bitmap bmp = Bitmap.createBitmap(colors, width, height,
                Bitmap.Config.ARGB_8888);
        if (bmp != null)
        {
            try {
                // 获取内置SD卡路径
                String sdCardPath = Environment.getExternalStorageDirectory().getPath();
                // 图片文件路径
                filePath3 = sdCardPath + File.separator + "screenshot.jpeg";
                File file = new File(filePath3);
                FileOutputStream os = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return filePath3;
    }
    private String screenShot2()
    {
        String mSavedPath =  "/mnt/sdcard/screenshot.png";
        execShellCmd("screencap -p /mnt/sdcard/screenshot.png" );
        return mSavedPath;
    }
    public static void wakeUpAndUnlock() {
        // 获取电源管理器对象
        PowerManager powerManager = (PowerManager) MyApplication.getContext().getSystemService(Context.POWER_SERVICE);
        boolean screenOn = powerManager.isScreenOn();
        if (!screenOn) {
            PowerManager.WakeLock wl = powerManager.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "showup");
            wl.acquire(); // 获取常亮锁
            wl.release(); // 释放
        }
        // 屏幕解锁
        KeyguardManager keyguardManager = (KeyguardManager) MyApplication.getContext().getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("showup");
        // 屏幕锁定
        keyguardLock.reenableKeyguard();
        keyguardLock.disableKeyguard(); // 解锁
    }
    private void playRing(){
        player=new MediaPlayer();
        try{
            player.setLooping(true);
            player.setDataSource(this,RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_NOTIFICATION));
            player.prepare();
            player.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static class closeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopRing();
        }
        public  void stopRing(){
            if(player==null){
                return;
            }
            player.stop();
            player.release();
            player=null;
        }
    }
    protected void initWakeup() {
        IWakeupListener listener = new SimpleWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);
    }

    private void initData() {
        Display display = getWindowManager().getDefaultDisplay();
        mWidth = display.getWidth();
        mHeight = display.getHeight();
        DisplayMetrics outMetric = new DisplayMetrics();
        display.getMetrics(outMetric);
        mScreenDensity = (int) outMetric.density;

        if(mediaProjection==null){
            mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        }
        Intent intent = new Intent(mediaProjectionManager.createScreenCaptureIntent());
        startActivityForResult(intent,REQUESTRESULT);
    }

    protected void initView() {
        btn=findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (status) {
                    case STATUS_NONE:
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
                        params.put(com.baidu.speech.asr.SpeechConstant.APP_ID, "11068966");
                        myWakeup.start(params);

                        status = STATUS_WAITING_READY;
                        btn.setText("停止");
                        if(builder==null){
                            builder=new Notification.Builder(getApplication());
                            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
                            builder.setAutoCancel(true);
                            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_foreground));
                            builder.setContentTitle("语音控制");
                            builder.setContentText("服务中");
                        }else {
                            builder.setContentText("服务中");
                        }
                        notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(NotificationManager.IMPORTANCE_DEFAULT,builder.build());
                        break;
                    case STATUS_WAITING_READY:
                        myWakeup.stop();
                        myRecognizer.stop();

                        status = STATUS_NONE;
                        btn.setText("启动");
                        if(builder==null){
                            builder=new Notification.Builder(getApplication());
                            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
                            builder.setAutoCancel(true);
                            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_foreground));
                            builder.setContentTitle("语音控制");
                            builder.setContentText("停止服务");
                        }else {
                            builder.setContentText("停止服务");
                        }
                        notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(NotificationManager.IMPORTANCE_DEFAULT,builder.build());
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        myWakeup.release();
        myRecognizer.release();
        super.onDestroy();
    }
    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mImageReader = ImageReader.newInstance(mWidth,mHeight, PixelFormat.RGBA_8888, 2);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
            mVirtualDisplay = mediaProjection.createVirtualDisplay("mediaprojection",mWidth,mHeight,
                    mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mImageReader.getSurface(),null,null);

        }
    }
}
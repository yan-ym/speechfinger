package com.example.yammy.speechfinger.wakeup;

import android.util.Log;

import android.os.Handler;

import com.example.yammy.speechfinger.IStatus;

public class SimpleWakeupListener implements IWakeupListener,IStatus {

    private static final String TAG = "SimpleWakeupListener";

    private Handler handler;



    public SimpleWakeupListener(Handler handler) {
        this.handler=handler;
    }

    @Override
    public void onSuccess(String word, WakeUpResult result) {
        //Logger.info(TAG, "唤醒成功，唤醒词：" + word);
        Log.d("succ",word);
        handler.sendMessage(handler.obtainMessage(STATUS_WAKEUP_SUCCESS));
    }@Override
    public void onStop() {
        //Logger.info(TAG, "唤醒词识别结束：");
        Log.d("stop","stop");
    }

    @Override
    public void onError(int errorCode, String errorMessge, WakeUpResult result) {
        //Logger.info(TAG, "唤醒错误：" + errorCode + ";错误消息：" + errorMessge + "; 原始返回" + result.getOrigalJson());
    }

    @Override
    public void onASrAudio(byte[] data, int offset, int length) {
        //Logger.error(TAG, "audio data： " + data.length);
    }

}

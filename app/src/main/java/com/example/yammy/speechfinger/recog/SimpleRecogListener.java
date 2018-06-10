package com.example.yammy.speechfinger.recog;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.yammy.speechfinger.IStatus;

public class SimpleRecogListener implements IRecogListener, IStatus {

    private static final String TAG = "SimpleRecogListener";

    private Handler handler;

    private long speechEndTime;

    private boolean needTime = true;
    /**
     * 识别的引擎当前的状态
     */
    protected int status = STATUS_NONE;

    public SimpleRecogListener(Handler handler){this.handler=handler;}

    @Override
    public void onAsrReady() {
        status = STATUS_READY;
        sendStatusMessage("引擎就绪，可以开始说话。");
    }

    @Override
    public void onAsrBegin() {
        status = STATUS_SPEAKING;
        sendStatusMessage("检测到用户说话");
    }

    @Override
    public void onAsrEnd() {
        status = STATUS_RECOGNITION;
        speechEndTime = System.currentTimeMillis();
        sendMessage("检测到用户说话结束");
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        sendStatusMessage("临时识别结果，结果是“" + results[0] + "”；原始json：" + recogResult.getOrigalJson());
    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        status = STATUS_FINISHED;
        String message = "识别结束，结果是”" + results[0] + "”";
        Log.d("result",message);
        sendStatusMessage(message + "“；原始json：" + recogResult.getOrigalJson());
        if (speechEndTime > 0) {
            long diffTime = System.currentTimeMillis() - speechEndTime;
            message += "；说话结束到识别结束耗时【" + diffTime + "ms】";

        }
        speechEndTime = 0;
        sendMessage(message, status, true);
        Message message1=handler.obtainMessage();
        message1.what = START_OCR;
        message1.obj=results[0];
        handler.sendMessage(message1);
    }

    @Override
    public void onAsrFinish(RecogResult recogResult) {
        status = STATUS_FINISHED;

    }


    @Override
    public void onAsrFinishError(int errorCode, int subErrorCode, String errorMessage, String descMessage,
                                 RecogResult recogResult) {
        status = STATUS_FINISHED;
        String message = "识别错误, 错误码：" + errorCode + " ," + subErrorCode + " ; " + descMessage;
        sendStatusMessage(message + "；错误消息:" + errorMessage + "；描述信息：" + descMessage);
        if (speechEndTime > 0) {
            long diffTime = System.currentTimeMillis() - speechEndTime;
            message += "。说话结束到识别结束耗时【" + diffTime + "ms】";
        }
        sendMessage(message, status, true);
        speechEndTime = 0;
    }

    @Override
    public void onAsrExit() {
        status = STATUS_NONE;
        sendStatusMessage("识别引擎结束并空闲中");
    }

    @Override
    public void onOfflineLoaded() {
        sendStatusMessage("【重要】asr.loaded：离线资源加载成功。没有此回调可能离线语法功能不能使用。");
    }

    @Override
    public void onOfflineUnLoaded() {
        sendStatusMessage(" 离线资源卸载成功。");
    }
    private void sendStatusMessage(String message) {
        sendMessage(message, status);
    }

    private void sendMessage(String message) {
        sendMessage(message, WHAT_MESSAGE_STATUS);
    }

    private void sendMessage(String message, int what) {
        sendMessage(message, what, false);
    }


    private void sendMessage(String message, int what, boolean highlight) {


        if (needTime && what != STATUS_FINISHED) {
            message += "  ;time=" + System.currentTimeMillis();
        }
        if (handler == null){
            Log.i(TAG, message );
            return;
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = status;
        if (highlight) {
            msg.arg2 = 1;
        }
        msg.obj = message + "\n";
        handler.sendMessage(msg);
    }
}

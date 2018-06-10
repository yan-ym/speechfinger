package com.example.yammy.speechfinger.wakeup;

import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;

public class WakeupEventAdapter implements EventListener {
    private IWakeupListener listener;

    private static final String TAG = "WakeupEventAdapter";

    public WakeupEventAdapter(IWakeupListener listener) {
        this.listener = listener;
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        if (SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS.equals(name)) { // 识别唤醒词成功
            WakeUpResult result = WakeUpResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()) { // error不为0依旧有可能是异常情况
                listener.onError(errorCode,wakeupError(errorCode), result);
            } else {
                String word = result.getWord();
                listener.onSuccess(word, result);

            }
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_ERROR.equals(name)) { // 识别唤醒词报错
            WakeUpResult result = WakeUpResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()) {
                listener.onError(errorCode,wakeupError(errorCode), result);
            }
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED.equals(name)) { // 关闭唤醒词
            listener.onStop();
        } else if (SpeechConstant.CALLBACK_EVENT_WAKEUP_AUDIO.equals(name)) { // 音频回调
            listener.onASrAudio(data, offset, length);
        }
    }

    public static String wakeupError(int errorCode) {
        String message = null;
        switch (errorCode) {
            case 1:
                message = "参数错误";
                break;
            case 2:
                message = "网络请求发生错误";
                break;
            case 3:
                message = "服务器数据解析错误";
                break;
            case 4:
                message = "网络不可用";
                break;
            default:
                message = "未知错误:" + errorCode;
                break;
        }
        return message;
    }
}

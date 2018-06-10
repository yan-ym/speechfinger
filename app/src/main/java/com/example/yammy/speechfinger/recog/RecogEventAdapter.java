package com.example.yammy.speechfinger.recog;

import android.speech.SpeechRecognizer;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;

public class RecogEventAdapter implements EventListener {

    private static final String TAG = "RecogEventAdapter";


    private IRecogListener listener;

    public RecogEventAdapter(IRecogListener listener) {
        this.listener = listener;
    }

    protected String currentJson;

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        currentJson = params;
        String logMessage = "name:" + name + "; params:" + params;

        // logcat 中 搜索RecogEventAdapter，即可以看见下面一行的日志
        Log.i(TAG, logMessage);
        if (false) { // 可以调试，不需要后续逻辑
            return;
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_LOADED)) {
            listener.onOfflineLoaded();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_UNLOADED)) {
            listener.onOfflineUnLoaded();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
            // 引擎准备就绪，可以开始说话
            listener.onAsrReady();

        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)) {
            // 检测到用户的已经开始说话
            listener.onAsrBegin();

        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)) {
            // 检测到用户的已经停止说话
            listener.onAsrEnd();

        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            RecogResult recogResult = RecogResult.parseJson(params);
            // 临时识别结果, 长语音模式需要从此消息中取出结果
            String[] results = recogResult.getResultsRecognition();
            if (recogResult.isFinalResult()) {
                listener.onAsrFinalResult(results, recogResult);
            } else if (recogResult.isPartialResult()) {
                listener.onAsrPartialResult(results, recogResult);
            }

        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
            // 识别结束， 最终识别结果或可能的错误
            RecogResult recogResult = RecogResult.parseJson(params);
            if (recogResult.hasError()) {
                int errorCode = recogResult.getError();
                int subErrorCode = recogResult.getSubError();
                //Logger.error(TAG, "asr error:" + params);
                listener.onAsrFinishError(errorCode, subErrorCode,recogError(errorCode), recogResult.getDesc(), recogResult);
            } else {
                listener.onAsrFinish(recogResult);
            }
        }  else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_EXIT)) {
            listener.onAsrExit();
        }
    }
    public static String recogError(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "音频问题";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "没有语音输入";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "其它客户端错误";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "权限不足";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "网络问题";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "没有匹配的识别结果";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "引擎忙";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "服务端错误";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "连接超时";
                break;
            default:
                message = "未知错误:" + errorCode;
                break;
        }
        return message;
    }
}

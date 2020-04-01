package com.yunda.smartglasses.audio;

import android.util.Log;

import com.ibbhub.mp3recorderlib.IAudioRecorder;
import com.ibbhub.mp3recorderlib.Mp3Recorder;
import com.ibbhub.mp3recorderlib.listener.AudioRecordListener;
import com.orhanobut.logger.Logger;

import java.io.File;

public class Mp3RecorderManager {
    private static final String TAG = Mp3RecorderManager.class.getSimpleName();

    public static void mkdirs(String filePath) {
        boolean mk = new File(filePath).mkdirs();
        Log.d(TAG, "mkdirs: " + mk);
    }

    public static Mp3Recorder initMp3Recorder() {
        Mp3Recorder mRecorder = new Mp3Recorder();
        mRecorder.setAudioListener(new AudioRecordListener() {
            @Override
            public void onGetVolume(int volume) {
                Logger.d("onGetVolume: -->" + volume);
            }
        });
        return mRecorder;
    }

    public static void startAudioRecord(IAudioRecorder mRecorder, String filePath) {
        try {
            mRecorder.start(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long stopAudioRecord(IAudioRecorder mRecorder) {
        try {
            return mRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

}

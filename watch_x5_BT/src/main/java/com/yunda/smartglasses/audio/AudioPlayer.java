package com.yunda.smartglasses.audio;

import android.media.MediaPlayer;

import com.blankj.utilcode.util.ToastUtils;

import java.io.File;
import java.io.IOException;

/**
 * <li>标题: 地铁列车360°外观故障检测系统
 * <li>说明: 音频播放器支持ogg,mp3,amr...
 * <li>创建人：邹旭
 * <li>创建日期：2019/6/24 14:53
 * <li>修改人:
 * <li>修改日期：
 * <li>修改内容：
 * <li>版本:  Copyright (c) 2008 运达科技公司
 */
public class AudioPlayer {
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private File soundFile;

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

//    public void setSoundFile(File soundFile) {
//        this.soundFile = soundFile;
//    }

    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    public void stop() {
        //如果在播放中，立刻停止。
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
        }
    }

    public void play() {
        this.play(this.soundFile, null);
    }

    public void play(File soundFile, MediaPlayer.OnCompletionListener listener) {
        this.soundFile = soundFile;
        try {
            //如果没在播放中，立刻开始播放。
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.setDataSource(soundFile.getPath());//指定音频文件路径
                mediaPlayer.prepare();//初始化播放器MediaPlaye
                mediaPlayer.setOnCompletionListener(mp -> {
                    //    mediaPlayer.release();
                    mediaPlayer.reset();
                    if (listener != null) {
                        listener.onCompletion(mp);
                    }
                    ToastUtils.showShort("已结束");
                });
                mediaPlayer.start();
//                tvRecordTime.setText("播放中");
            } else {
                ToastUtils.showShort("播放中...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

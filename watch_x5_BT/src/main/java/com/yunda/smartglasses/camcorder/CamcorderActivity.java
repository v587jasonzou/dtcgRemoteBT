/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunda.smartglasses.camcorder;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.blankj.utilcode.util.ScreenUtils;
import com.orhanobut.logger.Logger;
import com.yunda.smartglasses.R;
import com.yunda.smartglasses.camera.CameraHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This activity uses the camera/camcorder as the A/V source for the
 * {@link android.media.MediaRecorder} API. A {@link android.view.TextureView} is used as the
 * camera preview which limits the code to API 14+. This can be easily replaced with a
 * {@link android.view.SurfaceView} to run on older devices.
 */
public class CamcorderActivity extends AppCompatActivity {
    public static final String BUNDLE_VIDEO_PATH ="video_path";
    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button captureButton;

    //启用的相机
    int cameraId = CamcorderHelper.getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mPreview = findViewById(R.id.surface_view);
        captureButton = findViewById(R.id.button_capture);

//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 200);
        try {
            //配置相机预览
            configCameraPreview(cameraId, getWindowManager().getDefaultDisplay().getRotation());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //监听SurfaceTexture创建情况，当创建成功，需要重新设置预览Surface
        mPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (mCamera!=null) {
                    try {
                        //拍照模式下的 - 对焦模式
                        Camera.Parameters parm = mCamera.getParameters();
                        if (mMediaRecorder == null && parm.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parm.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        mCamera.setParameters(parm);
                        mCamera.setPreviewTexture(surface);
                        mCamera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                releaseMediaRecorder();
                releaseCamera();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * The capture button controls all user interaction. When recording, the button click
     * stops recording, releases {@link android.media.MediaRecorder} and
     * {@link android.hardware.Camera}. When not recording, it prepares the
     * {@link android.media.MediaRecorder} and starts recording.
     *
     * @param view the view generating the event.
     */
    public void onCaptureClick(View view) {
        if (isRecording) {
            // BEGIN_INCLUDE(stop_release_media_recorder)

            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            setCaptureButtonText("Capture");
            isRecording = false;
            releaseCamera();

            //返回录像结果
            Intent intent = new Intent();
            intent.putExtra(BUNDLE_VIDEO_PATH, mOutputFile.getPath());
            setResult(RESULT_OK,intent);
            finish();
            // END_INCLUDE(stop_release_media_recorder)

        } else {

            // BEGIN_INCLUDE(prepare_start_media_recorder)

            new MediaPrepareTask().execute(null, null, null);

            // END_INCLUDE(prepare_start_media_recorder)

        }
    }

    private void setCaptureButtonText(String title) {
        captureButton.setText(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean prepareVideoRecorder(int windowRotation) {
        int cameraId = CamcorderHelper.getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Size optimalSize = null;
        try {
            optimalSize = configCameraPreview(cameraId, windowRotation);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Maybe Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }


        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Use the same size for recording profile.
        /**
         *视频帧尺寸设置{@linkplain optimalSize}不合理，可能导致{@linkplain MediaRecorder#start()}崩溃(报错信息：MediaRecorder: start failed: -19)
         */
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;
        profile.videoBitRate = 1024 * 1024;//比特率,影响视频大小的关键设置

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //同时设置输出视频尺寸
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file
        mOutputFile = CamcorderHelper.getOutputMediaFile(getApplicationContext(), CamcorderHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            /**
             * @see  degree
             */
            //设置输出视频在播放时的方向角度(实测，可行 Android7.1 - 10,部分视频播放器，自动适配至全屏)
            mMediaRecorder.setOrientationHint(CamcorderHelper.getVideoPlaybackOrientation(cameraId, windowRotation));
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            /**
             * 在调用{@linkplain Camera#open(int)}之前，确保相机未被占用
             */
            releaseCamera();
            // initialize video camera
            final int rotation = CamcorderActivity.this.getWindowManager().getDefaultDisplay()
                                                       .getRotation();
            //如果报错：[start failed: -19],考虑摄像机配置了设备不支持的属性
            if (prepareVideoRecorder(rotation)) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                try {
                    mMediaRecorder.start();
                    isRecording = true;
                } catch (IllegalStateException e) {
                    e.printStackTrace();

                    // prepare didn't work, release the camera
                    releaseMediaRecorder();
                    return false;
                }

            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                CamcorderActivity.this.finish();
            }
            // inform the user that recording has started
            setCaptureButtonText("Stop");
        }
    }

    /**配置相机预览
     * @param cameraId
     * @param windowRotation
     * @return
     * @throws IOException
     */
    public Camera.Size configCameraPreview(int cameraId, int windowRotation) throws IOException {
        //// region (configure_preview)
//        mCamera = CameraHelper.getDefaultCameraInstance();
        mCamera = CamcorderHelper.getCameraInstance(cameraId);

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        int minScreenSize = Math.min(ScreenUtils.getScreenWidth(),ScreenUtils.getScreenHeight());
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        //NOTE:尺寸的设置影响预览成像效果(模糊)
        //这里第三个参数为最小尺寸 getPropPreviewSize方法会对从最小尺寸开始升序排列 取出所有支持尺寸的最小尺寸
        //注意：相机默认为0度方向(横向，纵向为90度)，这里匹配时，VIEW大小以横向传入(宽传入高)
        Camera.Size optimalSize;
//        optimalSize = CamcorderHelper.getOptimalVideoSize(mSupportedVideoSizes,
//                mSupportedPreviewSizes, mPreview.getHeight(), mPreview.getWidth());
        optimalSize = CameraHelper.getPropSizeForHeight(mSupportedPreviewSizes, minScreenSize);
        Logger.d(String.format("Camera.Size=%d*%d", optimalSize.width, optimalSize.height));

        List<String> supFm = parameters.getSupportedFocusModes();
        if (supFm.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }else if (supFm.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        // likewise for the camera object itself.
        //【note:设置相机预览尺寸，输出到预览View的图像数据，不一致会导致拉伸等问题】,【预览尺寸设置过大，可能导致预览画面卡顿】
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        mCamera.setParameters(parameters);
        //摄像头方向，默认为横向：0度
        //NOTE:前置摄像头，会执行水平镜像翻转
        mCamera.setDisplayOrientation(CamcorderHelper.getCameraDisplayOrientation(cameraId, windowRotation));
        mCamera.setPreviewTexture(mPreview.getSurfaceTexture());//{@linkplain mPreview.getSurfaceTexture()}可能为空，需要到监听器设置
//        try {
//            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
//            // with {@link SurfaceView}
//            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
//        } catch (IOException e) {
//            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
//        }
        //  endregion (configure_preview)
        return optimalSize;
    }

}

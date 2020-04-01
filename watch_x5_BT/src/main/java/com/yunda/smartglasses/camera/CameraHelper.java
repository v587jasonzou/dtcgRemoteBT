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

package com.yunda.smartglasses.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ScreenUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Camera related utilities.
 */
public class CameraHelper {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Iterate over supported camera video sizes to see which one best fits the
     * dimensions of the given view while maintaining the aspect ratio. If none can,
     * be lenient with the aspect ratio.
     *
     * @param supportedVideoSizes Supported camera video sizes.
     * @param previewSizes        Supported camera preview sizes.
     * @param w                   The width of the view.
     * @param h                   The height of the view.
     * @return Best match camera video size to fit in the view.
     */
    public static Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes,
                                                  List<Camera.Size> previewSizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        } else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * @return the default camera on the device. Return null if there is no camera on the device.
     */
    public static Camera getDefaultCameraInstance() {
        return Camera.open();
    }

    /**
     * @return the default camera on the device. Return null if there is no camera on the device.
     */
    public static Camera getCameraInstance(int cameraId) {
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }


    /**
     * @return the default rear/back facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultBackFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * @return the default front facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultFrontFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }


    /**
     * @param position Physical position of the camera i.e Camera.CameraInfo.CAMERA_FACING_FRONT
     *                 or Camera.CameraInfo.CAMERA_FACING_BACK.
     * @return the default camera on the device. Returns null if camera is not available.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static Camera getDefaultCamera(int position) {
        // Find the total number of cameras available
        int mNumberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return Camera.open(i);

            }
        }

        return null;
    }

    /**
     * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
     * is persistent and available to other applications like gallery.
     *
     * @param applicationContext
     * @param type               Media type. Can be video or image.
     * @return A file object pointing to the newly created file.
     */
    public static File getOutputMediaFile(Context applicationContext, int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        //SD卡pictures目录下
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraSample");
//        File mediaStorageDir = applicationContext.getExternalFilesDir(
//                Environment.DIRECTORY_PICTURES);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * 获取相机预览角度处理，含前置相机镜像处理
     *
     * @param cameraId
     * @param windowRotation
     * @return
     */
    public static int getCameraDisplayOrientation(int cameraId, int windowRotation) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        //假定选择对第一个摄像头
        Camera.getCameraInfo(cameraId, info);
//        int rotation = getWindowManager().getDefaultDisplay()
//                               .getRotation();
        int degrees = 0;
        switch (windowRotation) {
            case Surface.ROTATION_0:
                degrees = 0; break;
            case Surface.ROTATION_90:
                degrees = 90; break;
            case Surface.ROTATION_180:
                degrees = 180; break;
            case Surface.ROTATION_270:
                degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 获取相机录制视频，回放角度信息，无前置相机镜像处理
     *
     * @param cameraId
     * @param windowRotation
     * @return
     */
    public static int getVideoPlaybackOrientation(int cameraId, int windowRotation) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        //假定选择对第一个摄像头
        Camera.getCameraInfo(cameraId, info);
//        int rotation = getWindowManager().getDefaultDisplay()
//                                         .getRotation();
        int degrees = 0;
        switch (windowRotation) {
            case Surface.ROTATION_0:
                degrees = 0; break;
            case Surface.ROTATION_90:
                degrees = 90; break;
            case Surface.ROTATION_180:
                degrees = 180; break;
            case Surface.ROTATION_270:
                degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 在开始预览前 - 设置 预览尺寸，成片尺寸，预览控件尺寸
     */
    public static void setPreviewAndSurfaceSize(Camera camera, SurfaceView surfaceView, int screenWidth, Rect previewRect) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        //step1 预览尺寸
        int minHeight = ScreenUtils.getScreenWidth();
        //NOTE:尺寸的设置影响预览成像效果(模糊)
        //这里第三个参数为最小尺寸 getPropPreviewSize方法会对从最小尺寸开始升序排列 取出所有支持尺寸的最小尺寸
        Camera.Size previewSize = CameraHelper.getPropSizeForHeight(parameters.getSupportedPreviewSizes(), minHeight);
//        Camera.Size previewSize = CameraHelper.getOptimalVideoSize(parameters.getSupportedVideoSizes(),
//                parameters.getSupportedPreviewSizes(), surfaceView.getHeight(), surfaceView.getWidth());
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        //step2 图片尺寸
        Camera.Size pictrueSize = CameraHelper.getPropSizeForHeight(parameters.getSupportedPictureSizes(), minHeight);
        parameters.setPictureSize(pictrueSize.width, pictrueSize.height);

        camera.setParameters(parameters);

        //step3 预览控件 SurfaceView的尺寸
        /**
         * 设置surfaceView的尺寸 因为camera默认是『横屏』，所以取得支持尺寸也都是横屏的尺寸
         * 我们在startPreview方法里面把它矫正了过来，但是这里我们设置设置surfaceView的尺寸的时候要注意 previewSize.height<previewSize.width
         * previewSize.width才是surfaceView的高度
         * 一般相机都是屏幕的宽度 这里设置为屏幕宽度 高度自适应 你也可以设置自己想要的大小
         *
         */
        int picHeight = (screenWidth * previewSize.width) / previewSize.height;//利用横屏模式下的尺寸，计算出竖屏模式下的尺寸

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        params.width = screenWidth;
        params.height = picHeight;

        //返回预览，照片，surfaceView尺寸
        previewRect.right = params.width;
        previewRect.bottom = params.height;
    }

    /**矫正照片方向
     * @param cameraId
     * @param bitmap
     * @return
     */
    public static Bitmap rectifyPhotoOrientation(int cameraId, Bitmap bitmap) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        bitmap = rotaingImageView(info.facing, info.orientation, bitmap);
        return bitmap;
    }

    /**
     * 把相机拍照返回照片转正
     *
     * @param angle 旋转角度
     * @return bitmap 图片
     */
    public static Bitmap rotaingImageView(int facing, int angle, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //加入翻转 把相机拍照返回照片转正
        if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            matrix.postScale(-1, 1);
        }
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }


    /**
     * 获取所有支持的返回视频尺寸
     *
     * @param list
     * @param minHeight
     * @return
     */
    public static Camera.Size getPropSizeForHeight(List<Camera.Size> list, int minHeight) {
        Collections.sort(list, new CameraAscendSizeComparatorForHeight());

        int i = 0;
        for (Camera.Size s : list) {
            if ((s.height >= minHeight)) {
                LogUtils.i("s.height===" + s.height);
                break;
            }
            i++;
        }
        if (i == list.size()) {
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }

    //升序 按照高度
    public static class CameraAscendSizeComparatorForHeight implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.height == rhs.height) {
                return 0;
            } else if (lhs.height > rhs.height) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     * 请求拍照，并通过intent获取拍摄的照片
     *
     * @param activity
     * @param reqCode
     */
    public static void camera(Activity activity, int reqCode) {
        Intent intent = new Intent(activity, CameraActivity.class);
        activity.startActivityForResult(intent, reqCode);
    }


    /**
     * 获取第一个位于{@code facing}方向的摄像头
     * @param facing
     * @return
     */
    public static int getCameraId(int facing) {
        //相机id
        int cameraId=0;
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            //前置 or 后置
            if (cameraInfo.facing == facing) {
                cameraId = i;
            }
        }
        return cameraId;
    }
}

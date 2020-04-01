package com.yunda.smartglasses.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.madgaze.smartglass.hardware.KeyCodeHelper;
import com.yunda.smartglasses.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * <li>标题:
 * <li>说明: 自定义相机组件
 * <li>创建人：邹旭
 * <li>创建日期：2020/3/31 16:51
 * <li>修改人:
 * <li>修改日期：
 * <li>修改内容：
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {
    private Camera mCamera;
    private SurfaceView surfaceView;
    private int mCameraId = CameraHelper.getCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    private Context context;

    //屏幕宽高
    private int screenWidth;
    //底部高度 主要是计算切换正方形时的动画高度
    private ImageView img_camera;
    private Rect previewSize = new Rect();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        context = this;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

        img_camera = findViewById(R.id.img_camera);
        img_camera.setOnClickListener(v -> captrue());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //鲫鱼设备商提供的API，拦截按键监听
        switch (KeyCodeHelper.CheckKeyType(event.getKeyCode(), event)) {
            case CONFIRM:
                // Equivalent to Button A clicked.
                captrue();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = CameraHelper.getCameraInstance(mCameraId);
            startPreview(mCamera, surfaceView.getHolder());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * 预览相机
     */
    private void startPreview(Camera camera, SurfaceHolder holder) {
        try {
            CameraHelper.setPreviewAndSurfaceSize(camera, surfaceView, screenWidth, previewSize);

            camera.setPreviewDisplay(holder);

            //亲测的一个方法 基本覆盖所有手机 将预览矫正
            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            camera.setDisplayOrientation(CameraHelper.getCameraDisplayOrientation(mCameraId, rotation));

            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void captrue() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //将data 转换为位图 或者你也可以直接保存为文件使用 FileOutputStream
                //这里我相信大部分都有其他用处把 比如加个水印 后续再讲解
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap saveBitmap = CameraHelper.rectifyPhotoOrientation(mCameraId, bitmap);

                saveBitmap = Bitmap.createScaledBitmap(saveBitmap, previewSize.width(), previewSize.height(), true);

                //正方形 animHeight(动画高度)
//                saveBitmap = Bitmap.createBitmap(saveBitmap, 0, 0, screenWidth, screenWidth * 4 / 3);
                saveBitmap = Bitmap.createBitmap(saveBitmap, 0, 0, previewSize.width(), previewSize.height());

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String imgPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                        File.separator + "IMG_" + timeStamp + ".jpg";

                BitmapUtils.saveJPGE_After(context, saveBitmap, imgPath, 100);

                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }

                if (!saveBitmap.isRecycled()) {
                    saveBitmap.recycle();
                }

                Intent intent = new Intent();
                intent.putExtra(AppConstant.KEY.IMG_PATH, imgPath);
                intent.putExtra(AppConstant.KEY.PIC_WIDTH, screenWidth);
                intent.putExtra(AppConstant.KEY.PIC_HEIGHT, previewSize.height());
                setResult(Activity.RESULT_OK, intent);
                finish();

                //这里打印宽高 就能看到 CameraUtil.getInstance().getPropPictureSize(parameters.getSupportedPictureSizes(), 200);
                // 这设置的最小宽度影响返回图片的大小 所以这里一般这是1000左右把我觉得
//                Log.d("bitmapWidth==", bitmap.getWidth() + "");
//                Log.d("bitmapHeight==", bitmap.getHeight() + "");
            }
        });
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(mCamera, holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        startPreview(mCamera, holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

}

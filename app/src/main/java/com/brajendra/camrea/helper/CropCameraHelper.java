package com.brajendra.camrea.helper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.brajendra.camrea.view.CameraSurfaceView;

/**
 * Camera helper
 * Created by Brajendra on 2021/02/02.
 */
public class CropCameraHelper {
    private final String TAG = "CropCameraHelper";
    private ToneGenerator tone;
    private String filePath;
    private boolean isPreviewing;

    private static CropCameraHelper helper;
    private Camera camera;
    private CameraSurfaceView surfaceView;

    private Size resolution;

    //	Photo quality
    private int picQuality = 60;

    //	Photo Size
    private Size pictureSize;

    //	Flash mode (default: automatic, ANTIBANDING_OFF: off)
    private String flashlightStatus = Parameters.ANTIBANDING_OFF;

    public enum Flashlight {
        AUTO, ON, OFF
    }

    private CropCameraHelper() {
    }

    public static synchronized CropCameraHelper getInstance() {
        if (helper == null) {
            helper = new CropCameraHelper();
        }
        return helper;
    }

    /**
     * Set photo quality
     *
     * @param picQuality
     * @return
     */
    public CropCameraHelper setPicQuality(int picQuality) {
        this.picQuality = picQuality;
        return helper;
    }

    /**
     * Set flash mode
     *
     * @param status
     * @return
     */
    public CropCameraHelper setFlashlight(Flashlight status) {
        switch (status) {
            case AUTO:
                this.flashlightStatus = Parameters.FLASH_MODE_AUTO;
                break;
            case ON:
                this.flashlightStatus = Parameters.FLASH_MODE_ON;
                break;
            case OFF:
                this.flashlightStatus = Parameters.FLASH_MODE_OFF;
                break;
            default:
                this.flashlightStatus = Parameters.FLASH_MODE_AUTO;
        }
        return helper;
    }

    /**
     * Set file save path(default: /mnt/sdcard/DICM)
     *
     * @param path
     * @return
     */
    public CropCameraHelper setPictureSaveDictionaryPath(String path) {
        this.filePath = path;
        return helper;
    }

    public CropCameraHelper setMaskSurfaceView(CameraSurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        return helper;
    }

    /**
     * Open camera and open preview
     *
     * @param holder       SurfaceHolder
     * @param format       Image Format
     * @param width        SurfaceView width
     * @param height       SurfaceView height
     * @param screenWidth
     * @param screenHeight
     */
    public void openCamera(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight) {
        if (this.camera != null) {
            this.camera.release();
        }
        this.camera = Camera.open();
        this.initParameters(holder, format, width, height, screenWidth, screenHeight);
        this.startPreview();
    }

    /**
     * Take photo
     */
    public void tackPicture(final OnCaptureCallback callback) {
        this.camera.autoFocus(new AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean flag, Camera camera) {
                camera.takePicture(new ShutterCallback() {
                    @Override
                    public void onShutter() {
                        if (tone == null) {
                            //Prompt the user
                            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                        }
                        tone.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }, null, new PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        String filepath = savePicture(data);
                        boolean success = false;
                        if (filepath != null) {
                            success = true;
                        }
                        stopPreview();
                        callback.onCapture(success, filepath);
                    }
                });
            }
        });
    }

    /**
     * Crop and save photos
     *
     * @param data
     * @return
     */
    private String savePicture(byte[] data) {
        File imgFileDir = getImageDir();
        if (!imgFileDir.exists() && !imgFileDir.mkdirs()) {
            return null;
        }
        //File path
        String imgFilePath = imgFileDir.getPath() + File.separator + this.generateFileName();
        ;
        Bitmap b = this.cutImage(data);
        File imgFile = new File(imgFilePath);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(imgFile);
            bos = new BufferedOutputStream(fos);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception error) {
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (IOException e) {
            }
        }
        return imgFilePath;
    }

    /**
     * Generate picture name
     *
     * @return
     */
    private String generateFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault());
        String strDate = dateFormat.format(new Date());
        return "img_" + strDate + ".jpg";
    }

    /**
     * @return
     */
    private File getImageDir() {
        String path = null;
        if (this.filePath == null || this.filePath.equals("")) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        } else {
            path = Environment.getExternalStorageDirectory().getPath() + filePath;
        }
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    /**
     * Initialize camera parameters
     *
     * @param holder       SurfaceHolder
     * @param format
     * @param width
     * @param height
     * @param screenWidth
     * @param screenHeight
     */
    private void initParameters(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight) {
        try {
            Parameters p = this.camera.getParameters();

            this.camera.setPreviewDisplay(holder);

            if (width > height) {
                //Horizontal screen
                this.camera.setDisplayOrientation(0);
            } else {
                //Portrait
                this.camera.setDisplayOrientation(90);
            }

            //Photo quality
            p.set("jpeg-quality", picQuality);

            p.setPictureFormat(PixelFormat.JPEG);

            //Set the flash
            p.setFlashMode(this.flashlightStatus);

            //Set the best preview size
            List<Size> previewSizes = p.getSupportedPreviewSizes();
            //Set preview resolution
            if (this.resolution == null) {
                this.resolution = this.getOptimalPreviewSize(previewSizes, width, height);
            }
            try {
                p.setPreviewSize(this.resolution.width, this.resolution.height);
            } catch (Exception e) {
                Log.e(TAG, "Unsupported camera preview resolution: " + this.resolution.width + " × " + this.resolution.height);
            }

            if (this.pictureSize == null) {
                List<Size> pictureSizes = p.getSupportedPictureSizes();
                this.setPicutreSize(pictureSizes, screenWidth, screenHeight);
            }
            try {
                p.setPictureSize(this.pictureSize.width, this.pictureSize.height);
            } catch (Exception e) {
                Log.e(TAG, "Unsupported photo size: " + this.pictureSize.width + " × " + this.pictureSize.height);
            }

            this.camera.setParameters(p);
        } catch (Exception e) {
            Log.e(TAG, "Camera parameter setting error");
        }
    }

    /**
     * release Camera
     */
    public void releaseCamera() {
        if (this.camera != null) {
            if (this.isPreviewing) {
                this.stopPreview();
            }
            this.camera.setPreviewCallback(null);
            isPreviewing = false;
            this.camera.release();
            this.camera = null;
        }
    }

    private void stopPreview() {
        if (this.camera != null && this.isPreviewing) {
            this.camera.stopPreview();
            this.isPreviewing = false;
        }
    }

    public void startPreview() {
        if (this.camera != null) {
            this.camera.startPreview();
            this.camera.autoFocus(null);
            this.isPreviewing = true;
        }
    }

    /**
     * Crop photos
     *
     * @param data
     * @return
     */
    private Bitmap cutImage(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (this.surfaceView.getWidth() < this.surfaceView.getHeight()) {
            //Rotate photos in portrait
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        if (this.surfaceView == null) {
            return bitmap;
        } else {
            int[] sizes = this.surfaceView.getMaskSize();
            if (sizes[0] == 0 || sizes[1] == 0) {
                return bitmap;
            }
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int x = (w - sizes[0]) / 2;
            int y = (h - sizes[1]) / 2;
            return Bitmap.createBitmap(bitmap, x, y, sizes[0], sizes[1]);
        }
    }

    /**
     * Get the best preview size
     */
    private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) width / height;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double r = size.width * 1.0 / size.height * 1.0;
            if (r != 4 / 3 || r != 3 / 4 || r != 16 / 9 || r != 9 / 16) {
                continue;
            }

            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * Set the photo size to the closest screen size
     *
     * @param list
     * @return
     */
    private void setPicutreSize(List<Size> list, int screenWidth, int screenHeight) {
        int approach = Integer.MAX_VALUE;

        for (Size size : list) {
            int temp = Math.abs(size.width - screenWidth + size.height - screenHeight);
            System.out.println("approach: " + approach + ", temp: " + temp + ", size.width: " + size.width + ", size.height: " + size.height);
            if (approach > temp) {
                approach = temp;
                this.pictureSize = size;
            }
        }
//		//Descending
//		if(list.get(0).width>list.get(list.size()-1).width){
//			int len = list.size();
//			list = list.subList(0, len/2==0? len/2 : (len+1)/2);
//			this.pictureSize = list.get(list.size()-1);
//		}else{
//			int len = list.size();
//			list = list.subList(len/2==0? len/2 : (len-1)/2, len-1);
//			this.pictureSize = list.get(0);
//		}
    }
}

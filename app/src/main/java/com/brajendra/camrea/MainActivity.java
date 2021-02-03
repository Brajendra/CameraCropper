package com.brajendra.camrea;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.brajendra.camrea.helper.CropCameraHelper;
import com.brajendra.camrea.helper.OnCaptureCallback;
import com.brajendra.camrea.view.CameraSurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
/*
 * Created by Brajendra on 2021/02/02.
 */

public class MainActivity extends Activity implements OnCaptureCallback {
    private static final int STORAGE_REQUEST_CODE = 000;
    private static final int CAMREA = 111;

    private CameraSurfaceView surfaceview;
    private ImageView imageView;
    private Button btn_capture;
    private Button btn_recapture;
    private Button btn_cancel;
    private Button btn_ok;

    //	Save the file path after taking the photo
    private String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        requestPermission();

        this.setContentView(R.layout.activity_main);

        this.surfaceview = (CameraSurfaceView) findViewById(R.id.surface_view);
        this.imageView = (ImageView) findViewById(R.id.image_view);
        btn_capture = (Button) findViewById(R.id.btn_capture);
        btn_recapture = (Button) findViewById(R.id.btn_recapture);
        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);

        //Set the size of the rectangular area
        this.surfaceview.setMaskSize(900, 600);

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_capture.setEnabled(false);
                btn_ok.setEnabled(true);
                btn_recapture.setEnabled(true);
                CropCameraHelper.getInstance().tackPicture(MainActivity.this);
            }
        });

        btn_recapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_capture.setEnabled(true);
                btn_ok.setEnabled(false);
                btn_recapture.setEnabled(false);
                imageView.setVisibility(View.GONE);
                surfaceview.setVisibility(View.VISIBLE);
                deleteFile();
                CropCameraHelper.getInstance().startPreview();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Bitmap bitmap = BitmapFactory.decodeFile(filepath);
                Toast.makeText(MainActivity.this, "Picture height：" + bitmap.getHeight() + "..." + "Picture width：" + bitmap.getWidth() + "...size of picture：" + bitmap.getByteCount() / 10240, Toast.LENGTH_LONG).show();

                //Upload server to background
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] datas = baos.toByteArray();
                String imageDatasString = Base64.encodeToString(datas, Base64.DEFAULT);
                //  uploadOnServer(Base64.encodeToString(datas, Base64.NO_WRAP));
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deleteFile();
                finish();
            }
        });
    }

    /**
     * How about deleting picture files
     */
    private void deleteFile() {
        if (this.filepath == null || this.filepath.equals("")) {
            return;
        }
        File f = new File(this.filepath);
        if (f.exists()) {
            f.delete();
        }
    }

    @Override
    public void onCapture(boolean success, String filepath) {
        this.filepath = filepath;
        String message = "Take pictures successfully";
        if (!success) {
            message = "Photo failed";
            CropCameraHelper.getInstance().startPreview();
            this.imageView.setVisibility(View.GONE);
            this.surfaceview.setVisibility(View.VISIBLE);
        } else {
            this.imageView.setVisibility(View.VISIBLE);
            this.surfaceview.setVisibility(View.GONE);
            this.imageView.setImageBitmap(BitmapFactory.decodeFile(filepath));
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMREA);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST_CODE || requestCode == CAMREA) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
                Toast.makeText(this, "Camera and storage permissions must be turned on", Toast.LENGTH_SHORT).show();
            } else {
                //....
            }
        }
    }
}

package com.brajendra.camrea.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.brajendra.camrea.helper.CropCameraHelper;
import com.brajendra.camrea.util.Util;

/**
 * Custom camera Surface View
 * Created by Brajendra on 2021/02/02.
 */
public class CameraSurfaceView extends FrameLayout {

    private Context context;
    private MSurfaceView surfaceView;//Load the camera's mask
    private MaskView maskView;//Load the mask of the layout
    private int width;//Screen width
    private int height;//Screen height
    private int maskWidth;//Width of central transparent area
    private int maskHeight;//Height of central transparent area
    private int screenWidth;//Camera capture width
    private int screenHeight;//Camera capture height

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        surfaceView = new MSurfaceView(context);//The first canvas is used to load the camera
        maskView = new MaskView(context);//The second canvas is used to draw all the layout
        this.addView(surfaceView, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addView(maskView, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        CropCameraHelper.getInstance().setMaskSurfaceView(this);
    }

    public void setMaskSize(Integer width, Integer height) {
        maskHeight = height;
        maskWidth = width;
    }

    public int[] getMaskSize() {
        return new MaskSize().size;
    }

    private class MaskSize {
        private int[] size;

        private MaskSize() {
            this.size = new int[]{maskWidth, maskHeight, width, height};
        }
    }

    /**
     * Layout to host the camera
     */
    private class MSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder holder;

        public MSurfaceView(Context context) {
            super(context);
            this.holder = this.getHolder();
            //translucent transparent transparent
            this.holder.setFormat(PixelFormat.TRANSPARENT);
            this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            this.holder.addCallback(this);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            width = w;
            height = h;
            CropCameraHelper.getInstance().openCamera(holder, format, width, height, screenWidth, screenHeight);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            CropCameraHelper.getInstance().releaseCamera();
        }
    }

    /**
     * Mask all layout
     */
    private class MaskView extends View {
        private Paint linePaint;
        private Paint rectPaint;
        private Paint topTextPaint;
        private Paint bottomTextPaint;

        public MaskView(Context context) {
            super(context);

            //Paint that draws the rectangular boundary of the transparent area in the middle
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.TRANSPARENT);//Set the color of the middle area to transparent
            linePaint.setStyle(Style.STROKE);
            linePaint.setStrokeWidth(3f);
            linePaint.setAlpha(0);//The value range is 0~255, the smaller the value, the more transparent

            //Draw a rectangular shadow area around it
            rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rectPaint.setColor(Color.BLACK);
            rectPaint.setStyle(Style.FILL);
            rectPaint.setAlpha(170);//The value range is 0~255, the smaller the value, the more transparent

            //Draw the top middle prompt font
            topTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            topTextPaint.setColor(Color.WHITE);
            //topTextPaint.setStyle(Paint.Style.FILL);
            topTextPaint.setTextAlign(Paint.Align.CENTER);//Put the x,y coordinates in the middle of the font (default x,y coordinates are the font head)
            topTextPaint.setTextSize(Util.sp2px(context, 14));

            //Draw the top middle prompt font
            bottomTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bottomTextPaint.setColor(Color.parseColor("#A0A0A0"));
            //bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);//Put the x,y coordinates in the middle of the font (default x,y coordinates are the font head)
            bottomTextPaint.setTextSize(Util.sp2px(context, 12));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (maskHeight == 0 && maskWidth == 0) {
                return;
            }
            if (maskHeight == height || maskWidth == width) {
                return;
            }

            if ((height > width && maskHeight < maskWidth) || (height < width && maskHeight > maskWidth)) {
                int temp = maskHeight;
                maskHeight = maskWidth;
                maskWidth = temp;
            }

            //height：Screen height
            //width：Screen width
            //maskHeight：Middle transparent area height
            //maskWidth：Width of middle transparent area
            int h = Math.abs((height - maskHeight) / 2);//Top shadow height
            int w = Math.abs((width - maskWidth) / 2);//Right shadow width

            //Upper shadow
            canvas.drawRect(0, 0, width, h, this.rectPaint);
            //Right shadow
            canvas.drawRect(width - w, h, width, height - h, this.rectPaint);
            //Lower shadow
            canvas.drawRect(0, height - h, width, height, this.rectPaint);
            //Left shadow
            canvas.drawRect(0, h, w, h + maskHeight, this.rectPaint);
            //Medium and transparent
            canvas.drawRect(w, h, w + maskWidth, h + maskHeight, this.linePaint);
            canvas.save();//Save the top, bottom, left, and right
            //Middle-top-font
            canvas.rotate(90, width - w / 2, height / 2);//Rotate the canvas 90 degrees
            canvas.drawText("Please scan", width - w / 2, height / 2, topTextPaint);
            canvas.restore();//Restore the canvas to the last saved position to prevent this rotation from affecting the following operations
            canvas.save();//Save the top font
            //Middle-bottom-font
            canvas.rotate(90, w / 2, height / 2);//Rotate 90 degrees
            canvas.drawText("Please keep the light well, the background clean", w / 2, height / 2, bottomTextPaint);
            canvas.restore();//Restore the canvas to the last saved position to prevent this rotation from affecting the following operations
            canvas.save();//Save the bottom font

            //Print logo
            Log.e("Height width", "height:" + height + ",width:" + width + ",h:" + h + ",w:" + w + ",mskHeight:" + maskHeight + ",maskWidth:" + maskWidth);

            super.onDraw(canvas);
        }
    }
}

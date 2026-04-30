package tfsapps.smilechecker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {
    private final Object lock = new Object();
    private List<FaceDrawInfo> faceInfoList = new ArrayList<>();
    private Paint boxPaint;
    private Paint textPaint;
    private Paint textValuePaint;

    // Scale properties
    private int imageWidth;
    private int imageHeight;
    private boolean isFrontFacing = false;

    public static class FaceDrawInfo {
        RectF bounds;
        String emotionMessage;
        String emotionValue;
        boolean isFaceFrame;
        boolean isFaceMark;
        boolean isFaceValue;

        public FaceDrawInfo(RectF bounds, String emotionMessage, String emotionValue,
                boolean isFaceFrame, boolean isFaceMark, boolean isFaceValue) {
            this.bounds = bounds;
            this.emotionMessage = emotionMessage;
            this.emotionValue = emotionValue;
            this.isFaceFrame = isFaceFrame;
            this.isFaceMark = isFaceMark;
            this.isFaceValue = isFaceValue;
        }
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);

        textValuePaint = new Paint();
        textValuePaint.setColor(Color.GREEN);
        textValuePaint.setStyle(Paint.Style.FILL);
    }

    public void setCameraInfo(int imageWidth, int imageHeight, boolean isFrontFacing) {
        synchronized (lock) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.isFrontFacing = isFrontFacing;
        }
    }

    public void setFaces(List<FaceDrawInfo> faces) {
        synchronized (lock) {
            faceInfoList.clear();
            faceInfoList.addAll(faces);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            if (faceInfoList.isEmpty() || imageWidth == 0 || imageHeight == 0) {
                return;
            }

            // Calculate scale and sizes dynamically based on current view size
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            // Adjust scaling so the preview fits the view bounds. (Assuming FIT_CENTER)
            float scaleX = (float) viewWidth / imageWidth;
            float scaleY = (float) viewHeight / imageHeight;

            float scale = Math.min(scaleX, scaleY);

            float offsetX = (viewWidth - (imageWidth * scale)) / 2.0f;
            float offsetY = (viewHeight - (imageHeight * scale)) / 2.0f;

            // Adjust stroke and text size
            int minSize = Math.min(viewWidth, viewHeight);
            float strokeWidth = (minSize / 3.0f) * 0.01f;
            float textSize = (minSize / 3.0f) * 2.0f * 0.05f;

            boxPaint.setStrokeWidth(strokeWidth);
            textPaint.setTextSize(textSize);
            textValuePaint.setTextSize(textSize);

            for (FaceDrawInfo info : faceInfoList) {
                // Map bounding box to view coordinates
                RectF mappedBox = new RectF(
                        info.bounds.left * scale + offsetX,
                        info.bounds.top * scale + offsetY,
                        info.bounds.right * scale + offsetX,
                        info.bounds.bottom * scale + offsetY);

                // Handle front camera mirroring
                if (isFrontFacing) {
                    float left = viewWidth - mappedBox.right;
                    float right = viewWidth - mappedBox.left;
                    mappedBox.left = left;
                    mappedBox.right = right;
                }

                if (info.isFaceFrame) {
                    canvas.drawRect(mappedBox, boxPaint);
                }

                if (info.isFaceMark && info.emotionMessage != null && !info.emotionMessage.isEmpty()) {
                    canvas.drawText(info.emotionMessage, mappedBox.left, mappedBox.bottom + textSize, textPaint);
                }

                if (info.isFaceValue && info.emotionValue != null && !info.emotionValue.isEmpty()
                        && info.emotionMessage.toLowerCase().contains("smile")) {
                    canvas.drawText(info.emotionValue, mappedBox.left, mappedBox.top - textSize, textValuePaint);
                }
            }
        }
    }
}

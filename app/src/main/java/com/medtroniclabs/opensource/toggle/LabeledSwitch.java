package com.medtroniclabs.opensource.toggle;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.medtroniclabs.opensource.R;

public class LabeledSwitch extends ToggleableView {
    private int padding;

    private int colorOff;
    private int colorDisabled;

    private int textSize;

    private int outerRadii;
    private int thumbRadii;

    private Paint paint;

    private long startTime;

    private String labelOn;
    private String labelOff;

    private RectF thumbBounds;

    private RectF leftBgArc;
    private RectF rightBgArc;

    private RectF leftFgArc;
    private RectF rightFgArc;

    private float thumbOnCenterX;
    private float thumbOffCenterX;

    private static final String WHITE_BG = "#FFFFFF";
    private static final String BLUE_BG = "#6165de";

    public LabeledSwitch(Context context) {
        super(context);
        initView();
    }

    public LabeledSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public LabeledSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        this.isOn = false;
        this.labelOn = "High";
        this.labelOff = "Low";

        this.enabled = true;
        this.textSize = (int) (12f * getResources().getDisplayMetrics().density);

        paint = new Paint();
        paint.setAntiAlias(true);

        leftBgArc = new RectF();
        rightBgArc = new RectF();

        leftFgArc = new RectF();
        rightFgArc = new RectF();
        thumbBounds = new RectF();

        this.colorOff = Color.parseColor(WHITE_BG);
        this.colorDisabled = Color.parseColor(WHITE_BG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setTextSize(textSize);

//      Drawing Switch background here
        drawSwitchBackground(canvas);

//      Drawing Switch Labels here
        String maxChar = "N";
        float textCenter = paint.measureText(maxChar) / 2;
        if (isOn) {
            paint.setColor(Color.parseColor(BLUE_BG));//low-on

            float centerX = (width - padding - ((padding + (padding >>> 1)) + (thumbRadii << 1))) >>> 1;
            canvas.drawText(labelOff, (padding + (padding >>> 1)) + (thumbRadii << 1) + centerX - (paint.measureText(labelOff) / 2), (height >>> 1) + textCenter, paint);

            paint.setColor(Color.parseColor(WHITE_BG));//high-on

            int maxSize = width - (padding << 1) - (thumbRadii << 1);

            centerX = (((padding >>> 1) + maxSize) - padding) >>> 1;
            canvas.drawText(labelOn, padding + centerX - (paint.measureText(labelOn) / 2), (height >>> 1) + textCenter, paint);
        } else {
            paint.setColor(Color.parseColor("#D2D2D2"));//high-off

            int maxSize = width - (padding << 1) - (thumbRadii << 1);
            float centerX = (((padding >>> 1) + maxSize) - padding) >>> 1;
            canvas.drawText(labelOn, padding + centerX - (paint.measureText(labelOn) / 2), (height >>> 1) + textCenter, paint);

            paint.setColor(Color.parseColor(WHITE_BG));//low-off

            centerX = (width - padding - ((padding + (padding >>> 1)) + (thumbRadii << 1))) >>> 1;
            canvas.drawText(labelOff, (padding + (padding >>> 1)) + (thumbRadii << 1) + centerX - (paint.measureText(labelOff) / 2), (height >>> 1) + textCenter, paint);
        }

//      Drawing Switch Thumb here
        drawSwitchThumb(canvas);
    }

    private void drawSwitchThumb(Canvas canvas) {
        int alpha = (int) (((thumbBounds.centerX() - thumbOffCenterX) / (thumbOnCenterX - thumbOffCenterX)) * 255);
        alpha = (alpha < 0 ? 0 : (Math.min(alpha, 255)));
        int offColor = Color.argb(alpha, Color.red(colorOff), Color.green(colorOff), Color.blue(colorOff));
        paint.setColor(offColor);

        canvas.drawCircle(thumbBounds.centerX(), thumbBounds.centerY(), thumbRadii, paint);
        alpha = (int) (((thumbOnCenterX - thumbBounds.centerX()) / (thumbOnCenterX - thumbOffCenterX)) * 255);
        alpha = (alpha < 0 ? 0 : (Math.min(alpha, 255)));
        paint.setColor(Color.argb(alpha, Color.red(colorDisabled), Color.green(colorDisabled), Color.blue(colorDisabled)));
        canvas.drawCircle(thumbBounds.centerX(), thumbBounds.centerY(), thumbRadii, paint);
    }

    private void drawSwitchBackground(Canvas canvas) {
        int tx;
        if (isOn) {
            tx = Color.parseColor(BLUE_BG);
        } else {
            tx = colorDisabled;
        }
        if (isEnabled()) {
            paint.setColor(tx);
        } else {
            paint.setColor(colorDisabled);
        }
        canvas.drawArc(leftBgArc, 90, 180, false, paint);
        canvas.drawArc(rightBgArc, 90, -180, false, paint);
        canvas.drawRect(outerRadii, 0, (width - outerRadii), height, paint);

        paint.setColor(tx);

        int pd = padding / 10;
        canvas.drawArc(leftFgArc, 90, 180, false, paint);
        canvas.drawArc(rightFgArc, 90, -180, false, paint);
        canvas.drawRect(outerRadii, pd, (width - outerRadii), (height - pd), paint);

        paint.setColor(tx);

        canvas.drawArc(leftBgArc, 90, 180, false, paint);
        canvas.drawArc(rightBgArc, 90, -180, false, paint);
        canvas.drawRect(outerRadii, 0, (width - outerRadii), height, paint);

        if (isOn) {
            paint.setColor(Color.parseColor(BLUE_BG));
        } else {
            paint.setColor(Color.parseColor("#D2D2D2"));
        }

        canvas.drawArc(leftFgArc, 90, 180, false, paint);
        canvas.drawArc(rightFgArc, 90, -180, false, paint);
        canvas.drawRect(outerRadii, pd, (width - outerRadii), (height - pd), paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = getResources().getDimensionPixelSize(R.dimen.labeled_default_width);
        int desiredHeight = getResources().getDimensionPixelSize(R.dimen.labeled_default_height);

        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == View.MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == View.MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);

        outerRadii = Math.min(width, height) >>> 1;
        thumbRadii = (int) (Math.min(width, height) / (2.88f));
        padding = (height - thumbRadii) >>> 1;

        thumbBounds.set((width - padding - thumbRadii), padding, (width - padding), (height - padding));
        thumbOnCenterX = thumbBounds.centerX();

        thumbBounds.set(padding, padding, (padding + thumbRadii), (height - padding));
        thumbOffCenterX = thumbBounds.centerX();

        if (isOn) {
            thumbBounds.set((width - padding - thumbRadii), padding, (width - padding), (height - padding));
        } else {
            thumbBounds.set(padding, padding, (padding + thumbRadii), (height - padding));
        }

        leftBgArc.set(0, 0, outerRadii << 1, height);
        rightBgArc.set((width - (outerRadii << 1)), 0, width, height);

        int pd = padding / 10;
        leftFgArc.set(pd, pd, ((outerRadii << 1) - pd), (height - pd));
        rightFgArc.set((width - (outerRadii << 1) + pd), pd, (width - pd),(height - pd));
    }

    @Override
    public final boolean performClick() {
        super.performClick();
        ValueAnimator switchColor;
        if (isOn) {
            switchColor = ValueAnimator.ofFloat(width - padding - thumbRadii, padding);
            switchColor.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                thumbBounds.set(value, thumbBounds.top, value + thumbRadii, thumbBounds.bottom);
                invalidate();
            });
        } else {
            switchColor = ValueAnimator.ofFloat(padding, width - padding - thumbRadii);
            switchColor.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                thumbBounds.set(value, thumbBounds.top, value + thumbRadii, thumbBounds.bottom);
                invalidate();
            });
        }
        switchColor.setInterpolator(new AccelerateDecelerateInterpolator());
        switchColor.setDuration(250);
        switchColor.start();
        isOn = !isOn;
        if (onToggledListener != null) {
            onToggledListener.onSwitched(this, isOn);
        }
        return true;
    }

    @Override
    public final boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            float x = event.getX();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    startTime = System.currentTimeMillis();
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (x - (thumbRadii >>> 1) > padding && x + (thumbRadii >>> 1) < width - padding) {
                        thumbBounds.set(x - (thumbRadii >>> 1), thumbBounds.top, x + (thumbRadii >>> 1), thumbBounds.bottom);
                        invalidate();
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    long endTime = System.currentTimeMillis();
                    long span = endTime - startTime;
                    if (span < 200) {
                        performClick();
                    } else {
                        valueAnimation(x);
                    }
                    invalidate();
                    return true;
                }

                default: {
                    return super.onTouchEvent(event);
                }
            }
        } else {
            return false;
        }
    }

    private void valueAnimation(float x) {
        if (x >= width >>> 1) {
            ValueAnimator switchColor = ValueAnimator.ofFloat((x > (width - padding - thumbRadii) ? (width - padding - thumbRadii) : x), width - padding - thumbRadii);
            switchColor.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                thumbBounds.set(value, thumbBounds.top, value + thumbRadii, thumbBounds.bottom);
                invalidate();
            });
            switchColor.setInterpolator(new AccelerateDecelerateInterpolator());
            switchColor.setDuration(250);
            switchColor.start();
            isOn = true;
        } else {
            ValueAnimator switchColor = ValueAnimator.ofFloat((x < padding ? padding : x), padding);
            switchColor.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                thumbBounds.set(value, thumbBounds.top, value + thumbRadii, thumbBounds.bottom);
                invalidate();
            });
            switchColor.setInterpolator(new AccelerateDecelerateInterpolator());
            switchColor.setDuration(250);
            switchColor.start();
            isOn = false;
        }
        if (onToggledListener != null) {
            onToggledListener.onSwitched(this, isOn);
        }
    }

    @Override
    public void setOn(boolean on) {
        super.setOn(on);
        if (isOn) {
            thumbBounds.set((width - padding - thumbRadii), padding, (width - padding), (height - padding));
        } else {
            thumbBounds.set(padding, padding, (padding + thumbRadii), (height - padding));
        }
        invalidate();
    }
}

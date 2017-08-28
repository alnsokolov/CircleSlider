package hawk_inc.ru.circleslider;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.TreeMap;

/**
 * Created by Admin on 8/26/2017.
 */
public class CircleSlider extends View {

/*
    *
    * Here are all the values, that represent parameters
    * in xml layout file.
    *
*/
    private float mMaxValue, mValue;
    /*private float mTextSize;*/
    private boolean /*mShowText, */mEnabled, mFocused, mPlayAnimationIntro = true;
    private int mRotation, mMaxAngle;

/*
    *
    * Here are values and objects responsible for color.
    *
*/
    private int mColorAccent, mColorAccentDark/*, mTextColor*/;
    private Paint mLinePaint, mDarkLinePaint, mCirclePaint, mAreaPaint, mOuterAreaPaint/*, mTextPaint*/;

/*
    *
    * Here are values for width of the colored arc, gray ark
    * and for current radius of different circles. TreeMap "circles"
    * stores default values for all types of circles. The values get put
    * into it later.
    *
*/
    private  float STROKE_WIDTH = 6, DARK_STROKE_WIDTH = STROKE_WIDTH/2.5f;
    private  float CIRCLE_RADIUS = STROKE_WIDTH * 1.5f, AREA_RADIUS = STROKE_WIDTH * 4.9f;
    private enum State{
        NORMAL,
        FOCUSED,
        CLICKED,
        DISABLED
    }
    private TreeMap<State,Float> circles = new TreeMap<>();

/*
    *
    * Here are the "helper" values, which optimise drawing
    * speed.
    *
*/
    private RectF arc;
    private float radius, stopAngle, startAngle, centerX, centerY;
    private float circleX, circleY, /*textX, textY, */width, height;
    private boolean mDown;
    OnCircleSliderChangeListener mListener;
/*
    *
    * Here are values and objects used for animations.
    *
*/
    private  final int CLICK_DELAY_MS = 200, INTRO_DELAY_MS = 750;
    private ValueAnimator mClickAnimator, mUnClickAnimator, mFocusAnimator;
    private ValueAnimator mUnFocusAnimator, mIntroAnimator, mBounceAnimator;

    /*
        *
        * Here we get all the attributes from the item
        * and initialise all the objects through other
        * function.
        *
    */
    public CircleSlider(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context
                        .getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.CircleSlider, 0, 0);

        try{
            mMaxValue = a.getInteger(R.styleable.CircleSlider_max_value, 100);
            mValue = Math.min(a.getFloat(R.styleable.CircleSlider_value, 50), mMaxValue);

            /*mTextSize = a.getDimension(R.styleable.CircleSlider_text_size, 100);
            mShowText = a.getBoolean(R.styleable.CircleSlider_show_value, true);
            mTextColor = a.getColor(R.styleable.CircleSlider_text_color, Color.rgb(32,32,32));*/

            mEnabled = a.getBoolean(R.styleable.CircleSlider_enabled, true);
            mFocused = a.getBoolean(R.styleable.CircleSlider_focused, false);

            mRotation = a.getInteger(R.styleable.CircleSlider_rotation, 0);
            mMaxAngle = a.getInteger(R.styleable.CircleSlider_max_angle, 270);

            mPlayAnimationIntro = a.getBoolean(R.styleable.CircleSlider_play_intro, true);
        } finally {
            a.recycle();
        }

        init();
    }

    /*
        *
        * Here we initialise all the objects we will need.
        *
    */
    private void init(){
        mColorAccent = getResources().getColor(R.color.colorAccent);
        mColorAccentDark = getResources().getColor(R.color.colorAccentDark);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(mColorAccent);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(STROKE_WIDTH);
        mDarkLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDarkLinePaint.setColor(mColorAccentDark);
        mDarkLinePaint.setStyle(Paint.Style.STROKE);
        mDarkLinePaint.setStrokeWidth(DARK_STROKE_WIDTH);
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(mColorAccent);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAreaPaint.setColor(Color.argb(64, Color.red(mColorAccent),
                                            Color.green(mColorAccent),
                                            Color.blue(mColorAccent)));
        mAreaPaint.setStyle(Paint.Style.FILL);
        mOuterAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterAreaPaint.setColor(mColorAccent);
        mOuterAreaPaint.setStyle(Paint.Style.STROKE);
        mOuterAreaPaint.setStrokeWidth(DARK_STROKE_WIDTH);

        /*mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth(STROKE_WIDTH);*/

        arc = new RectF();

        circles.put(State.NORMAL, STROKE_WIDTH * 1.5f);
        circles.put(State.FOCUSED, STROKE_WIDTH * 4.9f);
        circles.put(State.CLICKED, STROKE_WIDTH * 2.5f);
        circles.put(State.DISABLED, STROKE_WIDTH);

        mClickAnimator = ValueAnimator.ofFloat(circles.get(State.NORMAL), circles.get(State.CLICKED));
        mClickAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mClickAnimator.setDuration(CLICK_DELAY_MS);
        mClickAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                CIRCLE_RADIUS = (float)valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        mUnClickAnimator = ValueAnimator.ofFloat(circles.get(State.CLICKED), circles.get(State.NORMAL));
        mUnClickAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mUnClickAnimator.setDuration(CLICK_DELAY_MS);
        mUnClickAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                CIRCLE_RADIUS = (float)valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        mFocusAnimator = ValueAnimator.ofFloat(circles.get(State.NORMAL), circles.get(State.FOCUSED));
        mFocusAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mFocusAnimator.setDuration(CLICK_DELAY_MS);
        mFocusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                AREA_RADIUS = (float)valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        mUnFocusAnimator = ValueAnimator.ofFloat(circles.get(State.FOCUSED), circles.get(State.NORMAL));
        mUnFocusAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mUnFocusAnimator.setDuration(CLICK_DELAY_MS);
        mUnFocusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                AREA_RADIUS = (float)valueAnimator.getAnimatedValue();

                if(valueAnimator.getCurrentPlayTime() >= CLICK_DELAY_MS - 1)
                    mFocused = false;

                invalidate();
            }
        });
        mIntroAnimator = ValueAnimator.ofFloat(0,mValue);
        mIntroAnimator.setInterpolator(new OvershootInterpolator(3f));
        mIntroAnimator.setDuration(INTRO_DELAY_MS);
        mIntroAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mValue = (float)valueAnimator.getAnimatedValue();
                update();
                invalidate();
            }
        });
        mBounceAnimator = ValueAnimator.ofFloat(circles.get(State.NORMAL),circles.get(State.CLICKED));
        mBounceAnimator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float v) {
                return (float)(Math.sin(v * Math.PI) * 0.75);
            }
        });
        mBounceAnimator.setDuration(CLICK_DELAY_MS);
        mBounceAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                CIRCLE_RADIUS = (float)valueAnimator.getAnimatedValue();
                invalidate();
            }
        });

        if(mPlayAnimationIntro) {
            new CountDownTimer(1000, 1) {
                @Override
                public void onTick(long l) {
                    if(mValue != 0) {
                        mValue = 0;
                        update();
                        invalidate();
                    }
                }

                @Override
                public void onFinish() {
                    mIntroAnimator.start();
                }
            }.start();
        }
    }



    /*
        *
        * Here we draw two arcs and one or two circles
        * (depends on focus of item), that represent
        * "CircleSlider"(or "CircleSeekBar").
        *
    */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(arc, 270 - mMaxAngle / 2 + mRotation, stopAngle, false, mLinePaint);
        canvas.drawArc(arc, 270 - mMaxAngle / 2 + mRotation + startAngle, mMaxAngle - startAngle, false, mDarkLinePaint);

        canvas.drawCircle(circleX, circleY, CIRCLE_RADIUS, mCirclePaint);

        if(mEnabled && mFocused)
            canvas.drawCircle(circleX, circleY, AREA_RADIUS, mAreaPaint);
        if(mDown && mFocused)
            canvas.drawCircle(circleX, circleY, AREA_RADIUS, mOuterAreaPaint);

        /*if(mShowText)
            canvas.drawText(Math.round(mValue)+"", textX, textY, mTextPaint);*/
    }

    /*
        *
        * Here we find current size of the item and
        * update everything accordingly.
        *
    */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());

        width = (float)w - xpad;
        height = (float)h - ypad;

        setMeasuredDimension((int) width,(int) height);

        radius = Math.min(Math.min(width, height) / 2.5f,
                (Math.min(width, height) - circles.get(State.FOCUSED)*2 - STROKE_WIDTH*2) / 2);
        centerX = width / 2.0f;
        centerY = height / 2.0f;

        update();
    }

    /*
        *
        * Here we update all the changing values.
        *
    */
    private void update(){
        arc.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        float angle = mValue * mMaxAngle / mMaxValue;

        circleX = centerX + radius*(float)Math.cos(Math.toRadians(90 + mMaxAngle / 2 - angle - mRotation));
        circleY = centerY - radius*(float)Math.sin(Math.toRadians(90 + mMaxAngle / 2 - angle - mRotation));

        /*Rect mTextBounds = new Rect();
        mTextPaint.getTextBounds(Math.round(mValue)+"",0,(Math.round(mValue)+"").length(),mTextBounds);
        float textWidth = mTextPaint.measureText(Math.round(mValue)+"");
        float textHeight = mTextBounds.height();
        textX = centerX - textWidth / 2.0f;
        textY = centerY + textHeight / 2f;*/

        if(!mEnabled){
            mLinePaint.setColor(mColorAccentDark);
            mCirclePaint.setColor(mColorAccentDark);
            mLinePaint.setStrokeWidth(DARK_STROKE_WIDTH);

            stopAngle = Math.max(angle - 1.5f * (float)(Math.toDegrees(Math.asin(CIRCLE_RADIUS / radius))), 0);
            startAngle = Math.min(angle + 1.5f * (float)(Math.toDegrees(Math.asin(CIRCLE_RADIUS / radius))), mMaxAngle);
        } else {
            mLinePaint.setColor(mColorAccent);
            mCirclePaint.setColor(mColorAccent);
            mLinePaint.setStrokeWidth(STROKE_WIDTH);

            stopAngle = angle  /*- (float)(Math.toDegrees(Math.asin(CIRCLE_RADIUS / radius)))*/;
            startAngle = angle /*+ (float)(Math.toDegrees(Math.asin(CIRCLE_RADIUS / radius)))*/;
        }
    }

    /*
        *
        * Here we draw everything according to user
        * touches.
        *
    */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:{
                if((Math.pow(event.getX() - circleX, 2) + Math.pow(event.getY() - circleY, 2)
                        <= 4 * CIRCLE_RADIUS * CIRCLE_RADIUS && mEnabled && !mFocused) ||
                    (Math.pow(event.getX() - circleX, 2) + Math.pow(event.getY() - circleY, 2)
                            <= Math.pow(circles.get(State.FOCUSED), 2) && mEnabled && mFocused)) {
                    mDown = true;
                    if(!mFocused)
                        mClickAnimator.start();
                    if(mListener != null)
                        mListener.OnStartTrackingTouch(this);
                }
                else
                    break;
            }
            case MotionEvent.ACTION_MOVE: {
                if(!mDown || !mEnabled)
                    break;

                float distX = event.getX() - centerX, distY = event.getY() - centerY;
                float angle = (float) (Math.atan2(-distY, distX));
                angle = (float) (angle / (Math.PI * 2.0f) * 360) - mRotation;
                while (angle < -90) angle += 360;

                float newValue = (90 + mMaxAngle / 2 - angle) * mMaxValue / mMaxAngle;
                newValue = newValue < 0 ? 0 : newValue;
                newValue = newValue > mMaxValue ? mMaxValue : newValue;

                if(Math.abs(newValue - mValue) < 0.5f * mMaxValue) {
                    mValue = newValue;
                    if(mListener != null)
                        mListener.OnValueChanged(mValue, true);
                    update();
                    invalidate();
                }

                return true;
            }
            case MotionEvent.ACTION_UP:
                if(mDown && !mFocused)
                    mUnClickAnimator.start();
                mDown = false;
                if(mListener != null)
                    mListener.OnStopTrackingTouch(this);
                invalidate();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mFocused)
            switch(keyCode){
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if(mValue < mMaxValue){
                        mValue++;
                        if(mListener != null)
                            mListener.OnValueChanged(mValue, true);
                        update();
                        invalidate();
                    }
                    mDown = true;
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if(mValue > 0){
                        mValue--;
                        if(mListener != null)
                            mListener.OnValueChanged(mValue, true);
                        update();
                        invalidate();
                    }
                    mDown = true;
                    return true;
            }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mDown = false;
            return true;
        }
        return false;
    }

    /*
            *
            * Here we find out if focus of the object has changed,
            * assign value accordingly and update everything else.
            *
        */
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if(gainFocus) {
            mFocusAnimator.start();
            mBounceAnimator.start();
            mFocused = true;
        }
        else
            mUnFocusAnimator.start();
        update();
        invalidate();
    }



    public void setOnCircleSliderChangeListener(OnCircleSliderChangeListener listener){
        mListener = listener;
    }


    /*
        *
        * Getters and setters of parameters.
        *
    */
    public float getValue(){
        return mValue;
    }

    public void setValue(int value){
        mValue = value;
        if(mListener != null)
            mListener.OnValueChanged(mValue, false);
        update();
        invalidate();
    }

    public int getMaxValue(){
        return (int)mMaxValue;
    }

    public void setMaxValue(int value){
        mMaxValue = value;
        update();
        invalidate();
    }

    /*public float getTextSize(){
        return mTextSize;
    }

    public void setTextSize(int textSize){
        mTextSize = textSize;
        update();
        invalidate();
    }

    public boolean isShownText(){
        return mShowText;
    }

    public void setShownText(boolean shownText){
        mShowText = shownText;
        invalidate();
    }

    public int getTextColor(){
        return mTextColor;
    }

    public void setTextColor(int color){
        mTextColor = color;
        mTextPaint.setColor(mTextColor);
        invalidate();
    }*/

    @Override
    public boolean isEnabled(){
        return mEnabled;
    }

    @Override
    public void setEnabled(boolean enabled){
        super.setEnabled(enabled);

        if(enabled)
            CIRCLE_RADIUS = circles.get(State.NORMAL);
        else
            CIRCLE_RADIUS = circles.get(State.DISABLED);

        mEnabled = enabled;
        update();
        invalidate();
    }

    @Override
    public boolean isFocused(){
        return mFocused;
    }

    public void setFocused(boolean focused){
        if(focused) {
            mFocusAnimator.start();
            mBounceAnimator.start();
            mFocused = true;
        }
        else
            mUnFocusAnimator.start();
    }

    public int getSliderRotation(){
        return mRotation;
    }

    public void setSliderRotation(int rotation){
        mRotation = rotation;
        update();
        invalidate();
    }

    public int getMaxAngle(){
        return mMaxAngle;
    }

    public void setMaxAngle(int angle){
        mMaxAngle = angle;
        update();
        invalidate();
    }

    public void setPlayAnimationIntro(boolean play){
        mPlayAnimationIntro = play;
    }

    public float getCurrentWidth(){
        return width;
    }

    public float getCurrentHeight(){
        return height;
    }

    /*
        *
        * It is a custom listener interface similar to OnSeekBarChangeListener.
        *
    */
    interface OnCircleSliderChangeListener{
        void OnValueChanged(float newValue, boolean fromUser);

        void OnStartTrackingTouch(CircleSlider circleSlider);

        void OnStopTrackingTouch(CircleSlider circleSlider);
    }
}
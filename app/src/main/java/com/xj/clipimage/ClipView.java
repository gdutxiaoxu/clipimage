package com.xj.clipimage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


/**
 * Created by jun xu on 19-4-8
 */
public class ClipView extends View {

    private static final String TAG = "ClipView";

    public static final int TYPE_ROUND = 1;
    public static final int TYPE_RECT = 2;
    public static final int TYPE_PALACE = 3;


    private Paint paint = new Paint();
    //画裁剪区域边框的画笔
    private Paint borderPaint = new Paint();

    //裁剪框水平方向间距
    private float mHorizontalPadding;
    //裁剪框边框宽度
    private int clipBorderWidth;
    //裁剪圆框的半径
    private int clipRadiusWidth;
    //裁剪框矩形宽度
    private int clipWidth;

    //裁剪框类别，（圆形、矩形），默认为圆形
    private ClipType clipType = ClipType.CIRCLE;
    private Xfermode xfermode;
    private Context context;

    // 九宫格相关
    //裁剪框边框画笔
    private Paint mPalaceBorderPaint;
    //裁剪框九宫格画笔
    private Paint mGuidelinePaint;
    //绘制裁剪边框四个角的画笔
    private Paint mCornerPaint;
    private float mScaleRadius;
    private float mCornerThickness;
    private float mBorderThickness;
    //四个角小短边的长度
    private float mCornerLength;

    public ClipView(Context context) {
        this(context, null);
    }

    public ClipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //去锯齿
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
        borderPaint.setStyle(Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(clipBorderWidth);
        borderPaint.setAntiAlias(true);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        this.context = context;
        initData();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.i(TAG, "onDraw: clipType =" + clipType);

        //通过Xfermode的DST_OUT来产生中间的透明裁剪区域，一定要另起一个Layer（层）
        canvas.saveLayer(0, 0, this.getWidth(), this.getHeight(), null, Canvas.ALL_SAVE_FLAG);

        //设置背景
        canvas.drawColor(Color.parseColor("#a8000000"));
        paint.setXfermode(xfermode);
        //绘制圆形裁剪框
        if (clipType == ClipType.CIRCLE) {
            //中间的透明的圆
            canvas.drawCircle(this.getWidth() / 2, this.getHeight() / 2, clipRadiusWidth, paint);
            //白色的圆边框
            canvas.drawCircle(this.getWidth() / 2, this.getHeight() / 2, clipRadiusWidth, borderPaint);
        } else if (clipType == ClipType.RECTANGLE) { //绘制矩形裁剪框
            //绘制中间白色的矩形蒙层
            canvas.drawRect(mHorizontalPadding, this.getHeight() / 2 - clipWidth / 2,
                    this.getWidth() - mHorizontalPadding, this.getHeight() / 2 + clipWidth / 2, paint);
            //绘制白色的矩形边框
            canvas.drawRect(mHorizontalPadding, this.getHeight() / 2 - clipWidth / 2,
                    this.getWidth() - mHorizontalPadding, this.getHeight() / 2 + clipWidth / 2, borderPaint);
        } else if (clipType == ClipType.PALACE) {
            //绘制中间的矩形
            canvas.drawRect(mHorizontalPadding, this.getHeight() / 2 - clipWidth / 2,
                    this.getWidth() - mHorizontalPadding, this.getHeight() / 2 + clipWidth / 2, paint);

            Rect clipRect = getClipRect();
            //绘制裁剪边框
            drawBorder(canvas, clipRect);
            //绘制九宫格引导线
            drawGuidelines(canvas, clipRect);
            //绘制裁剪边框的四个角
            drawCorners(canvas, clipRect);
        }
        //出栈，恢复到之前的图层，意味着新建的图层会被删除，新建图层上的内容会被绘制到canvas (or the previous layer)
        canvas.restore();
    }

    /**
     * 获取裁剪区域的Rect
     *
     * @return
     */
    public Rect getClipRect() {
        Rect rect = new Rect();
        //宽度的一半 - 圆的半径
        rect.left = (this.getWidth() / 2 - clipRadiusWidth);
        //宽度的一半 + 圆的半径
        rect.right = (this.getWidth() / 2 + clipRadiusWidth);
        //高度的一半 - 圆的半径
        rect.top = (this.getHeight() / 2 - clipRadiusWidth);
        //高度的一半 + 圆的半径
        rect.bottom = (this.getHeight() / 2 + clipRadiusWidth);
        return rect;
    }

    /**
     * 设置裁剪框边框宽度
     *
     * @param clipBorderWidth
     */
    public void setClipBorderWidth(int clipBorderWidth) {
        this.clipBorderWidth = clipBorderWidth;
        borderPaint.setStrokeWidth(clipBorderWidth);
        invalidate();
    }

    /**
     * 设置裁剪框水平间距
     *
     * @param mHorizontalPadding
     */
    public void setmHorizontalPadding(float mHorizontalPadding) {
        this.mHorizontalPadding = mHorizontalPadding;
        this.clipRadiusWidth = (int) (getScreenWidth(getContext()) - 2 * mHorizontalPadding) / 2;
        this.clipWidth = clipRadiusWidth * 2;
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public void setClipType(int type) {
        switch (type) {
            case TYPE_ROUND:
                this.setClipType(ClipView.ClipType.CIRCLE);
                break;
            case TYPE_RECT:
                this.setClipType(ClipView.ClipType.RECTANGLE);
                break;
            case TYPE_PALACE:
                this.setClipType(ClipView.ClipType.PALACE);
                break;
            default:
                this.setClipType(ClipView.ClipType.PALACE);
                break;
        }
    }


    /**
     * 设置裁剪框类别
     *
     * @param clipType
     */
    public void setClipType(ClipType clipType) {
        this.clipType = clipType;
    }

    /**
     * 裁剪框类别，圆形、矩形
     */
    public enum ClipType {
        CIRCLE(1), RECTANGLE(2), PALACE(3);

        private int value;

        ClipType(int value) {
            value = value;
        }
    }


    private void initData() {
        mPalaceBorderPaint = new Paint();
        mPalaceBorderPaint.setStyle(Style.STROKE);
        mPalaceBorderPaint.setStrokeWidth(dip2px(context, 1.0f));
        mPalaceBorderPaint.setColor(Color.parseColor("#FFFFFF"));

        mGuidelinePaint = new Paint();
        mGuidelinePaint.setStyle(Style.STROKE);
        mGuidelinePaint.setStrokeWidth(dip2px(context, 1.0f));
        mGuidelinePaint.setColor(Color.parseColor("#FFFFFF"));


        mCornerPaint = new Paint();
        mCornerPaint.setStyle(Style.STROKE);
        mCornerPaint.setStrokeWidth(dip2px(context, 3.5f));
        mCornerPaint.setColor(Color.WHITE);


        mScaleRadius = dip2px(context, 24);
        mBorderThickness = mPalaceBorderPaint.getStrokeWidth();
        mCornerThickness = mCornerPaint.getStrokeWidth();
        mCornerLength = dip2px(context, 25);
    }

    private void drawGuidelines(@NonNull Canvas canvas, Rect clipRect) {

        final float left = clipRect.left;
        final float top = clipRect.top;
        final float right = clipRect.right;
        final float bottom = clipRect.bottom;

        final float oneThirdCropWidth = (right - left) / 3;

        final float x1 = left + oneThirdCropWidth;
        //引导线竖直方向第一条线
        canvas.drawLine(x1, top, x1, bottom, mGuidelinePaint);
        final float x2 = right - oneThirdCropWidth;
        //引导线竖直方向第二条线
        canvas.drawLine(x2, top, x2, bottom, mGuidelinePaint);

        final float oneThirdCropHeight = (bottom - top) / 3;

        final float y1 = top + oneThirdCropHeight;
        //引导线水平方向第一条线
        canvas.drawLine(left, y1, right, y1, mGuidelinePaint);
        final float y2 = bottom - oneThirdCropHeight;
        //引导线水平方向第二条线
        canvas.drawLine(left, y2, right, y2, mGuidelinePaint);
    }

    private void drawBorder(@NonNull Canvas canvas, Rect clipRect) {

        canvas.drawRect(clipRect.left,
                clipRect.top,
                clipRect.right,
                clipRect.bottom,
                mPalaceBorderPaint);
    }


    private void drawCorners(@NonNull Canvas canvas, Rect clipRect) {

        final float left = clipRect.left;
        final float top = clipRect.top;
        final float right = clipRect.right;
        final float bottom = clipRect.bottom;

        //简单的数学计算
        final float lateralOffset = (mCornerThickness - mBorderThickness) / 2f;
        final float startOffset = mCornerThickness - (mBorderThickness / 2f);

        //左上角左面的短线
        canvas.drawLine(left - lateralOffset, top - startOffset, left - lateralOffset, top + mCornerLength, mCornerPaint);
        //左上角上面的短线
        canvas.drawLine(left - startOffset, top - lateralOffset, left + mCornerLength, top - lateralOffset, mCornerPaint);

        //右上角右面的短线
        canvas.drawLine(right + lateralOffset, top - startOffset, right + lateralOffset, top + mCornerLength, mCornerPaint);
        //右上角上面的短线
        canvas.drawLine(right + startOffset, top - lateralOffset, right - mCornerLength, top - lateralOffset, mCornerPaint);

        //左下角左面的短线
        canvas.drawLine(left - lateralOffset, bottom + startOffset, left - lateralOffset, bottom - mCornerLength, mCornerPaint);
        //左下角底部的短线
        canvas.drawLine(left - startOffset, bottom + lateralOffset, left + mCornerLength, bottom + lateralOffset, mCornerPaint);

        //右下角左面的短线
        canvas.drawLine(right + lateralOffset, bottom + startOffset, right + lateralOffset, bottom - mCornerLength, mCornerPaint);
        //右下角底部的短线
        canvas.drawLine(right + startOffset, bottom + lateralOffset, right - mCornerLength, bottom + lateralOffset, mCornerPaint);
    }

    public static int dip2px(@Nullable Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}

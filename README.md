
![Android 技术人](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/20190402200538.png)

**扫一扫，欢迎关注我的公众号。如果你有好的文章，也欢迎你的投稿。**


不少人反馈 BaseLibrary  这个库无法获取，解决方法见 [issue](https://github.com/gdutxiaoxu/clipimage/issues/2)

---


## 前言

在平时开发中，经常需要实现这样的功能，拍照 - 裁剪，相册 - 裁剪。当然，系统也有裁剪的功能，但是由于机型，系统兼容性等问题，在实际开发当中，我们通常会自己进行实现。今天，就让我们一起来看看怎样实现。

这篇博客实现的功能主要有仿微信，QQ 上传图像裁剪功能，包括拍照，从相册选取。裁剪框的样式有圆形，正方形，九宫格。

主要讲解的功能点
1. 使用说明
2. 整体的实现思路
3. 裁剪框的实现
4. 图片缩放的实现，包括放大，缩小，移动，裁剪等

我们先来看看我们实现的效果图

拍照裁剪的
![拍照裁剪的](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/camera.gif)

相册裁剪的
![相册裁剪的](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/album.gif)


---

## 使用说明

有两种调用方式


### 第一种

第一种，使用普通的 startActivityForResult 进行调用，并重写 onActivityResult 方法，在里面根据 requestCode 进行处理

```
ClipImageActivity.goToClipActivity(this, uri);
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    switch (requestCode) {
        case REQ_CLIP_AVATAR:  //剪切图片返回
            if (resultCode == RESULT_OK) {
                final Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                String cropImagePath = FileUtil.getRealFilePathFromUri(getApplicationContext(), uri);
				
				
	----
				
}

```

### 第二种

第二种调用 ClipImageActivity.goToClipActivity 方法，结果以 callBack 回调的方式返回回来，这种看起来比较直观点，个人也比较喜欢这种方法。它的实现原理是通过空白的 fragment 处理实现的，有兴趣的可以看我这一篇博客 [Android Fragment 的妙用 - 优雅地申请权限和处理 onActivityResult](https://blog.csdn.net/gdutxiaoxu/article/details/86498647)

```
ClipImageActivity.goToClipActivity(this, uri, new ActivityResultHelper.Callback() {
    @Override
    public void onActivityResult(int resultCode, Intent data) {
        
    }
});

```

---

## 整体实现思路

从上面的效果图我们可以看到，裁剪功能主要包括两大块
1. 裁剪框
2. 图片的缩放，移动，裁剪等

因此，为了方便日后的修改，我们将裁剪框的功能单独提取出来，图片缩放功能提出出来。即裁剪框单独一个 View。

下面，让我们一起来看看裁剪框功能的实现。

---

## 裁剪框功能的实现

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/20190425202444.png)

裁剪框主要有两层，第一层，裁剪框的实现（包括圆形，长方形，九宫格形状），第二层，在裁剪区域上面盖上一层蒙层。

### 蒙层

蒙层的实现我们是通过 Xfermode 实现的

```
xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);


//通过Xfermode的DST_OUT来产生中间的透明裁剪区域，一定要另起一个Layer（层）
canvas.saveLayer(0, 0, this.getWidth(), this.getHeight(), null, Canvas.ALL_SAVE_FLAG);

//设置背景
canvas.drawColor(Color.parseColor("#a8000000"));
paint.setXfermode(xfermode);

```



### 圆形裁剪框的实现

绘制圆形裁剪框很容易实现，主要确定圆心和半径即可



```
//中间的透明的圆
canvas.drawCircle(this.getWidth() / 2, this.getHeight() / 2, clipRadiusWidth, paint);
//白色的圆边框
canvas.drawCircle(this.getWidth() / 2, this.getHeight() / 2, clipRadiusWidth, borderPaint);
```

### 正方形裁剪框的实现

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/20190425202353.png)

绘制长方形的话主要要确定四个点的坐标 left ，top， right， botom。
很简单


```
left = mHorizontalPadding；
top = this.getHeight() / 2 - clipWidth / 2；
right =   this.getWidth() - mHorizontalPadding；
botom = this.getHeight() / 2 + clipWidth / 2；
```


```
//绘制中间白色的矩形蒙层
canvas.drawRect(mHorizontalPadding, this.getHeight() / 2 - clipWidth / 2,
        this.getWidth() - mHorizontalPadding, this.getHeight() / 2 + clipWidth / 2, paint);
//绘制白色的矩形边框
canvas.drawRect(mHorizontalPadding, this.getHeight() / 2 - clipWidth / 2,
        this.getWidth() - mHorizontalPadding, this.getHeight() / 2 + clipWidth / 2, borderPaint);
```

### 九宫格的

![](https://raw.githubusercontent.com/gdutxiaoxu/blog_pic/master/19_04/20190425203737.png)

九宫格的绘制稍微繁琐一点，分三个步骤
1. 绘制长方形边框
2. 绘制九宫格引导线
3. 绘制裁剪边框的是个直角


我们来看一下绘制九宫格引导线的
- 绘制竖直方向两条线
- 绘制水平方向两条线


```java
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
    can

```

绘制四个直角的

```javascript
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


```

图片裁剪框的实现到此讲解完毕，更多细节请参考 ClipView

---

## 图片的缩放，移动

### 实现原理简述

这里我们是通过 ClipViewLayout 实现的，它是 RelativeLayout 的子类，里面含有 ImageView 和 ClipView（裁剪框）。我们通过监听 ClipViewLayout 的 onTouchEvent 事件，设置 imageView 的图片矩阵。

我们先来了解一下，主要有三种模式，NONE，DRAG， ZOOM。NONE 表示初始模式，DRAG 表示拖拽模式，ZOOM 表示缩放模式


```
private static final int NONE = 0;
//动作标志：拖动
private static final int DRAG = 1;
//动作标志：缩放
private static final int ZOOM = 2;
```

当我们多个手指按下的时候，加入两个手指之间的距离超过 10，此时我们认为进入 ZOOM 模式。DRAG 模式的话当我们手指按下的时候进入。NONE 模式，当我们手机抬起的时候，进入复位模式。

```
switch (event.getAction() & MotionEvent.ACTION_MASK) {
    case MotionEvent.ACTION_DOWN:
        Log.d(TAG, "onTouchEvent: ACTION_DOWN");
        mSavedMatrix.set(mMatrix);
        //设置开始点位置
        mStart.set(event.getX(), event.getY());
        mode = DRAG;
        break;
    case MotionEvent.ACTION_POINTER_DOWN:
        //开始放下时候两手指间的距离
        mOldDist = spacing(event);
        if (mOldDist > 10f) {
            mSavedMatrix.set(mMatrix);
            midPoint(mMid, event);
            mode = ZOOM;
        }
        break;
    case MotionEvent.ACTION_UP:
    case MotionEvent.ACTION_POINTER_UP:
        mode = NONE;
        break;


```




接下来我们一起来看一下，我们 action_move 的时候，我们是怎样进行移动和缩放的。

移动的话相对比较简单，首先它会计算出我们这一次 event 事件相对我们 action_down 时候 event 事件的偏移量 dx， dy。接着调用 mMatrix.postTranslate(dx, dy)，进行矩阵的移动。最后，再检测是否超出边界。


```
case MotionEvent.ACTION_MOVE:
    Log.d(TAG, "onTouchEvent: mode =" + mode);
    if (mode == DRAG) { //拖动
        mMatrix.set(mSavedMatrix);
        float dx = event.getX() - mStart.x;
        float dy = event.getY() - mStart.y;

        mVerticalPadding = mClipView.getClipRect().top;
        mMatrix.postTranslate(dx, dy);
        //检查边界
        checkBorder();
    } 
	---
    mImageView.setImageMatrix(mMatrix);

```

边界检测 主要是检查缩放，移动后的图片矩阵的 left， top，right， bottom 是否在图片裁剪框之内，如果在的话，需要对图片矩阵进行移动。确保边界不在裁剪框之内。

```
/**
 * 边界检测
 */
private void checkBorder() {
    RectF rect = getMatrixRectF(mMatrix);
    float deltaX = 0;
    float deltaY = 0;
    int width = mImageView.getWidth();
    int height = mImageView.getHeight();
    // 如果宽或高大于屏幕，则控制范围 ; 这里的0.001是因为精度丢失会产生问题，但是误差一般很小，所以我们直接加了一个0.01
    if (rect.width() + 0.01 >= width - 2 * mHorizontalPadding) {
        // 图片矩阵的最左边 > 裁剪边框的左边
        if (rect.left > mHorizontalPadding) {
            deltaX = -rect.left + mHorizontalPadding;
        }
        // 图片矩阵的最右边 < 裁剪边框的右边
        if (rect.right < width - mHorizontalPadding) {
            deltaX = width - mHorizontalPadding - rect.right;
        }
    }
    if (rect.height() + 0.01 >= height - 2 * mVerticalPadding) {
        // 图片矩阵的 top > 裁剪边框的 top
        if (rect.top > mVerticalPadding) {
            deltaY = -rect.top + mVerticalPadding;
        }
        // 图片矩阵的 bottom < 裁剪边框的 bottom
        if (rect.bottom < height - mVerticalPadding) {
            deltaY = height - mVerticalPadding - rect.bottom;
        }
    }

    Log.i(TAG, "checkBorder: deltaX=" + deltaX + " deltaY = " + deltaY);

    mMatrix.postTranslate(deltaX, deltaY);
}
```

裁剪功能的实现

```
/**
 * 获取剪切图
 */
public Bitmap clip() {
    mImageView.setDrawingCacheEnabled(true);
    mImageView.buildDrawingCache();
    Rect rect = mClipView.getClipRect();
    Bitmap cropBitmap = null;
    Bitmap zoomedCropBitmap = null;
    try {
        cropBitmap = Bitmap.createBitmap(mImageView.getDrawingCache(), rect.left, rect.top, rect.width(), rect.height());
		// 对图片进行压缩，这里因为 mPreViewW 与宽高是相等的，所以压缩比例是 1:1，可以根据需要自己调整
        zoomedCropBitmap = BitmapUtil.zoomBitmap(cropBitmap, mPreViewW, mPreViewW);
    } catch (Exception e) {
        e.printStackTrace();
    }
    if (cropBitmap != null) {
        cropBitmap.recycle();
    }
    // 释放资源
    mImageView.destroyDrawingCache();
    return zoomedCropBitmap;
}
```

---

## 题外话

这个 Demo 涉及到的 Android 技术点其实是蛮多的，可以说是麻雀虽小，五脏俱全。Android 7.0 图片拍照适配，6.0 动态权限申请，Android 使用空白 fragment  处理  onActivityResult，动态权限申请，自定义 View，View 的事件分发机制等等。

这篇博客主要是介绍个人认为比较重要的技术点，其他的可以自行取了解。最后，提供一下 demo 下载地址： https://github.com/gdutxiaoxu/clipimage


### 特别鸣谢

文中很多代码参考了以下两篇文章，在他们的基础之上进行了修改。由于时间的关系，并没有对裁剪框进行更多细节化的定制，比如图片比例，自定义属性的暴露等，有兴趣的话可以自己进行添加

[android-headimage-cliper](https://github.com/wsy858/android-headimage-cliper)

[Android 高仿微信头像截取 打造不一样的自定义控件](https://blog.csdn.net/lmj623565791/article/details/39761281)

github 源码地址

[clipimage](https://github.com/gdutxiaoxu/clipimage)




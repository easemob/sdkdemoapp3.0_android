package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

import com.hyphenate.util.EMLog;

/**
 * Created by lzan13 on 2017/3/25
 * <p>
 * 自定义ViewGroup类，会根据子控件的宽度自动换行，
 */
public class MemberViewGroup extends ViewGroup {
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnScreenModeChangeListener {
        /**
         * @param isFullScreenMode
         * @param fullScreenView   Is null if {isFullScreenMode} is false.
         */
        void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView);
    }

    public interface OnPageStatusListener {
        void onPageCountChange(int count);

        void onPageScroll(int page);
    }

    private static final String TAG = "EaseViewGroup1";

    private static final int MAX_SIZE_PER_PAGE = 9;

    private OnItemClickListener onItemClickListener;
    private OnScreenModeChangeListener onScreenModeChangeListener;
    private OnPageStatusListener onPageStatusListener;
    //滚动计算辅助类
    private Scroller mScroller;
    private float mLastMotionX = 0;
    private PointF actionDownPoint = new PointF();
    private int pageWidth = 0;
    private int screenHeight = 0;
    // current page index.
    private int pageIndex = 0;
    private int pageCount = 0;
    private View fullScreenView;

    // Android系统认为touch事件为滑动事件的最小距离
    int touchSlop;

    public MemberViewGroup(Context context) {
        super(context);
        init();
    }

    public MemberViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MemberViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //初始化辅助类
        mScroller = new Scroller(getContext());

        //测量屏幕宽高,该view默认为全屏,若当前view不为全屏,则该计算方式需要修改.
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Point p = new Point();
        wm.getDefaultDisplay().getSize(p);
        pageWidth = p.x;
        screenHeight = p.y;

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        calculatePageCount();
        if (isFullScreenMode()) {
            EMLog.i(TAG, "addView, isFullScreenMode: " + isFullScreenMode());
            // 全屏模式下不进行子view的大小设置和滑动
            return;
        }
        if (pageCount == 1) {
            calculateChildrenParamsAndSet();
        } else {
            int viewBorder = pageWidth / 3;
            setViewParams(child, viewBorder);
        }
        int index = pageCount - 1;
        // Always scroll to the last page.
        scrollTo(index, false);
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        calculatePageCount();
        int index = pageIndex == pageCount ? --pageIndex : pageIndex;
        if (isFullScreenMode()) {
            if (fullScreenView == view) {
                // 全屏状态,并且当前全屏view被移除
                resetAllViews(view);
            }
        } else {
            if (pageCount == 1) {
                calculateChildrenParamsAndSet();
            }
            scrollTo(index, false);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //子控件的个数
        int count = getChildCount();
        //ViewParent 的右边x的布局限制值
        int rightLimit = pageWidth;

        //存储基准的left top (子类.layout(),里的坐标是基于父控件的坐标，所以 x应该是从0+父控件左内边距开始，y从0+父控件上内边距开始)
        int baseLeft = 0;
        int baseTop = 0;
        //存储现在的left top
        int curLeft = baseLeft;
        int curTop = baseTop;

        //子View
        View child = null;
        //子view用于layout的 l t r b
        int viewL, viewT, viewR, viewB;
        //子View的LayoutParams
        MarginLayoutParams params = null;
        //子View Layout需要的宽高(包含margin)，用于计算是否越界
        int childWidth;
        int childHeight;
        //子View 本身的宽高
        int childW, childH;

        //临时增加一个temp 存储上一个View的高度 解决过长的两行View导致显示不正确的bug
        int lastChildHeight = 0;
        //
        for (int i = 0; i < count; i++) {
            if ((i + 1) % MAX_SIZE_PER_PAGE == 1) {// A new page start.
                int p = (i + 1) / MAX_SIZE_PER_PAGE;// current page.
                baseLeft = pageWidth * p + getPaddingLeft();
                baseTop = getPaddingTop();
                rightLimit = pageWidth * (p + 1);

                curLeft = baseLeft;
                curTop = baseTop;
            }

            child = getChildAt(i);
            //如果gone，不布局了
            if (View.GONE == child.getVisibility()) {
                continue;
            }
            //获取子View本身的宽高:
            childW = child.getMeasuredWidth();
            childH = child.getMeasuredHeight();
            //获取子View的LayoutParams，用于获取其margin
            params = (MarginLayoutParams) child.getLayoutParams();
            //子View需要的宽高 为 本身宽高+marginLeft + marginRight
            childWidth = childW + params.leftMargin + params.rightMargin;
            childHeight = childH + params.topMargin + params.bottomMargin;

            //这里要考虑padding，所以右边界为 ViewParent宽度(包含padding) -ViewParent右内边距
            if (curLeft + childWidth > rightLimit) {
                //如果当前行已经放不下该子View了 需要换行放置：
                //在新的一行布局子View，左x就是baseLeft，上y是 top +前一行高(这里假设的是每一行行高一样)，
                curTop = curTop + lastChildHeight;
                //layout时要考虑margin
                viewL = baseLeft + params.leftMargin;
                viewT = curTop + params.topMargin;
                viewR = viewL + childW;
                viewB = viewT + childH;
                //child.layout(baseLeft + params.leftMargin, curTop + params.topMargin, baseLeft + params.leftMargin + child.getMeasuredWidth(), curTop + params.topMargin + child.getMeasuredHeight());
                //EMLog.i(TAG,"新的一行:" +"   ,baseLeft:"+baseLeft +"  curTop:"+curTop+"  baseLeft+childWidth:"+(baseLeft+childWidth)+"  curTop+childHeight:"+ ( curTop+childHeight));
                curLeft = baseLeft + childWidth;
            } else {
                //当前行可以放下子View:
                viewL = curLeft + params.leftMargin;
                viewT = curTop + params.topMargin;
                viewR = viewL + childW;
                viewB = viewT + childH;
                //child.layout(curLeft + params.leftMargin, curTop + params.topMargin, curLeft + params.leftMargin + child.getMeasuredWidth(), curTop + params.topMargin + child.getMeasuredHeight());
                //EMLog.i(TAG,"当前行:"+changed +"   ,curLeft:"+curLeft +"  curTop:"+curTop+"  curLeft+childWidth:"+(curLeft+childWidth)+"  curTop+childHeight:"+(curTop+childHeight));
                curLeft = curLeft + childWidth;
            }
            lastChildHeight = childHeight;
            //布局子View
            child.layout(viewL, viewT, viewR, viewB);
        }
    }

    /**
     * 重写onMeasure方法，这里循环设置当前自定义控件的子控件的大小
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取系统传递过来测量出的宽度 高度，以及相应的测量模式。
        //如果测量模式为 EXACTLY( 确定的dp值，match_parent)，则可以调用setMeasuredDimension()设置，
        //如果测量模式为 AT_MOST(wrap_content),则需要经过计算再去调用setMeasuredDimension()设置
        int widthMeasure = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasure = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        //计算宽度 高度 //wrap_content测量模式下会使用到:
        //存储最后计算出的宽度，
        int maxLineWidth = 0;
        //存储最后计算出的高度
        int totalHeight = 0;
        //存储当前行的宽度
        int curLineWidth = 0;
        //存储当前行的高度
        int curLineHeight = 0;

        // 得到内部元素的个数
        int count = getChildCount();

        //存储子View
        View child = null;
        //存储子View的LayoutParams
        MarginLayoutParams params = null;
        //子View Layout需要的宽高(包含margin)，用于计算是否越界
        int childWidth;
        int childHeight;

        //遍历子View 计算父控件宽高
        for (int i = 0; i < count; i++) {
            child = getChildAt(i);
            //如果gone，不测量了
            if (View.GONE == child.getVisibility()) {
                continue;
            }
            //先测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            //获取子View的LayoutParams，(子View的LayoutParams的对象类型，取决于其ViewGroup的generateLayoutParams()方法的返回的对象类型，这里返回的是MarginLayoutParams)
            params = (MarginLayoutParams) child.getLayoutParams();
            //子View需要的宽度 为 子View 本身宽度+marginLeft + marginRight
            childWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
            childHeight = child.getMeasuredHeight() + params.topMargin + params.bottomMargin;

            //如果当前的行宽度大于 父控件允许的最大宽度 则要换行
            //父控件允许的最大宽度 如果要适配 padding 这里要- getPaddingLeft() - getPaddingRight()
            //即为测量出的宽度减去父控件的左右边距
            if (curLineWidth + childWidth > widthMeasure - getPaddingLeft() - getPaddingRight()) {
                //通过比较 当前行宽 和以前存储的最大行宽,得到最新的最大行宽,用于设置父控件的宽度
                maxLineWidth = Math.max(maxLineWidth, curLineWidth);
                //父控件的高度增加了，为当前高度+当前行的高度
                totalHeight += curLineHeight;
                //换行后 刷新 当前行 宽高数据： 因为新的一行就这一个View，所以为当前这个view占用的宽高(要加上View 的 margin)
                curLineWidth = childWidth;
                curLineHeight = childHeight;
            } else {
                //不换行：叠加当前行宽 和 比较当前行高:
                curLineWidth += childWidth;
                curLineHeight = Math.max(curLineHeight, childHeight);
            }
            //如果已经是最后一个View,要比较当前行的 宽度和最大宽度，叠加一共的高度
            if (i == count - 1) {
                maxLineWidth = Math.max(maxLineWidth, curLineWidth);
                totalHeight += childHeight;
            }
        }

        //适配padding,如果是wrap_content,则除了子控件本身占据的控件，还要在加上父控件的padding
        int measuredWidth = widthMode != MeasureSpec.EXACTLY ? maxLineWidth + getPaddingLeft() + getPaddingRight() : widthMeasure;
        setMeasuredDimension(
                measuredWidth * pageCount,
                heightMode != MeasureSpec.EXACTLY ? totalHeight + getPaddingTop() + getPaddingBottom() : heightMeasure);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mScroller.isFinished()) {
            return true;
        }
        int action = event.getAction();
        //判断触发的时间
        switch (action) {
            //按下事件
            case MotionEvent.ACTION_DOWN:
                actionDownPoint.x = event.getX();
                actionDownPoint.y = event.getY();
                EMLog.i(TAG, "onInterceptTouchEvent ACTION_DOWN: downPointX: " + actionDownPoint.x);
                //获取现在的x坐标
                mLastMotionX = event.getX();
                break;
            //移动事件
            case MotionEvent.ACTION_MOVE:
                if (isFullScreenMode()) { // 全屏状态下禁止屏幕跟随指头滑动
                    return true;
                }

                float x = event.getX();
                EMLog.i(TAG, "onInterceptTouchEvent: move action: " + x);
                //计算移动的偏移量
                float delt = mLastMotionX - x;
                //重置手指位置
                mLastMotionX = x;

                if ((pageIndex == 0 && delt < 0) || (pageIndex == pageCount - 1 && delt > 0)) {
                    delt /= 5;
                }

                //滚动
                scrollBy((int) delt, 0);
                break;
            //手指抬起事件
            case MotionEvent.ACTION_UP:
                EMLog.i(TAG, "onTouchEvent: " + getScrollX());
                float delta = actionDownPoint.x - event.getX();
                if (Math.abs(delta) < touchSlop && Math.abs(actionDownPoint.y - event.getY()) < touchSlop) {
                    // 滑动距离小于系统设置的最小距离,当作点击事件处理
                    performClick((int) event.getX(), (int) event.getY());
                } else { // 页面需要滑动
                    if (isFullScreenMode()) { // 全屏状态下禁止页面滑动
                        return true;
                    }

                    if (Math.abs(delta) < pageWidth / 3) { // 滑动距离未超过1/3,恢复到当前页
                        scrollTo(pageIndex, true);
                    } else { // 换页
                        int index = pageIndex;
                        if (delta < 0) { // 滑动到上一页
                            if (index > 0) { // 已经在第一页
                                index--;
                            }
                        } else { // 滑动到下一页
                            if (index < pageCount - 1) { // 已经在最后一页
                                index++;
                            }
                        }
                        scrollTo(index, true);
                    }
                }

                actionDownPoint.set(0, 0);
                break;
        }
        return true;
    }

    /**
     * 滚动时需要重写的方法，用于控制滚动
     */
    @Override
    public void computeScroll() {
        //判断滚动时候停止
        if (mScroller.computeScrollOffset()) {
            //滚动到指定的位置
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //这句话必须写，否则不能实时刷新
            postInvalidate();
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams lp) {
        return new MarginLayoutParams(lp);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    public int getPageCount() {
        return pageCount;
    }

    public int currentIndex() {
        return pageIndex;
    }

    /**
     * 设置子控件的点击监听
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnScreenModeChangeListener(OnScreenModeChangeListener listener) {
        onScreenModeChangeListener = listener;
    }

    public void setOnPageStatusListener(OnPageStatusListener listener) {
        onPageStatusListener = listener;
    }

    public void performClick(int x, int y) {
        // item click
        Pair<Integer, View> result = findTargetView(x, y);
        if (result == null) {
            EMLog.i(TAG, "onTouchEvent: no child on click point.");
        } else {
            EMLog.i(TAG, "onTouchEvent: child click , index: " + result.first + ", child view: " + result.second);
            handleItemClickAction(result.second, result.first);
        }
    }

    public boolean isFullScreenMode() {
        return fullScreenView != null;
    }

    public View getFullScreenView() {
        return fullScreenView;
    }

    private void scrollTo(int index, boolean smooth) {
        if (!smooth) {
            scrollTo(index * pageWidth, 0);
        } else {
            smoothScrollTo(index * pageWidth, 0);
        }

        if (onPageStatusListener != null && index != this.pageIndex) {
            onPageStatusListener.onPageScroll(index);
        }

        this.pageIndex = index;
    }

    private void smoothScrollTo(int x, int y) {
        EMLog.i(TAG, "smoothScrollTo: " + getScrollX() + ", target : " + x);
        int deltaX = x - getScrollX();
        int deltaY = y - getScrollY();
        mScroller.startScroll(getScrollX(), 0, deltaX, deltaY);
        invalidate();
    }

    private void calculateChildrenParamsAndSet() {
        int childCount = getChildCount();
        int itemWidth = pageWidth;
        if (childCount > 4) { // 3 x 3
            itemWidth = pageWidth / 3;
        } else if (childCount > 1) { // 2 x 2
            itemWidth = pageWidth / 2;
        }

        for (int i = 0; i < getChildCount(); i++) {
            setViewParams(getChildAt(i), itemWidth);
        }
    }

    private void setViewParams(View target, int viewBorder) {
        ViewGroup.LayoutParams params = target.getLayoutParams();
        params.width = viewBorder;
        params.height = viewBorder;
        target.setLayoutParams(params);
    }

    private void calculatePageCount() {
        int count = (getChildCount() - 1) / MAX_SIZE_PER_PAGE + 1;
        if (pageCount != count && onPageStatusListener != null) {
            onPageStatusListener.onPageCountChange(count);
        }
        pageCount = count;
    }

    private Pair<Integer, View> findTargetView(int x, int y) {
        int start = pageIndex * MAX_SIZE_PER_PAGE;
        int count = getChildCount() - start > MAX_SIZE_PER_PAGE ? MAX_SIZE_PER_PAGE : getChildCount() - start;
        View result;
        for (int i = start; i < start + count; i++) {
            result = getChildAt(i);
            int[] location = new int[2];
            result.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            int right = left + result.getMeasuredWidth();
            int bottom = top + result.getMeasuredHeight();
            if (new Rect(left, top, right, bottom).contains(x, y)) {
                return new Pair<>(i, result);
            }
        }
        return null;
    }

    private void handleItemClickAction(View v, int index) {
        if (isFullScreenMode()) {
            resetAllViews(v);
        } else {
            // 仅当开启视频后才能被点击进入全屏
            if (v instanceof ConferenceMemberView && !((ConferenceMemberView) v).isVideoOff()) {
                fullScreen(v);
            }
        }

        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, index);
        }
    }

    private void fullScreen(View view) {
        fullScreenView = view;

        int pageIndex = indexOfChild(view) / MAX_SIZE_PER_PAGE;

        int start = pageIndex * MAX_SIZE_PER_PAGE;
        int end = getChildCount() - start > MAX_SIZE_PER_PAGE ? start + MAX_SIZE_PER_PAGE : getChildCount();

        // 只更改child view所在页的所有子view的layout parameters.
        for (int i = start; i < end; i++) {
            View child = getChildAt(i);
            LayoutParams lp = child.getLayoutParams();
            if (view == child) {
                lp.width = pageWidth;
                lp.height = screenHeight;
            } else {
                lp.width = 0;
                lp.height = 0;
            }
            child.setLayoutParams(lp);
        }

        if (view instanceof ConferenceMemberView) {
            ((ConferenceMemberView) view).setFullScreen(isFullScreenMode());
        }

        if (onScreenModeChangeListener != null) {
            onScreenModeChangeListener.onScreenModeChange(isFullScreenMode(), fullScreenView);
        }
    }

    private void resetAllViews(View view) {
        fullScreenView = null;

        if (pageCount == 1) {
            calculateChildrenParamsAndSet();
        } else { // 只计算当前页的LayoutParameters.
            int pageIndex = indexOfChild(view) / MAX_SIZE_PER_PAGE;

            int start = pageIndex * MAX_SIZE_PER_PAGE;
            int end = getChildCount() - start > MAX_SIZE_PER_PAGE ? start + MAX_SIZE_PER_PAGE : getChildCount();

            int itemWidth = pageWidth / 3;

            for (int i = start; i < end; i++) {
                setViewParams(getChildAt(i), itemWidth);
            }
        }

        if (view instanceof ConferenceMemberView) {
            ((ConferenceMemberView) view).setFullScreen(isFullScreenMode());
        }

        if (onScreenModeChangeListener != null) {
            onScreenModeChangeListener.onScreenModeChange(isFullScreenMode(), fullScreenView);
        }
    }
}

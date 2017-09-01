package com.hyphenate.chatuidemo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lzan13 on 2017/3/25
 *
 * 自定义ViewGroup类，会根据子控件的宽度自动换行，
 */
public class EaseViewGroup extends ViewGroup {

    public EaseViewGroup(Context context) {
        super(context);
    }

    public EaseViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int totalHeight = 0;
        int totalWidth = 0;
        int tempHeight = 0;
        // 获取子控件的个数
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            tempHeight = (h > tempHeight) ? h : tempHeight;
            // 判断当前宽度是否超过了 ViewGroup 的宽度
            if ((w + totalWidth) > right) {
                totalWidth = 0;
                totalHeight += tempHeight;
                tempHeight = 0;
            }
            child.layout(totalWidth, totalHeight, totalWidth + w, totalHeight + h);
            totalWidth += w;
        }
    }

    /**
     * 重写onMeasure方法，这里循环设置当前自定义控件的子控件的大小
     */
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    /**
     * 设置子控件的点击监听
     */
    public void setItemOnClickListener() {

    }

    public interface OnMLViewGroupListener {
        void onItemClick(View view, int position, long id);
    }
}

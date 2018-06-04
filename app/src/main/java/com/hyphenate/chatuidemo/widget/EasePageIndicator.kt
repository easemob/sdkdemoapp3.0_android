package com.hyphenate.chatuidemo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.hyphenate.chatuidemo.R
import com.hyphenate.chatuidemo.utils.DisplayUtils

/**
 * Created by zhangsong on 18-5-23.
 */
class EasePageIndicator : LinearLayout {
    companion object {
        val TAG = "PageIndicator"
    }

    private var indicators: ArrayList<View> = ArrayList()
    private var checkedPosition: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleRes: Int) : super(context, attributeSet, defStyleRes)

    fun setup(num: Int) {
        var count = num
        if (num < 0) {
            count = 0
        }

        val delta = count - indicators.size
        if (delta > 0) { // add indicator
            for (i in 0 until delta) {
                indicators.add(createIndicator())
            }
        } else { // remove indicator from end
            for (i in indicators.size - 1 downTo indicators.size + delta) {
                indicators.removeAt(i)
                removeViewAt(i)
            }
        }
    }

    fun setItemChecked(position: Int) {
        if (position >= indicators.size || position < 0) {
            return
        }

        if (checkedPosition > -1 && checkedPosition < indicators.size) {
            indicators[checkedPosition].isSelected = false
        }
        checkedPosition = position
        indicators[checkedPosition].isSelected = true
    }

    fun getCheckedPosition(): Int {
        return checkedPosition
    }

    private fun createIndicator(): View {
        val indicator = View(context)
        var lp = ViewGroup.LayoutParams(DisplayUtils.dp2px(context, 16f), DisplayUtils.dp2px(context, 4f))
        indicator.layoutParams = lp
        indicator.background = context.resources.getDrawable(R.drawable.em_indicator_selector)
        addView(indicator)
        lp = indicator.layoutParams as MarginLayoutParams
        val margin = DisplayUtils.dp2px(context, 5f)
        lp.setMargins(margin, 0, margin, 0)
        indicator.requestLayout()
        return indicator
    }
}
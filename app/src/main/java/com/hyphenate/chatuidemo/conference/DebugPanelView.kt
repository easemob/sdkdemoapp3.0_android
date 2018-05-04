package com.hyphenate.chatuidemo.conference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.hyphenate.chat.EMConferenceStream
import com.hyphenate.chatuidemo.R
import com.hyphenate.util.EMLog
import kotlinx.android.synthetic.main.em_layout_debug_panel.view.*

/**
 * Created by zhangsong on 18-4-28.
 */
class DebugPanelView : LinearLayout {
    interface OnButtonClickListener {
        fun onCloseClick(v: View)
    }

    companion object {
        val TAG = "DebugPanelView"
    }

    private var onButtonClickListener: OnButtonClickListener? = null
    private var streamList: ArrayList<EMConferenceStream> = ArrayList()
    private var streamAdapter: StreamAdapter? = null
    private var activatedView: View? = null
    private val onClickListener: View.OnClickListener by lazy {
        OnClickListener { v ->
            when (v) {
                btn_close -> {
                    EMLog.i(TAG, "btn_close clicked.")
                    onButtonClickListener?.onCloseClick(btn_close)
                }
            }
        }
    }

    private val onItemClickListener by lazy {
        AdapterView.OnItemClickListener { _, v, i: Int, _ ->
            val stream = streamList[i]
            activatedView?.isActivated = false
            v.isActivated = true
            activatedView = v
            showDebugInfo(stream)
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        init()
    }

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle) {
        init()
    }

    fun setOnButtonClickListener(listener: OnButtonClickListener) {
        onButtonClickListener = listener
    }

    fun setStreamListAndNotify(streamList: List<EMConferenceStream>) {
        this.streamList.addAll(streamList)
        streamAdapter!!.notifyDataSetChanged()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.em_layout_debug_panel, this)
        btn_close.setOnClickListener(onClickListener)
        streamAdapter = StreamAdapter(context, streamList)
        list_stream.adapter = streamAdapter
        list_stream.onItemClickListener = onItemClickListener
    }

    private fun showDebugInfo(stream: EMConferenceStream) {
        EMLog.i(TAG, "showDebugInfo, username: " + stream.username)

        container_debug_info.removeAllViews()

        val usernameTV = TextView(context)
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        usernameTV.layoutParams = lp
        usernameTV.setTextColor(Color.parseColor("#FFFFFF"))
        usernameTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        usernameTV.text = stream.username

        container_debug_info.addView(usernameTV)
    }

    private class StreamAdapter(var context: Context, var streamList: List<EMConferenceStream>) : BaseAdapter() {
        companion object {
            val TAG = "StreamAdapter"
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var contentView = convertView

            var viewHolder: ViewHolder? = null

            if (contentView == null) {
                contentView = LayoutInflater.from(context).inflate(R.layout.em_item_layout_debug, null)
                viewHolder = ViewHolder(contentView)
                contentView!!.tag = viewHolder
            } else {
                viewHolder = contentView.tag as ViewHolder?
            }

            viewHolder!!.userNameTV?.text = streamList[position].username

            return contentView
        }

        override fun getItem(position: Int): Any {
            return streamList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return streamList.size
        }

        private class ViewHolder {
            var userNameTV: TextView? = null

            constructor(v: View) {
                userNameTV = v.findViewById(R.id.tv_username) as TextView?
            }
        }

    }
}
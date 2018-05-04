package com.hyphenate.chatuidemo.conference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMConferenceStream
import com.hyphenate.chat.EMStreamStatistics
import com.hyphenate.chatuidemo.R
import com.hyphenate.util.EMLog
import kotlinx.android.synthetic.main.em_layout_debug_detail.view.*
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
    private var currentStream: EMConferenceStream? = null
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

    private val streamStatisticsMap by lazy {
        HashMap<String, EMStreamStatistics>()
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
        this.streamList.clear()
        this.streamList.addAll(streamList)
        streamAdapter!!.notifyDataSetChanged()
    }

    fun onStreamStatisticsChange(statistics: EMStreamStatistics) {
        streamStatisticsMap.put(statistics.streamId, statistics)
        if (statistics.streamId == currentStream?.streamId) {
            // Current stream statistics is showing, update it.
            post {
                // Run on ui thread.
                showDebugInfo(currentStream)
            }
        }
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.em_layout_debug_panel, this)
        btn_close.setOnClickListener(onClickListener)
        streamAdapter = StreamAdapter(context, streamList)
        list_stream.adapter = streamAdapter
        list_stream.onItemClickListener = onItemClickListener
    }

    private fun showDebugInfo(stream: EMConferenceStream?) {
        currentStream = stream
        EMLog.i(TAG, "showDebugInfo, username: " + stream?.username)

        tv_stream_id.text = "Stream Id: " + currentStream?.streamId
        tv_username_debug.text = "Username: " + stream?.username

        val statistics = streamStatisticsMap[currentStream?.streamId]
        EMLog.i(TAG, "showDebugInfo, stream?.username: " + stream?.username + ", EMClient.getInstance().currentUser: " + EMClient.getInstance().currentUser)
        if (stream?.username == EMClient.getInstance().currentUser) {
            // TODO: show local statistics info.
        } else {
            EMLog.i(TAG, "showDebugInfo, statistics: " + statistics.toString())
            tv_resolution.text = "Resolution: " + statistics?.remoteHeight + " x " + statistics?.remoteWidth
            tv_video_fps.text = "Video Fps: " + statistics?.remoteFps
            tv_video_bitrate.text = "Video Bitrate: " + statistics?.remoteVideoBps
            tv_video_pack_loss.text = "Video Package Loss: " + statistics?.remoteVideoPacketsLost
            tv_audio_bitrate.text = "Audio Bitrate: " + statistics?.remoteAudioBps
            tv_audio_pack_loss.text = "Audio Package Loss: " + statistics?.remoteAudioPacketsLost
        }
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
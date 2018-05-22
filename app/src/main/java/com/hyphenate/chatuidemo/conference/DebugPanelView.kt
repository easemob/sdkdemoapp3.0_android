package com.hyphenate.chatuidemo.conference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AbsListView.CHOICE_MODE_SINGLE
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
        AdapterView.OnItemClickListener { _, _, i: Int, _ ->
            val stream = streamList[i]
            list_stream.setItemChecked(i, true)
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

    // list列表变化
    fun setStreamListAndNotify(streamList: List<EMConferenceStream>) {
        this.streamList.clear()
        this.streamList.addAll(streamList)

        post {
            streamAdapter!!.notifyDataSetChanged()

            if (list_stream.checkedItemPosition >= streamList.size) {
                // 有item被移除
                val index: Int = streamList.indexOf(currentStream)
                if (index > -1) { // 被移除的item不是之前选中的item
                    list_stream.setItemChecked(index, true)
                } else { // 被移除的item是之前选中的item
                    currentStream = null
                }
            }

            if (currentStream == null) { // 第一次初始化,默认选中第一个
                currentStream = streamList[0]
                list_stream.setItemChecked(0, true)
            }
        }
    }

    fun onStreamStatisticsChange(statistics: EMStreamStatistics) {
        streamStatisticsMap.put(statistics.streamId, statistics)
        if (currentStream?.streamId != null && statistics.streamId.startsWith(currentStream!!.streamId)) {
            // Current stream statistics is showing, update it.
            post {
                // Run on ui thread.
                showDebugInfo(currentStream)
            }
        }
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.em_layout_debug_panel, this)
        tv_version.text = "video debug panel version." + EMClient.VERSION
        btn_close.setOnClickListener(onClickListener)
        streamAdapter = StreamAdapter(context, streamList)
        list_stream.adapter = streamAdapter
        list_stream.onItemClickListener = onItemClickListener
        list_stream.choiceMode = CHOICE_MODE_SINGLE
    }

    private fun showDebugInfo(stream: EMConferenceStream?) {
        currentStream = stream
        EMLog.i(TAG, "showDebugInfo, username: " + stream?.username + ", streamId: " + stream?.streamId)

        tv_stream_id.text = "Stream Id: " + currentStream?.streamId
        tv_username_debug.text = "Username: " + stream?.username

        val targetKey: String? = streamStatisticsMap.keys.firstOrNull { it.startsWith(currentStream!!.streamId) }
        val statistics = streamStatisticsMap[targetKey]
        EMLog.i(TAG, "showDebugInfo, stream?.username: " + stream?.username + ", EMClient.getInstance().currentUser: " + EMClient.getInstance().currentUser)
        if (stream?.username == EMClient.getInstance().currentUser) {
            EMLog.i(TAG, "showDebugInfo, local statistics: " + statistics.toString())
            tv_resolution.text = "Encode Resolution: " + statistics?.localEncodedWidth + " x " + statistics?.localEncodedHeight
            tv_video_fps.text = "Video Encode Fps: " + statistics?.localEncodedFps
            tv_video_bitrate.text = "Video Bitrate: " + statistics?.localVideoActualBps
            tv_video_pack_loss.text = "Video Package Loss: " + statistics?.localVideoPacketsLost
            tv_audio_bitrate.text = "Audio Bitrate: " + statistics?.localAudioBps
            tv_audio_pack_loss.text = "Audio Package Loss: " + statistics?.localAudioPacketsLostrate
        } else {
            EMLog.i(TAG, "showDebugInfo, remote statistics: " + statistics.toString())
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
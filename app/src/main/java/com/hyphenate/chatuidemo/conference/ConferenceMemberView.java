package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.media.EMCallSurfaceView;
import com.superrtc.sdk.VideoView;

/**
 * Created by lzan13 on 2017/8/21.
 */
public class ConferenceMemberView extends RelativeLayout {

    private Context context;

    private EMCallSurfaceView surfaceView;
    private ImageView avatarView;
    private ImageView audioOffView;
    private ImageView talkingView;
    private TextView nameView;

    private boolean isVideoOff = true;
    private boolean isAudioOff = false;
    private boolean isDesktop = false;
    private boolean isFullScreenMode = false;
    private String streamId;


    public ConferenceMemberView(Context context) {
        this(context, null);
    }

    public ConferenceMemberView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConferenceMemberView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.em_widget_conference_view, this);
        init();
    }

    private void init() {
        surfaceView = (EMCallSurfaceView) findViewById(R.id.item_surface_view);
        avatarView = (ImageView) findViewById(R.id.img_call_avatar);
        audioOffView = (ImageView) findViewById(R.id.icon_mute);
        talkingView = (ImageView) findViewById(R.id.icon_talking);
        nameView = (TextView) findViewById(R.id.text_name);

        surfaceView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
    }

    public EMCallSurfaceView getSurfaceView() {
        return surfaceView;
    }

    /**
     * 更新静音状态
     */
    public void setAudioOff(boolean state) {
        if (isDesktop) {
            return;
        }
        isAudioOff = state;

        if (isFullScreenMode) {
            return;
        }

        if (isAudioOff) {
            audioOffView.setVisibility(View.VISIBLE);
        } else {
            audioOffView.setVisibility(View.GONE);
        }
    }

    public boolean isAudioOff() {
        return isAudioOff;
    }

    /**
     * 更新视频显示状态
     */
    public void setVideoOff(boolean state) {
        isVideoOff = state;
        if (isVideoOff) {
            avatarView.setVisibility(View.VISIBLE);
        } else {
            avatarView.setVisibility(View.GONE);
        }
    }

    public boolean isVideoOff() {
        return isVideoOff;
    }

    public void setDesktop(boolean desktop) {
        isDesktop = desktop;
        if (isDesktop) {
            avatarView.setVisibility(View.GONE);
        }
    }

    /**
     * 更新说话状态
     */
    public void setTalking(boolean talking) {
        if (isDesktop) {
            return;
        }

        if (isFullScreenMode) {
            return;
        }

        if (talking) {
            talkingView.setVisibility(VISIBLE);
        } else {
            talkingView.setVisibility(GONE);
        }
    }

    /**
     * 设置当前 view 对应的 stream 的用户，主要用来语音通话时显示对方头像
     */
    public void setUsername(String username) {
        avatarView.setImageResource(R.drawable.em_call_video_default);
        nameView.setText(username);
    }

    /**
     * 设置当前控件显示的 Stream Id
     */
    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setFullScreen(boolean fullScreen) {
        isFullScreenMode = fullScreen;

        if (fullScreen) {
            talkingView.setVisibility(GONE);
            nameView.setVisibility(GONE);
            audioOffView.setVisibility(GONE);
        } else {
            nameView.setVisibility(VISIBLE);
            if (isAudioOff) {
                audioOffView.setVisibility(VISIBLE);
            }

            surfaceView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
        }
    }

    public void setScaleMode(VideoView.EMCallViewScaleMode mode) {
        surfaceView.setScaleMode(mode);
    }

    public VideoView.EMCallViewScaleMode getScaleMode() {
        return surfaceView.getScaleMode();
    }
}

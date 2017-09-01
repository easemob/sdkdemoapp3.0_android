package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.media.EMCallSurfaceView;
import com.superrtc.sdk.VideoView;

/**
 * Created by lzan13 on 2017/8/21.
 */
public class ConferenceMemberView extends RelativeLayout {

    private Context context;

    private EMCallSurfaceView surfaceView;
    private ImageView avatarView;
    private ImageView muteIcon;
    private ImageView videoIcon;
    private View activateView;
    private boolean isPubOrSub = false;

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
        muteIcon = (ImageView) findViewById(R.id.icon_mute);
        videoIcon = (ImageView) findViewById(R.id.icon_video);
        activateView = findViewById(R.id.icon_activate);

        surfaceView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
    }

    public EMCallSurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void updateMuteState(boolean state) {
        if (state) {
            muteIcon.setVisibility(View.VISIBLE);
        } else {
            muteIcon.setVisibility(View.GONE);
        }
    }

    public void updateVideoState(boolean state) {
        if (state) {
            videoIcon.setVisibility(View.VISIBLE);
        } else {
            videoIcon.setVisibility(View.GONE);
        }
    }

    /**
     * 判断是否已订阅
     */
    public boolean isPubOrSub() {
        return isPubOrSub;
    }

    /**
     * 设置当前画面状态
     */
    public void setPubOrSub(boolean activate) {
        isPubOrSub = activate;
        if (isPubOrSub) {
            avatarView.setVisibility(View.GONE);
            activateView.setBackgroundResource(R.color.holo_green_light);
        } else {
            avatarView.setVisibility(View.VISIBLE);
            activateView.setBackgroundResource(R.color.holo_red_light);
        }
    }
}

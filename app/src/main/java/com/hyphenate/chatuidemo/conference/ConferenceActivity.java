package com.hyphenate.chatuidemo.conference;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMConferenceListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConference;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chat.EMStreamParam;
import com.hyphenate.chat.EMStreamStatistics;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.BaseActivity;
import com.hyphenate.chatuidemo.widget.EaseViewGroup;
import com.hyphenate.chatuidemo.widget.FloatWindow;
import com.hyphenate.util.EMLog;
import com.superrtc.mediamanager.ScreenCaptureManager;
import com.superrtc.sdk.VideoView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by lzan13 on 2017/8/15.
 * 多人音视频会议界面
 */
public class ConferenceActivity extends BaseActivity implements EMConferenceListener {

    private final String TAG = this.getClass().getSimpleName();
    private final int REQUEST_CODE_INVITE = 1001;

    private ConferenceActivity activity;
    private EMConferenceListener conferenceListener;

    private AudioManager audioManager;

    private EMConference conference;
    private EMStreamParam normalParam;
    private EMStreamParam desktopParam;
    private boolean isCreator = false;
    private String confId = "";
    private String password = "";

    private int screenWidth;

    private List<EMConferenceStream> streamList = new ArrayList<>();

    private ConferenceMemberView localView;
    private IncomingCallView incomingCallView;
    private EaseViewGroup callConferenceViewGroup;

    // ------ tools panel relevant start ------
    // tools panel的父view
    private View toolsPanelView;
    // tools panel中显示会议成员名称的TextView
    private TextView membersTV;
    // tools panel中显示会议成员数量的TextView
    private TextView memberCountTV;
    // tools panel中显示时间的TextView
    private TextView callTimeView;
    // 麦克风开关
    private ImageButton micSwitch;
    // 摄像头开关
    private ImageButton cameraSwitch;
    // 话筒开关
    private ImageButton speakerSwitch;
    // 屏幕分享开关
    private ImageButton screenShareSwitch;
    // 前后摄像头切换
    private ImageButton changeCameraSwitch;
    // 挂断按钮
    private ImageButton hangupBtn;
    // 显示debug信息按钮
    private ImageButton debugBtn;
    // 邀请其他成员加入的按钮
    private ImageButton inviteBtn;
    // 全屏模式下改变视频显示模式的按钮,只在全屏模式下显示
    private ImageButton scaleModeBtn;
    // 显示悬浮窗的按钮
    private ImageButton closeBtn;
    // 退出全屏模式的按钮,只在全屏模式下显示
    private ImageButton zoominBtn;
    // ------ tools panel relevant end ------

    private DebugPanelView debugPanelView;

    // ------ full screen views start -------
    private View membersLayout;
    private TextView membersTVMain;
    private TextView memberCountTVMain;
    private TextView callTimeViewMain;
    private View talkingLayout;
    private ImageView talkingImage;
    private TextView talkerTV;
    // ------ full screen views end -------

    private TimeHandler timeHandler;
    // TODO: for test
    private EMConferenceStream globalStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();

        EMClient.getInstance().conferenceManager().addConferenceListener(conferenceListener);
        DemoHelper.getInstance().pushActivity(activity);
    }

    /**
     * 初始化
     */
    private void init() {
        activity = this;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;

        incomingCallView = (IncomingCallView) findViewById(R.id.incoming_call_view);
        callConferenceViewGroup = (EaseViewGroup) findViewById(R.id.surface_view_group);

        toolsPanelView = findViewById(R.id.layout_tools_panel);
        inviteBtn = (ImageButton) findViewById(R.id.btn_invite);
        membersTV = (TextView) findViewById(R.id.tv_members);
        memberCountTV = (TextView) findViewById(R.id.tv_member_count);
        callTimeView = (TextView) findViewById(R.id.tv_call_time);
        micSwitch = (ImageButton) findViewById(R.id.btn_mic_switch);
        cameraSwitch = (ImageButton) findViewById(R.id.btn_camera_switch);
        speakerSwitch = (ImageButton) findViewById(R.id.btn_speaker_switch);
        screenShareSwitch = (ImageButton) findViewById(R.id.btn_desk_share);
        changeCameraSwitch = (ImageButton) findViewById(R.id.btn_change_camera_switch);
        hangupBtn = (ImageButton) findViewById(R.id.btn_hangup);
        debugBtn = (ImageButton) findViewById(R.id.btn_debug);
        scaleModeBtn = (ImageButton) findViewById(R.id.btn_scale_mode);
        closeBtn = (ImageButton) findViewById(R.id.btn_close);
        zoominBtn = (ImageButton) findViewById(R.id.btn_zoomin);

        debugPanelView = (DebugPanelView) findViewById(R.id.layout_debug_panel);

        membersLayout = findViewById(R.id.layout_members);
        membersTVMain = (TextView) findViewById(R.id.tv_members_main);
        memberCountTVMain = (TextView) findViewById(R.id.tv_member_count_main);
        callTimeViewMain = (TextView) findViewById(R.id.tv_call_time_main);
        talkingLayout = findViewById(R.id.layout_talking);
        talkingImage = (ImageView) findViewById(R.id.icon_talking);
        talkerTV = (TextView) findViewById(R.id.tv_talker);

        incomingCallView.setOnActionListener(onActionListener);
        callConferenceViewGroup.setOnItemClickListener(onItemClickListener);
        callConferenceViewGroup.setOnScreenModeChangeListener(onScreenModeChangeListener);
        inviteBtn.setOnClickListener(listener);
        micSwitch.setOnClickListener(listener);
        speakerSwitch.setOnClickListener(listener);
        cameraSwitch.setOnClickListener(listener);
        screenShareSwitch.setOnClickListener(listener);
        changeCameraSwitch.setOnClickListener(listener);
        hangupBtn.setOnClickListener(listener);
        debugBtn.setOnClickListener(listener);
        scaleModeBtn.setOnClickListener(listener);
        closeBtn.setOnClickListener(listener);
        zoominBtn.setOnClickListener(listener);

        debugPanelView.setOnButtonClickListener(onButtonClickListener);

        conferenceListener = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        confId = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_ID);
        password = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_PASS);

        normalParam = new EMStreamParam();
        normalParam.setStreamType(EMConferenceStream.StreamType.NORMAL);
        normalParam.setVideoOff(true);
        normalParam.setAudioOff(false);

        desktopParam = new EMStreamParam();
        desktopParam.setAudioOff(true);
        desktopParam.setVideoOff(true);
        desktopParam.setStreamType(EMConferenceStream.StreamType.DESKTOP);

        micSwitch.setActivated(normalParam.isAudioOff());
        cameraSwitch.setActivated(normalParam.isVideoOff());
        speakerSwitch.setActivated(true);
        openSpeaker();

        isCreator = getIntent().getBooleanExtra(Constant.EXTRA_CONFERENCE_IS_CREATOR, false);
        if (isCreator) {
            incomingCallView.setVisibility(View.GONE);
            callConferenceViewGroup.post(new Runnable() {
                @Override
                public void run() {
                    // Do this after callConferenceViewGroup init finish.
                    selectUserToJoinConference();
                }
            });
        } else {
            initLocalConferenceView();
            String inviter = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_INVITER);
            incomingCallView.setInviteInfo(String.format(getString(R.string.tips_invite_to_join), inviter));
            incomingCallView.setVisibility(View.VISIBLE);
        }

        timeHandler = new TimeHandler();
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_invite:
//                    selectUserToJoinConference();
                    // TODO: for test
                    addConferenceView(globalStream);
                    break;
                case R.id.btn_mic_switch:
                    voiceSwitch();
                    break;
                case R.id.btn_speaker_switch:
                    if (speakerSwitch.isActivated()) {
                        closeSpeaker();
                    } else {
                        openSpeaker();
                    }
                    break;
                case R.id.btn_camera_switch:
                    videoSwitch();
                    break;
                case R.id.btn_desk_share:
                    if (screenShareSwitch.isActivated()) {
                        screenShareSwitch.setActivated(false);
                        unpublish(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP));
                    } else {
                        screenShareSwitch.setActivated(true);
                        publishDesktop();
                    }
                    break;
                case R.id.btn_change_camera_switch:
                    changeCamera();
                    break;
                case R.id.btn_hangup:
                    exitConference();
                    break;
                case R.id.btn_debug:
                    EMLog.i(TAG, "Button debug clicked!!!");
                    EMClient.getInstance().conferenceManager().enableStatistics(true);
                    openDebugPanel();
                    break;
                case R.id.btn_scale_mode: // 全屏状态下切换视频scale mode
                    changeFullScreenScaleMode();
                    break;
                case R.id.btn_close: // 显示悬浮框
                    showFloatWindow();
                    break;
                case R.id.btn_zoomin:
                    // exit full screen mode.
                    callConferenceViewGroup.performClick(100, 100);
                    break;
            }
        }
    };

    private IncomingCallView.OnActionListener onActionListener = new IncomingCallView.OnActionListener() {
        @Override
        public void onPickupClick(@NotNull View v) {
            incomingCallView.setVisibility(View.GONE);
            joinConference();
        }

        @Override
        public void onRejectClick(@NotNull View v) {
            finish();
        }
    };

    /**
     * 初始化多人音视频画面管理控件
     */
    private void initLocalConferenceView() {
        localView = new ConferenceMemberView(activity);
        localView.setVideoOff(normalParam.isVideoOff());
        localView.setAudioOff(normalParam.isAudioOff());
        localView.setUsername(EMClient.getInstance().getCurrentUser());
        EMClient.getInstance().conferenceManager().setLocalSurfaceView(localView.getSurfaceView());

        callConferenceViewGroup.addView(localView);
    }

    /**
     * 添加一个展示远端画面的 view
     */
    private void addConferenceView(EMConferenceStream stream) {
        EMLog.d(TAG, "add conference view -start- " + stream.getMemberName());
        streamList.add(stream);
        final ConferenceMemberView memberView = new ConferenceMemberView(activity);
        callConferenceViewGroup.addView(memberView);
        memberView.setUsername(stream.getUsername());
        memberView.setStreamId(stream.getStreamId());
        memberView.setAudioOff(stream.isAudioOff());
        memberView.setVideoOff(stream.isVideoOff());
        memberView.setDesktop(stream.getStreamType() == EMConferenceStream.StreamType.DESKTOP);
        subscribe(stream, memberView);
        EMLog.d(TAG, "add conference view -end-" + stream.getMemberName());
        debugPanelView.setStreamListAndNotify(streamList);
    }

    private DebugPanelView.OnButtonClickListener onButtonClickListener = new DebugPanelView.OnButtonClickListener() {
        @Override
        public void onCloseClick(@NotNull View v) {
            EMClient.getInstance().conferenceManager().enableStatistics(false);
            openToolsPanel();
        }
    };

    private EaseViewGroup.OnItemClickListener onItemClickListener = new EaseViewGroup.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
        }
    };

    private EaseViewGroup.OnScreenModeChangeListener onScreenModeChangeListener = new EaseViewGroup.OnScreenModeChangeListener() {
        @Override
        public void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView) {
            if (isFullScreenMode) { // 全屏模式
                toolsPanelView.setBackgroundColor(getResources().getColor(R.color.color_transparent));

                membersTV.setVisibility(View.INVISIBLE);
                memberCountTV.setVisibility(View.INVISIBLE);
                callTimeView.setVisibility(View.INVISIBLE);

                membersLayout.setVisibility(View.VISIBLE);
                talkingLayout.setVisibility(View.VISIBLE);
                callTimeViewMain.setVisibility(View.VISIBLE);

                scaleModeBtn.setVisibility(View.VISIBLE);
                closeBtn.setVisibility(View.GONE);
                zoominBtn.setVisibility(View.VISIBLE);
            } else { // 非全屏模式
                toolsPanelView.setBackgroundColor(getResources().getColor(R.color.bg_tools_panel));

                membersTV.setVisibility(View.VISIBLE);
                memberCountTV.setVisibility(View.VISIBLE);
                callTimeView.setVisibility(View.VISIBLE);

                scaleModeBtn.setVisibility(View.INVISIBLE);
                closeBtn.setVisibility(View.VISIBLE);
                zoominBtn.setVisibility(View.GONE);

                // invisible the full-screen mode views.
                membersLayout.setVisibility(View.GONE);
                talkingLayout.setVisibility(View.GONE);
                callTimeViewMain.setVisibility(View.GONE);
            }
        }
    };

    /**
     * 移除指定位置的 View，移除时如果已经订阅需要取消订阅
     */
    private void removeConferenceView(EMConferenceStream stream) {
        int index = streamList.indexOf(stream);
        final ConferenceMemberView memberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(index + 1);
        streamList.remove(stream);
        callConferenceViewGroup.removeView(memberView);
        debugPanelView.setStreamListAndNotify(streamList);
    }

    /**
     * 更新指定 View
     */
    private void updateConferenceMemberView(EMConferenceStream stream) {
        int position = streamList.indexOf(stream);
        ConferenceMemberView conferenceMemberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(position + 1);
        conferenceMemberView.setAudioOff(stream.isAudioOff());
        conferenceMemberView.setVideoOff(stream.isVideoOff());

        // 更新当前正在显示的悬浮窗.
        if (FloatWindow.getInstance(getApplicationContext()).isShowing() && position == 0) {
            int type = streamList.get(0).isVideoOff() ? 0 : 1;
            FloatWindow.getInstance(getApplicationContext()).updateFloatWindow(type);
        }
    }

    /**
     * 更新当前说话者
     */
    private void currSpeakers(List<String> speakers) {
        for (int i = 0; i < callConferenceViewGroup.getChildCount(); i++) {
            if (talkingLayout.getVisibility() == View.VISIBLE) {
                // full screen mode.
                if (speakers.size() == 0) {
                    talkingImage.setVisibility(View.GONE);
                    talkerTV.setText("");
                } else {
                    talkingImage.setVisibility(View.VISIBLE);
                    String lastStreamId = speakers.get(speakers.size() - 1);
                    String speaker = null;
                    for (EMConferenceStream stream : streamList) {
                        if (stream.getStreamId().equals(lastStreamId)) {
                            speaker = stream.getUsername();
                            break;
                        }
                    }
                    talkerTV.setText(speaker);
                }
            }

            ConferenceMemberView view = (ConferenceMemberView) callConferenceViewGroup.getChildAt(i);
            view.setTalking(speakers.contains(view.getStreamId()));
        }
    }

    /**
     * 作为创建者创建并加入会议
     */
    private void createAndJoinConference(final EMValueCallBack<EMConference> callBack) {
        EMClient.getInstance().conferenceManager().createAndJoinConference(password, new EMValueCallBack<EMConference>() {
            @Override
            public void onSuccess(final EMConference value) {
                EMLog.e(TAG, "create and join conference success");
                conference = value;
                startAudioTalkingMonitor();
                publish();
                timeHandler.startTime();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Create and join conference success", Toast.LENGTH_SHORT).show();
                        if (callBack != null) {
                            callBack.onSuccess(value);
                        }
                    }
                });
            }

            @Override
            public void onError(final int error, final String errorMsg) {
                EMLog.e(TAG, "Create and join conference failed error " + error + ", msg " + errorMsg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callBack != null) {
                            callBack.onError(error, errorMsg);
                        }
                    }
                });
            }
        });
    }

    /**
     * 作为成员直接根据 confId 和 password 加入会议
     */
    private void joinConference() {
        hangupBtn.setVisibility(View.VISIBLE);
        EMClient.getInstance().conferenceManager().joinConference(confId, password, new EMValueCallBack<EMConference>() {
            @Override
            public void onSuccess(EMConference value) {
                conference = value;
                startAudioTalkingMonitor();
                publish();
                timeHandler.startTime();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Join conference success", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "join conference failed error " + error + ", msg " + errorMsg);
            }
        });
    }

    /**
     * 邀请他人加入会议
     */
    private void selectUserToJoinConference() {
        Intent intent = new Intent(activity, ConferenceInviteActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_INVITE);
    }

    private void inviteUserToJoinConference(String[] contacts) {
        try {
            JSONObject object = new JSONObject();
            int type = 0;
            if (!cameraSwitch.isActivated()) {
                type = 1;
            }
            object.put("type", type);
            object.put(Constant.EXTRA_CONFERENCE_INVITER, EMClient.getInstance().getCurrentUser());
            for (int i = 0; i < contacts.length; i++) {
                EMClient.getInstance()
                        .conferenceManager()
                        .inviteUserToJoinConference(conference.getConferenceId(), conference.getPassword(), contacts[i],
                                object.toString(), new EMValueCallBack() {
                                    @Override
                                    public void onSuccess(Object value) {
                                        EMLog.e(TAG, "invite join conference success");
                                    }

                                    @Override
                                    public void onError(int error, String errorMsg) {
                                        EMLog.e(TAG, "invite join conference failed " + error + ", " + errorMsg);
                                    }
                                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ScreenCaptureManager.RECORD_REQUEST_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ScreenCaptureManager.getInstance().start(resultCode, data);
                }
            } else if (requestCode == REQUEST_CODE_INVITE) {
                final String[] members = data.getStringArrayExtra("members");
                if (isCreator && conference == null) {
                    initLocalConferenceView();

                    createAndJoinConference(new EMValueCallBack<EMConference>() {
                        @Override
                        public void onSuccess(EMConference value) {
                            inviteUserToJoinConference(members);
                        }

                        @Override
                        public void onError(int error, String errorMsg) {

                        }
                    });
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            // 只有第一次创建会议时,若选择取消需要finish当前activity.
            boolean needFinish = (isCreator && conference == null);
            if (needFinish) finish();
        }
    }

    /**
     * 退出会议
     */
    private void exitConference() {
        stopAudioTalkingMonitor();
        timeHandler.stopTime();
        EMClient.getInstance().conferenceManager().exitConference(new EMValueCallBack() {
            @Override
            public void onSuccess(Object value) {
                finish();
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "exit conference failed " + error + ", " + errorMsg);
                finish();
            }
        });
    }

    private void startAudioTalkingMonitor() {
        EMClient.getInstance().conferenceManager().startMonitorSpeaker(300);
    }

    private void stopAudioTalkingMonitor() {
        EMClient.getInstance().conferenceManager().stopMonitorSpeaker();
    }

    /**
     * 开始推自己的数据
     */
    private void publish() {
        EMClient.getInstance().conferenceManager().publish(normalParam, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
                conference.setPubStreamId(value, EMConferenceStream.StreamType.NORMAL);
                localView.setStreamId(value);
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "publish failed: error=" + error + ", msg=" + errorMsg);
            }
        });
    }


    private void startScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ScreenCaptureManager.getInstance().state == ScreenCaptureManager.State.IDLE) {
                ScreenCaptureManager.getInstance().init(activity);
                ScreenCaptureManager.getInstance().setScreenCaptureCallback(new ScreenCaptureManager.ScreenCaptureCallback() {
                    @Override
                    public void onBitmap(Bitmap bitmap) {
                        EMClient.getInstance().conferenceManager().inputExternalVideoData(bitmap);
                    }
                });
            }
        }
    }

    public void publishDesktop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            desktopParam.setShareView(null);
        } else {
            desktopParam.setShareView(activity.getWindow().getDecorView());
        }
        EMClient.getInstance().conferenceManager().publish(desktopParam, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
                conference.setPubStreamId(value, EMConferenceStream.StreamType.DESKTOP);
                startScreenCapture();
            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }

    /**
     * 停止推自己的数据
     */
    private void unpublish(String publishId) {
        if (ScreenCaptureManager.getInstance().state == ScreenCaptureManager.State.RUNNING) {
            if (!TextUtils.isEmpty(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))
                    && publishId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))) {
                ScreenCaptureManager.getInstance().stop();
            }
        }
        EMClient.getInstance().conferenceManager().unpublish(publishId, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "unpublish failed: error=" + error + ", msg=" + errorMsg);
            }
        });
    }

    /**
     * 订阅指定成员 stream
     */
    private void subscribe(EMConferenceStream stream, final ConferenceMemberView memberView) {
        EMClient.getInstance().conferenceManager().subscribe(stream, memberView.getSurfaceView(), new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }

    /**
     * 取消订阅指定成员 stream
     */
    private void unsubscribe(EMConferenceStream stream) {
        EMClient.getInstance().conferenceManager().unsubscribe(stream, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
            }

            @Override
            public void onError(int error, String errorMsg) {

            }
        });
    }

    /**
     * 打开扬声器
     * 主要是通过扬声器的开关以及设置音频播放模式来实现
     * 1、MODE_NORMAL：是正常模式，一般用于外放音频
     * 2、MODE_IN_CALL：
     * 3、MODE_IN_COMMUNICATION：这个和 CALL 都表示通讯模式，不过 CALL 在华为上不好使，故使用 COMMUNICATION
     * 4、MODE_RINGTONE：铃声模式
     */
    public void openSpeaker() {
        // 检查是否已经开启扬声器
        if (!audioManager.isSpeakerphoneOn()) {
            // 打开扬声器
            audioManager.setSpeakerphoneOn(true);
        }
        // 开启了扬声器之后，因为是进行通话，声音的模式也要设置成通讯模式
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        speakerSwitch.setActivated(true);
    }

    /**
     * 关闭扬声器，即开启听筒播放模式
     * 更多内容看{@link #openSpeaker()}
     */
    public void closeSpeaker() {
        // 检查是否已经开启扬声器
        if (audioManager.isSpeakerphoneOn()) {
            // 关闭扬声器
            audioManager.setSpeakerphoneOn(false);
        }
        // 设置声音模式为通讯模式
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        speakerSwitch.setActivated(false);
    }

    /**
     * 语音开关
     */
    private void voiceSwitch() {
        if (normalParam.isAudioOff()) {
            normalParam.setAudioOff(false);
            EMClient.getInstance().conferenceManager().openVoiceTransfer();
        } else {
            normalParam.setAudioOff(true);
            EMClient.getInstance().conferenceManager().closeVoiceTransfer();
        }
        micSwitch.setActivated(normalParam.isAudioOff());
        localView.setAudioOff(normalParam.isAudioOff());
    }

    /**
     * 视频开关
     */
    private void videoSwitch() {
        if (normalParam.isVideoOff()) {
            normalParam.setVideoOff(false);
            EMClient.getInstance().conferenceManager().openVideoTransfer();
        } else {
            normalParam.setVideoOff(true);
            EMClient.getInstance().conferenceManager().closeVideoTransfer();
        }
        cameraSwitch.setActivated(normalParam.isVideoOff());
        localView.setVideoOff(normalParam.isVideoOff());
    }

    /**
     * 切换摄像头
     */
    private void changeCamera() {
        EMClient.getInstance().conferenceManager().switchCamera();
    }

    @Override
    public void onBackPressed() {
//        exitConference();
        showFloatWindow();
    }


    @Override
    protected void onDestroy() {
        EMClient.getInstance().conferenceManager().removeConferenceListener(conferenceListener);
        DemoHelper.getInstance().popActivity(activity);
        super.onDestroy();
    }

    /**
     * --------------------------------------------------------------------
     * 多人音视频会议回调方法
     */

    @Override
    public void onMemberJoined(final String username) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, username + " joined conference!", Toast.LENGTH_SHORT).show();
                updateConferenceMembers();
            }
        });
    }

    @Override
    public void onMemberExited(final String username) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, username + " removed conference!", Toast.LENGTH_SHORT).show();
                updateConferenceMembers();
            }
        });
    }

    @Override
    public void onStreamAdded(final EMConferenceStream stream) {
        globalStream = stream;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream add!", Toast.LENGTH_SHORT)
                        .show();
                addConferenceView(stream);
            }
        });
    }

    @Override
    public void onStreamRemoved(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream removed!", Toast.LENGTH_SHORT).show();
                if (streamList.contains(stream)) {
                    removeConferenceView(stream);
                }
            }
        });
    }

    @Override
    public void onStreamUpdate(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream update!", Toast.LENGTH_SHORT).show();
                updateConferenceMemberView(stream);
            }
        });
    }

    @Override
    public void onPassiveLeave(final int error, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Passive exit " + error + ", message" + message, Toast.LENGTH_SHORT).show();
                // 当前用户被踢出会议,如果显示了悬浮窗,隐藏
                if (FloatWindow.getInstance(getApplicationContext()).isShowing()) {
                    FloatWindow.getInstance(getApplicationContext()).removeFloatWindow();
                }
            }
        });
    }

    @Override
    public void onConferenceState(final ConferenceState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "State=" + state, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStreamStatistics(EMStreamStatistics statistics) {
        EMLog.i(TAG, "onStreamStatistics" + statistics.toString());
        debugPanelView.onStreamStatisticsChange(statistics);
    }

    @Override
    public void onStreamSetup(final String streamId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (streamId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.NORMAL))
                        || streamId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))) {
                    Toast.makeText(activity, "Publish setup streamId=" + streamId, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Subscribe setup streamId=" + streamId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSpeakers(final List<String> speakers) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currSpeakers(speakers);
            }
        });
    }

    /**
     * 收到其他人的会议邀请
     *
     * @param confId    会议 id
     * @param password  会议密码
     * @param extension 邀请扩展内容
     */
    @Override
    public void onReceiveInvite(final String confId, String password, String extension) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Receive invite " + confId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openDebugPanel() {
        Animator animator = ObjectAnimator.ofFloat(toolsPanelView, "translationY", 0, toolsPanelView.getHeight());
        animator.setDuration(300).start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                toolsPanelView.setVisibility(View.GONE);

                debugPanelView.setVisibility(View.VISIBLE);
                Animator animator = ObjectAnimator.ofFloat(debugPanelView, "translationY", debugPanelView.getHeight(), 0);
                animator.setDuration(150).start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private void openToolsPanel() {
        Animator animator = ObjectAnimator.ofFloat(debugPanelView, "translationY", 0, debugPanelView.getHeight());
        animator.setDuration(300).start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                debugPanelView.setVisibility(View.GONE);

                toolsPanelView.setVisibility(View.VISIBLE);
                Animator animator = ObjectAnimator.ofFloat(toolsPanelView, "translationY", toolsPanelView.getHeight(), 0);
                animator.setDuration(150).start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private void updateConferenceMembers() {
        List<String> members = EMClient.getInstance().conferenceManager().getConferenceMemberList();
        String count = members.size() > 0 ? "(" + members.size() + ")" : "";
        String membersStr = getMembersStr(members);

        membersTV.setText(membersStr);
        memberCountTV.setText(count);

        membersTVMain.setText(membersStr);
        memberCountTVMain.setText(count);
    }

    private String getMembersStr(List<String> members) {
        String result = "";
        for (int i = 0; i < members.size(); i++) {
            if (i == 0) {
                result += members.get(i);
                continue;
            }

            result += ", " + members.get(i);
        }
        return result;
    }

    private void updateConferenceTime(String time) {
        callTimeView.setText(time);
        callTimeViewMain.setText(time);
    }

    private void changeFullScreenScaleMode() {
        if (!callConferenceViewGroup.isFullScreenMode()) {
            return;
        }

        ConferenceMemberView fullScreenView = (ConferenceMemberView) callConferenceViewGroup.getFullScreenView();
        if (fullScreenView.getScaleMode() == VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFit) {
            fullScreenView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
            scaleModeBtn.setImageResource(R.drawable.em_call_scale_fit);
        } else {
            fullScreenView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFit);
            scaleModeBtn.setImageResource(R.drawable.em_call_scale_fill);
        }
    }

    private void showFloatWindow() {
        int type;
        String username;
        if (streamList.size() > 0) { // 如果会议中有其他成员,则显示第一个成员
            type = streamList.get(0).isVideoOff() ? 0 : 1;
            username = streamList.get(0).getUsername();
        } else { // 会议中无其他成员,显示自己信息
            type = normalParam.isVideoOff() ? 0 : 1;
            username = EMClient.getInstance().getCurrentUser();
        }

        FloatWindow.getInstance(getApplicationContext()).addFloatWindow(type, username);
        ConferenceActivity.this.moveTaskToBack(false);
    }

    private class TimeHandler extends Handler {
        private final int MSG_TIMER = 0;
        private DateFormat dateFormat = null;
        private int timePassed = 0;

        public TimeHandler() {
            dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void startTime() {
            sendEmptyMessageDelayed(MSG_TIMER, 1000);
        }

        public void stopTime() {
            removeMessages(MSG_TIMER);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                // TODO: update calling time.
                timePassed++;
                String time = dateFormat.format(timePassed * 1000);
                updateConferenceTime(time);
                sendEmptyMessageDelayed(MSG_TIMER, 1000);
                return;
            }
            super.handleMessage(msg);
        }
    }
}

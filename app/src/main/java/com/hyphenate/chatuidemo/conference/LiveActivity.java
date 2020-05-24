package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMConferenceListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConference;
import com.hyphenate.chat.EMConferenceAttribute;
import com.hyphenate.chat.EMConferenceManager;
import com.hyphenate.chat.EMConferenceMember;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMStreamParam;
import com.hyphenate.chat.EMStreamStatistics;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.BaseActivity;
import com.hyphenate.chatuidemo.utils.PhoneStateManager;
import com.hyphenate.chatuidemo.widget.EasePageIndicator;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.EasyUtils;
import com.superrtc.mediamanager.ScreenCaptureManager;
import com.superrtc.sdk.VideoView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by lzan13 on 2017/8/15.
 * 直播界面
 */
public class LiveActivity extends BaseActivity implements EMConferenceListener {
    private final String TAG = this.getClass().getSimpleName();

    private static final int STATE_AUDIENCE = 0;
    private static final int STATE_TALKER = 1;

    private LiveActivity activity;
    private EMConferenceListener conferenceListener;

    private AudioManager audioManager;

    private EMConference conference;
    private EMStreamParam normalParam;
    private String confId = "";
    private String password = "";

    private ConferenceMemberView localView;
    private MemberViewGroup callConferenceViewGroup;

    private EasePageIndicator pageIndicator;
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
    private ImageView micSwitchCover;
    // 摄像头开关
    private ImageButton cameraSwitch;
    private ImageView cameraSwitchCover;
    // 话筒开关
    private ImageButton speakerSwitch;
    // 屏幕分享开关
    private ImageButton screenShareSwitch;
    // 前后摄像头切换
    private ImageButton changeCameraSwitch;
    private ImageView changeCameraSwitchCover;
    // 挂断按钮
    private ImageButton hangupBtn;
    // 显示debug信息按钮
    private ImageButton debugBtn;
    // 申请视频连麦按钮
    private ImageButton videoConnectBtn;
    private ImageView videoConnectBtnCover;
    // 全屏模式下改变视频显示模式的按钮,只在全屏模式下显示
    private ImageButton scaleModeBtn;
    // ------ tools panel relevant end ------

    // ------ full screen views start -------
    private View stateCoverMain;
    private View membersLayout;
    private TextView membersTVMain;
    private TextView memberCountTVMain;
    private TextView callTimeViewMain;
    private View talkingLayout;
    private ImageView talkingImage;
    private TextView talkerTV;
    // ------ full screen views end -------

    private TimeHandler timeHandler;

    private List<EMConferenceStream> streamList = new ArrayList<>();

    private String inviter;
    // 如果该值不为null，则证明为群组入口的直播
    private String groupId;
    // 标识当前用户角色
    private EMConferenceManager.EMConferenceRole currentRole;
    // 标识当前上麦按钮状态
    private int btnState = STATE_AUDIENCE;

    // 如果groupId不为null,则表示呼叫类型为群组呼叫,显示的联系人只能是该群组中成员
    // 若groupId为null,则表示呼叫类型为联系人呼叫,显示的联系人为当前账号所有好友.
    public static void startLive(Context context, String groupId) {
        Intent i = new Intent(context, LiveActivity.class);
        i.putExtra(Constant.EXTRA_CONFERENCE_IS_CREATOR, true);
        i.putExtra(Constant.EXTRA_CONFERENCE_GROUP_ID, groupId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    // 如果groupId不为null,则表示呼叫类型为群组呼叫,显示的联系人只能是该群组中成员
    // 若groupId为null,则表示呼叫类型为联系人呼叫,显示的联系人为当前账号所有好友.
    public static void watch(Context context, String conferenceId, String password, String inviter) {
        Intent i = new Intent(context, LiveActivity.class);
        i.putExtra(Constant.EXTRA_CONFERENCE_ID, conferenceId);
        i.putExtra(Constant.EXTRA_CONFERENCE_PASS, password);
        i.putExtra(Constant.EXTRA_CONFERENCE_INVITER, inviter);
        i.putExtra(Constant.EXTRA_CONFERENCE_IS_CREATOR, false);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();

        EMClient.getInstance().conferenceManager().addConferenceListener(conferenceListener);
        DemoHelper.getInstance().pushActivity(activity);
        registerMessageListener();
    }

    /**
     * 初始化
     */
    private void init() {
        activity = this;

        callConferenceViewGroup = findViewById(R.id.surface_view_group);
        toolsPanelView = findViewById(R.id.layout_tools_panel);

        videoConnectBtn = findViewById(R.id.btn_request_connect);
        videoConnectBtnCover = findViewById(R.id.btn_request_connect_cover);
        membersTV = findViewById(R.id.tv_members);
        memberCountTV = findViewById(R.id.tv_member_count);
        callTimeView = findViewById(R.id.tv_call_time);
        micSwitch = findViewById(R.id.btn_mic_switch);
        micSwitchCover = findViewById(R.id.btn_mic_switch_cover);
        cameraSwitch = findViewById(R.id.btn_camera_switch);
        cameraSwitchCover = findViewById(R.id.btn_camera_switch_cover);
        speakerSwitch = findViewById(R.id.btn_speaker_switch);
        screenShareSwitch = findViewById(R.id.btn_desk_share);
        changeCameraSwitch = findViewById(R.id.btn_change_camera_switch);
        changeCameraSwitchCover = findViewById(R.id.btn_change_camera_switch_cover);
        hangupBtn = findViewById(R.id.btn_hangup);
        debugBtn = findViewById(R.id.btn_debug);
        scaleModeBtn = findViewById(R.id.btn_scale_mode);

        pageIndicator = findViewById(R.id.indicator);

        stateCoverMain = findViewById(R.id.state_cover_main);
        membersLayout = findViewById(R.id.layout_members);
        membersTVMain = findViewById(R.id.tv_members_main);
        memberCountTVMain = findViewById(R.id.tv_member_count_main);
        callTimeViewMain = findViewById(R.id.tv_call_time_main);
        talkingLayout = findViewById(R.id.layout_talking);
        talkingImage = findViewById(R.id.icon_talking);
        talkerTV = findViewById(R.id.tv_talker);

        callConferenceViewGroup.setOnItemClickListener(onItemClickListener);
        callConferenceViewGroup.setOnScreenModeChangeListener(onScreenModeChangeListener);
        callConferenceViewGroup.setOnPageStatusListener(onPageStatusListener);
        videoConnectBtn.setOnClickListener(listener);
        micSwitch.setOnClickListener(listener);
        speakerSwitch.setOnClickListener(listener);
        cameraSwitch.setOnClickListener(listener);
        screenShareSwitch.setOnClickListener(listener);
        changeCameraSwitch.setOnClickListener(listener);
        hangupBtn.setOnClickListener(listener);
        debugBtn.setOnClickListener(listener);
        scaleModeBtn.setOnClickListener(listener);

        conferenceListener = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        normalParam = new EMStreamParam();
        normalParam.setStreamType(EMConferenceStream.StreamType.NORMAL);
        normalParam.setVideoOff(false);
        normalParam.setAudioOff(false);

        micSwitch.setActivated(normalParam.isAudioOff());
        cameraSwitch.setActivated(normalParam.isVideoOff());
        speakerSwitch.setActivated(true);
        openSpeaker();

        boolean isCreator = getIntent().getBooleanExtra(Constant.EXTRA_CONFERENCE_IS_CREATOR, false);
        groupId = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_GROUP_ID);
        if (isCreator) {
            // 开始直播
            createAndJoinConference(new EMValueCallBack<EMConference>() {
                @Override
                public void onSuccess(EMConference value) {
                    // invite audiences.
                    inviteUserToJoinConference();
                }
                @Override
                public void onError(int error, String errorMsg) {
                }
            });

        } else {
            confId = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_ID);
            password = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_PASS);
            inviter = getIntent().getStringExtra(Constant.EXTRA_CONFERENCE_INVITER);

            joinConference();
        }

        timeHandler = new TimeHandler();
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_request_connect: // 申请视频连麦
                    if (currentRole == EMConferenceManager.EMConferenceRole.Admin) return;

                    if (btnState == STATE_AUDIENCE) { // 当前按钮状态是观众，需要变成主播
                        if (currentRole == EMConferenceManager.EMConferenceRole.Audience) { // 发送消息，申请上麦
                            String content = EMClient.getInstance().getCurrentUser() + " " + getString(R.string.alert_request_tobe_talker);
                            sendRequestMessage(content, inviter, Constant.OP_REQUEST_TOBE_SPEAKER);
                        } else { // 已经是主播，直接推流
                            publish();
                            setRequestBtnState(STATE_TALKER);
                        }
                    } else if (btnState == STATE_TALKER) { // 当前按钮状态是主播，需要下麦
                        if (currentRole == EMConferenceManager.EMConferenceRole.Talker) { // 申请下麦
                            // 下麦
                            unpublish(conference.getPubStreamId(EMConferenceStream.StreamType.NORMAL));
                            setRequestBtnState(STATE_AUDIENCE);
                            // 请求管理员改变自己角色
                            String content = EMClient.getInstance().getCurrentUser() + " " + getString(R.string.alert_request_tobe_audience);
                            sendRequestMessage(content, inviter, Constant.OP_REQUEST_TOBE_AUDIENCE);
                        }
                    }
                    break;
                case R.id.btn_mic_switch:
                    voiceSwitch();
                    break;
                case R.id.btn_speaker_switch:
                    speakerSwitch();
                    break;
                case R.id.btn_camera_switch:
                    videoSwitch();
                    break;
                case R.id.btn_desk_share:
                    break;
                case R.id.btn_change_camera_switch:
                    changeCamera();
                    break;
                case R.id.btn_hangup:
                    exitConference();
                    break;
                case R.id.btn_debug:
                    break;
                case R.id.btn_scale_mode: // 全屏状态下切换视频scale mode
                    changeFullScreenScaleMode();
                    break;
            }
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
        EMLog.d(TAG, "add conference view -start- " + stream.toString());
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
    }

    /**
     * 移除指定位置的 View，移除时如果已经订阅需要取消订阅
     */
    private void removeConferenceView(EMConferenceStream stream) {
        int index = streamList.indexOf(stream);
        ConferenceMemberView memberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(index);
        streamList.remove(stream);
        callConferenceViewGroup.removeView(memberView);
    }

    private boolean isPublishing() {
        return localView != null;
    }

    /**
     * 更新指定 View
     */
    private void updateConferenceMemberView(EMConferenceStream stream) {
        int position = streamList.indexOf(stream);
        ConferenceMemberView conferenceMemberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(position);
        conferenceMemberView.setAudioOff(stream.isAudioOff());
        conferenceMemberView.setVideoOff(stream.isVideoOff());
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
                    EMLog.i("currSpeakers", "currSpeakers: " + lastStreamId);
                    String speaker = null;
                    for (EMConferenceStream stream : streamList) {
                        EMLog.i("currSpeakers", "stream: " + stream.getStreamId());
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

    private MemberViewGroup.OnItemClickListener onItemClickListener = new MemberViewGroup.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
        }
    };

    private MemberViewGroup.OnScreenModeChangeListener onScreenModeChangeListener = new MemberViewGroup.OnScreenModeChangeListener() {
        @Override
        public void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView) {
            if (isFullScreenMode) { // 全屏模式
                toolsPanelView.setBackgroundColor(getResources().getColor(R.color.color_transparent));

                membersTV.setVisibility(View.INVISIBLE);
                memberCountTV.setVisibility(View.INVISIBLE);
                callTimeView.setVisibility(View.INVISIBLE);

                stateCoverMain.setVisibility(View.VISIBLE);
                membersLayout.setVisibility(View.VISIBLE);
                talkingLayout.setVisibility(View.VISIBLE);
                callTimeViewMain.setVisibility(View.VISIBLE);

                scaleModeBtn.setVisibility(View.VISIBLE);
            } else { // 非全屏模式
                toolsPanelView.setBackgroundColor(getResources().getColor(R.color.bg_tools_panel));

                membersTV.setVisibility(View.VISIBLE);
                memberCountTV.setVisibility(View.VISIBLE);
                callTimeView.setVisibility(View.VISIBLE);

                scaleModeBtn.setVisibility(View.INVISIBLE);

                // invisible the full-screen mode views.
                stateCoverMain.setVisibility(View.GONE);
                membersLayout.setVisibility(View.GONE);
                talkingLayout.setVisibility(View.GONE);
                callTimeViewMain.setVisibility(View.GONE);
            }
        }
    };

    private MemberViewGroup.OnPageStatusListener onPageStatusListener = new MemberViewGroup.OnPageStatusListener() {
        @Override
        public void onPageCountChange(int count) {
            // 多于1页时显示indicator.
            pageIndicator.setup(count > 1 ? count : 0);
        }

        @Override
        public void onPageScroll(int page) {
            pageIndicator.setItemChecked(page);
        }
    };

    /**
     * 作为创建者创建并加入会议
     */
    private void createAndJoinConference(final EMValueCallBack<EMConference> callBack) {
        EMClient.getInstance().conferenceManager().createAndJoinConference(EMConferenceManager.EMConferenceType.LiveStream,
                "", true, false, false, new EMValueCallBack<EMConference>() {
                    @Override
                    public void onSuccess(final EMConference value) {
                        EMLog.e(TAG, "create and join conference success" + value.toString());
                        currentRole = value.getConferenceRole();
                        conference = value;
                        startAudioTalkingMonitor();
                        timeHandler.startTime();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                publish();
                                // 申请连麦、下麦按钮对于管理员不能点击
                                videoConnectBtnCover.setVisibility(View.VISIBLE);

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
        EMClient.getInstance().conferenceManager().joinConference(confId, password,new EMValueCallBack<EMConference>() {
            @Override
            public void onSuccess(final EMConference value) {
                EMLog.e(TAG, "join conference success" + value.toString());
                currentRole = value.getConferenceRole();
                conference = value;
                timeHandler.startTime();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Join conference success", Toast.LENGTH_SHORT).show();
                        // 加入会议的成员身份为主播
                        if (value.getConferenceRole() == EMConferenceManager.EMConferenceRole.Talker) {
                            publish();
                            // 设置连麦按钮为‘申请下麦’
                            setRequestBtnState(STATE_TALKER);
                        }
                    }
                });
            }

            @Override
            public void onError(final int error, final String errorMsg) {
                EMLog.e(TAG, "join conference failed error " + error + ", msg " + errorMsg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Join conference failed " + error + " " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }
        });
    }

    private void inviteUserToJoinConference() {
        if (TextUtils.isEmpty(groupId)) { // 联系人界面入口直播，邀请全部联系人
            Collection<EaseUser> contacts = DemoHelper.getInstance().getContactList().values();
            for (EaseUser contact : contacts) {
                // 通过消息的方式邀请对方加入
                sendInviteMessage(contact.getUsername(), false);
            }
        } else { // 群组界面入口直播，发送一条群消息
            sendInviteMessage(groupId, true);
        }
    }

    /**
     * 通过消息的形式邀请他人加入会议
     *
     * @param to 被邀请人
     */
    private void sendInviteMessage(String to, boolean isGroupChat) {
        final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(to, EMConversation.EMConversationType.Chat, true);
        final EMMessage message = EMMessage.createTxtSendMessage(String.format(getString(R.string.msg_live_invite),
                EMClient.getInstance().getCurrentUser(), conference.getConferenceId()), to);
        message.setAttribute(Constant.EM_CONFERENCE_OP, Constant.OP_INVITE);
        message.setAttribute(Constant.EM_CONFERENCE_ID, conference.getConferenceId());
        message.setAttribute(Constant.EM_CONFERENCE_PASSWORD, conference.getPassword());
        message.setAttribute(Constant.EM_CONFERENCE_TYPE, conference.getConferenceType().code);
        if (isGroupChat) {
            message.setChatType(EMMessage.ChatType.GroupChat);
        }
        // 扩展字段对于音视频会议不是必须的,只是增加了额外用于显示或者判断音视频会议类型的信息.
//        message.setAttribute(Constant.MSG_ATTR_EXTENSION, extension);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite join conference success");
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite join conference error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    private void sendRequestMessage(String content, String to, String op) {
        final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(to, EMConversation.EMConversationType.Chat, true);
        final EMMessage message = EMMessage.createTxtSendMessage(content, to);
        message.setAttribute(Constant.EM_CONFERENCE_OP, op);
        message.setAttribute(Constant.EM_CONFERENCE_ID, conference.getConferenceId());
        message.setAttribute(Constant.EM_CONFERENCE_PASSWORD, conference.getPassword());
        message.setAttribute(Constant.EM_MEMBER_NAME, EasyUtils.getMediaRequestUid(EMClient.getInstance().getOptions().getAppKey(), EMClient.getInstance().getCurrentUser()));
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite join conference success");
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite join conference error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    /**
     * 退出会议
     */
    private void exitConference() {
        stopAudioTalkingMonitor();
        timeHandler.stopTime();

        // Stop to watch the phone call state.
        PhoneStateManager.get(LiveActivity.this).removeStateCallback(phoneStateCallback);

        if (currentRole == EMConferenceManager.EMConferenceRole.Admin) { // 管理员退出时销毁会议
            EMClient.getInstance().conferenceManager().destroyConference(new EMValueCallBack() {
                @Override
                public void onSuccess(Object value) {
                    EMLog.i(TAG, "destroyConference success");
                    finish();
                }

                @Override
                public void onError(int error, String errorMsg) {
                    EMLog.e(TAG, "destroyConference failed " + error + ", " + errorMsg);
                    finish();
                }
            });
        } else {
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
        EMLog.i(TAG, "publish start, params: " + normalParam.toString());

        initLocalConferenceView();
        setIcons(EMConferenceManager.EMConferenceRole.Talker);
        // 确保streamList中的stream跟viewGroup中的view位置对应。
        addOrUpdateStreamList(null, "local-stream");

        EMClient.getInstance().conferenceManager().publish(normalParam, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
                conference.setPubStreamId(value, EMConferenceStream.StreamType.NORMAL);
                localView.setStreamId(value);
                addOrUpdateStreamList("local-stream", value);

                // Start to watch the phone call state.
                PhoneStateManager.get(LiveActivity.this).addStateCallback(phoneStateCallback);
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "publish failed: error=" + error + ", msg=" + errorMsg);
            }
        });
    }

    /**
     * 停止推自己的数据
     */
    private void unpublish(final String publishId) {
        if (ScreenCaptureManager.getInstance().state == ScreenCaptureManager.State.RUNNING) {
            if (!TextUtils.isEmpty(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))
                    && publishId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))) {
                ScreenCaptureManager.getInstance().stop();
            }
        }
        EMClient.getInstance().conferenceManager().unpublish(publishId, new EMValueCallBack<String>() {
            @Override
            public void onSuccess(String value) {
                EMConferenceStream target = null;
                for (EMConferenceStream stream : streamList) {
                    if (stream.getStreamId().equals(publishId)) {
                        target = stream;
                        break;
                    }
                }

                final EMConferenceStream finalTarget = target;
                if (target != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // remove local view
                            removeConferenceView(finalTarget);
                            // set icons to audience state.
                            setIcons(EMConferenceManager.EMConferenceRole.Audience);
                        }
                    });
                }

                localView = null;
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
        if (!isPublishing()) return;

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
     * 扬声器开关
     */
    private void speakerSwitch() {
        if (speakerSwitch.isActivated()) {
            closeSpeaker();
        } else {
            openSpeaker();
        }
    }

    /**
     * 视频开关
     */
    private void videoSwitch() {
        if (!isPublishing()) return;

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
        if (!isPublishing()) return;

        EMClient.getInstance().conferenceManager().switchCamera();
    }

    // 当前设备通话状态监听器
    PhoneStateManager.PhoneStateCallback phoneStateCallback = new PhoneStateManager.PhoneStateCallback() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:   // 电话响铃
                    break;
                case TelephonyManager.CALL_STATE_IDLE:      // 电话挂断
                    // resume current voice conference.
                    if (normalParam.isAudioOff()) {
                        try {
                            EMClient.getInstance().callManager().resumeVoiceTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    if (normalParam.isVideoOff()) {
                        try {
                            EMClient.getInstance().callManager().resumeVideoTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:   // 来电接通 或者 去电，去电接通  但是没法区分
                    // pause current voice conference.
                    if (!normalParam.isAudioOff()) {
                        try {
                            EMClient.getInstance().callManager().pauseVoiceTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!normalParam.isVideoOff()) {
                        try {
                            EMClient.getInstance().callManager().pauseVideoTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        EMClient.getInstance().conferenceManager().removeConferenceListener(conferenceListener);
        DemoHelper.getInstance().popActivity(activity);
        super.onDestroy();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);
    }

    /**
     * --------------------------------------------------------------------
     * 多人音视频会议回调方法
     */

    @Override
    public void onMemberJoined(final EMConferenceMember member) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, member.memberName + " joined conference!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMemberExited(final EMConferenceMember member) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, member.memberName + " removed conference!", Toast.LENGTH_SHORT).show();

                if (EMClient.getInstance().getCurrentUser().equals(member.memberName)) {
                    setRequestBtnState(STATE_AUDIENCE);
                }
            }
        });
    }

    @Override
    public void onStreamAdded(final EMConferenceStream stream) {
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

    //
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
    public void onPassiveLeave(final int error, final String message) { // 当前用户被踢出会议
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Passive exit " + error + ", message" + message, Toast.LENGTH_SHORT).show();
                finish();
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

    @Override
    public void onRoleChanged(EMConferenceManager.EMConferenceRole role) {
        EMLog.i(TAG, "onRoleChanged, role: " + role);
        currentRole = role;

        if (role == EMConferenceManager.EMConferenceRole.Talker) {
            // 管理员把当前用户角色更改为主播或管理员。
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EMLog.i(TAG, "onRoleChanged, start publish, params: " + normalParam.toString());
                    publish();
                    setRequestBtnState(STATE_TALKER);
                }
            });
        } else if (role == EMConferenceManager.EMConferenceRole.Audience) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 下麦
                    unpublish(conference.getPubStreamId(EMConferenceStream.StreamType.NORMAL));
                    setRequestBtnState(STATE_AUDIENCE);
                }
            });
        }
    }

    @Override
    public void onAttributesUpdated(EMConferenceAttribute[] attributes) {

    }

    private void setRequestBtnState(int state) {
        btnState = state;
        if (state == STATE_AUDIENCE) {
            videoConnectBtn.setImageResource(R.drawable.em_call_request_connect);
        } else if (state == STATE_TALKER) {
            videoConnectBtn.setImageResource(R.drawable.em_call_request_disconnect);
        }
    }

    private void setIcons(EMConferenceManager.EMConferenceRole role) {
        if (role == EMConferenceManager.EMConferenceRole.Audience) {
            changeCameraSwitchCover.setVisibility(View.VISIBLE);
            cameraSwitchCover.setVisibility(View.VISIBLE);
            micSwitchCover.setVisibility(View.VISIBLE);
        } else {
            changeCameraSwitchCover.setVisibility(View.GONE);
            cameraSwitchCover.setVisibility(View.GONE);
            micSwitchCover.setVisibility(View.GONE);
        }
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

    private void addOrUpdateStreamList(String originStreamId, String targetStreamId) {
        EMConferenceStream localStream = null;
        if (originStreamId != null) {
            for (EMConferenceStream stream : streamList) {
                if (originStreamId.equals(stream.getStreamId())) {
                    localStream = stream;
                    break;
                }
            }
            if (localStream != null) {
                localStream.setStreamId(targetStreamId);
            }
        } else {
            localStream = new EMConferenceStream();
            localStream.setUsername(EMClient.getInstance().getCurrentUser());
            localStream.setStreamId(targetStreamId);
            streamList.add(localStream);
        }
    }

    protected void registerMessageListener() {
        EMMessageListener messageListener = new EMMessageListener() {
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                for (EMMessage msg : messages) {
                    String op = msg.getStringAttribute(Constant.EM_CONFERENCE_OP, "");
                    if (Constant.OP_REQUEST_TOBE_SPEAKER.equals(op)) {
                        // 把该申请上线消息从会话中删除
                        EMConversation conversation = EMClient.getInstance().chatManager().getConversation(msg.getFrom(), EMConversation.EMConversationType.Chat, true);
                        conversation.removeMessage(msg.getMsgId());

                        final String jid = msg.getStringAttribute(Constant.EM_MEMBER_NAME, "");
                        final String content = EasyUtils.useridFromJid(jid) + " " + getString(R.string.alert_request_tobe_talker);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                EaseAlertDialog dialog = new EaseAlertDialog(LiveActivity.this, getString(R.string.prompt), content, null, null, true) {
                                    @Override
                                    public void onCancel(View view) {
                                        super.onCancel(view);
                                        EMLog.i(TAG, "onCancel");
                                    }

                                    @Override
                                    public void onOk(View view) {
                                        super.onOk(view);
                                        EMLog.i(TAG, "onOk");
                                        // changeRole.
                                        EMClient.getInstance().conferenceManager().grantRole(conference.getConferenceId()
                                                , new EMConferenceMember(jid, null,null)
                                                , EMConferenceManager.EMConferenceRole.Talker, new EMValueCallBack<String>() {
                                                    @Override
                                                    public void onSuccess(String value) {
                                                        EMLog.i(TAG, "changeRole success, result: " + value);
                                                    }

                                                    @Override
                                                    public void onError(int error, String errorMsg) {
                                                        EMLog.i(TAG, "changeRole failed, error: " + error + " - " + errorMsg);
                                                    }
                                                });
                                    }
                                };
                                if (!LiveActivity.this.isFinishing()) {
                                    dialog.show();
                                } else {
                                    EMLog.i(TAG, "activity is finishing when dialog want to show.");
                                }
                            }
                        });
                    } else if (Constant.OP_REQUEST_TOBE_AUDIENCE.equals(op)) {
                        // 把该申请下线消息从会话中删除
                        EMConversation conversation = EMClient.getInstance().chatManager().getConversation(msg.getFrom(), EMConversation.EMConversationType.Chat, true);
                        conversation.removeMessage(msg.getMsgId());

                        String jid = msg.getStringAttribute(Constant.EM_MEMBER_NAME, "");
                        // changeRole.
                        EMClient.getInstance().conferenceManager().grantRole(conference.getConferenceId()
                                , new EMConferenceMember(jid, null, null)
                                , EMConferenceManager.EMConferenceRole.Audience, new EMValueCallBack<String>() {
                                    @Override
                                    public void onSuccess(String value) {
                                        EMLog.i(TAG, "changeRole success, result: " + value);
                                    }

                                    @Override
                                    public void onError(int error, String errorMsg) {
                                        EMLog.i(TAG, "changeRole failed, error: " + error + " - " + errorMsg);
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
            }

            @Override
            public void onMessageRead(List<EMMessage> messages) {
            }

            @Override
            public void onMessageDelivered(List<EMMessage> message) {
            }

            @Override
            public void onMessageRecalled(List<EMMessage> messages) {
            }

            @Override
            public void onMessageChanged(EMMessage message, Object change) {
            }
        };

        EMClient.getInstance().chatManager().addMessageListener(messageListener);
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

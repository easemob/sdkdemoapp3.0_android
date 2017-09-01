package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.conference.EMConference;
import com.hyphenate.chat.conference.EMConferenceListener;
import com.hyphenate.chat.conference.EMConferenceMember;
import com.hyphenate.chat.conference.EMConferenceStream;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.BaseActivity;
import com.hyphenate.chatuidemo.widget.EaseViewGroup;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzan13 on 2017/8/15.
 * 多人音视频会议界面
 */
public class ConferenceActivity extends BaseActivity implements EMConferenceListener {

    private final String TAG = this.getClass().getSimpleName();
    private ConferenceActivity activity;
    private EMConferenceListener conferenceListener;

    private AudioManager audioManager;

    private boolean isCreator = false;

    // 屏幕宽度，为了处理画面展示大小
    private int screenWidth;

    // 会议实体类对象，保存了会议的一些简单信息
    private EMConference conference;

    private List<EMConferenceStream> streamList = new ArrayList<>();

    private ConferenceMemberView localView;
    private EaseViewGroup callConferenceViewGroup;
    private View rootView;
    private View controlLayout;
    private RelativeLayout surfaceLayout;
    private ImageButton exitFullScreenBtn;
    private ImageButton inviteJoinBtn;
    private TextView callTimeView;
    private ImageButton micSwitch;
    private ImageButton cameraSwitch;
    private ImageButton speakerSwitch;
    private ImageButton changeCameraSwitch;
    private ImageButton cancelBtn;
    private ImageButton exitBtn;
    private ImageButton addBtn;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        init();

        initConferenceViewGroup();
    }

    /**
     * 初始化
     */
    private void init() {
        activity = this;

        callConferenceViewGroup = (EaseViewGroup) findViewById(R.id.surface_view_group);
        rootView = findViewById(R.id.layout_root);
        controlLayout = findViewById(R.id.layout_call_control);
        surfaceLayout = (RelativeLayout) findViewById(R.id.layout_surface_container);
        exitFullScreenBtn = (ImageButton) findViewById(R.id.btn_exit_full_screen);
        inviteJoinBtn = (ImageButton) findViewById(R.id.btn_invite_join);
        callTimeView = (TextView) findViewById(R.id.text_call_time);
        micSwitch = (ImageButton) findViewById(R.id.btn_mic_switch);
        cameraSwitch = (ImageButton) findViewById(R.id.btn_camera_switch);
        speakerSwitch = (ImageButton) findViewById(R.id.btn_speaker_switch);
        changeCameraSwitch = (ImageButton) findViewById(R.id.btn_change_camera_switch);
        cancelBtn = (ImageButton) findViewById(R.id.btn_cancel);
        exitBtn = (ImageButton) findViewById(R.id.btn_exit);
        addBtn = (ImageButton) findViewById(R.id.btn_add);

        exitFullScreenBtn.setOnClickListener(listener);
        inviteJoinBtn.setOnClickListener(listener);
        micSwitch.setOnClickListener(listener);
        speakerSwitch.setOnClickListener(listener);
        cameraSwitch.setOnClickListener(listener);
        changeCameraSwitch.setOnClickListener(listener);
        cancelBtn.setOnClickListener(listener);
        exitBtn.setOnClickListener(listener);
        addBtn.setOnClickListener(listener);

        conferenceListener = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        String confId = getIntent().getStringExtra("confId");
        String password = getIntent().getStringExtra("password");
        if (!TextUtils.isEmpty(confId)) {
            conference = new EMConference();
            conference.setConferenceId(confId);
            conference.setPassword(password);
        }
        // 会议不存在就创建，正常流程是从外边传递获取
        if (conference == null) {
            conference = new EMConference();
        }
        conference.setEnableVideo(true);
        micSwitch.setActivated(!conference.isEnableVoice());
        cameraSwitch.setActivated(!conference.isEnableVideo());
        speakerSwitch.setActivated(conference.isEnableSpeaker());

        isCreator = getIntent().getBooleanExtra("isCreator", false);
        if (isCreator) {
            creatorCreateConference();
            cancelBtn.setVisibility(View.GONE);
            addBtn.setVisibility(View.GONE);
        } else {
            memberGetConference();
            exitBtn.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_exit_full_screen:
                    Toast.makeText(activity, "暂未实现最小化", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btn_invite_join:
                    inviteUserToJoinConference();
                    break;
                case R.id.btn_mic_switch:
                    voiceSwitch();
                    break;
                case R.id.btn_speaker_switch:
                    if (conference.isEnableSpeaker()) {
                        closeSpeaker();
                    } else {
                        openSpeaker();
                    }
                    break;
                case R.id.btn_camera_switch:
                    videoSwitch();
                    break;
                case R.id.btn_change_camera_switch:
                    changeCamera();
                    break;
                case R.id.btn_cancel:
                    finish();
                    break;
                case R.id.btn_exit:
                    exitConference();
                    break;
                case R.id.btn_add:
                    joinConference();
                    break;
            }
        }
    };

    /**
     * 初始化多人音视频画面管理控件
     */
    private void initConferenceViewGroup() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(0, 0);
        lp.width = screenWidth;
        lp.height = screenWidth;
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        callConferenceViewGroup.setLayoutParams(lp);

        localView = new ConferenceMemberView(activity);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        params.width = screenWidth;
        params.height = screenWidth;
        localView.setLayoutParams(params);
        localView.updateVideoState(!conference.isEnableVideo());
        localView.updateMuteState(!conference.isEnableVoice());
        callConferenceViewGroup.addView(localView);
        EMClient.getInstance().conferenceManager().setLocalSurfaceView(localView.getSurfaceView());
        localView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (conference.isEnablePublish()) {
                    stopPublish();
                } else {
                    startPublish();
                }
            }
        });
    }

    /**
     * 添加一个展示远端画面的 view
     */
    private void addConferenceView() {
        final ConferenceMemberView memberView = new ConferenceMemberView(activity);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        params.width = screenWidth / 3;
        params.height = screenWidth / 3;
        memberView.setLayoutParams(params);
        callConferenceViewGroup.addView(memberView);
        //设置 view 点击监听
        memberView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                int index = callConferenceViewGroup.indexOfChild(view);
                final EMConferenceStream stream = streamList.get(index - 1);
                if (memberView.isPubOrSub()) {
                    // 已订阅就取消订阅
                    EMClient.getInstance().conferenceManager().stopSubscribeStream(stream);
                } else {
                    // 未订阅就去订阅
                    EMClient.getInstance().conferenceManager().startSubscribeStream(stream, memberView.getSurfaceView());
                }
            }
        });
    }

    /**
     * 移除指定位置的 View，移除时如果已经订阅需要取消订阅
     */
    private void removeConferenceView(EMConferenceStream stream) {
        int index = streamList.indexOf(stream);
        final ConferenceMemberView memberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(index + 1);
        if (memberView.isPubOrSub()) {
            // 已订阅就取消订阅
            EMClient.getInstance().conferenceManager().stopSubscribeStream(stream);
        }
        streamList.remove(stream);
        callConferenceViewGroup.removeView(memberView);
    }

    /**
     * 更新指定 View
     */
    private void updateConferenceView(EMConferenceStream stream) {
        int position = streamList.indexOf(stream);
        ConferenceMemberView conferenceMemberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(position + 1);
        conferenceMemberView.updateMuteState(stream.isAudioOff());
        conferenceMemberView.updateVideoState(stream.isVideoOff());
        int memberViewSize = 0;
        if (streamList.size() > 8) {
            memberViewSize = screenWidth / 4;
        } else if (streamList.size() > 3) {
            memberViewSize = screenWidth / 3;
        } else if (streamList.size() >= 1) {
            memberViewSize = screenWidth / 2;
        } else {
            memberViewSize = screenWidth;
        }
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(0, 0);
        lp.width = memberViewSize;
        lp.height = memberViewSize;
        for (int i = 0; i < callConferenceViewGroup.getChildCount(); i++) {
            ConferenceMemberView view = (ConferenceMemberView) callConferenceViewGroup.getChildAt(i);
            view.setLayoutParams(lp);
        }
    }

    /**
     * 作为会议创建者创建会议
     */
    private void creatorCreateConference() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    // 创建者需要先创建会议，得到 ticket，然后邀请其他人加入
                    EMConference temp = EMClient.getInstance().conferenceManager().creatorCreateConference("");
                    // 这里因为外部可能已经设置了 conference 其他信息，所以没有直接 =
                    conference.setTicket(temp.getTicket());
                    conference.setConferenceId(temp.getConferenceId());
                    conference.setPassword(temp.getPassword());
                    joinConference();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 作为成员获取会议信息
     */
    private void memberGetConference() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    // 成员加入前需要根据收到的 ConferenceId 和 Password 获取
                    EMConference temp = EMClient.getInstance()
                            .conferenceManager()
                            .memberGetConference(conference.getConferenceId(), conference.getPassword());
                    // 这里因为外部可能已经设置了 conference 其他信息，所以没有直接 =
                    conference.setTicket(temp.getTicket());
                    conference.setConferenceId(temp.getConferenceId());
                    conference.setPassword(temp.getPassword());
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 加入会议，不论创建者还是其他成员，都需要主动去调用加入会议方法
     */
    private void joinConference() {
        cancelBtn.setVisibility(View.GONE);
        exitBtn.setVisibility(View.VISIBLE);
        addBtn.setVisibility(View.GONE);
        EMClient.getInstance().conferenceManager().joinConference(conference, "{'extension':'member ext'}");
    }

    private void inviteUserToJoinConference() {
        Intent intent = new Intent(activity, ConferenceInviteJoinActivity.class);
        activity.startActivityForResult(intent, 0);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final String[] members = data.getStringArrayExtra("members");
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        for (int i = 0; i < members.length; i++) {
                            EMClient.getInstance()
                                    .conferenceManager()
                                    .inviteUserToJoinConference(conference.getConferenceId(), conference.getPassword(),
                                            members[i], "{'extension':'invite ext'}");
                        }
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * 退出会议
     */
    private void exitConference() {
        EMClient.getInstance().conferenceManager().exitConference();
    }

    /**
     * 开始推自己的数据
     */
    private void startPublish() {
        EMClient.getInstance().conferenceManager().startPublishStream(conference);
    }

    /**
     * 停止推自己的数据
     */
    private void stopPublish() {
        EMClient.getInstance().conferenceManager().stopPublishStream();
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
        conference.setEnableSpeaker(true);
        speakerSwitch.setActivated(conference.isEnableSpeaker());
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
        conference.setEnableSpeaker(false);
        speakerSwitch.setActivated(conference.isEnableSpeaker());
    }

    /**
     * 语音开关
     */
    private void voiceSwitch() {
        if (conference.isEnableVoice()) {
            conference.setEnableVoice(false);
            EMClient.getInstance().conferenceManager().closeVoiceTransfer();
        } else {
            conference.setEnableVoice(true);
            EMClient.getInstance().conferenceManager().openVoiceTransfer();
        }
        micSwitch.setActivated(!conference.isEnableVoice());
        localView.updateMuteState(!conference.isEnableVoice());
    }

    /**
     * 视频开关
     */
    private void videoSwitch() {
        if (conference.isEnableVideo()) {
            conference.setEnableVideo(false);
            EMClient.getInstance().conferenceManager().closeVideoTransfer();
        } else {
            conference.setEnableVideo(true);
            EMClient.getInstance().conferenceManager().openVideoTransfer();
        }
        cameraSwitch.setActivated(!conference.isEnableVideo());
        localView.updateVideoState(!conference.isEnableVideo());
    }

    /**
     * 切换摄像头
     */
    private void changeCamera() {
        if (EMClient.getInstance().conferenceManager().getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            changeCameraSwitch.setImageResource(R.drawable.ic_camera_rear_white_24dp);
        } else {
            changeCameraSwitch.setImageResource(R.drawable.ic_camera_front_white_24dp);
        }
        EMClient.getInstance().conferenceManager().switchCamera();
    }

    @Override protected void onStart() {
        super.onStart();
        EMClient.getInstance().conferenceManager().addConferenceListener(conferenceListener);
    }

    @Override protected void onStop() {
        super.onStop();
        EMClient.getInstance().conferenceManager().removeConferenceListener(conferenceListener);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (conference.getStatus() != EMConference.ConferenceStage.PROCESSING) {
            exitConference();
        }
    }

    /**
     * --------------------------------------------------------------------
     * 多人音视频会议回调方法
     */

    @Override public void onMemberJoined(final EMConferenceMember member) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, member.getUsername() + " joined conference!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onMemberExited(final EMConferenceMember member) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, member.getUsername() + " removed conference!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onStreamAdded(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream add!", Toast.LENGTH_SHORT).show();
                streamList.add(stream);
                addConferenceView();
                updateConferenceView(stream);
            }
        });
    }

    @Override public void onStreamRemoved(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream removed!", Toast.LENGTH_SHORT).show();
                if (streamList.contains(stream)) {
                    removeConferenceView(stream);
                    updateConferenceView(stream);
                }
            }
        });
    }

    @Override public void onStreamUpdate(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream update!", Toast.LENGTH_SHORT).show();
                updateConferenceView(stream);
            }
        });
    }

    @Override public void onPassiveLeave(final ConferenceError error, final String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "Passive exit " + error + ", message" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onState(final ConferenceState state, final String confId, final Object object) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "State=" + state + ", confId=" + confId + ", object=" + object, Toast.LENGTH_SHORT)
                        .show();
            }
        });
        switch (state) {
            case STATE_EXIT_CONFERENCE:
                // 会议退出成功，更改会议进展状态为准备中
                conference.setStatus(EMConference.ConferenceStage.READY);
                streamList.clear();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        callConferenceViewGroup.removeAllViews();
                        finish();
                    }
                });
                break;
        }
    }

    @Override public void onError(final ConferenceError error, final String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "Error " + error + ", message" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onPublish(ConferenceState state, ConferenceError error, String message) {
        switch (state) {
            case STATE_PUBLISH_CANCEL:
                if (error == null) {
                    conference.setEnablePublish(false);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            localView.setPubOrSub(conference.isEnablePublish());
                        }
                    });
                }
                break;
            case STATE_PUBLISH_SETUP:
                if (error == null) {
                    conference.setEnablePublish(true);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            localView.setPubOrSub(conference.isEnablePublish());
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onSubscribe(ConferenceState state, final EMConferenceStream stream, ConferenceError error, String message) {
        switch (state) {
            case STATE_SUBSCRIBE_CANCEL:
                if (error == null && streamList.contains(stream)) {
                    int index = streamList.indexOf(stream);
                    final ConferenceMemberView cancelView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(index + 1);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            cancelView.setPubOrSub(false);
                        }
                    });
                }
                break;
            case STATE_SUBSCRIBE_SETUP:
                if (error == null && streamList.contains(stream)) {
                    int index = streamList.indexOf(stream);
                    final ConferenceMemberView setupView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(index + 1);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            setupView.updateMuteState(stream.isAudioOff());
                            setupView.updateVideoState(stream.isVideoOff());
                            setupView.setPubOrSub(true);
                        }
                    });
                }
                break;
        }
    }
    /**
     * 收到其他人的会议邀请
     *
     * @param confId 会议 id
     * @param password 会议密码
     * @param extension 邀请扩展内容
     */
    @Override public void onReceiveInvite(final String confId, String password, String extension) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "Receive new conference invite " + confId, Toast.LENGTH_LONG).show();
            }
        });
        //conference.setConferenceId(confId);
        //conference.setPassword(password);
        //conference.setExtension(extension);
    }
}

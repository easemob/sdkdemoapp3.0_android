package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConference;
import com.hyphenate.EMConferenceListener;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chat.EMStreamParam;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.BaseActivity;
import com.hyphenate.chatuidemo.widget.EaseViewGroup;
import com.hyphenate.util.EMLog;
import com.superrtc.mediamanager.ScreenCaptureManager;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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
    private int screenHeight;
    private boolean isFullScreen = false;

    private List<EMConferenceStream> streamList = new ArrayList<>();

    private ConferenceMemberView localView;
    private EaseViewGroup callConferenceViewGroup;
    private View rootView;
    private View controlLayout;
    private RelativeLayout surfaceLayout;
    private ImageButton inviteJoinBtn;
    private TextView callTimeView;
    private ImageButton micSwitch;
    private ImageButton cameraSwitch;
    private ImageButton speakerSwitch;
    private ImageButton screenShareSwitch;
    private ImageButton changeCameraSwitch;
    private ImageButton cancelBtn;
    private ImageButton exitBtn;
    private ImageButton addBtn;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();

        initConferenceViewGroup();
        EMClient.getInstance().conferenceManager().addConferenceListener(conferenceListener);
        DemoHelper.getInstance().pushActivity(activity);
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
        inviteJoinBtn = (ImageButton) findViewById(R.id.btn_invite_join);
        callTimeView = (TextView) findViewById(R.id.text_call_time);
        micSwitch = (ImageButton) findViewById(R.id.btn_mic_switch);
        cameraSwitch = (ImageButton) findViewById(R.id.btn_camera_switch);
        speakerSwitch = (ImageButton) findViewById(R.id.btn_speaker_switch);
        screenShareSwitch = (ImageButton) findViewById(R.id.btn_desktop_switch);
        changeCameraSwitch = (ImageButton) findViewById(R.id.btn_change_camera_switch);
        cancelBtn = (ImageButton) findViewById(R.id.btn_cancel);
        exitBtn = (ImageButton) findViewById(R.id.btn_exit);
        addBtn = (ImageButton) findViewById(R.id.btn_add);

        inviteJoinBtn.setOnClickListener(listener);
        micSwitch.setOnClickListener(listener);
        speakerSwitch.setOnClickListener(listener);
        cameraSwitch.setOnClickListener(listener);
        screenShareSwitch.setOnClickListener(listener);
        changeCameraSwitch.setOnClickListener(listener);
        cancelBtn.setOnClickListener(listener);
        exitBtn.setOnClickListener(listener);
        addBtn.setOnClickListener(listener);

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
            createAndJoinConference();
            cancelBtn.setVisibility(View.GONE);
            addBtn.setVisibility(View.GONE);
        } else {
            exitBtn.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_invite_join:
                    inviteUserToJoinConference();
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
                case R.id.btn_desktop_switch:
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
        screenHeight = dm.heightPixels;

        localView = new ConferenceMemberView(activity);
        callConferenceViewGroup.addView(localView);
        ViewGroup.LayoutParams params = localView.getLayoutParams();
        params.width = screenWidth;
        params.height = screenWidth;
        localView.setLayoutParams(params);
        localView.setVideoOff(normalParam.isVideoOff());
        localView.setAudioOff(normalParam.isAudioOff());
        localView.setUsername(EMClient.getInstance().getCurrentUser());
        localView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFullScreen) {
                    updateConferenceViewGroup();
                }else{
                    fullScreen(localView);
                }
            }
        });
        EMClient.getInstance().conferenceManager().setLocalSurfaceView(localView.getSurfaceView());
    }

    /**
     * 添加一个展示远端画面的 view
     */
    private void addConferenceView(EMConferenceStream stream) {
        EMLog.d(TAG, "add conference view -start- " + stream.getMemberName());
        streamList.add(stream);
        final ConferenceMemberView memberView = new ConferenceMemberView(activity);
        callConferenceViewGroup.addView(memberView);
        ViewGroup.LayoutParams params = memberView.getLayoutParams();
        params.width = screenWidth;
        params.height = screenWidth;
        memberView.setLayoutParams(params);
        memberView.setUsername(stream.getUsername());
        memberView.setStreamId(stream.getStreamId());
        memberView.setAudioOff(stream.isAudioOff());
        memberView.setVideoOff(stream.isVideoOff());
        memberView.setDesktop(stream.getStreamType() == EMConferenceStream.StreamType.DESKTOP);
        subscribe(stream, memberView);
        EMLog.d(TAG, "add conference view -end-" + stream.getMemberName());
        memberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFullScreen) {
                    updateConferenceViewGroup();
                }else{
                    fullScreen(memberView);
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
        streamList.remove(stream);
        callConferenceViewGroup.removeView(memberView);
    }

    /**
     * 更新指定 View
     */
    private void updateConferenceMemberView(EMConferenceStream stream) {
        int position = streamList.indexOf(stream);
        ConferenceMemberView conferenceMemberView = (ConferenceMemberView) callConferenceViewGroup.getChildAt(position + 1);
        conferenceMemberView.setAudioOff(stream.isAudioOff());
        conferenceMemberView.setVideoOff(stream.isVideoOff());
    }

    /**
     * 更新所有 Member view
     */
    private void updateConferenceViewGroup() {
        isFullScreen = false;
        int memberViewSize;
        if (streamList.size() > 8) {
            memberViewSize = screenWidth / 4;
        } else if (streamList.size() > 3) {
            memberViewSize = screenWidth / 3;
        } else if (streamList.size() >= 1) {
            memberViewSize = screenWidth / 2;
        } else {
            memberViewSize = screenWidth;
        }
        ConferenceMemberView view;
        ViewGroup.LayoutParams params;
        for (int i = 0; i < callConferenceViewGroup.getChildCount(); i++) {
            view = (ConferenceMemberView) callConferenceViewGroup.getChildAt(i);
            params = view.getLayoutParams();
            params.width = memberViewSize;
            params.height = memberViewSize;
            view.setLayoutParams(params);
        }
    }

    /**
     * 点击全屏显示
     */
    public void fullScreen(ConferenceMemberView currView) {
        if (currView.isVideoOff()) {
            return;
        }
        isFullScreen = true;
        ViewGroup.LayoutParams params;
        ConferenceMemberView view;
        for (int i = 0; i < callConferenceViewGroup.getChildCount(); i++) {
            view = (ConferenceMemberView) callConferenceViewGroup.getChildAt(i);
            if (currView != view) {
                params = view.getLayoutParams();
                params.width = 1;
                params.height = 1;
                view.setLayoutParams(params);
            } else {
                params = view.getLayoutParams();
                params.width = screenWidth;
                params.height = screenHeight;
                view.setLayoutParams(params);
            }
        }
    }

    /**
     * 更新当前说话者
     */
    private void currSpeakers(List<String> speakers){
        localView.setTalking(speakers.contains(localView.getStreamId()));
        for (int i=0; i<callConferenceViewGroup.getChildCount(); i++) {
            ConferenceMemberView view = (ConferenceMemberView) callConferenceViewGroup.getChildAt(i);
            view.setTalking(speakers.contains(view.getStreamId()));
        }
    }

    /**
     * 作为创建者创建并加入会议
     */
    private void createAndJoinConference() {
        EMClient.getInstance().conferenceManager().createAndJoinConference(password, new EMValueCallBack<EMConference>() {
            @Override public void onSuccess(EMConference value) {
                EMLog.e(TAG, "create and join conference success");
                conference = value;
                startAudioTalkingMonitor();
                publish();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(activity, "Create and join conference success", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "Create and join conference failed error " + error + ", msg " + errorMsg);
            }
        });
    }

    /**
     * 作为成员直接根据 confId 和 password 加入会议
     */
    private void joinConference() {
        cancelBtn.setVisibility(View.GONE);
        exitBtn.setVisibility(View.VISIBLE);
        addBtn.setVisibility(View.GONE);
        EMClient.getInstance().conferenceManager().joinConference(confId, password, new EMValueCallBack<EMConference>() {
            @Override public void onSuccess(EMConference value) {
                conference = value;
                startAudioTalkingMonitor();
                publish();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(activity, "Join conference success", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "join conference failed error " + error + ", msg " + errorMsg);
            }
        });
    }

    /**
     * 邀请他人加入会议
     */
    private void inviteUserToJoinConference() {
        if (conference == null) {
            Toast.makeText(activity, R.string.conference_invite_error, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(activity, ConferenceInviteJoinActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_INVITE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ScreenCaptureManager.RECORD_REQUEST_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ScreenCaptureManager.getInstance().start(resultCode, data);
                }
            } else if (requestCode == REQUEST_CODE_INVITE) {
                final String[] members = data.getStringArrayExtra("members");
                try {
                    JSONObject object = new JSONObject();
                    int type = 0;
                    if (!cameraSwitch.isActivated()) {
                        type = 1;
                    }
                    object.put("type", type);
                    object.put("creater", EMClient.getInstance().getCurrentUser());
                    for (int i = 0; i < members.length; i++) {
                        EMClient.getInstance()
                                .conferenceManager()
                                .inviteUserToJoinConference(conference.getConferenceId(), conference.getPassword(), members[i],
                                        object.toString(), new EMValueCallBack() {
                                            @Override public void onSuccess(Object value) {
                                                EMLog.e(TAG, "invite join conference success");
                                            }

                                            @Override public void onError(int error, String errorMsg) {
                                                EMLog.e(TAG, "invite join conference failed " + error + ", " + errorMsg);
                                            }
                                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 退出会议
     */
    private void exitConference() {
        stopAudioTalkingMonitor();
        EMClient.getInstance().conferenceManager().exitConference(new EMValueCallBack() {
            @Override public void onSuccess(Object value) {
                finish();
            }

            @Override public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "exit conference failed " + error + ", " + errorMsg);
                finish();
            }
        });
    }

    private void startAudioTalkingMonitor(){
        EMClient.getInstance().conferenceManager().startMonitorSpeaker(300);
    }

    private void stopAudioTalkingMonitor(){
        EMClient.getInstance().conferenceManager().stopMonitorSpeaker();
    }

    /**
     * 开始推自己的数据
     */
    private void publish() {
        EMClient.getInstance().conferenceManager().publish(normalParam, new EMValueCallBack<String>() {
            @Override public void onSuccess(String value) {
                conference.setPubStreamId(value, EMConferenceStream.StreamType.NORMAL);
                localView.setStreamId(value);
            }

            @Override public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "publish failed: error=" + error + ", msg=" + errorMsg);
            }
        });
    }


    private void startScreenCapture(){
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

    public void publishDesktop(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            desktopParam.setShareView(null);
        }else{
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
            @Override public void onSuccess(String value) {}

            @Override public void onError(int error, String errorMsg) {
                EMLog.e(TAG, "unpublish failed: error=" + error + ", msg=" + errorMsg);
            }
        });
    }

    /**
     * 订阅指定成员 stream
     */
    private void subscribe(EMConferenceStream stream, final ConferenceMemberView memberView) {
        EMClient.getInstance().conferenceManager().subscribe(stream, memberView.getSurfaceView(), new EMValueCallBack<String>() {
            @Override public void onSuccess(String value) {
            }

            @Override public void onError(int error, String errorMsg) {

            }
        });
    }

    /**
     * 取消订阅指定成员 stream
     */
    private void unsubscribe(EMConferenceStream stream) {
        EMClient.getInstance().conferenceManager().unsubscribe(stream, new EMValueCallBack<String>() {
            @Override public void onSuccess(String value) {
            }

            @Override public void onError(int error, String errorMsg) {

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
        if (EMClient.getInstance().conferenceManager().getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            changeCameraSwitch.setImageResource(R.drawable.ic_camera_rear_white_24dp);
        } else {
            changeCameraSwitch.setImageResource(R.drawable.ic_camera_front_white_24dp);
        }
        EMClient.getInstance().conferenceManager().switchCamera();
    }

    @Override
    public void onBackPressed() {
//        exitConference();
    }


    @Override protected void onDestroy() {
        EMClient.getInstance().conferenceManager().removeConferenceListener(conferenceListener);
        DemoHelper.getInstance().popActivity(activity);
        super.onDestroy();
    }

    /**
     * --------------------------------------------------------------------
     * 多人音视频会议回调方法
     */

    @Override public void onMemberJoined(final String username) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, username + " joined conference!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onMemberExited(final String username) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, username + " removed conference!", Toast.LENGTH_SHORT).show();
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
                updateConferenceViewGroup();
            }
        });
    }

    @Override public void onStreamRemoved(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream removed!", Toast.LENGTH_SHORT).show();
                if (streamList.contains(stream)) {
                    removeConferenceView(stream);
                    updateConferenceViewGroup();
                }
            }
        });
    }

    @Override public void onStreamUpdate(final EMConferenceStream stream) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, stream.getUsername() + " stream update!", Toast.LENGTH_SHORT).show();
                updateConferenceMemberView(stream);
            }
        });
    }

    @Override public void onPassiveLeave(final int error, final String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "Passive exit " + error + ", message" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onConferenceState(final ConferenceState state) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "State=" + state, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onStreamSetup(final String streamId) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (streamId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.NORMAL))
                        ||streamId.equals(conference.getPubStreamId(EMConferenceStream.StreamType.DESKTOP))) {
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
     * @param confId 会议 id
     * @param password 会议密码
     * @param extension 邀请扩展内容
     */
    @Override public void onReceiveInvite(final String confId, String password, String extension) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(activity, "Receive invite " + confId, Toast.LENGTH_LONG).show();
            }
        });
    }
}

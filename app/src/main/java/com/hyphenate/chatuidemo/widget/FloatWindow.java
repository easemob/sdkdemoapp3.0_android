package com.hyphenate.chatuidemo.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.conference.ConferenceActivity;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.media.EMCallSurfaceView;
import com.superrtc.sdk.VideoView;

/**
 * Created by lzan13 on 2017/3/27.
 * <p>
 * float window control
 */
public class FloatWindow {
    private static final String TAG = "FloatWindow";

    private final int REFRESH_UI = 0;
    private final int REFRESH_TIME = 1;

    private Context context;

    private static FloatWindow instance;

    private WindowManager windowManager = null;
    private WindowManager.LayoutParams layoutParams = null;

    private View floatView;
    private ImageView avatarView;
    private EMCallSurfaceView localView;

    // local broadcast
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver broadcastReceiver;

    public FloatWindow(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static FloatWindow getInstance(Context context) {
        if (instance == null) {
            instance = new FloatWindow(context);
        }
        return instance;
    }

    /**
     * add float window
     */
    public void addFloatWindow(int callType, String username) { // 0: voice call; 1: video call;
        if (floatView != null) {
            return;
        }
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        floatView = LayoutInflater.from(context).inflate(R.layout.em_widget_call_float_window, null);
        windowManager.addView(floatView, layoutParams);
        avatarView = (ImageView) floatView.findViewById(R.id.iv_avatar);
        EaseUserUtils.setUserAvatar(context, username, avatarView);

        updateFloatWindow(callType);

        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ConferenceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                removeFloatWindow();
            }
        });

        floatView.setOnTouchListener(new View.OnTouchListener() {
            boolean result = false;

            float x = 0;
            float y = 0;
            float startX = 0;
            float startY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        result = false;
                        x = event.getX();
                        y = event.getY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 20 || Math.abs(event.getRawY() - startY) > 20) {
                            result = true;
                        }
                        layoutParams.x = (int) (event.getRawX() - x);
                        layoutParams.y = (int) (event.getRawY() - y - 25);
                        windowManager.updateViewLayout(floatView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return result;
            }
        });
        registerBroadcast();
    }

    public void updateFloatWindow(int callType) {
        if (callType == 0) {
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.VISIBLE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.GONE);
            refreshCallTime();
        } else {
            setupSurfaceView();
        }
    }

    public boolean isShowing() {
        return floatView != null;
    }

    /**
     * set call surface view
     */
    private void setupSurfaceView() {
        floatView.findViewById(R.id.layout_call_voice).setVisibility(View.GONE);
        floatView.findViewById(R.id.layout_call_video).setVisibility(View.VISIBLE);

        RelativeLayout surfaceLayout = (RelativeLayout) floatView.findViewById(R.id.layout_call_video);

        surfaceLayout.removeAllViews();

        localView = new EMCallSurfaceView(context);
        RelativeLayout.LayoutParams localParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        localView.setZOrderOnTop(false);
        localView.setZOrderMediaOverlay(true);
        surfaceLayout.addView(localView, localParams);

        localView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
        EMClient.getInstance().conferenceManager().updateLocalSurfaceView(localView);
    }

    /**
     * 停止悬浮窗
     */
    public void removeFloatWindow() {
        Log.i(TAG, "removeFloatWindow: ");
        unregisterBroadcast();
        if (localView != null) {
            if (localView.getRenderer() != null) {
                localView.getRenderer().dispose();
            }
            localView.release();
            localView = null;
        }
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
            floatView = null;
        }
    }

    /**
     * Register call broadcast receiver
     */
    private void registerBroadcast() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isUpdateState = intent.getBooleanExtra("update_state", false);
                boolean isUpdateTime = intent.getBooleanExtra("update_time", false);
                if (isUpdateState) {
                    Message msg = handler.obtainMessage(REFRESH_UI);
                    msg.obj = intent;
                    handler.sendMessage(msg);
                }
                if (localView == null) { // Voice call type.
                    Message msg = handler.obtainMessage(REFRESH_TIME);
                    handler.sendMessage(msg);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.BROADCAST_ACTION_CALL);
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    private void unregisterBroadcast() {
        if (localBroadcastManager != null && broadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(broadcastReceiver);
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_UI:
                    Intent intent = (Intent) msg.obj;
                    EMCallStateChangeListener.CallState callState =
                            (EMCallStateChangeListener.CallState) intent.getExtras().get("call_state");
                    EMCallStateChangeListener.CallError callError =
                            (EMCallStateChangeListener.CallError) intent.getExtras().get("call_error");
                    refreshCallView(callState, callError);
                    break;
                case REFRESH_TIME:
                    refreshCallTime();
                    break;
            }
        }
    };

    /**
     * refresh call ui
     */
    private void refreshCallView(EMCallStateChangeListener.CallState callState, EMCallStateChangeListener.CallError callError) {
        switch (callState) {
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case ACCEPTED:
                break;
            case DISCONNECTED:
//                CallManager.getInstance().removeFloatWindow();
                break;
            case NETWORK_DISCONNECTED:
                Toast.makeText(context, "Remote network disconnected!", Toast.LENGTH_SHORT).show();
                break;
            case NETWORK_NORMAL:
                Toast.makeText(context, "Remote network connected!", Toast.LENGTH_SHORT).show();
                break;
            case NETWORK_UNSTABLE:
                if (callError == EMCallStateChangeListener.CallError.ERROR_NO_DATA) {
                    Toast.makeText(context, "No call data!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Remote network unstable!", Toast.LENGTH_SHORT).show();
                }
                break;
            case VIDEO_PAUSE:
                Toast.makeText(context, "Remote pause video", Toast.LENGTH_SHORT).show();
                break;
            case VIDEO_RESUME:
                Toast.makeText(context, "Remote resume video", Toast.LENGTH_SHORT).show();
                break;
            case VOICE_PAUSE:
                Toast.makeText(context, "Remote pause voice", Toast.LENGTH_SHORT).show();
                break;
            case VOICE_RESUME:
                Toast.makeText(context, "Remote resume voice", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    private void refreshCallTime() {
//        int time = CallManager.getInstance().getCallTime();
//        if (!callTimeView.isShown()) {
//            callTimeView.setVisibility(View.VISIBLE);
//        }
//        callTimeView.setText(DateUtils.toTimeBySecond(time));
    }
}

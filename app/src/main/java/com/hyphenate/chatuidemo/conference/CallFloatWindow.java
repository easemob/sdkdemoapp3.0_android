package com.hyphenate.chatuidemo.conference;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.model.EaseCompat;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.media.EMCallSurfaceView;
import com.hyphenate.util.EMLog;
import com.superrtc.sdk.VideoView;

/**
 * Created by lzan13 on 2017/3/27.
 * <p>
 * float window control
 */
public class CallFloatWindow {
    private static final String TAG = "FloatWindow";

    private Context context;

    private static CallFloatWindow instance;

    private WindowManager windowManager = null;
    private WindowManager.LayoutParams layoutParams = null;

    private View floatView;
    private ImageView avatarView;
    private EMCallSurfaceView surfaceView;

    private int screenWidth;
    private int floatViewWidth;

    public CallFloatWindow(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        screenWidth = point.x;
    }

    public static CallFloatWindow getInstance(Context context) {
        if (instance == null) {
            instance = new CallFloatWindow(context);
        }
        return instance;
    }

    /**
     * add float window
     */
    public void show() { // 0: voice call; 1: video call;
        if (floatView != null) {
            return;
        }
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.type = EaseCompat.getSupportedWindowType();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        floatView = LayoutInflater.from(context).inflate(R.layout.em_widget_call_float_window, null);
        windowManager.addView(floatView, layoutParams);
        floatView.post(new Runnable() {
            @Override
            public void run() {
                // Get the size of floatView;
                if(floatView != null) {
                    floatViewWidth = floatView.getWidth();
                }
            }
        });
        avatarView = (ImageView) floatView.findViewById(R.id.iv_avatar);

        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ConferenceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                dismiss();
            }
        });

        floatView.setOnTouchListener(new View.OnTouchListener() {
            boolean result = false;

            int left;
            int top;
            float startX = 0;
            float startY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        result = false;
                        startX = event.getRawX();
                        startY = event.getRawY();

                        left = layoutParams.x;
                        top = layoutParams.y;

                        EMLog.i(TAG, "startX: " + startX + ", startY: " + startY + ", left: " + left + ", top: " + top);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 20 || Math.abs(event.getRawY() - startY) > 20) {
                            result = true;
                        }

                        int deltaX = (int) (startX - event.getRawX());

                        layoutParams.x = left + deltaX;
                        layoutParams.y = (int) (top + event.getRawY() - startY);
                        EMLog.i(TAG, "startX: " + (event.getRawX() - startX) + ", startY: " + (event.getRawY() - startY)
                                + ", left: " + left + ", top: " + top);
                        windowManager.updateViewLayout(floatView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        smoothScrollToBorder();
                        break;
                }
                return result;
            }
        });
    }

    public void update(EMConferenceStream stream) {
        if (!isShowing()) {
            return;
        }

        if (stream.isVideoOff()) { // 视频未开启
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.VISIBLE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.GONE);
        } else { // 视频已开启
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.GONE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.VISIBLE);
            prepareSurfaceView();

            boolean isSelf = stream.getUsername().equals(EMClient.getInstance().getCurrentUser());
            if (isSelf) {
                EMClient.getInstance().conferenceManager().updateLocalSurfaceView(surfaceView);
            } else {
                EMClient.getInstance().conferenceManager().updateRemoteSurfaceView(stream.getStreamId(), surfaceView);
            }
        }
    }

    public boolean isShowing() {
        return floatView != null;
    }

    /**
     * 停止悬浮窗
     */
    public void dismiss() {
        Log.i(TAG, "dismiss: ");
        if (surfaceView != null) {
            if (surfaceView.getRenderer() != null) {
                surfaceView.getRenderer().dispose();
            }
            surfaceView.release();
            surfaceView = null;
        }
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
            floatView = null;
        }
    }

    /**
     * set call surface view
     */
    private void prepareSurfaceView() {
        RelativeLayout surfaceLayout = (RelativeLayout) floatView.findViewById(R.id.layout_call_video);

        surfaceLayout.removeAllViews();

        surfaceView = new EMCallSurfaceView(context);
        RelativeLayout.LayoutParams localParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(true);
        surfaceLayout.addView(surfaceView, localParams);

        surfaceView.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
    }

    private void smoothScrollToBorder() {
        EMLog.i(TAG, "screenWidth: " + screenWidth + ", floatViewWidth: " + floatViewWidth);
        int splitLine = screenWidth / 2 - floatViewWidth / 2;
        final int left = layoutParams.x;
        final int top = layoutParams.y;
        int targetX;

        if (left < splitLine) {
            // 滑动到最左边
            targetX = 0;
        } else {
            // 滑动到最右边
            targetX = screenWidth - floatViewWidth;
        }

        ValueAnimator animator = ValueAnimator.ofInt(left, targetX);
        animator.setDuration(100)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (floatView == null) return;

                        int value = (int) animation.getAnimatedValue();
                        EMLog.i(TAG, "onAnimationUpdate, value: " + value);
                        layoutParams.x = value;
                        layoutParams.y = top;
                        windowManager.updateViewLayout(floatView, layoutParams);
                    }
                });
        animator.start();
    }
}

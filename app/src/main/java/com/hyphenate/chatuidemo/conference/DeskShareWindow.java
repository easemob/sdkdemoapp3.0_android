package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.model.EaseCompat;
import com.hyphenate.media.EMCallSurfaceView;
import com.hyphenate.util.EMLog;

/**
 * Created by lzan13 on 2017/3/27.
 * <p>
 * float window control
 */
public class DeskShareWindow {
    private static final String TAG = "FloatWindow";

    private Context context;

    private static DeskShareWindow instance;

    private WindowManager windowManager = null;

    private View floatView;
    private WindowManager.LayoutParams layoutParams = null;

    public DeskShareWindow(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static DeskShareWindow getInstance(Context context) {
        if (instance == null) {
            instance = new DeskShareWindow(context);
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
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.type = EaseCompat.getSupportedWindowType();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        floatView = LayoutInflater.from(context).inflate(R.layout.em_widget_desk_share_window, null);
        windowManager.addView(floatView, layoutParams);

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
                        break;
                }
                return result;
            }
        });
    }

    public boolean isShowing() {
        return floatView != null;
    }

    /**
     * 停止悬浮窗
     */
    public void dismiss() {
        Log.i(TAG, "dismiss: ");
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
            floatView = null;
        }
    }
}

package com.hyphenate.chatuidemo.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.exceptions.HyphenateException;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 公告信息详情界面
 * Created by lzan13 on 2017/3/20.
 */
public class AfficheDetailsActivity extends BaseActivity {

    // 界面控件
    private EaseTitleBar easeTitleBar;
    private TextView titleView;
    private LinearLayout afficheLayout;

    private EMMessage message;

    @Override protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.em_activity_affiche_details);

        easeTitleBar = (EaseTitleBar) findViewById(R.id.title_bar);
        easeTitleBar.setLeftImageResource(R.drawable.ease_mm_title_back);
        easeTitleBar.setTitle("公告详情");
        easeTitleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        titleView = (TextView) findViewById(R.id.text_title);

        afficheLayout = (LinearLayout) findViewById(R.id.layout_affiche);

        initView();
    }

    /**
     * 初始化公告详情
     */
    private void initView() {
        EMConversation conversation = EMClient.getInstance()
                .chatManager()
                .getConversation(EaseConstant.AFFICHE_CONVERSATION_ID);

        String msgId = getIntent().getStringExtra(Constant.MSG_ID);
        message = conversation.getMessage(msgId, true);
        // 设置公告标题
        titleView.setText(((EMTextMessageBody) message.getBody()).getMessage());
        try {
            JSONArray array = message.getJSONArrayAttribute("info");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object.has("txt")) {
                    TextView textView = new TextView(this);
                    textView.setText(object.optString("txt"));
                    afficheLayout.addView(textView);
                } else if (object.has("img")) {
                    ImageView imageView = new ImageView(this);
                    Glide.with(this).load(object.optString("img")).into(imageView);
                    afficheLayout.addView(imageView);
                }
            }
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
    }
}

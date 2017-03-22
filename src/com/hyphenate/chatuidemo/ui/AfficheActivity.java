package com.hyphenate.chatuidemo.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.widget.EaseTitleBar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 公告信息列表界面
 * Created by lzan13 on 2017/3/20.
 */
public class AfficheActivity extends BaseActivity {

    // 界面控件
    private EaseTitleBar easeTitleBar;
    private ListView listView;
    private List<EMMessage> messages = new ArrayList<>();
    private AfficheAdapter afficheAdapter;

    private EMConversation conversation;
    private int pagesize = 15;

    @Override protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.ease_activity_simple_list);

        easeTitleBar = (EaseTitleBar) findViewById(R.id.title_bar);
        easeTitleBar.setTitle("系统公告");
        easeTitleBar.setLeftImageResource(R.drawable.ease_mm_title_back);
        easeTitleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        listView = (ListView) findViewById(R.id.list_view);

        conversation = EMClient.getInstance()
                .chatManager()
                .getConversation(EaseConstant.AFFICHE_CONVERSATION_ID,
                        EMConversation.EMConversationType.Chat, true);
        conversation.markAllMessagesAsRead();
        messages = conversation.getAllMessages();
        int msgCount = messages != null ? messages.size() : 0;
        if (msgCount < conversation.getAllMsgCount() && msgCount < pagesize) {
            String msgId = null;
            if (messages != null && messages.size() > 0) {
                msgId = messages.get(0).getMsgId();
            }
            conversation.loadMoreMsgFromDB(msgId, pagesize - msgCount);
        }

        loadMessageData();
        // 设置adapter
        afficheAdapter = new AfficheAdapter(this, R.layout.ease_row_simple, messages);
        listView.setAdapter(afficheAdapter);
        Button button = new Button(this);
        button.setText("加载更多");
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                loadMoreAffiche();
            }
        });
        listView.addFooterView(button);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                    long id) {
                return onListItemLongClick(position);
            }
        });
    }

    /**
     * 加载消息数据
     */
    private void loadMessageData() {
        if (messages == null) {
            messages = conversation.getAllMessages();
        }
        messages.clear();
        messages.addAll(conversation.getAllMessages());
        // 反向排序
        Collections.reverse(messages);
    }

    /**
     * 点击公告进入详情页
     *
     * @param position 当前点击公告位置
     */
    private void onListItemClick(int position) {
        EMMessage message = messages.get(position);
        startActivity(new Intent(this, AfficheDetailsActivity.class).putExtra(EaseConstant.MSG_ID,
                message.getMsgId()));
    }

    private boolean onListItemLongClick(int position) {

        return false;
    }

    protected void loadMoreAffiche() {
        try {
            List<EMMessage> list =
                    conversation.loadMoreMsgFromDB(messages.get(messages.size() - 1).getMsgId(),
                            pagesize);
            if (list.size() > 0) {
                loadMessageData();
                afficheAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(AfficheActivity.this,
                        getResources().getString(com.hyphenate.easeui.R.string.no_more_messages),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 自定义Adapter 适配器
     *
     * @author lzan13
     */
    private class AfficheAdapter extends ArrayAdapter<EMMessage> {
        private Context context;
        private int resource;
        private List<EMMessage> messageList;

        public AfficheAdapter(Context context, int resource, List<EMMessage> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.messageList = objects;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(resource, null);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.avatar);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            String title = ((EMTextMessageBody) messageList.get(position).getBody()).getMessage();
            viewHolder.textView.setText(title);

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}

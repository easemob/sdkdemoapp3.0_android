package com.hyphenate.chatuidemo.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.hyphenate.EMMessageListener;
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
public class AfficheActivity extends BaseActivity implements EMMessageListener {

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
     * 点击公告进入详情页
     *
     * @param position 当前点击公告位置
     */
    private void onListItemClick(int position) {
        EMMessage message = messages.get(position);
        startActivity(new Intent(this, AfficheDetailsActivity.class).putExtra(EaseConstant.MSG_ID,
                message.getMsgId()));
    }

    private boolean onListItemLongClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确定删除公告？");
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                conversation.removeMessage(messages.get(position).getMsgId());
                refresh();
            }
        });
        // 设置触摸对话框外围不触发事件，防止误触碰
        builder.create().setCanceledOnTouchOutside(false);
        builder.show();
        return true;
    }

    private void refresh() {
        loadMessageData();
        if (afficheAdapter != null) {
            afficheAdapter.notifyDataSetChanged();
        }
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
     * 加载更多公告
     */
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

    @Override public void onMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            if (message.getFrom().equals(EaseConstant.AFFICHE_CONVERSATION_ID)) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        refresh();
                    }
                });
            }
        }
    }

    @Override public void onCmdMessageReceived(List<EMMessage> messages) {

    }

    @Override public void onMessageRead(List<EMMessage> messages) {

    }

    @Override public void onMessageDelivered(List<EMMessage> messages) {

    }

    @Override public void onMessageChanged(EMMessage message, Object change) {

    }

    @Override protected void onResume() {
        super.onResume();
        EMClient.getInstance().chatManager().addMessageListener(this);
        refresh();
    }

    @Override protected void onStop() {
        super.onStop();
        EMClient.getInstance().chatManager().removeMessageListener(this);
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
                viewHolder.titleView = (TextView) convertView.findViewById(R.id.title);
                viewHolder.stateView = (TextView) convertView.findViewById(R.id.state);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            EMMessage message = messageList.get(position);
            String title = ((EMTextMessageBody) message.getBody()).getMessage();
            viewHolder.titleView.setText(title);
            if (message.isUnread()) {
                viewHolder.stateView.setVisibility(View.VISIBLE);
                viewHolder.stateView.setText("未读");
            } else {
                viewHolder.stateView.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView stateView;
    }
}

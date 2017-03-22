package com.hyphenate.chatuidemo.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMNormalFileMessageBody;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.utils.FileUtil;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseTitleBar;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 分享内容给好友
 * Created by lzan13 on 2017/3/16.
 */
public class SharedActivity extends BaseActivity {

    // 打开当前分享界面的 Intent 对象，其中带有要分享的数据
    private Intent intent = null;
    private String action = null;
    private String type = null;

    // 界面控件
    private EaseTitleBar easeTitleBar;
    private ListView listView;
    private List<EaseUser> list = new ArrayList<>();
    private ContactsAdapter contactsAdapter;

    // 定义分享的消息
    private EMMessage message;

    @Override protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.ease_activity_simple_list);

        if (!EMClient.getInstance().isLoggedInBefore()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        easeTitleBar = (EaseTitleBar) findViewById(R.id.title_bar);
        easeTitleBar.setTitle("选择好友");
        easeTitleBar.setLeftImageResource(R.drawable.ease_mm_title_back);
        easeTitleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        listView = (ListView) findViewById(R.id.list_view);

        // 获取联系人数据源
        list.addAll(DemoHelper.getInstance().getContactList().values());
        // 设置adapter
        contactsAdapter = new ContactsAdapter(this, R.layout.ease_row_simple, list);
        listView.setAdapter(contactsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });

        initShared();
    }

    /**
     * 初始化分享
     */
    private void initShared() {
        intent = getIntent();
        action = intent.getAction();
        type = intent.getType();
        if (action.equals(Intent.ACTION_SEND) && !TextUtils.isEmpty(type)) {
            if (type.equals("text/plain")) {
                // 分享文字
                handleShareText();
            } else if (type.startsWith("image/")) {
                // 分享图片
                handleShareImage();
            } else {
                // 分享文件
                handleShareFile();
            }
        }
    }

    /**
     * 处理文字内容分享
     */
    private void handleShareText() {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        // 经测试分享文本文件时走的也是分享文本这里，但是却获取不到文本内容，只能转到文件分享那里去
        if (TextUtils.isEmpty(sharedText)) {
            handleShareFile();
            return;
        }
        message = EMMessage.createSendMessage(EMMessage.Type.TXT);
        EMTextMessageBody body = new EMTextMessageBody(sharedText);
        message.addBody(body);
    }

    /**
     * 处理图片分享
     */
    private void handleShareImage() {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path = FileUtil.getPath(this, uri);

        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "读取图片原地址失败，无法分享", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        message = EMMessage.createSendMessage(EMMessage.Type.IMAGE);
        EMImageMessageBody body = new EMImageMessageBody(file);
        message.addBody(body);
    }

    /**
     * 处理文件分享
     */
    private void handleShareFile() {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path = FileUtil.getPath(this, uri);
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "读取文件原地址失败，无法分享", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        message = EMMessage.createSendMessage(EMMessage.Type.IMAGE);
        EMNormalFileMessageBody body = new EMNormalFileMessageBody(file);
        message.addBody(body);
    }

    private void onListItemClick(int position) {
        EaseUser easeUser = list.get(position);
        Toast.makeText(this, "分享给 " + easeUser.getNickname(), Toast.LENGTH_LONG).show();
        message.setTo(easeUser.getUsername());
        EMClient.getInstance().chatManager().sendMessage(message);

        // start chat acitivity
        Intent intent = new Intent(this, ChatActivity.class);
        // it's single chat
        intent.putExtra(Constant.EXTRA_USER_ID, easeUser.getUsername());
        startActivity(intent);
        finish();
    }

    /**
     * 自定义Adapter 适配器
     *
     * @author lzan13
     */
    private class ContactsAdapter extends ArrayAdapter<EaseUser> {
        private Context context;
        private int resource;
        private List<EaseUser> contacts;

        public ContactsAdapter(Context context, int resource, List<EaseUser> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.contacts = objects;
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
            viewHolder.imageView.setImageResource(R.drawable.em_default_avatar);
            viewHolder.textView.setText(contacts.get(position).getNickname());
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}

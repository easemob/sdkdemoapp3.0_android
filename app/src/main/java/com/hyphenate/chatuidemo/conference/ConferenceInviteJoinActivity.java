package com.hyphenate.chatuidemo.conference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.ui.BaseActivity;
import com.hyphenate.easeui.adapter.EaseContactAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseUserUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzan13 on 2017/8/25.
 * 邀请好友加入会议选择界面
 */
public class ConferenceInviteJoinActivity extends BaseActivity {
    private SelectContactAdapter contactAdapter;
    private List<String> existMembers;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_invite);

        if (existMembers == null) {
            existMembers = new ArrayList<>();
        }
        List<String> memberList = EMClient.getInstance().conferenceManager().getConferenceMemberList();
        for (String username : memberList) {
            existMembers.add(username);
        }
        // get contact list
        final List<EaseUser> alluserList = new ArrayList<EaseUser>();
        for (EaseUser user : DemoHelper.getInstance().getContactList().values()) {
            if (!user.getUsername().equals(Constant.NEW_FRIENDS_USERNAME)
                    & !user.getUsername().equals(Constant.GROUP_USERNAME)
                    & !user.getUsername().equals(Constant.CHAT_ROOM)
                    & !user.getUsername().equals(Constant.CHAT_ROBOT)) {
                alluserList.add(user);
            }
        }

        ListView listView = (ListView) findViewById(R.id.list);
        contactAdapter = new SelectContactAdapter(this, R.layout.em_row_contact_with_checkbox, alluserList);
        listView.setAdapter(contactAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
                checkBox.toggle();
            }
        });

        findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                List<String> var = getToBeAddMembers();
                setResult(RESULT_OK, getIntent().putExtra("members", var.toArray(new String[var.size()])));
                finish();
            }
        });
    }

    /**
     * get selected members
     */
    private List<String> getToBeAddMembers() {
        List<String> members = new ArrayList<String>();
        int length = contactAdapter.isCheckedArray.length;
        for (int i = 0; i < length; i++) {
            String username = contactAdapter.getItem(i).getUsername();
            if (contactAdapter.isCheckedArray[i] && !existMembers.contains(username)) {
                members.add(username);
            }
        }
        return members;
    }

    /**
     * adapter
     */
    private class SelectContactAdapter extends EaseContactAdapter {

        private boolean[] isCheckedArray;
        private Context mContext;

        public SelectContactAdapter(Context context, int resource, List<EaseUser> users) {
            super(context, resource, users);
            mContext = context;
            isCheckedArray = new boolean[users.size()];
        }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            final String username = getItem(position).getUsername();

            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
            ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
            TextView nameView = (TextView) view.findViewById(R.id.name);
            EaseUserUtils.setUserAvatar(mContext, username, avatarView);
            EaseUserUtils.setUserNick(username, nameView);
            if (checkBox != null) {
                if (existMembers != null && existMembers.contains(username)) {
                    checkBox.setButtonDrawable(R.drawable.em_checkbox_bg_gray_selector);
                } else {
                    checkBox.setButtonDrawable(R.drawable.em_checkbox_bg_selector);
                }

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // check the exist members
                        if (existMembers.contains(username)) {
                            isChecked = true;
                            checkBox.setChecked(true);
                        }
                        isCheckedArray[position] = isChecked;
                    }
                });
                // keep exist members checked
                if (existMembers.contains(username)) {
                    checkBox.setChecked(true);
                    isCheckedArray[position] = true;
                } else {
                    checkBox.setChecked(isCheckedArray[position]);
                }
            }

            return view;
        }
    }

    public void back(View view) {
        finish();
    }
}

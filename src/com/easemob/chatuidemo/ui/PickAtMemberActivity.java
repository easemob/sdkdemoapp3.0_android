/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easemob.chatuidemo.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.R;
import com.easemob.easeui.EaseConstant;

public class PickAtMemberActivity extends BaseActivity {

    private String groupId;
    private EMGroup group;
    private ListView listView;
    private MemberAdapter memberAdapter;
    private List<String> members = new ArrayList<String>();
    private List<String> copyMembers = new ArrayList<String>();
    
    // 搜索过滤框
    private EditText searchView;
    private ImageButton clearSearch;
    // 自定义过滤器
    private PickMemberFilter memberFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_pick_contact_no_checkbox);

        // 获取传过来的groupid
        groupId = getIntent().getStringExtra("groupId");
        group = EMGroupManager.getInstance().getGroup(groupId);
        members.addAll(group.getMembers());
        members.remove(EMChatManager.getInstance().getCurrentUser());
        listView = (ListView) findViewById(R.id.list);
        // 设置adapter
        memberAdapter = new MemberAdapter(this, R.layout.em_row_group, members);
        listView.setAdapter(memberAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });
        updateGroup();

        clearSearch = (ImageButton) findViewById(R.id.search_clear);
        clearSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView = (EditText) findViewById(R.id.query);
        searchView.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                memberAdapter.getFilter().filter(s);
                if (s.length() > 0) {
                    clearSearch.setVisibility(View.VISIBLE);
                } else {
                    clearSearch.setVisibility(View.INVISIBLE);
                }
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                
            }
        });
    }

    /**
     * 群成员选择列表点击事件
     * 
     * @param position
     *            ListView 被点击的项
     */
    protected void onListItemClick(int position) {
        setResult(RESULT_OK, getIntent().putExtra(EaseConstant.EXTRA_USER_ID, members.get(position)));
        finish();
    }

    public void back(View view) {
        // setResult(RESULT_OK);
        finish();
    }

    /**
     * 为了保证群成员的正确性，每次都去服务器获取群成员列表
     */
    private void updateGroup() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final EMGroup returnGroup = EMGroupManager.getInstance().getGroupFromServer(groupId);
                    // 更新本地数据
                    EMGroupManager.getInstance().createOrUpdateLocalGroup(returnGroup);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            members.clear();
                            members.addAll(group.getMembers());
                            copyMembers.clear();
                            copyMembers.addAll(group.getMembers());
                            memberAdapter.notifyDataSetChanged();
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(PickAtMemberActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 自定义Adapter 适配器，并实现搜索过滤器
     * 
     * @author lzan13
     *
     */
    private class MemberAdapter extends ArrayAdapter<String> {
        private Context context;
        private int resource;
        private List<String> memberList;

        public MemberAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.memberList = objects;
            copyMembers.addAll(objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(resource, null);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.avatar);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.imageView.setImageResource(R.drawable.em_default_avatar);
            viewHolder.textView.setText(memberList.get(position));
            return convertView;
        }

        @Override
        public Filter getFilter() {
            if(memberFilter == null){
                memberFilter = new PickMemberFilter(memberList);
            }
            return memberFilter;
        }
    }

    /**
     * 自定义搜索框过滤器，实现被@ 成员搜索过滤
     * 
     * @author lzan13
     * 
     */
    protected class PickMemberFilter extends Filter {
        List<String> originalMembers = null;

        public PickMemberFilter(List<String> list) {
            this.originalMembers = list;
        }

        @Override
        protected synchronized FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (originalMembers == null) {
                originalMembers = new ArrayList<String>();
            }
            if (prefix == null || prefix.length() == 0) {
                results.values = copyMembers;
                results.count = copyMembers.size();
            } else {
                String prefixString = prefix.toString();
                ArrayList<String> newMembers = new ArrayList<String>();
                for (int i = 0; i < originalMembers.size(); i++) {
                    String username = originalMembers.get(i);
                    if (username.startsWith(prefixString)) {
                        newMembers.add(username);
                    }
                }
                results.values = newMembers;
                results.count = newMembers.size();
            }
            return results;
        }

        @Override
        protected synchronized void publishResults(CharSequence constraint, FilterResults results) {
            members.clear();
            members.addAll((List<String>) results.values);
            if (results.count > 0) {
                memberAdapter.notifyDataSetChanged();
            } else {
                memberAdapter.notifyDataSetInvalidated();
            }
        }
    }
    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

}

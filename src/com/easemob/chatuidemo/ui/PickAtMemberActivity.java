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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHelper;
import com.easemob.chatuidemo.R;
import com.easemob.easeui.EaseConstant;
import com.easemob.easeui.adapter.EaseContactAdapter;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.widget.EaseSidebar;
import com.easemob.util.EMLog;

public class PickAtMemberActivity extends BaseActivity {

    private String groupId;
    private EMGroup group;
	private ListView listView;
	private MemberAdapter memberAdapter;
	private List<String> members = new ArrayList<String>();

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
	}

	protected void onListItemClick(int position) {
//		if (position != 0) {
			setResult(RESULT_OK, getIntent().putExtra(EaseConstant.EXTRA_USER_ID, members.get(position)));
			finish();
//		}
	}

	public void back(View view) {
//	    setResult(RESULT_OK);
		finish();
	}

	private void updateGroup() {
	    new Thread(new Runnable() {
            public void run() {
                try {
                    final EMGroup returnGroup = EMGroupManager.getInstance().getGroupFromServer(groupId);
                    // 更新本地数据
                    EMGroupManager.getInstance().createOrUpdateLocalGroup(returnGroup);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            memberAdapter.clear();
                            List<String> members = new ArrayList<String>();
                            members.addAll(group.getMembers());
                            memberAdapter.addAll(members);
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

	private class MemberAdapter extends ArrayAdapter<String>{
	    private Context context;
	    private int resource;
	    private List<String> objects;

        public MemberAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if(convertView == null){
                convertView = LayoutInflater.from(context).inflate(resource, null);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.avatar);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.imageView.setImageResource(R.drawable.em_default_avatar);
            viewHolder.textView.setText(objects.get(position));
            return convertView;
        }
        
        
	}
	
	private static class ViewHolder{
        ImageView imageView;
        TextView textView;
    }

}

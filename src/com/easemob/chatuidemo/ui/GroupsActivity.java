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

import java.util.List;

import com.easemob.EMValueCallBack;
import com.easemob.chat.EMClient;
import com.easemob.chat.EMGroup;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.adapter.GroupAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class GroupsActivity extends BaseActivity {
	public static final String TAG = "GroupsActivity";
	private ListView groupListView;
	protected List<EMGroup> grouplist;
	private GroupAdapter groupAdapter;
	private InputMethodManager inputMethodManager;
	public static GroupsActivity instance;
	private View progressBar;
	private SwipeRefreshLayout swipeRefreshLayout;
	
	
	Handler handler = new Handler(){
	    public void handleMessage(android.os.Message msg) {
	        swipeRefreshLayout.setRefreshing(false);
	        switch (msg.what) {
            case 0:
                refresh();
                break;
            case 1:
                Toast.makeText(GroupsActivity.this, R.string.Failed_to_get_group_chat_information, Toast.LENGTH_LONG).show();
                break;

            default:
                break;
            }
	    };
	};

		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_fragment_groups);

		instance = this;
		inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		grouplist = EMClient.getInstance().groupManager().getAllGroups();
		groupListView = (ListView) findViewById(R.id.list);
		//show group list
        groupAdapter = new GroupAdapter(this, 1, grouplist);
        groupListView.setAdapter(groupAdapter);
		
		swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
		swipeRefreshLayout.setColorSchemeResources(R.color.holo_blue_bright, R.color.holo_green_light,
		                R.color.holo_orange_light, R.color.holo_red_light);
		//下拉刷新
		swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
			    EMClient.getInstance().groupManager().asyncGetGroupsFromServer(new EMValueCallBack<List<EMGroup>>() {
                    
                    @Override
                    public void onSuccess(List<EMGroup> value) {
                        handler.sendEmptyMessage(0);
                    }
                    
                    @Override
                    public void onError(int error, String errorMsg) {
                        handler.sendEmptyMessage(1);
                    }
                });
			}
		});
		
		groupListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position == 1) {
					// 新建群聊
					startActivityForResult(new Intent(GroupsActivity.this, NewGroupActivity.class), 0);
				} else if (position == 2) {
					// 添加公开群
					startActivityForResult(new Intent(GroupsActivity.this, PublicGroupsActivity.class), 0);
				} else {
					// 进入群聊
					Intent intent = new Intent(GroupsActivity.this, ChatActivity.class);
					// it is group chat
					intent.putExtra("chatType", Constant.CHATTYPE_GROUP);
					intent.putExtra("userId", groupAdapter.getItem(position - 3).getGroupId());
					startActivityForResult(intent, 0);
				}
			}

		});
		groupListView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
					if (getCurrentFocus() != null)
						inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
								InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}
		});
		
	}

	/**
	 * 进入公开群聊列表
	 */
	public void onPublicGroups(View view) {
		startActivity(new Intent(this, PublicGroupsActivity.class));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	
	private void refresh(){
	    grouplist = EMClient.getInstance().groupManager().getAllGroups();
        groupAdapter = new GroupAdapter(this, 1, grouplist);
        groupListView.setAdapter(groupAdapter);
        groupAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}
	

	/**
	 * 返回
	 * 
	 * @param view
	 */
	public void back(View view) {
		finish();
	}
}

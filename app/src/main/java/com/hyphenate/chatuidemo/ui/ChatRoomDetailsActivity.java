/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
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
package com.hyphenate.chatuidemo.ui;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.easeui.widget.EaseAlertDialog.AlertDialogUser;
import com.hyphenate.easeui.widget.EaseExpandGridView;

public class ChatRoomDetailsActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "ChatRoomDetailsActivity";
	private static final int REQUEST_CODE_EXIT = 1;
	private static final int REQUEST_CODE_EXIT_DELETE = 2;
	private static final int REQUEST_CODE_CLEAR_ALL_HISTORY = 3;

	String longClickUsername = null;

	private String roomId;
	private ProgressBar loadingPB;
	private Button exitBtn;
	private Button deleteBtn;
	private EMChatRoom room;
	private GridAdapter adapter;
	private ProgressDialog progressDialog;

	public static ChatRoomDetailsActivity instance;
	
	String st = "";

	private List<String> memberList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_activity_group_details);
		instance = this;
		st = getResources().getString(R.string.people);
		RelativeLayout clearAllHistory = (RelativeLayout) findViewById(R.id.clear_all_history);
		clearAllHistory.setVisibility(View.GONE);
		EaseExpandGridView userGridview = (EaseExpandGridView) findViewById(R.id.gridview);
		userGridview.setVisibility(View.VISIBLE);
		loadingPB = (ProgressBar) findViewById(R.id.progressBar);
		exitBtn = (Button) findViewById(R.id.btn_exit_grp);
		deleteBtn = (Button) findViewById(R.id.btn_exitdel_grp);
		RelativeLayout blacklistLayout = (RelativeLayout) findViewById(R.id.rl_blacklist);
		RelativeLayout changeGroupNameLayout = (RelativeLayout) findViewById(R.id.rl_change_group_name);

		RelativeLayout blockGroupMsgLayout = (RelativeLayout) findViewById(R.id.rl_switch_block_groupmsg);
		RelativeLayout showChatRoomIdLayout = (RelativeLayout) findViewById(R.id.rl_group_id);
		RelativeLayout showChatRoomNickLayout = (RelativeLayout) findViewById(R.id.rl_group_nick);
		RelativeLayout showChatRoomOwnerLayout = (RelativeLayout) findViewById(R.id.rl_group_owner);
		TextView chatRoomIdTextView = (TextView) findViewById(R.id.tv_group_id);
		TextView chatRoomNickTextView = (TextView) findViewById(R.id.tv_group_nick_value);
		TextView chatRoomOwnerTextView = (TextView) findViewById(R.id.tv_group_owner_value);
		
		findViewById(R.id.rl_search).setVisibility(View.GONE);
		


		// get room id
		roomId = getIntent().getStringExtra("roomId");
		 
		showChatRoomIdLayout.setVisibility(View.VISIBLE);
		chatRoomIdTextView.setText(getResources().getString(R.string.chat_room) + " ID："+roomId);
		showChatRoomNickLayout.setVisibility(View.VISIBLE);
		showChatRoomOwnerLayout.setVisibility(View.VISIBLE);
		 
		room = EMClient.getInstance().chatroomManager().getChatRoom(roomId);
		if(room == null){
		    return;
		}
		chatRoomNickTextView.setText(room.getName());
		chatRoomOwnerTextView.setText(room.getOwner());

		exitBtn.setVisibility(View.GONE);
		deleteBtn.setVisibility(View.GONE);
		blacklistLayout.setVisibility(View.GONE);
		changeGroupNameLayout.setVisibility(View.GONE);
		blockGroupMsgLayout.setVisibility(View.GONE);
		
		
		((TextView) findViewById(R.id.group_name)).setText(room.getName());
		memberList = new java.util.ArrayList<String>();
		memberList.addAll(room.getMemberList());
		adapter = new GridAdapter(this, R.layout.em_grid, memberList);
		userGridview.setAdapter(adapter);
		
		updateRoom();


		clearAllHistory.setOnClickListener(this);
		blacklistLayout.setOnClickListener(this);
		changeGroupNameLayout.setOnClickListener(this);

	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String st1 = getResources().getString(R.string.being_added);
		String st2 = getResources().getString(R.string.is_quit_the_group_chat);
		String st3 = getResources().getString(R.string.chatting_is_dissolution);
		String st4 = getResources().getString(R.string.are_empty_group_of_news);
		String st5 = getResources().getString(R.string.is_modify_the_group_name);
		final String st6 = getResources().getString(R.string.Modify_the_group_name_successful);
		final String st7 = getResources().getString(R.string.change_the_group_name_failed_please);
		String st8 = getResources().getString(R.string.Are_moving_to_blacklist);
		final String st9 = getResources().getString(R.string.failed_to_move_into);
		
		final String stsuccess = getResources().getString(R.string.Move_into_blacklist_success);
		if (resultCode == RESULT_OK) {
			if (progressDialog == null) {
				progressDialog = new ProgressDialog(ChatRoomDetailsActivity.this);
				progressDialog.setMessage(st1);
				progressDialog.setCanceledOnTouchOutside(false);
			}
			switch (requestCode) {
			case REQUEST_CODE_EXIT: // quit the group
				progressDialog.setMessage(st2);
				progressDialog.show();
				exitGroup();
				break;

			default:
				break;
			}
		}
	}


	public void exitGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class), REQUEST_CODE_EXIT);

	}


	public void exitDeleteGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class).putExtra("deleteToast", getString(R.string.dissolution_group_hint)),
				REQUEST_CODE_EXIT_DELETE);

	}

	/**
	 * clear conversation history in group
	 */
	public void clearGroupHistory() {
		EMConversation conversation = EMClient.getInstance().chatManager().getConversation(room.getId(), EMConversationType.ChatRoom);
		if (conversation != null) {
			conversation.clearAllMessages();
		}
		Toast.makeText(this, R.string.messages_are_empty, Toast.LENGTH_SHORT).show();
	}

	/**
	 * exit group
	 * 
	 * @param groupId
	 */
	private void exitGroup() {
		new Thread(new Runnable() {
			public void run() {
				try {
					EMClient.getInstance().chatroomManager().leaveChatRoom(roomId);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
							if(ChatActivity.activityInstance != null)
							    ChatActivity.activityInstance.finish();
						}
					});
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "Failed to quit group: " + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}
	
	protected void updateRoom() {
		new Thread(new Runnable() {
			public void run() {
				try {
					room = EMClient.getInstance().chatroomManager().fetchChatRoomFromServer(roomId, true);

					runOnUiThread(new Runnable() {
						public void run() {
							((TextView) findViewById(R.id.group_name)).setText(room.getName());
							loadingPB.setVisibility(View.INVISIBLE);
							refreshMembers();
							if (EMClient.getInstance().getCurrentUser().equals(room.getOwner())) {
								// show dismiss button
								exitBtn.setVisibility(View.GONE);
								deleteBtn.setVisibility(View.GONE);
							} else {
								// show exit button
								exitBtn.setVisibility(View.GONE);
								deleteBtn.setVisibility(View.GONE);

							}
						}
					});

				} catch (Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							loadingPB.setVisibility(View.INVISIBLE);
						}
					});
				}
			}
		}).start();
	}

	private void refreshMembers(){
        memberList.clear();
        memberList.addAll(room.getMemberList());
        
        adapter.notifyDataSetChanged();
    }
    
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.clear_all_history: // clear conversation history
			String st9 = getResources().getString(R.string.sure_to_empty_this);
			new EaseAlertDialog(ChatRoomDetailsActivity.this, null, st9, null, new AlertDialogUser() {
                
                @Override
                public void onResult(boolean confirmed, Bundle bundle) {
                    if(confirmed){
                        clearGroupHistory();
                    }
                }
            }, true).show();
			break;

		default:
			break;
		}

	}

	/**
	 * group member gridadapter
	 * 
	 * @author admin_new
	 * 
	 */
	private class GridAdapter extends ArrayAdapter<String> {

		private int res;

		public GridAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			res = textViewResourceId;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
		    @SuppressWarnings("UnusedAssignment") ViewHolder holder = null;
			if (convertView == null) {
			    holder = new ViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(res, null);
				holder.imageView = (ImageView) convertView.findViewById(R.id.iv_avatar);
				holder.textView = (TextView) convertView.findViewById(R.id.tv_name);
				holder.badgeDeleteView = (ImageView) convertView.findViewById(R.id.badge_delete);
				convertView.setTag(holder);
			}else{
			    holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);
			// group member item
			final String username = getItem(position);
			holder.textView.setText(username);
			EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
                    // do nothing here, you can show group member's profile here
				}
			});

			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount();
		}
	}


	public void back(View view) {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}
	
	private static class ViewHolder{
	    ImageView imageView;
	    TextView textView;
	    ImageView badgeDeleteView;
	}

}

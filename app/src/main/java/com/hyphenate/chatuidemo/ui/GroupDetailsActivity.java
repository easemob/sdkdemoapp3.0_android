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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMucSharedFile;
import com.hyphenate.chat.EMPushConfigs;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.ui.EaseGroupListener;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.easeui.widget.EaseAlertDialog.AlertDialogUser;
import com.hyphenate.easeui.widget.EaseExpandGridView;
import com.hyphenate.easeui.widget.EaseSwitchButton;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupDetailsActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "GroupDetailsActivity";
	private static final int REQUEST_CODE_ADD_USER = 0;
	private static final int REQUEST_CODE_EXIT = 1;
	private static final int REQUEST_CODE_EXIT_DELETE = 2;
	private static final int REQUEST_CODE_EDIT_GROUPNAME = 5;
	private static final int REQUEST_CODE_EDIT_GROUP_DESCRIPTION = 6;
	private static final int REQUEST_CODE_EDIT_GROUP_EXTENSION = 7;


	private String groupId;
	private ProgressBar loadingPB;
	private Button exitBtn;
	private Button deleteBtn;
	private EMGroup group;
	private GridAdapter membersAdapter;
	private OwnerAdminAdapter ownerAdminAdapter;
	private ProgressDialog progressDialog;
	private TextView announcementText;


	public static GroupDetailsActivity instance;

	
	String st = "";

	private EaseSwitchButton switchButton;
	private EaseSwitchButton offlinePushSwitch;
	private EMPushConfigs pushConfigs;

	private String operationUserId = "";

	private List<String> adminList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> memberList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> muteList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> blackList = Collections.synchronizedList(new ArrayList<String>());

	GroupChangeListener groupChangeListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
        groupId = getIntent().getStringExtra("groupId");
        group = EMClient.getInstance().groupManager().getGroup(groupId);

        // we are not supposed to show the group if we don't find the group
        if(group == null){
            finish();
            return;
        }
        
		setContentView(R.layout.em_activity_group_details);

		instance = this;
		st = getResources().getString(R.string.people);
		RelativeLayout clearAllHistory = (RelativeLayout) findViewById(R.id.clear_all_history);
		loadingPB = (ProgressBar) findViewById(R.id.progressBar);
		exitBtn = (Button) findViewById(R.id.btn_exit_grp);
		deleteBtn = (Button) findViewById(R.id.btn_exitdel_grp);
		RelativeLayout changeGroupNameLayout = (RelativeLayout) findViewById(R.id.rl_change_group_name);
		RelativeLayout changeGroupDescriptionLayout = (RelativeLayout) findViewById(R.id.rl_change_group_description);
		RelativeLayout changeGroupExtension = (RelativeLayout) findViewById(R.id.rl_change_group_extension);
		RelativeLayout idLayout = (RelativeLayout) findViewById(R.id.rl_group_id);
		idLayout.setVisibility(View.VISIBLE);
		TextView idText = (TextView) findViewById(R.id.tv_group_id_value);

		RelativeLayout rl_switch_block_groupmsg = (RelativeLayout) findViewById(R.id.rl_switch_block_groupmsg);
		switchButton = (EaseSwitchButton) findViewById(R.id.switch_btn);
		RelativeLayout searchLayout = (RelativeLayout) findViewById(R.id.rl_search);

		RelativeLayout blockOfflineLayout = (RelativeLayout) findViewById(R.id.rl_switch_block_offline_message);
		offlinePushSwitch = (EaseSwitchButton) findViewById(R.id.switch_block_offline_message);

		RelativeLayout announcementLayout = (RelativeLayout) findViewById(R.id.layout_group_announcement);
		announcementText = (TextView) findViewById(R.id.tv_group_announcement_value);

		RelativeLayout sharedFilesLayout = (RelativeLayout) findViewById(R.id.layout_share_files);

		idText.setText(groupId);
		if (group.getOwner() == null || "".equals(group.getOwner())
				|| !group.getOwner().equals(EMClient.getInstance().getCurrentUser())) {
			exitBtn.setVisibility(View.GONE);
			deleteBtn.setVisibility(View.GONE);
		}
		// show dismiss button if you are owner of group
		if (EMClient.getInstance().getCurrentUser().equals(group.getOwner())) {
			exitBtn.setVisibility(View.GONE);
			deleteBtn.setVisibility(View.VISIBLE);
		}

		//get push configs
		pushConfigs = EMClient.getInstance().pushManager().getPushConfigs();

		groupChangeListener = new GroupChangeListener();
		EMClient.getInstance().groupManager().addGroupChangeListener(groupChangeListener);
		
		((TextView) findViewById(R.id.group_name)).setText(group.getGroupName() + "(" + group.getMemberCount() + st);

		membersAdapter = new GridAdapter(this, R.layout.em_grid_owner, new ArrayList<String>());
		EaseExpandGridView userGridview = (EaseExpandGridView) findViewById(R.id.gridview);
		userGridview.setAdapter(membersAdapter);

		ownerAdminAdapter = new OwnerAdminAdapter(this, R.layout.em_grid_owner, new ArrayList<String>());
		EaseExpandGridView ownerAdminGridview = (EaseExpandGridView) findViewById(R.id.owner_and_administrators_grid_view);
		ownerAdminGridview.setAdapter(ownerAdminAdapter);

		// 保证每次进详情看到的都是最新的group
		updateGroup();

		clearAllHistory.setOnClickListener(this);
		changeGroupNameLayout.setOnClickListener(this);
		changeGroupDescriptionLayout.setOnClickListener(this);
		changeGroupExtension.setOnClickListener(this);
		rl_switch_block_groupmsg.setOnClickListener(this);
        searchLayout.setOnClickListener(this);
		blockOfflineLayout.setOnClickListener(this);
		announcementLayout.setOnClickListener(this);
		sharedFilesLayout.setOnClickListener(this);
	}



	boolean isCurrentOwner(EMGroup group) {
		String owner = group.getOwner();
		if (owner == null || owner.isEmpty()) {
			return false;
		}
		return owner.equals(EMClient.getInstance().getCurrentUser());
	}

	boolean isCurrentAdmin(EMGroup group) {
		synchronized (adminList) {
			String currentUser = EMClient.getInstance().getCurrentUser();
			for (String admin : adminList) {
				if (currentUser.equals(admin)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean isAdmin(String id) {
		synchronized (adminList) {
			for (String admin : adminList) {
				if (id.equals(admin)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean isInBlackList(String id) {
		synchronized (blackList) {
			if (id != null && !id.isEmpty()) {
				for (String item : blackList) {
					if (id.equals(item)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean isInMuteList(String id) {
		synchronized (muteList) {
			if (id != null && !id.isEmpty()) {
				for (String item : muteList) {
					if (id.equals(item)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean isCanAddMember(EMGroup group) {
		if (group.isMemberAllowToInvite() ||
				isAdmin(EMClient.getInstance().getCurrentUser()) ||
				isCurrentOwner(group)) {
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String st1 = getResources().getString(R.string.being_added);
		String st2 = getResources().getString(R.string.is_quit_the_group_chat);
		String st3 = getResources().getString(R.string.chatting_is_dissolution);
		String st4 = getResources().getString(R.string.are_empty_group_of_news);
		final String st5 = getResources().getString(R.string.is_modify_the_group_name);
		final String st6 = getResources().getString(R.string.Modify_the_group_name_successful);
		final String st7 = getResources().getString(R.string.change_the_group_name_failed_please);

		final String st8 = getResources().getString(R.string.is_modify_the_group_description);
		final String st9 = getResources().getString(R.string.Modify_the_group_description_successful);
		final String st10 = getResources().getString(R.string.change_the_group_description_failed_please);
		final String st11 = getResources().getString(R.string.Modify_the_group_extension_successful);
		final String st12 = getResources().getString(R.string.change_the_group_extension_failed_please);

		if (resultCode == RESULT_OK) {
			if (progressDialog == null) {
				progressDialog = new ProgressDialog(GroupDetailsActivity.this);
				progressDialog.setMessage(st1);
				progressDialog.setCanceledOnTouchOutside(false);
			}
			switch (requestCode) {
			case REQUEST_CODE_ADD_USER:// 添加群成员
				final String[] newmembers = data.getStringArrayExtra("newmembers");
				progressDialog.setMessage(st1);
				progressDialog.show();
				addMembersToGroup(newmembers);
				break;
			case REQUEST_CODE_EXIT: // 退出群
				progressDialog.setMessage(st2);
				progressDialog.show();
				exitGrop();
				break;
			case REQUEST_CODE_EXIT_DELETE: // 解散群
				progressDialog.setMessage(st3);
				progressDialog.show();
				deleteGrop();
				break;

			case REQUEST_CODE_EDIT_GROUPNAME: //修改群名称
				final String returnData = data.getStringExtra("data");
				if(!TextUtils.isEmpty(returnData)){
					progressDialog.setMessage(st5);
					progressDialog.show();
					
					new Thread(new Runnable() {
						public void run() {
							try {
								EMClient.getInstance().groupManager().changeGroupName(groupId, returnData);
								runOnUiThread(new Runnable() {
									public void run() {
										((TextView) findViewById(R.id.group_name)).setText(group.getGroupName() + "(" + group.getMemberCount() + ")");
										progressDialog.dismiss();
										Toast.makeText(getApplicationContext(), st6, Toast.LENGTH_SHORT).show();
									}
								});

							} catch (HyphenateException e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									public void run() {
										progressDialog.dismiss();
										Toast.makeText(getApplicationContext(), st7, Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					}).start();
				}
				break;
			case REQUEST_CODE_EDIT_GROUP_DESCRIPTION:
				final String returnData1 = data.getStringExtra("data");
				if(!TextUtils.isEmpty(returnData1)){
					progressDialog.setMessage(st5);
					progressDialog.show();

					new Thread(new Runnable() {
						public void run() {
							try {
								EMClient.getInstance().groupManager().changeGroupDescription(groupId, returnData1);
								runOnUiThread(new Runnable() {
									public void run() {
										progressDialog.dismiss();
										Toast.makeText(getApplicationContext(), st9, Toast.LENGTH_SHORT).show();
									}
								});
							} catch (HyphenateException e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									public void run() {
										progressDialog.dismiss();
										Toast.makeText(getApplicationContext(), st10, Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					}).start();
				}
				break;
				case REQUEST_CODE_EDIT_GROUP_EXTENSION:
					{
					final String returnExtension = data.getStringExtra("data");
					if (!TextUtils.isEmpty(returnExtension)) {
						progressDialog.setMessage(st5);
						progressDialog.show();

						new Thread(new Runnable() {
							public void run() {
								try {
									EMClient.getInstance().groupManager().updateGroupExtension(groupId, returnExtension);
									runOnUiThread(new Runnable() {
										public void run() {
											progressDialog.dismiss();
											Toast.makeText(getApplicationContext(), st11, Toast.LENGTH_SHORT).show();
										}
									});
								} catch (HyphenateException e) {
									e.printStackTrace();
									runOnUiThread(new Runnable() {
										public void run() {
											progressDialog.dismiss();
											Toast.makeText(getApplicationContext(), st12, Toast.LENGTH_SHORT).show();
										}
									});
								}
							}
						}).start();
					}
				}
				break;

			default:
				break;
			}
		}
	}

	private void refreshOwnerAdminAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ownerAdminAdapter.clear();
				ownerAdminAdapter.add(group.getOwner());
				synchronized (adminList) {
					ownerAdminAdapter.addAll(adminList);
				}
				ownerAdminAdapter.notifyDataSetChanged();
			}
		});
	}

	private void debugList(String str, List<String> list) {
		EMLog.d(TAG, str);
		for (String member : list) {
			EMLog.d(TAG, "    " + member);
		}
	}

	private void refreshMembersAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				debugList("memberList", memberList);
				debugList("muteList", muteList);
				debugList("blackList", blackList);

				membersAdapter = new GridAdapter(GroupDetailsActivity.this, R.layout.em_grid_owner, new ArrayList<String>());

				membersAdapter.clear();
				synchronized (memberList) {
					membersAdapter.addAll(memberList);
				}

				synchronized (blackList) {
					membersAdapter.addAll(blackList);
				}
				membersAdapter.notifyDataSetChanged();

				EaseExpandGridView userGridview = (EaseExpandGridView) findViewById(R.id.gridview);
				userGridview.setAdapter(membersAdapter);
			}
		});
	}

	/**
	 * 点击退出群组按钮
	 * 
	 * @param view
	 */
	public void exitGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class), REQUEST_CODE_EXIT);

	}

	/**
	 * 点击解散群组按钮
	 * 
	 * @param view
	 */
	public void exitDeleteGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class).putExtra("deleteToast", getString(R.string.dissolution_group_hint)),
				REQUEST_CODE_EXIT_DELETE);

	}

	/**
	 * 清空群聊天记录
	 */
	private void clearGroupHistory() {

		EMConversation conversation = EMClient.getInstance().chatManager().getConversation(group.getGroupId(), EMConversationType.GroupChat);
		if (conversation != null) {
			conversation.clearAllMessages();
		}
		Toast.makeText(this, R.string.messages_are_empty, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 退出群组
	 * 
	 */
	private void exitGrop() {
		String st1 = getResources().getString(R.string.Exit_the_group_chat_failure);
		new Thread(new Runnable() {
			public void run() {
				try {
					EMClient.getInstance().groupManager().leaveGroup(groupId);
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
							Toast.makeText(getApplicationContext(), getResources().getString(R.string.Exit_the_group_chat_failure) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	/**
	 * 解散群组
	 * 
	 */
	private void deleteGrop() {
		final String st5 = getResources().getString(R.string.Dissolve_group_chat_tofail);
		new Thread(new Runnable() {
			public void run() {
				try {
					EMClient.getInstance().groupManager().destroyGroup(groupId);
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
							Toast.makeText(getApplicationContext(), st5 + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	/**
	 * 增加群成员
	 * 
	 * @param newmembers
	 */
	private void addMembersToGroup(final String[] newmembers) {
		final String st6 = getResources().getString(R.string.Add_group_members_fail);
		new Thread(new Runnable() {
			
			public void run() {
				try {
					// 创建者调用add方法
					if (EMClient.getInstance().getCurrentUser().equals(group.getOwner())) {
						EMClient.getInstance().groupManager().addUsersToGroup(groupId, newmembers);
					} else {
						// 一般成员调用invite方法
						EMClient.getInstance().groupManager().inviteUser(groupId, newmembers, null);
					}
					updateGroup();
					refreshMembersAdapter();
					runOnUiThread(new Runnable() {
						public void run() {
							((TextView) findViewById(R.id.group_name)).setText(group.getGroupName() + "(" + group.getMemberCount()
									+ st);
							progressDialog.dismiss();
						}
					});
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), st6 + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.rl_switch_block_groupmsg: // 屏蔽或取消屏蔽群组
				toggleBlockGroup();
				break;

			case R.id.clear_all_history: // 清空聊天记录
				String st9 = getResources().getString(R.string.sure_to_empty_this);
				new EaseAlertDialog(GroupDetailsActivity.this, null, st9, null, new AlertDialogUser() {

					@Override
					public void onResult(boolean confirmed, Bundle bundle) {
						if(confirmed){
							clearGroupHistory();
						}
					}
				}, true).show();

				break;

			case R.id.rl_change_group_name:
				startActivityForResult(new Intent(this, EditActivity.class).putExtra("data", group.getGroupName()).putExtra("editable", isCurrentOwner(group)),
						REQUEST_CODE_EDIT_GROUPNAME);
				break;
			case R.id.rl_change_group_description:
				startActivityForResult(new Intent(this, EditActivity.class).putExtra("data", group.getDescription()).
						putExtra("title", getString(R.string.change_the_group_description)).putExtra("editable", isCurrentOwner(group)),
						REQUEST_CODE_EDIT_GROUP_DESCRIPTION);
				break;
			case R.id.rl_search:
				startActivity(new Intent(this, GroupSearchMessageActivity.class).putExtra("groupId", groupId));

				break;
			case R.id.rl_switch_block_offline_message:
				toggleBlockOfflineMsg();
				break;
			case R.id.layout_group_announcement:
				showAnnouncementDialog();
				break;
			case R.id.layout_share_files:
				startActivity(new Intent(this, SharedFilesActivity.class).putExtra("groupId", groupId));
				break;
			case R.id.rl_change_group_extension:
				startActivityForResult(new Intent(this, EditActivity.class).putExtra("data", group.getExtension()).
						putExtra("title", getString(R.string.change_the_group_extension)).putExtra("editable", isCurrentOwner(group)), REQUEST_CODE_EDIT_GROUP_EXTENSION);
				break;
			default:
				break;
		}

	}

	private void showAnnouncementDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.group_announcement);
		if(group.getOwner().equals(EMClient.getInstance().getCurrentUser()) ||
				group.getAdminList().contains(EMClient.getInstance().getCurrentUser())){
			final EditText et = new EditText(GroupDetailsActivity.this);
			et.setText(group.getAnnouncement());
			builder.setView(et);
			builder.setNegativeButton(R.string.cancel,null)
					.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String text = et.getText().toString();
					if(!text.equals(group.getAnnouncement())){
						dialog.dismiss();
						updateAnnouncement(text);
					}
				}
			});
		}else{
			builder.setMessage(group.getAnnouncement());
			builder.setPositiveButton(R.string.ok, null);
		}
		builder.show();
	}

	/**
	 * update with the passed announcement
	 * @param announcement
	 */
	private void updateAnnouncement(final String announcement) {
		createProgressDialog();
		progressDialog.setMessage("Updating ...");
		progressDialog.show();

		EMClient.getInstance().groupManager().asyncUpdateGroupAnnouncement(groupId, announcement,
				new EMCallBack() {
					@Override
					public void onSuccess() {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
								announcementText.setText(announcement);
							}
						});
					}

					@Override
					public void onError(int code, final String error) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
								Toast.makeText(GroupDetailsActivity.this, "update fail," + error, Toast.LENGTH_SHORT).show();
							}
						});
					}

					@Override
					public void onProgress(int progress, String status) {
					}
				});

	}


	private void toggleBlockOfflineMsg() {
		if(EMClient.getInstance().pushManager().getPushConfigs() == null){
			return;
		}
		progressDialog = createProgressDialog();
		progressDialog.setMessage("processing...");
		progressDialog.show();
//		final ArrayList list = (ArrayList) Arrays.asList(groupId);
		final List<String> list = new ArrayList<String>();
		list.add(groupId);
		new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(offlinePushSwitch.isSwitchOpen()) {
                        EMClient.getInstance().pushManager().updatePushServiceForGroup(list, false);
                    }else{
                        EMClient.getInstance().pushManager().updatePushServiceForGroup(list, true);
                    }
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressDialog.dismiss();
							if(offlinePushSwitch.isSwitchOpen()){
								offlinePushSwitch.closeSwitch();
							}else{
								offlinePushSwitch.openSwitch();
							}
						}
					});
                } catch (HyphenateException e) {
                    e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(GroupDetailsActivity.this, "progress failed", Toast.LENGTH_SHORT).show();
						}
					});
                }
            }
        }).start();
	}

	private ProgressDialog createProgressDialog(){
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(GroupDetailsActivity.this);
			progressDialog.setCanceledOnTouchOutside(false);
		}
		return progressDialog;
	}

	private void toggleBlockGroup() {
		if(switchButton.isSwitchOpen()){
			EMLog.d(TAG, "change to unblock group msg");
			createProgressDialog();
			progressDialog.setMessage(getString(R.string.Is_unblock));
			progressDialog.show();
			new Thread(new Runnable() {
		        public void run() {
		            try {
		                EMClient.getInstance().groupManager().unblockGroupMessage(groupId);
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                    	switchButton.closeSwitch();
		                        progressDialog.dismiss();
		                    }
		                });
		            } catch (Exception e) {
		                e.printStackTrace();
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                        progressDialog.dismiss();
		                        Toast.makeText(getApplicationContext(), R.string.remove_group_of, Toast.LENGTH_LONG).show();
		                    }
		                });
		                
		            }
		        }
		    }).start();
			
		} else {
			String st8 = getResources().getString(R.string.group_is_blocked);
			final String st9 = getResources().getString(R.string.group_of_shielding);
			EMLog.d(TAG, "change to block group msg");
			createProgressDialog();
			progressDialog.setMessage(st8);
			progressDialog.show();
			new Thread(new Runnable() {
		        public void run() {
		            try {
		                EMClient.getInstance().groupManager().blockGroupMessage(groupId);
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                    	switchButton.openSwitch();
		                        progressDialog.dismiss();
		                    }
		                });
		            } catch (Exception e) {
		                e.printStackTrace();
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                        progressDialog.dismiss();
		                        Toast.makeText(getApplicationContext(), st9, Toast.LENGTH_LONG).show();
		                    }
		                });
		            }
		            
		        }
		    }).start();
		}
	}

	class MemberMenuDialog extends Dialog {

		private MemberMenuDialog(@NonNull Context context) {
			super(context);

			init();
		}

		void init() {
			final MemberMenuDialog dialog = this;
			dialog.setTitle("group");
			dialog.setContentView(R.layout.em_chatroom_member_menu);

			int ids[] = { R.id.menu_item_add_admin,
					R.id.menu_item_rm_admin,
					R.id.menu_item_remove_member,
					R.id.menu_item_add_to_blacklist,
					R.id.menu_item_remove_from_blacklist,
					R.id.menu_item_transfer_owner,
					R.id.menu_item_mute,
					R.id.menu_item_unmute};

			for (int id : ids) {
				LinearLayout linearLayout = (LinearLayout)dialog.findViewById(id);
				linearLayout.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v) {
						dialog.dismiss();
						loadingPB.setVisibility(View.VISIBLE);

						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									switch (v.getId()) {
										case R.id.menu_item_add_admin:
											EMClient.getInstance().groupManager().addGroupAdmin(groupId, operationUserId);
											break;
										case R.id.menu_item_rm_admin:
											EMClient.getInstance().groupManager().removeGroupAdmin(groupId, operationUserId);
											break;
										case R.id.menu_item_remove_member:
											EMClient.getInstance().groupManager().removeUserFromGroup(groupId, operationUserId);
											break;
										case R.id.menu_item_add_to_blacklist:
											EMClient.getInstance().groupManager().blockUser(groupId, operationUserId);
											break;
										case R.id.menu_item_remove_from_blacklist:
											EMClient.getInstance().groupManager().unblockUser(groupId, operationUserId);
											break;
										case R.id.menu_item_mute:
											List<String> muteMembers = new ArrayList<String>();
											muteMembers.add(operationUserId);
											EMClient.getInstance().groupManager().muteGroupMembers(groupId, muteMembers, 20 * 60 * 1000);
											break;
										case R.id.menu_item_unmute:
											List<String> list = new ArrayList<String>();
											list.add(operationUserId);
											EMClient.getInstance().groupManager().unMuteGroupMembers(groupId, list);
											break;
										case R.id.menu_item_transfer_owner:
											EMClient.getInstance().groupManager().changeOwner(groupId, operationUserId);
											break;
										default:
											break;
									}
									updateGroup();
								} catch (final HyphenateException e) {
									runOnUiThread(new Runnable() {
										              @Override
										              public void run() {
											              Toast.makeText(GroupDetailsActivity.this, e.getDescription(), Toast.LENGTH_SHORT).show();
										              }
									              }
									);
									e.printStackTrace();

								} finally {
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											loadingPB.setVisibility(View.INVISIBLE);
										}
									});
								}
							}
						}).start();
					}
				});
			}
		}

		void setVisibility(boolean[] visibilities) throws Exception {
			if (ids.length != visibilities.length) {
				throw new Exception("");
			}

			for (int i = 0; i < ids.length; i++) {
				View view = this.findViewById(ids[i]);
				view.setVisibility(visibilities[i] ? View.VISIBLE : View.GONE);
			}
		}

		int[] ids = {
				R.id.menu_item_transfer_owner,
				R.id.menu_item_add_admin,
				R.id.menu_item_rm_admin,
				R.id.menu_item_remove_member,
				R.id.menu_item_add_to_blacklist,
				R.id.menu_item_remove_from_blacklist,
				R.id.menu_item_mute,
				R.id.menu_item_unmute
		};
	}

	/**
	 * 群组Owner和管理员gridadapter
	 *
	 * @author admin_new
	 *
	 */
	private class OwnerAdminAdapter extends ArrayAdapter<String> {

		private int res;

		public OwnerAdminAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			res = textViewResourceId;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			ViewHolder holder = null;
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

			final String username = getItem(position);
			convertView.setVisibility(View.VISIBLE);
			button.setVisibility(View.VISIBLE);
			EaseUserUtils.setUserNick(username, holder.textView);
			EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);

			LinearLayout id_background = (LinearLayout) convertView.findViewById(R.id.l_bg_id);
			id_background.setBackgroundColor(convertView.getResources().getColor(
					position == 0 ? R.color.holo_red_light :
							(isInMuteList(username) ? R.color.gray_normal : R.color.holo_orange_light)));

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCurrentOwner(group)) {
						return;
					}
					if (username.equals(group.getOwner())) {
						return;
					}
					operationUserId = username;
					MemberMenuDialog dialog = new MemberMenuDialog(GroupDetailsActivity.this);
					dialog.show();

					boolean[] adminVisibilities = {
							true,       //R.id.menu_item_transfer_owner,
							false,      //R.id.menu_item_add_admin,
							true,       //R.id.menu_item_rm_admin,
							false,      //R.id.menu_item_remove_member,
							false,      //R.id.menu_item_add_to_blacklist,
							false,      //R.id.menu_item_remove_from_blacklist,
							false,      //R.id.menu_item_mute,
							false,      //R.id.menu_item_unmute
					};
					try {
						dialog.setVisibility(adminVisibilities);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount();
		}
	}


	/**
	 * 群组成员gridadapter
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
		    ViewHolder holder = null;
			if (convertView == null) {
			    holder = new ViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(res, null);
				holder.imageView = (ImageView) convertView.findViewById(R.id.iv_avatar);
				holder.textView = (TextView) convertView.findViewById(R.id.tv_name);
				convertView.setTag(holder);
			}else{
			    holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);

			// add button
			if (position == getCount() - 1) {
				holder.textView.setText("");
				holder.imageView.setImageResource(R.drawable.em_smiley_add_btn);
				if (isCanAddMember(group)) {
					convertView.setVisibility(View.VISIBLE);
					button.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							final String st11 = getResources().getString(R.string.Add_a_button_was_clicked);
							EMLog.d(TAG, st11);
							// 进入选人页面
							startActivityForResult(
									(new Intent(GroupDetailsActivity.this, GroupPickContactsActivity.class).putExtra("groupId", groupId)),
									REQUEST_CODE_ADD_USER);
						}
					});
				} else {
					convertView.setVisibility(View.INVISIBLE);
				}
				return  convertView;
			} else {
				// members
				final String username = getItem(position);
				EaseUserUtils.setUserNick(username, holder.textView);
				EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);

				LinearLayout id_background = (LinearLayout) convertView.findViewById(R.id.l_bg_id);
				if (isInMuteList(username)) {
					id_background.setBackgroundColor(convertView.getResources().getColor(R.color.gray_normal));
				} else if (isInBlackList(username)) {
					id_background.setBackgroundColor(convertView.getResources().getColor(R.color.holo_black));
				} else {
					id_background.setBackgroundColor(convertView.getResources().getColor(R.color.holo_blue_bright));
				}

				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!isCurrentOwner(group) && !isCurrentAdmin(group)) {
							return;
						}
						operationUserId = username;
						MemberMenuDialog dialog = new MemberMenuDialog(GroupDetailsActivity.this);
						dialog.show();

						boolean[] normalVisibilities = {
								false,      //R.id.menu_item_transfer_owner,
								isCurrentOwner(group) ? true : false,       //R.id.menu_item_add_admin,
								false,      //R.id.menu_item_rm_admin,
								true,       //R.id.menu_item_remove_member,
								true,       //R.id.menu_item_add_to_blacklist,
								false,      //R.id.menu_item_remove_from_blacklist,
								true,       //R.id.menu_item_mute,
								false,      //R.id.menu_item_unmute
						};

						boolean[] blackListVisibilities = {
								false,      //R.id.menu_item_transfer_owner,
								false,      //R.id.menu_item_add_admin,
								false,      //R.id.menu_item_rm_admin,
								false,      //R.id.menu_item_remove_member,
								false,      //R.id.menu_item_add_to_blacklist,
								true,       //R.id.menu_item_remove_from_blacklist,
								false,      //R.id.menu_item_mute,
								false,      //R.id.menu_item_unmute
						};

						boolean[] muteListVisibilities = {
								false,      //R.id.menu_item_transfer_owner,
								isCurrentOwner(group) ? true : false,       //R.id.menu_item_add_admin,
								false,      //R.id.menu_item_rm_admin,
								true,       //R.id.menu_item_remove_member,
								true,       //R.id.menu_item_add_to_blacklist,
								false,      //R.id.menu_item_remove_from_blacklist,
								false,      //R.id.menu_item_mute,
								true,       //R.id.menu_item_unmute
						};

						boolean inBlackList = isInBlackList(username);
						boolean inMuteList = isInMuteList(username);
						try {
							if (inBlackList) {
								dialog.setVisibility(blackListVisibilities);
							} else if (inMuteList) {
								dialog.setVisibility(muteListVisibilities);
							} else {
								dialog.setVisibility(normalVisibilities);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}

			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount() + 1;
		}
	}

	protected void updateGroup() {
		new Thread(new Runnable() {
			public void run() {
				try {
					if(pushConfigs == null){
						EMClient.getInstance().pushManager().getPushConfigsFromServer();
					}

					try {
						group = EMClient.getInstance().groupManager().getGroupFromServer(groupId);

						adminList.clear();
						adminList.addAll(group.getAdminList());
						memberList.clear();
						EMCursorResult<String> result = null;
						do {
							// page size set to 20 is convenient for testing, should be applied to big value
							result = EMClient.getInstance().groupManager().fetchGroupMembers(groupId,
									result != null ? result.getCursor() : "",
									20);
							EMLog.d(TAG, "fetchGroupMembers result.size:" + result.getData().size());
							memberList.addAll(result.getData());
						} while (result.getCursor() != null && !result.getCursor().isEmpty());

						muteList.clear();
						muteList.addAll(EMClient.getInstance().groupManager().fetchGroupMuteList(groupId, 0, 200).keySet());
						blackList.clear();
						blackList.addAll(EMClient.getInstance().groupManager().fetchGroupBlackList(groupId, 0, 200));

					} catch (Exception e) {
						 //e.printStackTrace();  // User may have no permission for fetch mute, fetch black list operation
					} finally {
						memberList.remove(group.getOwner());
						memberList.removeAll(adminList);
					}

					try {
						EMClient.getInstance().groupManager().fetchGroupAnnouncement(groupId);
					} catch (HyphenateException e) {
						e.printStackTrace();
					}

					runOnUiThread(new Runnable() {
						public void run() {
							refreshOwnerAdminAdapter();
							refreshMembersAdapter();

//							refreshUIVisibility();
							((TextView) findViewById(R.id.group_name)).setText(group.getGroupName() + "(" + group.getMemberCount()
									+ ")");
							loadingPB.setVisibility(View.INVISIBLE);

							if (EMClient.getInstance().getCurrentUser().equals(group.getOwner())) {
								// 显示解散按钮
								exitBtn.setVisibility(View.GONE);
								deleteBtn.setVisibility(View.VISIBLE);
							} else {
								// 显示退出按钮
								exitBtn.setVisibility(View.VISIBLE);
								deleteBtn.setVisibility(View.GONE);
							}

							// update block
							EMLog.d(TAG, "group msg is blocked:" + group.isMsgBlocked());
							if (group.isMsgBlocked()) {
								switchButton.openSwitch();
							} else {
							    switchButton.closeSwitch();
							}
							List<String> disabledIds = EMClient.getInstance().pushManager().getNoPushGroups();
							if(disabledIds != null && disabledIds.contains(groupId)){
								offlinePushSwitch.openSwitch();
							}else{
								offlinePushSwitch.closeSwitch();
							}

							announcementText.setText(group.getAnnouncement());

							RelativeLayout changeGroupNameLayout = (RelativeLayout) findViewById(R.id.rl_change_group_name);
							RelativeLayout changeGroupDescriptionLayout = (RelativeLayout) findViewById(R.id.rl_change_group_description);
							boolean isOwner = isCurrentOwner(group);
							exitBtn.setVisibility(isOwner ? View.GONE : View.VISIBLE);
							deleteBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
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
		EMClient.getInstance().groupManager().removeGroupChangeListener(groupChangeListener);
		super.onDestroy();
		instance = null;

	}
	
	private static class ViewHolder{
	    ImageView imageView;
	    TextView textView;
	    ImageView badgeDeleteView;
	}
    
    private class GroupChangeListener extends EaseGroupListener {

		@Override
		public void onInvitationAccepted(String groupId, String inviter, String reason) {
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					memberList = group.getMembers();
					memberList.remove(group.getOwner());
					memberList.removeAll(adminList);
					memberList.removeAll(muteList);
					refreshMembersAdapter();
				}
            });
		}

		@Override
		public void onUserRemoved(String groupId, String groupName) {
			finish();
		}

		@Override
		public void onGroupDestroyed(String groupId, String groupName) {
			finish();
		}

	    @Override
	    public void onMuteListAdded(String groupId, final List<String> mutes, final long muteExpire) {
		    updateGroup();
	    }

	    @Override
	    public void onMuteListRemoved(String groupId, final List<String> mutes) {
		    updateGroup();
	    }

	    @Override
	    public void onAdminAdded(String groupId, String administrator) {
		    updateGroup();
	    }

	    @Override
	    public void onAdminRemoved(String groupId, String administrator) {
		    updateGroup();
	    }

	    @Override
	    public void onOwnerChanged(String groupId, String newOwner, String oldOwner) {
		    runOnUiThread(new Runnable() {

			    @Override
			    public void run() {
				    Toast.makeText(GroupDetailsActivity.this, "onOwnerChanged", Toast.LENGTH_LONG).show();
			    }
		    });
		    updateGroup();
	    }
	    
	    @Override
	    public void onMemberJoined(String groupId, String member) {
	        EMLog.d(TAG, "onMemberJoined");
	        updateGroup();
	    }
	    
	    @Override
	    public void onMemberExited(String groupId, String member) {
	        EMLog.d(TAG, "onMemberExited");
            updateGroup();
	    }

		@Override
		public void onAnnouncementChanged(String groupId, final String announcement) {
			if(groupId.equals(GroupDetailsActivity.this.groupId)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						announcementText.setText(announcement);
					}
				});
			}
		}

		@Override
		public void onSharedFileAdded(String groupId, final EMMucSharedFile sharedFile) {
			if(groupId.equals(GroupDetailsActivity.this.groupId)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(GroupDetailsActivity.this, "Group added a share file", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

		@Override
		public void onSharedFileDeleted(String groupId, String fileId) {
			if(groupId.equals(GroupDetailsActivity.this.groupId)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(GroupDetailsActivity.this, "Group deleted a share file", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

}

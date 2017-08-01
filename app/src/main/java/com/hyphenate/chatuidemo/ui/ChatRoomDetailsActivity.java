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
import com.hyphenate.EMChatRoomChangeListener;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.easeui.widget.EaseAlertDialog.AlertDialogUser;
import com.hyphenate.easeui.widget.EaseExpandGridView;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRoomDetailsActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "ChatRoomDetailsActivity";
	private static final int REQUEST_CODE_EXIT = 1;
	private static final int REQUEST_CODE_EXIT_DELETE = 2;
	private static final int REQUEST_CODE_CLEAR_ALL_HISTORY = 3;
	private static final int REQUEST_CODE_EDIT_CHAT_ROOM_NAME= 4;
	private static final int REQUEST_CODE_EDIT_CHAT_ROOM_DESCRIPTION = 5;

	String operationUserId;

	private String roomId;
	private ProgressBar loadingPB;
	private EMChatRoom room;
	private OwnerAdminAdapter ownerAdminAdapter;
	private MemberAdapter membersAdapter;
	private ProgressDialog progressDialog;
	private TextView announcementText;

	public static ChatRoomDetailsActivity instance;

	String st = "";

	private List<String> adminList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> memberList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> muteList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> blackList = Collections.synchronizedList(new ArrayList<String>());

	ChatRoomListener chatRoomListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_activity_chatroom_details);
		instance = this;
		st = getResources().getString(R.string.people);
		loadingPB = (ProgressBar) findViewById(R.id.progressBar);

		TextView chatRoomIdTextView = (TextView) findViewById(R.id.tv_chat_room_id_value);
		TextView chatRoomNickTextView = (TextView) findViewById(R.id.tv_chat_room_nick_value);

		// get room id
		roomId = getIntent().getStringExtra("roomId");
		room = EMClient.getInstance().chatroomManager().getChatRoom(roomId);
		if (room == null) {
			return;
		}

        chatRoomListener = new ChatRoomListener();
		EMClient.getInstance().chatroomManager().addChatRoomChangeListener(chatRoomListener);

		chatRoomIdTextView.setText(roomId);
		chatRoomNickTextView.setText(room.getName());

        RelativeLayout changeChatRoomNameLayout = (RelativeLayout) findViewById(R.id.rl_change_chatroom_name);
        RelativeLayout changeChatRoomDescriptionLayout = (RelativeLayout) findViewById(R.id.rl_change_chatroom_detail);

		// adapter data list
		List<String> ownerAdminList = new ArrayList<String>();
		ownerAdminList.add(room.getOwner());
		ownerAdminList.addAll(room.getAdminList());
		ownerAdminAdapter = new OwnerAdminAdapter(this, R.layout.em_grid_owner, ownerAdminList);
		EaseExpandGridView ownerAdminGridView = (EaseExpandGridView) findViewById(R.id.owner_and_administrators);
		ownerAdminGridView.setAdapter(ownerAdminAdapter);

		// normal member list & black list && mute list
		// most show 500 members, most show 500 mute members, most show 500 black list
		List<String> memberMuteBlockList = new ArrayList<String>();
		memberMuteBlockList.addAll(room.getMemberList());
		membersAdapter = new MemberAdapter(this, R.layout.em_grid_owner, memberMuteBlockList);
		EaseExpandGridView userGridView = (EaseExpandGridView) findViewById(R.id.gridview);
		userGridView.setAdapter(membersAdapter);

		RelativeLayout announcementLayout = (RelativeLayout) findViewById(R.id.layout_group_announcement);
		announcementText = (TextView) findViewById(R.id.tv_group_announcement_value);

		updateRoom();

		changeChatRoomNameLayout.setOnClickListener(this);
		announcementLayout.setOnClickListener(this);

		final EMChatRoom finalRoom = room;
		new Thread(new Runnable() {
			@Override
			public void run() {
				String owner = finalRoom.getOwner();
				List<String> administratorList = finalRoom.getAdminList();
				membersAdapter.notifyDataSetChanged();
			}
		}).start();
	}

	boolean isCurrentOwner(EMChatRoom room) {
		String owner = room.getOwner();
		if (owner == null || owner.isEmpty()) {
			return false;
		}
		return owner.equals(EMClient.getInstance().getCurrentUser());
	}

	boolean isCurrentAdmin(EMChatRoom room) {
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

	@SuppressWarnings("UnusedAssignment")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String st1 = getResources().getString(R.string.being_added);
		String st2 = getResources().getString(R.string.is_quit_the_chat_room);
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
				case REQUEST_CODE_EXIT_DELETE:
					progressDialog.setMessage(st2);
					progressDialog.show();
					destroyChatRoom();
					break;
				case REQUEST_CODE_EDIT_CHAT_ROOM_NAME:
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								EMClient.getInstance().chatroomManager().changeChatRoomSubject(roomId, data.getStringExtra("data"));
							} catch (Exception e) {
								e.printStackTrace();
							}
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									TextView tv = (TextView) findViewById(R.id.tv_chat_room_nick_value);
									tv.setText(data.getStringExtra("data"));
								}
							});
						}
					}).start();
					break;
				case REQUEST_CODE_EDIT_CHAT_ROOM_DESCRIPTION:
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								EMClient.getInstance().chatroomManager().changeChatroomDescription(roomId, data.getStringExtra("data"));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
					break;
				default:
					break;
			}
		}
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
	 * @param
	 */
	private void destroyChatRoom() {
		new Thread(new Runnable() {
			public void run() {
				try {
                    EMClient.getInstance().chatroomManager().destroyChatRoom(roomId);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
							if (ChatActivity.activityInstance != null)
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
					room = EMClient.getInstance().chatroomManager().fetchChatRoomFromServer(roomId);
					adminList.clear();
					adminList.addAll(room.getAdminList());

					// page size set to 20 is convenient for testing, should be applied to big value
					EMCursorResult<String> result = new EMCursorResult<String>();
					memberList.clear();
					do {
						result = EMClient.getInstance().chatroomManager().fetchChatRoomMembers(roomId, result.getCursor(), 20);
						memberList.addAll(result.getData());
					} while (result.getCursor() != null && !result.getCursor().isEmpty());

					memberList.remove(room.getOwner());
					memberList.removeAll(adminList);

					try {
						EMClient.getInstance().chatroomManager().fetchChatRoomAnnouncement(roomId);
					} catch (HyphenateException e) {
						e.printStackTrace();
					}

					// those two operation need authentication, may failed
					muteList.clear();
					muteList.addAll(EMClient.getInstance().chatroomManager().fetchChatRoomMuteList(roomId, 0, 500).keySet());
					blackList.clear();
					blackList.addAll(EMClient.getInstance().chatroomManager().fetchChatRoomBlackList(roomId, 0, 500));
					memberList.removeAll(muteList);
					memberList.removeAll(blackList);

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					runOnUiThread(new Runnable() {
						public void run() {
                            refreshOwnerAdminAdapter();
                            refreshMembersAdapter();

                            TextView chatRoomTitle = (TextView) findViewById(R.id.tv_chatroom_name);
                            chatRoomTitle.setText(room.getName());
							TextView chatRoomNickTextView = (TextView) findViewById(R.id.tv_chat_room_nick_value);
							chatRoomNickTextView.setText(room.getName());
							loadingPB.setVisibility(View.INVISIBLE);

							announcementText.setText(room.getAnnouncement());

							Button destroyButton = (Button)ChatRoomDetailsActivity.this.findViewById(R.id.btn_destroy_chatroom);
							destroyButton.setVisibility(EMClient.getInstance().getCurrentUser().equals(room.getOwner()) ?
									View.VISIBLE : View.GONE);
                        }
					});
				}
			}
		}).start();
	}

	private void refreshOwnerAdminAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ownerAdminAdapter.clear();
				ownerAdminAdapter.add(room.getOwner());
				synchronized (adminList) {
					ownerAdminAdapter.addAll(adminList);
				}
				ownerAdminAdapter.notifyDataSetChanged();
			}
		});
	}

	private void refreshMembersAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				membersAdapter.clear();
				synchronized (memberList) {
					membersAdapter.addAll(memberList);
				}
				synchronized (muteList) {
					membersAdapter.addAll(muteList);
				}
				synchronized (blackList) {
					membersAdapter.addAll(blackList);
				}
				membersAdapter.notifyDataSetChanged();
            }
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.clear_all_history: // clear conversation history
				String st9 = getResources().getString(R.string.sure_to_empty_this);
				new EaseAlertDialog(ChatRoomDetailsActivity.this, null, st9, null, new AlertDialogUser() {

					@Override
					public void onResult(boolean confirmed, Bundle bundle) {
						if (confirmed) {
							clearGroupHistory();
						}
					}
				}, true).show();
				break;
			case R.id.rl_change_chatroom_name:
				startActivityForResult(new Intent(this, EditActivity.class).putExtra("data", room.getName()).putExtra("title", "edit chat room name").
								putExtra("editable", isCurrentOwner(room)),
						REQUEST_CODE_EDIT_CHAT_ROOM_NAME);
				break;
			case R.id.rl_change_chatroom_detail:
				startActivityForResult(new Intent(this, EditActivity.class).putExtra("data", room.getDescription()).putExtra("title", "edit chat room detail").
								putExtra("editable", isCurrentOwner(room)),
						REQUEST_CODE_EDIT_CHAT_ROOM_DESCRIPTION);
				break;
			case R.id.layout_group_announcement:
				showAnnouncementDialog();
				break;
			default:
				break;
		}
	}

    private final int[] ids = {
            R.id.menu_item_transfer_owner,
            R.id.menu_item_add_admin,
            R.id.menu_item_rm_admin,
            R.id.menu_item_remove_member,
            R.id.menu_item_add_to_blacklist,
            R.id.menu_item_remove_from_blacklist,
            R.id.menu_item_mute,
            R.id.menu_item_unmute
    };

    void setVisibility(Dialog viewGroups, int[] ids, boolean[] visivilities) throws Exception {
        if (ids.length != visivilities.length) {
            throw new Exception("");
        }

        for (int i = 0; i < ids.length; i++) {
            View view = viewGroups.findViewById(ids[i]);
            view.setVisibility(visivilities[i] ? View.VISIBLE : View.GONE);
        }
    }

	private void showAnnouncementDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.group_announcement);
		if(room.getOwner().equals(EMClient.getInstance().getCurrentUser()) ||
				room.getAdminList().contains(EMClient.getInstance().getCurrentUser())){
			final EditText et = new EditText(ChatRoomDetailsActivity.this);
			et.setText(room.getAnnouncement());
			builder.setView(et);
			builder.setNegativeButton(R.string.cancel,null)
					.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final String text = et.getText().toString();
							if(!text.equals(room.getAnnouncement())){
								dialog.dismiss();
								updateAnnouncement(text);
							}
						}
					});
		}else{
			builder.setMessage(room.getAnnouncement());
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

		EMClient.getInstance().groupManager().asyncUpdateGroupAnnouncement(roomId, announcement,
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
								Toast.makeText(ChatRoomDetailsActivity.this, "update fail," + error, Toast.LENGTH_LONG).show();
							}
						});
					}

					@Override
					public void onProgress(int progress, String status) {
					}
				});

	}
	private ProgressDialog createProgressDialog(){
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(ChatRoomDetailsActivity.this);
			progressDialog.setCanceledOnTouchOutside(false);
		}
		return progressDialog;
	}

    private class OwnerAdminAdapter extends ArrayAdapter<String> {
		private int res;

		/**
		 * Owner and Administrator list
		 *
		 * @param context
		 * @param textViewResourceId
		 * @param ownerAndAdministratorList the first element should be owner
		 */
		public OwnerAdminAdapter(Context context, int textViewResourceId, List<String> ownerAndAdministratorList) {
			super(context, textViewResourceId, ownerAndAdministratorList);
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
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);
			// group member item
			final String username = getItem(position);
			holder.textView.setText(username);
			EaseUserUtils.setUserNick(username, holder.textView);
			EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);
			LinearLayout id_background = (LinearLayout) convertView.findViewById(R.id.l_bg_id);
			id_background.setBackgroundColor(convertView.getResources().getColor(
					position == 0 ? R.color.holo_red_light: R.color.holo_orange_light));
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCurrentOwner(room)) {
						return;
					}
					if (username.equals(room.getOwner())) {
						return;
					}
					// do nothing here, you can show group member's profile here
					operationUserId = username;
					Dialog dialog = createMemberMenuDialog();
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
                        setVisibility(dialog, ids, adminVisibilities);
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

	Dialog createMemberMenuDialog() {
		final Dialog dialog = new Dialog(ChatRoomDetailsActivity.this);
		dialog.setTitle("chat room");
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
										EMClient.getInstance().chatroomManager().addChatRoomAdmin(roomId, operationUserId);
										break;
									case R.id.menu_item_rm_admin:
										EMClient.getInstance().chatroomManager().removeChatRoomAdmin(roomId, operationUserId);
										break;
                                    case R.id.menu_item_remove_member: {
                                            List<String> list = new ArrayList<String>();
                                            list.add(operationUserId);
                                            EMClient.getInstance().chatroomManager().removeChatRoomMembers(roomId, list);
                                        }
                                        break;
									case R.id.menu_item_add_to_blacklist: {
											List<String> list = new ArrayList<String>();
											list.add(operationUserId);
											EMClient.getInstance().chatroomManager().blockChatroomMembers(roomId, list);
										}
										break;
									case R.id.menu_item_remove_from_blacklist: {
											List<String> list1 = new ArrayList<String>();
											list1.add(operationUserId);
											EMClient.getInstance().chatroomManager().unblockChatRoomMembers(roomId, list1);
										}
										break;
									case R.id.menu_item_mute:
										List<String> muteMembers = new ArrayList<String>();
										muteMembers.add(operationUserId);
										EMClient.getInstance().chatroomManager().muteChatRoomMembers(roomId, muteMembers, 20 * 60 * 1000);
										break;
									case R.id.menu_item_unmute:
										List<String> list = new ArrayList<String>();
										list.add(operationUserId);
										EMClient.getInstance().chatroomManager().unMuteChatRoomMembers(roomId, list);
										break;
									case R.id.menu_item_transfer_owner:
										EMClient.getInstance().chatroomManager().changeOwner(roomId, operationUserId);
										break;
									default:
										break;
								}
								updateRoom();
							} catch (final HyphenateException e) {
                                runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      Toast.makeText(ChatRoomDetailsActivity.this, e.getDescription(), Toast.LENGTH_SHORT).show();
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
		return dialog;
	}


	/**
	 * group member grid adapter
	 *
	 * @author admin_new
	 */
	private class MemberAdapter extends ArrayAdapter<String> {

		private int res;

		public MemberAdapter(Context context, int textViewResourceId, List<String> objects) {
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
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);
			// group member item
			final String username = getItem(position);
			holder.textView.setText(username);
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
					if (!isCurrentOwner(room) && !isCurrentAdmin(room)) {
						return;
					}


					// do nothing here, you can show group member's profile here
					operationUserId = username;
					Dialog dialog = createMemberMenuDialog();
					dialog.show();

                    boolean[] normalVisibilities = {
                            false,      //R.id.menu_item_transfer_owner,
                            isCurrentOwner(room) ? true : false,       //R.id.menu_item_add_admin,
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
                            isCurrentOwner(room) ? true : false,       //R.id.menu_item_add_admin,
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
                            setVisibility(dialog, ids, blackListVisibilities);
                        } else if (inMuteList) {
                            setVisibility(dialog, ids, muteListVisibilities);
                        } else {
                            setVisibility(dialog, ids, normalVisibilities);
                        }
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
		EMClient.getInstance().chatroomManager().removeChatRoomListener(chatRoomListener);
		super.onDestroy();
		instance = null;
	}

	private static class ViewHolder {
		ImageView imageView;
		TextView textView;
		ImageView badgeDeleteView;
	}

	public void onDestroyChatRoomClick(View v) {
        startActivityForResult(new Intent(this, ExitGroupDialog.class).putExtra("deleteToast", getString(R.string.dissolution_group_hint)),
                REQUEST_CODE_EXIT_DELETE);
	}

	private class ChatRoomListener implements EMChatRoomChangeListener {

		@Override
		public void onChatRoomDestroyed(String roomId, String roomName) {
			finish();
		}

		@Override
		public void onMemberJoined(final String roomId, final String participant) {
			if (roomId.equals(ChatRoomDetailsActivity.this.roomId)) {
				updateRoom();
			}
		}

		@Override
		public void onMemberExited(final String roomId, final String roomName, final String participant) {
			if (roomId.equals(ChatRoomDetailsActivity.this.roomId)) {
				updateRoom();
			}
		}

		@Override
		public void onRemovedFromChatRoom(final String roomId, final String roomName, final String participant) {
			if (roomId.equals(ChatRoomDetailsActivity.this.roomId)) {
				if (participant.equals(EMClient.getInstance().getCurrentUser())) {
					finish();
				}
			}
		}

		@Override
		public void onMuteListAdded(final String chatRoomId, final List<String> mutes, final long expireTime) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final StringBuilder sb = new StringBuilder();
                        for (String mute : mutes) {
                            sb.append(mute + " ");
                        }
                        Toast.makeText(ChatRoomDetailsActivity.this, "onMuteListAdded: " + sb.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
				updateRoom();
			}
		}

		@Override
		public void onMuteListRemoved(final String chatRoomId, final List<String> mutes) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId)) {
				final StringBuilder sb = new StringBuilder();
				for (String mute : mutes) {
					sb.append(mute + " ");
				}
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ChatRoomDetailsActivity.this, "onMuteListRemoved: " + sb.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
				refreshMembersAdapter();
			}
		}

		@Override
		public void onAdminAdded(final String chatRoomId, final String admin) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ChatRoomDetailsActivity.this, "onAdminAdded: " + admin, Toast.LENGTH_SHORT).show();
                    }
                });
				updateRoom();
			}
		}

		@Override
		public void onAdminRemoved(final String chatRoomId, final String admin) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ChatRoomDetailsActivity.this, "onAdminRemoved: " + admin, Toast.LENGTH_SHORT).show();
                    }
                });
				updateRoom();
			}
		}

		@Override
		public void onOwnerChanged(final String chatRoomId, final String newOwner, final String oldOwner) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ChatRoomDetailsActivity.this, "onOwnerChanged newOwner:" + newOwner + "  oldOwner" + oldOwner, Toast.LENGTH_SHORT).show();
                    }
                });
				updateRoom();
			}
		}

		@Override
		public void onAnnouncementChanged(String chatRoomId, final String announcement) {
			if (chatRoomId.equals(ChatRoomDetailsActivity.this.roomId))
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					announcementText.setText(announcement);
				}
			});
		}
	}
}

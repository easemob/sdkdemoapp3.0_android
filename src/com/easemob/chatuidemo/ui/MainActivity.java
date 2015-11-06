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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.easemob.EMCallBack;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMConversation.EMConversationType;
import com.easemob.chat.EMMessage;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.db.InviteMessgeDao;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.chatuidemo.domain.InviteMessage;
import com.easemob.easeui.utils.EaseCommonUtils;
import com.easemob.util.EMLog;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

public class MainActivity extends BaseActivity implements EMEventListener {

	protected static final String TAG = "MainActivity";
	// 未读消息textview
	private TextView unreadLabel;
	// 未读通讯录textview
	private TextView unreadAddressLable;

	private Button[] mTabs;
	private ContactListFragment contactListFragment;
	// private conversationListFragment conversationListFragment;
//	private ChatAllHistoryFragment conversationListFragment;
	private SettingsFragment settingFragment;
	private Fragment[] fragments;
	private int index;
	// 当前fragment的index
	private int currentTabIndex;
	// 账号在别处登录
	public boolean isConflict = false;
	// 账号被移除
	private boolean isCurrentAccountRemoved = false;
	

	/**
	 * 检查当前用户是否被删除
	 */
	public boolean getCurrentAccountRemoved() {
		return isCurrentAccountRemoved;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null && savedInstanceState.getBoolean(Constant.ACCOUNT_REMOVED, false)) {
			// 防止被移除后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
			// 三个fragment里加的判断同理
		    DemoHelper.getInstance().logout(true,null);
			finish();
			startActivity(new Intent(this, LoginActivity.class));
			return;
		} else if (savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false)) {
			// 防止被T后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
			// 三个fragment里加的判断同理
			finish();
			startActivity(new Intent(this, LoginActivity.class));
			return;
		}
		setContentView(R.layout.em_activity_main);
		initView();

		//umeng api
		MobclickAgent.updateOnlineConfig(this);
		UmengUpdateAgent.setUpdateOnlyWifi(false);
		UmengUpdateAgent.update(this);

		if (getIntent().getBooleanExtra(Constant.ACCOUNT_CONFLICT, false) && !isConflictDialogShow) {
			showConflictDialog();
		} else if (getIntent().getBooleanExtra(Constant.ACCOUNT_REMOVED, false) && !isAccountRemovedDialogShow) {
			showAccountRemovedDialog();
		}

		inviteMessgeDao = new InviteMessgeDao(this);
		userDao = new UserDao(this);
		conversationListFragment = new ConversationListFragment();
		contactListFragment = new ContactListFragment();
		settingFragment = new SettingsFragment();
		fragments = new Fragment[] { conversationListFragment, contactListFragment, settingFragment };
		// 添加显示第一个fragment
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, conversationListFragment)
				.add(R.id.fragment_container, contactListFragment).hide(contactListFragment).show(conversationListFragment)
				.commit();
		
		// 注册群组和联系人监听
        DemoHelper.getInstance().registerGroupAndContactListener();
		registerBroadcastReceiver();
		
		
		//内部测试方法，请忽略
        registerInternalDebugReceiver();
	}

	
	/**
	 * 初始化组件
	 */
	private void initView() {
		unreadLabel = (TextView) findViewById(R.id.unread_msg_number);
		unreadAddressLable = (TextView) findViewById(R.id.unread_address_number);
		mTabs = new Button[3];
		mTabs[0] = (Button) findViewById(R.id.btn_conversation);
		mTabs[1] = (Button) findViewById(R.id.btn_address_list);
		mTabs[2] = (Button) findViewById(R.id.btn_setting);
		// 把第一个tab设为选中状态
		mTabs[0].setSelected(true);
	}

	/**
	 * button点击事件
	 * 
	 * @param view
	 */
	public void onTabClicked(View view) {
		switch (view.getId()) {
		case R.id.btn_conversation:
			index = 0;
			break;
		case R.id.btn_address_list:
			index = 1;
			break;
		case R.id.btn_setting:
			index = 2;
			break;
		}
		if (currentTabIndex != index) {
			FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
			trx.hide(fragments[currentTabIndex]);
			if (!fragments[index].isAdded()) {
				trx.add(R.id.fragment_container, fragments[index]);
			}
			trx.show(fragments[index]).commit();
		}
		mTabs[currentTabIndex].setSelected(false);
		// 把当前tab设为选中状态
		mTabs[index].setSelected(true);
		currentTabIndex = index;
	}

	/**
	 * 监听事件
     */
	@Override
	public void onEvent(EMNotifierEvent event) {
		switch (event.getEvent()) {
		case EventNewMessage: // 普通消息
		{
			EMMessage message = (EMMessage) event.getData();

			// 提示新消息
			DemoHelper.getInstance().getNotifier().onNewMsg(message);

			refreshUIWithMessage();
			break;
		}

		case EventOfflineMessage: {
		    refreshUIWithMessage();
			break;
		}

		case EventConversationListChanged: {
		    refreshUIWithMessage();
		    break;
		}
		
		default:
			break;
		}
	}

	private void refreshUIWithMessage() {
		runOnUiThread(new Runnable() {
			public void run() {
				// 刷新bottom bar消息未读数
				updateUnreadLabel();
				if (currentTabIndex == 0) {
					// 当前页面如果为聊天历史页面，刷新此页面
					if (conversationListFragment != null) {
						conversationListFragment.refresh();
					}
				}
			}
		});
	}

	@Override
	public void back(View view) {
		super.back(view);
	}
	
	private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_CONTACT_CHANAGED);
        intentFilter.addAction(Constant.ACTION_GROUP_CHANAGED);
        broadcastReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUnreadLabel();
                updateUnreadAddressLable();
                if (currentTabIndex == 0) {
                    // 当前页面如果为聊天历史页面，刷新此页面
                    if (conversationListFragment != null) {
                        conversationListFragment.refresh();
                    }
                } else if (currentTabIndex == 1) {
                    if(contactListFragment != null) {
                        contactListFragment.refresh();
                    }
                }
                String action = intent.getAction();
                if(action.equals(Constant.ACTION_GROUP_CHANAGED)){
                    if (EaseCommonUtils.getTopActivity(MainActivity.this).equals(GroupsActivity.class.getName())) {
                        GroupsActivity.instance.onResume();
                    }
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }
	
	private void unregisterBroadcastReceiver(){
	    broadcastManager.unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();		
		
		if (conflictBuilder != null) {
			conflictBuilder.create().dismiss();
			conflictBuilder = null;
		}
		unregisterBroadcastReceiver();

		try {
            unregisterReceiver(internalDebugReceiver);
        } catch (Exception e) {
        }
	}

	/**
	 * 刷新未读消息数
	 */
	public void updateUnreadLabel() {
		int count = getUnreadMsgCountTotal();
		if (count > 0) {
			unreadLabel.setText(String.valueOf(count));
			unreadLabel.setVisibility(View.VISIBLE);
		} else {
			unreadLabel.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * 刷新申请与通知消息数
	 */
	public void updateUnreadAddressLable() {
		runOnUiThread(new Runnable() {
			public void run() {
				int count = getUnreadAddressCountTotal();
				if (count > 0) {
//					unreadAddressLable.setText(String.valueOf(count));
					unreadAddressLable.setVisibility(View.VISIBLE);
				} else {
					unreadAddressLable.setVisibility(View.INVISIBLE);
				}
			}
		});

	}

	/**
	 * 获取未读申请与通知消息
	 * 
	 * @return
	 */
	public int getUnreadAddressCountTotal() {
		int unreadAddressCountTotal = 0;
		unreadAddressCountTotal = inviteMessgeDao.getUnreadMessagesCount();
		return unreadAddressCountTotal;
	}

	/**
	 * 获取未读消息数
	 * 
	 * @return
	 */
	public int getUnreadMsgCountTotal() {
		int unreadMsgCountTotal = 0;
		int chatroomUnreadMsgCount = 0;
		unreadMsgCountTotal = EMChatManager.getInstance().getUnreadMsgsCount();
		for(EMConversation conversation:EMChatManager.getInstance().getAllConversations().values()){
			if(conversation.getType() == EMConversationType.ChatRoom)
			chatroomUnreadMsgCount=chatroomUnreadMsgCount+conversation.getUnreadMsgCount();
		}
		return unreadMsgCountTotal-chatroomUnreadMsgCount;
	}

	private InviteMessgeDao inviteMessgeDao;
	private UserDao userDao;




	/**
	 * 保存提示新消息
	 * 
	 * @param msg
	 */
	private void notifyNewIviteMessage(InviteMessage msg) {
		saveInviteMsg(msg);
		// 提示有新消息
		DemoHelper.getInstance().getNotifier().viberateAndPlayTone(null);

		// 刷新bottom bar消息未读数
		updateUnreadAddressLable();
		// 刷新好友页面ui
		if (currentTabIndex == 1)
			contactListFragment.refresh();
	}

	/**
	 * 保存邀请等msg
	 * 
	 * @param msg
	 */
	private void saveInviteMsg(InviteMessage msg) {
		// 保存msg
		inviteMessgeDao.saveMessage(msg);
		//保存未读数，这里没有精确计算
		inviteMessgeDao.saveUnreadMessageCount(1);
	}


	@Override
	protected void onResume() {
		super.onResume();
		
		if (!isConflict && !isCurrentAccountRemoved) {
			updateUnreadLabel();
			updateUnreadAddressLable();
		}

		// unregister this event listener when this activity enters the
		// background
		DemoHelper sdkHelper = DemoHelper.getInstance();
		sdkHelper.pushActivity(this);

		// register the event listener when enter the foreground
		EMChatManager.getInstance().registerEventListener(this,
				new EMNotifierEvent.Event[] { EMNotifierEvent.Event.EventNewMessage ,EMNotifierEvent.Event.EventOfflineMessage, EMNotifierEvent.Event.EventConversationListChanged});
	}

	@Override
	protected void onStop() {
		EMChatManager.getInstance().unregisterEventListener(this);
		DemoHelper sdkHelper = DemoHelper.getInstance();
		sdkHelper.popActivity(this);

		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isConflict", isConflict);
		outState.putBoolean(Constant.ACCOUNT_REMOVED, isCurrentAccountRemoved);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(false);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private android.app.AlertDialog.Builder conflictBuilder;
	private android.app.AlertDialog.Builder accountRemovedBuilder;
	private boolean isConflictDialogShow;
	private boolean isAccountRemovedDialogShow;
    private BroadcastReceiver internalDebugReceiver;
    private ConversationListFragment conversationListFragment;
    private BroadcastReceiver broadcastReceiver;
    private LocalBroadcastManager broadcastManager;

	/**
	 * 显示帐号在别处登录dialog
	 */
	private void showConflictDialog() {
		isConflictDialogShow = true;
		DemoHelper.getInstance().logout(false,null);
		String st = getResources().getString(R.string.Logoff_notification);
		if (!MainActivity.this.isFinishing()) {
			// clear up global variables
			try {
				if (conflictBuilder == null)
					conflictBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
				conflictBuilder.setTitle(st);
				conflictBuilder.setMessage(R.string.connect_conflict);
				conflictBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						conflictBuilder = null;
						finish();
						startActivity(new Intent(MainActivity.this, LoginActivity.class));
					}
				});
				conflictBuilder.setCancelable(false);
				conflictBuilder.create().show();
				isConflict = true;
			} catch (Exception e) {
				EMLog.e(TAG, "---------color conflictBuilder error" + e.getMessage());
			}

		}

	}

	/**
	 * 帐号被移除的dialog
	 */
	private void showAccountRemovedDialog() {
		isAccountRemovedDialogShow = true;
		DemoHelper.getInstance().logout(true,null);
		String st5 = getResources().getString(R.string.Remove_the_notification);
		if (!MainActivity.this.isFinishing()) {
			// clear up global variables
			try {
				if (accountRemovedBuilder == null)
					accountRemovedBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
				accountRemovedBuilder.setTitle(st5);
				accountRemovedBuilder.setMessage(R.string.em_user_remove);
				accountRemovedBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						accountRemovedBuilder = null;
						finish();
						startActivity(new Intent(MainActivity.this, LoginActivity.class));
					}
				});
				accountRemovedBuilder.setCancelable(false);
				accountRemovedBuilder.create().show();
				isCurrentAccountRemoved = true;
			} catch (Exception e) {
				EMLog.e(TAG, "---------color userRemovedBuilder error" + e.getMessage());
			}

		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getBooleanExtra(Constant.ACCOUNT_CONFLICT, false) && !isConflictDialogShow) {
			showConflictDialog();
		} else if (intent.getBooleanExtra(Constant.ACCOUNT_REMOVED, false) && !isAccountRemovedDialogShow) {
			showAccountRemovedDialog();
		}
	}
	
	/**
	 * 内部测试代码，开发者请忽略
	 */
	private void registerInternalDebugReceiver() {
	    internalDebugReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                DemoHelper.getInstance().logout(true,new EMCallBack() {
                    
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // 重新显示登陆页面
                                finish();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                
                            }
                        });
                    }
                    
                    @Override
                    public void onProgress(int progress, String status) {}
                    
                    @Override
                    public void onError(int code, String message) {}
                });
            }
        };
        IntentFilter filter = new IntentFilter(getPackageName() + ".em_internal_debug");
        registerReceiver(internalDebugReceiver, filter);
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		//getMenuInflater().inflate(R.menu.context_tab_contact, menu);
	}
}

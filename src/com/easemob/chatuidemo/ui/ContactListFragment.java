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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.easemob.chat.EMContactManager;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoSDKHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.db.InviteMessgeDao;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.easeui.controller.EaseSDKHelper;
import com.easemob.easeui.controller.EaseSDKHelper.HXSyncListener;
import com.easemob.easeui.domain.EaseSystemUser;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.widget.EaseContactList;
import com.easemob.easeui.widget.EaseTitleBar;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;

/**
 * 联系人列表页
 * 
 */
public class ContactListFragment extends Fragment {
	public static final String TAG = "ContactlistFragment";
	private List<EaseUser> contactList;
	private ListView listView;
	private boolean hidden;
	private InputMethodManager inputMethodManager;
	private List<String> blackList;
	ImageButton clearSearch;
	EditText query;
	HXContactSyncListener contactSyncListener;
	HXBlackListSyncListener blackListSyncListener;
	View progressBar;
	Handler handler = new Handler();
    private EaseUser toBeProcessUser;
    private String toBeProcessUsername;
    private EaseTitleBar titleBar;
    private EaseContactList contactListLayout;

	class HXContactSyncListener implements EaseSDKHelper.HXSyncListener {
		@Override
		public void onSyncSucess(final boolean success) {
			EMLog.d(TAG, "on contact list sync success:" + success);
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
				    getActivity().runOnUiThread(new Runnable(){

		                @Override
		                public void run() {
		                    if(success){
		                        progressBar.setVisibility(View.GONE);
                                refresh();
		                    }else{
		                        String s1 = getResources().getString(R.string.get_failed_please_check);
		                        Toast.makeText(getActivity(), s1, 1).show();
		                        progressBar.setVisibility(View.GONE);
		                    }
		                }
		                
		            });
				}
			});
		}
	}
	
	class HXBlackListSyncListener implements HXSyncListener{

        @Override
        public void onSyncSucess(boolean success) {
            getActivity().runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    blackList = EMContactManager.getInstance().getBlackListUsernames();
                    refresh();
                }
                
            });
        }
	    
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.em_fragment_contact_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//防止被T后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
		if(savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false))
		    return;
		inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

		titleBar = (EaseTitleBar) getView().findViewById(R.id.title_bar);
		contactListLayout = (EaseContactList) getView().findViewById(R.id.contact_list);
		
		//黑名单列表
		blackList = EMContactManager.getInstance().getBlackListUsernames();
		contactList = new ArrayList<EaseUser>();
		// 获取设置contactlist
		getContactList();
		//设置数据
		contactListLayout.setContactList(contactList);
		contactListLayout.init();
		
		listView = contactListLayout.getListView();
		
		//搜索框
		query = (EditText) getView().findViewById(R.id.query);
		query.setHint(R.string.search);
		clearSearch = (ImageButton) getView().findViewById(R.id.search_clear);
		query.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				contactListLayout.filter(s);
				if (s.length() > 0) {
					clearSearch.setVisibility(View.VISIBLE);
				} else {
					clearSearch.setVisibility(View.INVISIBLE);
					
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});
		clearSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				query.getText().clear();
				hideSoftKeyboard();
			}
		});
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String username = ((EaseUser)listView.getItemAtPosition(position)).getUsername();
				if (Constant.NEW_FRIENDS_USERNAME.equals(username)) {
					// 进入申请与通知页面
				    EaseSystemUser user = (EaseSystemUser) ((DemoSDKHelper)EaseSDKHelper.getInstance()).getContactList().get(Constant.NEW_FRIENDS_USERNAME);
					user.setUnreadMsgCount(0);
					startActivity(new Intent(getActivity(), NewFriendsMsgActivity.class));
				} else if (Constant.GROUP_USERNAME.equals(username)) {
					// 进入群聊列表页面
					startActivity(new Intent(getActivity(), GroupsActivity.class));
				} else if(Constant.CHAT_ROOM.equals(username)){
					//进入聊天室列表页面
				    startActivity(new Intent(getActivity(), PublicChatRoomsActivity.class));
				}else if(Constant.CHAT_ROBOT.equals(username)){
					//进入Robot列表页面
					startActivity(new Intent(getActivity(), RobotsActivity.class));
				}else {
					// demo中直接进入聊天页面，实际一般是进入用户详情页
					startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("userId", username));
				}
			}
		});
		listView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 隐藏软键盘
				hideSoftKeyboard();
				return false;
			}
		});

		
		// 进入添加好友页
		titleBar.getRightLayout().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), AddContactActivity.class));
			}
		});
		registerForContextMenu(listView);
		
		progressBar = (View) getView().findViewById(R.id.progress_bar);

		contactSyncListener = new HXContactSyncListener();
		EaseSDKHelper.getInstance().addSyncContactListener(contactSyncListener);
		
		blackListSyncListener = new HXBlackListSyncListener();
		EaseSDKHelper.getInstance().addSyncBlackListListener(blackListSyncListener);
		
		if (!EaseSDKHelper.getInstance().isContactsSyncedWithServer()) {
			progressBar.setVisibility(View.VISIBLE);
		} else {
			progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (((AdapterContextMenuInfo) menuInfo).position > 3) {
		    toBeProcessUser = (EaseUser) listView.getItemAtPosition(((AdapterContextMenuInfo) menuInfo).position);
		    toBeProcessUsername = toBeProcessUser.getUsername();
			getActivity().getMenuInflater().inflate(R.menu.em_context_contact_list, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.delete_contact) {
			try {
                // 删除此联系人
                deleteContact(toBeProcessUser);
                // 删除相关的邀请消息
                InviteMessgeDao dao = new InviteMessgeDao(getActivity());
                dao.deleteMessage(toBeProcessUser.getUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
			return true;
		}else if(item.getItemId() == R.id.add_to_blacklist){
			moveToBlacklist(toBeProcessUsername);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		this.hidden = hidden;
		if (!hidden) {
			refresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!hidden) {
			refresh();
		}
	}

	/**
	 * 删除联系人
	 * 
	 * @param toDeleteUser
	 */
	public void deleteContact(final EaseUser tobeDeleteUser) {
		String st1 = getResources().getString(R.string.deleting);
		final String st2 = getResources().getString(R.string.Delete_failed);
		final ProgressDialog pd = new ProgressDialog(getActivity());
		pd.setMessage(st1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					EMContactManager.getInstance().deleteContact(tobeDeleteUser.getUsername());
					// 删除db和内存中此用户的数据
					UserDao dao = new UserDao(getActivity());
					dao.deleteContact(tobeDeleteUser.getUsername());
					((DemoSDKHelper)EaseSDKHelper.getInstance()).getContactList().remove(tobeDeleteUser.getUsername());
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							contactList.remove(tobeDeleteUser);
							contactListLayout.refresh();

						}
					});
				} catch (final Exception e) {
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st2 + e.getMessage(), 1).show();
						}
					});

				}

			}
		}).start();

	}

	/**
	 * 把user移入到黑名单
	 */
	private void moveToBlacklist(final String username){
		final ProgressDialog pd = new ProgressDialog(getActivity());
		String st1 = getResources().getString(R.string.Is_moved_into_blacklist);
		final String st2 = getResources().getString(R.string.Move_into_blacklist_success);
		final String st3 = getResources().getString(R.string.Move_into_blacklist_failure);
		pd.setMessage(st1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					//加入到黑名单
					EMContactManager.getInstance().addUserToBlackList(username,false);
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st2, 0).show();
							refresh();
						}
					});
				} catch (EaseMobException e) {
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st3, 0).show();
						}
					});
				}
			}
		}).start();
		
	}
	
	// 刷新ui
	public void refresh() {
		getContactList();
		contactListLayout.refresh();
	}

	@Override
	public void onDestroy() {
		if (contactSyncListener != null) {
			EaseSDKHelper.getInstance().removeSyncContactListener(contactSyncListener);
			contactSyncListener = null;
		}
		
		if(blackListSyncListener != null){
		    EaseSDKHelper.getInstance().removeSyncBlackListListener(blackListSyncListener);
		}
		
		super.onDestroy();
	}
	
	public void showProgressBar(boolean show) {
		if (progressBar != null) {
			if (show) {
				progressBar.setVisibility(View.VISIBLE);
			} else {
				progressBar.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * 获取联系人列表，并过滤掉黑名单和排序
	 */
	private void getContactList() {
		contactList.clear();
		//获取本地好友列表
		Map<String, EaseUser> users = ((DemoSDKHelper)EaseSDKHelper.getInstance()).getContactList();
		Iterator<Entry<String, EaseUser>> iterator = users.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, EaseUser> entry = iterator.next();
			if (!entry.getKey().equals(Constant.NEW_FRIENDS_USERNAME)
			        && !entry.getKey().equals(Constant.GROUP_USERNAME)
			        && !entry.getKey().equals(Constant.CHAT_ROOM)
					&& !entry.getKey().equals(Constant.CHAT_ROBOT)
					&& !blackList.contains(entry.getKey()))
				contactList.add(entry.getValue());
		}
		// 排序
		Collections.sort(contactList, new Comparator<EaseUser>() {

			@Override
			public int compare(EaseUser lhs, EaseUser rhs) {
				return lhs.getUsername().compareTo(rhs.getUsername());
			}
		});

		if(users.get(Constant.CHAT_ROBOT)!=null){
			contactList.add(0, users.get(Constant.CHAT_ROBOT));
		}
		// 加入"群聊"和"聊天室"
        if(users.get(Constant.CHAT_ROOM) != null)
            contactList.add(0, users.get(Constant.CHAT_ROOM));
        if(users.get(Constant.GROUP_USERNAME) != null)
            contactList.add(0, users.get(Constant.GROUP_USERNAME));
        
		// 把"申请与通知"添加到首位
		if(users.get(Constant.NEW_FRIENDS_USERNAME) != null)
		    contactList.add(0, users.get(Constant.NEW_FRIENDS_USERNAME));
		
	}
	
	void hideSoftKeyboard() {
        if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getActivity().getCurrentFocus() != null)
                inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	    if(((MainActivity)getActivity()).isConflict){
	    	outState.putBoolean("isConflict", true);
	    }else if(((MainActivity)getActivity()).getCurrentAccountRemoved()){
	    	outState.putBoolean(Constant.ACCOUNT_REMOVED, true);
	    }
	    
	}
}

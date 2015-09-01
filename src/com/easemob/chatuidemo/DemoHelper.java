package com.easemob.chatuidemo;

import java.util.Map;

import android.content.Context;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chatuidemo.db.DemoDBManager;
import com.easemob.chatuidemo.domain.RobotUser;
import com.easemob.chatuidemo.parse.UserProfileManager;
import com.easemob.chatuidemo.utils.PreferenceUtils;
import com.easemob.easeui.controller.EaseUI;
import com.easemob.easeui.domain.EaseUser;

public class DemoHelper {
	private EaseUI easeUI;

	private Map<String, EaseUser> contactList;

	private Map<String, RobotUser> robotList;

	private UserProfileManager userProManager;

	private static DemoHelper instance = null;
	
	public boolean isVoiceCalling;
    public boolean isVideoCalling;

	private String username;

	private DemoHelper() {
	}

	public synchronized static DemoHelper getInstance() {
		if (instance == null) {
			instance = new DemoHelper();
		}
		return instance;
	}

	/**
	 * init helper
	 * 
	 * @param context
	 *            application context
	 */
	public void init(Context context) {
		if (EaseUI.getInstance().init(context)) {
			easeUI = EaseUI.getInstance();
		}
	}

	/**
	 * 是否登录成功过
	 * 
	 * @return
	 */
	public boolean isLoggedIn() {
		return EMChat.getInstance().isLoggedIn();
	}

	/**
	 * 退出登录
	 * 
	 * @param unbindDeviceToken
	 *            设备token(使用GCM才有)
	 * @param callback
	 *            callback
	 */
	public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
		endCall();
		EMChatManager.getInstance().logout(unbindDeviceToken, new EMCallBack() {

			@Override
			public void onSuccess() {
				setContactList(null);
				setRobotList(null);
				getUserProfileManager().reset();
				DemoDBManager.getInstance().closeDB();

				if (callback != null) {
					callback.onSuccess();
				}

			}

			@Override
			public void onProgress(int progress, String status) {
				if (callback != null) {
					callback.onProgress(progress, status);
				}
			}

			@Override
			public void onError(int code, String error) {
				if (callback != null) {
					callback.onError(code, error);
				}
			}
		});
	}

	/**
	 * 设置好友user list到内存中
	 * 
	 * @param contactList
	 */
	public void setContactList(Map<String, EaseUser> contactList) {
		this.contactList = contactList;
	}
	
	/**
     * 保存单个user 
     */
    public void saveContact(EaseUser user){
    	contactList.put(user.getUsername(), user);
    	DemoDBManager.getInstance().saveContact(user);
    }
    
    /**
     * 获取好友list
     *
     * @return
     */
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            contactList = DemoDBManager.getInstance().getContactList();
        }
        
        return contactList;
    }
    
    /**
     * 设置当前用户的环信id
     * @param username
     */
    public void setCurrentUserName(String username){
    	this.username = username;
    	PreferenceUtils.getInstance().setCurrentUserName(username);
    }
    
    /**
     * 获取当前用户的环信id
     */
    public String getCurrentUsernName(){
    	if(username == null){
    		username = PreferenceUtils.getInstance().getCurrentUsername();
    	}
    	return username;
    }

	public void setRobotList(Map<String, RobotUser> robotList) {
		this.robotList = robotList;
	}
	public Map<String, RobotUser> getRobotList() {
		if (isLoggedIn() && robotList == null) {
			robotList = DemoDBManager.getInstance().getRobotList();
		}
		return robotList;
	}


	public UserProfileManager getUserProfileManager() {
		if (userProManager == null) {
			userProManager = new UserProfileManager();
		}
		return userProManager;
	}

	void endCall() {
		try {
			EMChatManager.getInstance().endCall();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

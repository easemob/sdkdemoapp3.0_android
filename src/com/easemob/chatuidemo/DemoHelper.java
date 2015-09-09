package com.easemob.chatuidemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.EMConnectionListener;
import com.easemob.EMError;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.EMValueCallBack;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.EMMessage.ChatType;
import com.easemob.chat.EMMessage.Type;
import com.easemob.chatuidemo.db.DemoDBManager;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.chatuidemo.domain.RobotUser;
import com.easemob.chatuidemo.parse.UserProfileManager;
import com.easemob.chatuidemo.receiver.CallReceiver;
import com.easemob.chatuidemo.ui.ChatActivity;
import com.easemob.chatuidemo.ui.MainActivity;
import com.easemob.chatuidemo.ui.VideoCallActivity;
import com.easemob.chatuidemo.ui.VoiceCallActivity;
import com.easemob.chatuidemo.utils.PreferenceManager;
import com.easemob.easeui.R;
import com.easemob.easeui.controller.EaseUI;
import com.easemob.easeui.controller.EaseUI.EaseSettingsProvider;
import com.easemob.easeui.controller.EaseUI.EaseUserProfileProvider;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.model.EaseNotifier;
import com.easemob.easeui.model.EaseNotifier.EaseNotificationInfoProvider;
import com.easemob.easeui.utils.EaseCommonUtils;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;

public class DemoHelper {
    /**
     * 数据同步listener
     */
    static public interface DataSyncListener {
        /**
         * 同步完毕
         * @param success true：成功同步到数据，false失败
         */
        public void onSyncComplete(boolean success);
    }

    protected static final String TAG = "DemoHelper";
    
	private EaseUI easeUI;
	
    /**
     * EMEventListener
     */
    protected EMEventListener eventListener = null;

	private Map<String, EaseUser> contactList;

	private Map<String, RobotUser> robotList;

	private UserProfileManager userProManager;

	private static DemoHelper instance = null;
	
	/**
     * HuanXin sync groups status listener
     */
    private List<DataSyncListener> syncGroupsListeners;

    /**
     * HuanXin sync contacts status listener
     */
    private List<DataSyncListener> syncContactsListeners;

    /**
     * HuanXin sync blacklist status listener
     */
    private List<DataSyncListener> syncBlackListListeners;

    private boolean isSyncingGroupsWithServer = false;

    private boolean isSyncingContactsWithServer = false;

    private boolean isSyncingBlackListWithServer = false;
    
    private boolean isGroupsSyncedWithServer = false;

    private boolean isContactsSyncedWithServer = false;

    private boolean isBlackListSyncedWithServer = false;
    
    private boolean alreadyNotified = false;
	
	public boolean isVoiceCalling;
    public boolean isVideoCalling;

	private String username;

    private Context appContext;

    private CallReceiver callReceiver;

    private EMConnectionListener connectionListener;

    private DemoSettingsModel settingsModel;

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
		    appContext = context;
		    //设为调试模式，打成正式包时，最好设为false，以免消耗额外的资源
		    EMChat.getInstance().setDebugMode(true);
		    easeUI = EaseUI.getInstance();
		    //调用easeui的api设置providers
		    setEaseUIProviders();
			//初始化PreferenceManager
			PreferenceManager.init(context);
			//初始化用户管理类
			getUserProfileManager().init(context);
			//设置全局监听
			setListeners();
			
		}
	}

    protected void setEaseUIProviders() {
        //需要easeui库显示用户头像和昵称设置此provider
        easeUI.setUserProfileProvider(new EaseUserProfileProvider() {
            
            @Override
            public EaseUser getUser(String username) {
                return getUserInfo(username);
            }
        });
        
        settingsModel = new DemoSettingsModel(appContext);
        //不设置，则使用easeui默认的
        easeUI.setSettingsProvider(new EaseSettingsProvider() {
            
            @Override
            public boolean isSpeakerOpened() {
                return settingsModel.getSettingMsgSpeaker();
            }
            
            @Override
            public boolean isMsgVibrateAllowed(EMMessage message) {
                return settingsModel.getSettingMsgVibrate();
            }
            
            @Override
            public boolean isMsgSoundAllowed(EMMessage message) {
                return settingsModel.getSettingMsgSound();
            }
            
            @Override
            public boolean isMsgNotifyAllowed(EMMessage message) {
                if(message == null){
                    return settingsModel.getSettingMsgNotification();
                }
                if(!settingsModel.getSettingMsgNotification()){
                    return false;
                }else{
                    //如果允许新消息提示
                    //屏蔽的用户和群组不提示用户
                    String chatUsename = null;
                    List<String> notNotifyIds = null;
                    // 获取设置的不提示新消息的用户或者群组ids
                    if (message.getChatType() == ChatType.Chat) {
                        chatUsename = message.getFrom();
                        notNotifyIds = settingsModel.getDisabledIds();
                    } else {
                        chatUsename = message.getTo();
                        notNotifyIds = settingsModel.getDisabledGroups();
                    }

                    if (notNotifyIds == null || !notNotifyIds.contains(chatUsename)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        });
        //不设置，则使用easeui默认的
        easeUI.getNotifier().setNotificationInfoProvider(new EaseNotificationInfoProvider() {
            
            @Override
            public String getTitle(EMMessage message) {
              //修改标题,这里使用默认
                return null;
            }
            
            @Override
            public int getSmallIcon(EMMessage message) {
              //设置小图标，这里为默认
                return 0;
            }
            
            @Override
            public String getDisplayedText(EMMessage message) {
                // 设置状态栏的消息提示，可以根据message的类型做相应提示
                String ticker = EaseCommonUtils.getMessageDigest(message, appContext);
                if(message.getType() == Type.TXT){
                    ticker = ticker.replaceAll("\\[.{2,3}\\]", "[表情]");
                }
                EaseUser user = getUserInfo(message.getFrom());
                if(user != null){
                    return getUserInfo(message.getFrom()).getNick() + ": " + ticker;
                }else{
                    return message.getFrom() + ": " + ticker;
                }
            }
            
            @Override
            public String getLatestText(EMMessage message, int fromUsersNum, int messageNum) {
                return null;
                // return fromUsersNum + "个基友，发来了" + messageNum + "条消息";
            }
            
            @Override
            public Intent getLaunchIntent(EMMessage message) {
                //设置点击通知栏跳转事件
                Intent intent = new Intent(appContext, ChatActivity.class);
                //有电话时优先跳转到通话页面
                if(isVideoCalling){
                    intent = new Intent(appContext, VideoCallActivity.class);
                }else if(isVoiceCalling){
                    intent = new Intent(appContext, VoiceCallActivity.class);
                }else{
                    ChatType chatType = message.getChatType();
                    if (chatType == ChatType.Chat) { // 单聊信息
                        intent.putExtra("userId", message.getFrom());
                        intent.putExtra("chatType", Constant.CHATTYPE_SINGLE);
                    } else { // 群聊信息
                        // message.getTo()为群聊id
                        intent.putExtra("userId", message.getTo());
                        if(chatType == ChatType.GroupChat){
                            intent.putExtra("chatType", Constant.CHATTYPE_GROUP);
                        }else{
                            intent.putExtra("chatType", Constant.CHATTYPE_CHATROOM);
                        }
                        
                    }
                }
                return intent;
            }
        });
    }
    
    protected void setListeners(){
        syncGroupsListeners = new ArrayList<DataSyncListener>();
        syncContactsListeners = new ArrayList<DataSyncListener>();
        syncBlackListListeners = new ArrayList<DataSyncListener>();
        
        isGroupsSyncedWithServer = PreferenceManager.getInstance().isGroupsSynced();
        isContactsSyncedWithServer = PreferenceManager.getInstance().isContactSynced();
        isBlackListSyncedWithServer = PreferenceManager.getInstance().isBacklistSynced();
        
        // create the global connection listener
        connectionListener = new EMConnectionListener() {
            @Override
            public void onDisconnected(int error) {
                if (error == EMError.USER_REMOVED) {
                    onCurrentAccountRemoved();
                }else if (error == EMError.CONNECTION_CONFLICT) {
                    onConnectionConflict();
                }
            }

            @Override
            public void onConnected() {
                boolean groupSynced = DemoHelper.getInstance().isGroupsSyncedWithServer();
                boolean contactSynced = DemoHelper.getInstance().isContactsSyncedWithServer();
                
                // in case group and contact were already synced, we supposed to notify sdk we are ready to receive the events
                if(groupSynced && contactSynced){
                    new Thread(){
                        @Override
                        public void run(){
                            DemoHelper.getInstance().notifyForRecevingEvents();
                        }
                    }.start();
                }else{
                    if(!groupSynced){
                        asyncFetchGroupsFromServer(null);
                    }
                    
                    if(!contactSynced){
                        asyncFetchContactsFromServer(null);
                    }
                    
                    if(!DemoHelper.getInstance().isBlackListSyncedWithServer()){
                        asyncFetchBlackListFromServer(null);
                    }
                }
            }
        };
        
        
        IntentFilter callFilter = new IntentFilter(EMChatManager.getInstance().getIncomingCallBroadcastAction());
        if(callReceiver == null){
            callReceiver = new CallReceiver();
        }

        //注册通话广播接收者
        appContext.registerReceiver(callReceiver, callFilter);    
        //注册连接监听
        EMChatManager.getInstance().addConnectionListener(connectionListener);       
        //注册消息事件监听
        initEventListener();
        
    }
    
    protected void onConnectionConflict(){
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constant.ACCOUNT_CONFLICT, true);
        appContext.startActivity(intent);
    }
    
    protected void onCurrentAccountRemoved(){
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constant.ACCOUNT_REMOVED, true);
        appContext.startActivity(intent);
    }
	
	private EaseUser getUserInfo(String username){
	    //获取user信息，demo是从内存的好友列表里获取，
        //实际开发中，可能还需要从服务器获取用户信息,
        //从服务器获取的数据，最好缓存起来，避免频繁的网络请求
        EaseUser user = null;
        if(username.equals(EMChatManager.getInstance().getCurrentUser()))
            return getUserProfileManager().getCurrentUserInfo();
        user = getContactList().get(username);
        //TODO 获取不在好友列表里的群成员具体信息，即陌生人信息，demo未实现
        if(user == null && getRobotList() != null){
            user = getRobotList().get(username);
        }
        return user;
	}
	
	 /**
     * 全局事件监听
     * 因为可能会有UI页面先处理到这个消息，所以一般如果UI页面已经处理，这里就不需要再次处理
     * activityList.size() <= 0 意味着所有页面都已经在后台运行，或者已经离开Activity Stack
     */
    protected void initEventListener() {
        eventListener = new EMEventListener() {
            private BroadcastReceiver broadCastReceiver = null;
            
            @Override
            public void onEvent(EMNotifierEvent event) {
                EMMessage message = null;
                if(event.getData() instanceof EMMessage){
                    message = (EMMessage)event.getData();
                    EMLog.d(TAG, "receive the event : " + event.getEvent() + ",id : " + message.getMsgId());
                }
                
                switch (event.getEvent()) {
                case EventNewMessage:
                    //应用在后台，不需要刷新UI,通知栏提示新消息
                    if(!easeUI.hasForegroundActivies()){
                        getNotifier().onNewMsg(message);
                    }
                    break;
                case EventOfflineMessage:
                    if(!easeUI.hasForegroundActivies()){
                        EMLog.d(TAG, "received offline messages");
                        List<EMMessage> messages = (List<EMMessage>) event.getData();
                        getNotifier().onNewMesg(messages);
                    }
                    break;
                // below is just giving a example to show a cmd toast, the app should not follow this
                // so be careful of this
                case EventNewCMDMessage:
                { 
                    
                    EMLog.d(TAG, "收到透传消息");
                    //获取消息body
                    CmdMessageBody cmdMsgBody = (CmdMessageBody) message.getBody();
                    final String action = cmdMsgBody.action;//获取自定义action
                    
                    //获取扩展属性 此处省略
                    //message.getStringAttribute("");
                    EMLog.d(TAG, String.format("透传消息：action:%s,message:%s", action,message.toString()));
                    final String str = appContext.getString(R.string.receive_the_passthrough);
                    
                    final String CMD_TOAST_BROADCAST = "easemob.demo.cmd.toast";
                    IntentFilter cmdFilter = new IntentFilter(CMD_TOAST_BROADCAST);
                    
                    if(broadCastReceiver == null){
                        broadCastReceiver = new BroadcastReceiver(){

                            @Override
                            public void onReceive(Context context, Intent intent) {
                                // TODO Auto-generated method stub
                                Toast.makeText(appContext, intent.getStringExtra("cmd_value"), Toast.LENGTH_SHORT).show();
                            }
                        };
                        
                      //注册广播接收者
                        appContext.registerReceiver(broadCastReceiver,cmdFilter);
                    }

                    Intent broadcastIntent = new Intent(CMD_TOAST_BROADCAST);
                    broadcastIntent.putExtra("cmd_value", str+action);
                    appContext.sendBroadcast(broadcastIntent, null);
                    
                    break;
                }
                case EventDeliveryAck:
                    message.setDelivered(true);
                    break;
                case EventReadAck:
                    message.setAcked(true);
                    break;
                // add other events in case you are interested in
                default:
                    break;
                }
                
            }
        };
        
        EMChatManager.getInstance().registerEventListener(eventListener);
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
	 *            是否解绑设备token(使用GCM才有)
	 * @param callback
	 *            callback
	 */
	public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
		endCall();
		EMChatManager.getInstance().logout(unbindDeviceToken, new EMCallBack() {

			@Override
			public void onSuccess() {
			    reset();
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
	 * 获取消息通知类
	 * @return
	 */
	public EaseNotifier getNotifier(){
	    return easeUI.getNotifier();
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
    	PreferenceManager.getInstance().setCurrentUserName(username);
    }
    
    /**
     * 获取当前用户的环信id
     */
    public String getCurrentUsernName(){
    	if(username == null){
    		username = PreferenceManager.getInstance().getCurrentUsername();
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

	 /**
     * update user list to cach And db
     *
     * @param contactList
     */
    public void updateContactList(List<EaseUser> contactInfoList) {
         for (EaseUser u : contactInfoList) {
            contactList.put(u.getUsername(), u);
         }
         ArrayList<EaseUser> mList = new ArrayList<EaseUser>();
         mList.addAll(contactList.values());
         DemoDBManager.getInstance().saveContactList(mList);
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
	
	  public void addSyncGroupListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (!syncGroupsListeners.contains(listener)) {
	            syncGroupsListeners.add(listener);
	        }
	    }

	    public void removeSyncGroupListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (syncGroupsListeners.contains(listener)) {
	            syncGroupsListeners.remove(listener);
	        }
	    }

	    public void addSyncContactListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (!syncContactsListeners.contains(listener)) {
	            syncContactsListeners.add(listener);
	        }
	    }

	    public void removeSyncContactListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (syncContactsListeners.contains(listener)) {
	            syncContactsListeners.remove(listener);
	        }
	    }

	    public void addSyncBlackListListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (!syncBlackListListeners.contains(listener)) {
	            syncBlackListListeners.add(listener);
	        }
	    }

	    public void removeSyncBlackListListener(DataSyncListener listener) {
	        if (listener == null) {
	            return;
	        }
	        if (syncBlackListListeners.contains(listener)) {
	            syncBlackListListeners.remove(listener);
	        }
	    }
	
	/**
    * 同步操作，从服务器获取群组列表
    * 该方法会记录更新状态，可以通过isSyncingGroupsFromServer获取是否正在更新
    * 和HXPreferenceUtils.getInstance().getSettingSyncGroupsFinished()获取是否更新已经完成
    * @throws EaseMobException
    */
   public synchronized void asyncFetchGroupsFromServer(final EMCallBack callback){
       if(isSyncingGroupsWithServer){
           return;
       }
       
       isSyncingGroupsWithServer = true;
       
       new Thread(){
           @Override
           public void run(){
               try {
                   EMGroupManager.getInstance().getGroupsFromServer();
                   
                   // in case that logout already before server returns, we should return immediately
                   if(!EMChat.getInstance().isLoggedIn()){
                       return;
                   }
                   
                   PreferenceManager.getInstance().setGroupsSynced(true);
                   
                   isGroupsSyncedWithServer = true;
                   isSyncingGroupsWithServer = false;
                   
                   //通知listener同步群组完毕
                   noitifyGroupSyncListeners(true);
                   if(isContactsSyncedWithServer()){
                       notifyForRecevingEvents();
                   }
                   if(callback != null){
                       callback.onSuccess();
                   }
               } catch (EaseMobException e) {
                   PreferenceManager.getInstance().setGroupsSynced(false);
                   isGroupsSyncedWithServer = false;
                   isSyncingGroupsWithServer = false;
                   noitifyGroupSyncListeners(false);
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
           
           }
       }.start();
   }

   public void noitifyGroupSyncListeners(boolean success){
       for (DataSyncListener listener : syncGroupsListeners) {
           listener.onSyncComplete(success);
       }
   }
   
   public void asyncFetchContactsFromServer(final EMValueCallBack<List<String>> callback){
       if(isSyncingContactsWithServer){
           return;
       }
       
       isSyncingContactsWithServer = true;
       
       new Thread(){
           @Override
           public void run(){
               List<String> usernames = null;
               try {
                   usernames = EMContactManager.getInstance().getContactUserNames();
                   // in case that logout already before server returns, we should return immediately
                   if(!EMChat.getInstance().isLoggedIn()){
                       return;
                   }
                  
                   Map<String, EaseUser> userlist = new HashMap<String, EaseUser>();
                   for (String username : usernames) {
                       EaseUser user = new EaseUser(username);
                       userlist.put(username, user);
                   }
                   // 存入内存
                   DemoHelper.getInstance().getContactList().putAll(userlist);
                    // 存入db
                   UserDao dao = new UserDao(DemoApplication.getInstance().getApplicationContext());
                   List<EaseUser> users = new ArrayList<EaseUser>(userlist.values());
                   dao.saveContactList(users);

                   PreferenceManager.getInstance().setContactSynced(true);
                   
                   isContactsSyncedWithServer = true;
                   isSyncingContactsWithServer = false;
                   
                   //通知listeners联系人同步完毕
                   notifyContactsSyncListener(true);
                   if(isGroupsSyncedWithServer()){
                       notifyForRecevingEvents();
                   }
                   
                   
                   DemoHelper.getInstance().getUserProfileManager().asyncFetchContactInfosFromServer(usernames,new EMValueCallBack<List<EaseUser>>() {

                       @Override
                       public void onSuccess(List<EaseUser> uList) {
                           updateContactList(uList);
                           getUserProfileManager().notifyContactInfosSyncListener(true);
                       }

                       @Override
                       public void onError(int error, String errorMsg) {
                       }
                   });
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (EaseMobException e) {
                   PreferenceManager.getInstance().setContactSynced(false);
                   isContactsSyncedWithServer = false;
                   isSyncingContactsWithServer = false;
                   noitifyGroupSyncListeners(false);
                   e.printStackTrace();
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
               
           }
       }.start();
   }

   public void notifyContactsSyncListener(boolean success){
       for (DataSyncListener listener : syncContactsListeners) {
           listener.onSyncComplete(success);
       }
   }
   
   public void asyncFetchBlackListFromServer(final EMValueCallBack<List<String>> callback){
       
       if(isSyncingBlackListWithServer){
           return;
       }
       
       isSyncingBlackListWithServer = true;
       
       new Thread(){
           @Override
           public void run(){
               try {
                   List<String> usernames = EMContactManager.getInstance().getBlackListUsernamesFromServer();
                   
                   // in case that logout already before server returns, we should return immediately
                   if(!EMChat.getInstance().isLoggedIn()){
                       return;
                   }
                   
                   PreferenceManager.getInstance().setBlacklistSynced(true);
                   
                   isBlackListSyncedWithServer = true;
                   isSyncingBlackListWithServer = false;
                   
                   EMContactManager.getInstance().saveBlackList(usernames);
                   notifyBlackListSyncListener(true);
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (EaseMobException e) {
                   PreferenceManager.getInstance().setBlacklistSynced(false);
                   
                   isBlackListSyncedWithServer = false;
                   isSyncingBlackListWithServer = true;
                   e.printStackTrace();
                   
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }
               
           }
       }.start();
   }
	
	public void notifyBlackListSyncListener(boolean success){
        for (DataSyncListener listener : syncBlackListListeners) {
            listener.onSyncComplete(success);
        }
    }
    
    public boolean isSyncingGroupsWithServer() {
        return isSyncingGroupsWithServer;
    }

    public boolean isSyncingContactsWithServer() {
        return isSyncingContactsWithServer;
    }

    public boolean isSyncingBlackListWithServer() {
        return isSyncingBlackListWithServer;
    }
    
    public boolean isGroupsSyncedWithServer() {
        return isGroupsSyncedWithServer;
    }

    public boolean isContactsSyncedWithServer() {
        return isContactsSyncedWithServer;
    }

    public boolean isBlackListSyncedWithServer() {
        return isBlackListSyncedWithServer;
    }
	
	public synchronized void notifyForRecevingEvents(){
        if(alreadyNotified){
            return;
        }
        
        // 通知sdk，UI 已经初始化完毕，注册了相应的receiver和listener, 可以接受broadcast了
        EMChat.getInstance().setAppInited();
        alreadyNotified = true;
    }
	
    synchronized void reset(){
        isSyncingGroupsWithServer = false;
        isSyncingContactsWithServer = false;
        isSyncingBlackListWithServer = false;
        
        PreferenceManager.getInstance().setGroupsSynced(false);
        PreferenceManager.getInstance().setContactSynced(false);
        PreferenceManager.getInstance().setBlacklistSynced(false);
        
        isGroupsSyncedWithServer = false;
        isContactsSyncedWithServer = false;
        isBlackListSyncedWithServer = false;
        
        alreadyNotified = false;
        
        setContactList(null);
        setRobotList(null);
        getUserProfileManager().reset();
        DemoDBManager.getInstance().closeDB();
    }

    public void pushActivity(Activity activity) {
        easeUI.pushActivity(activity);
    }

    public void popActivity(Activity activity) {
        easeUI.popActivity(activity);
    }
    
    public DemoSettingsModel getSettingsModel(){
        return settingsModel;
    }

}

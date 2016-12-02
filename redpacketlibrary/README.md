# 环信红包集成文档


## 1. redpacketlibrary简介

**redpacketlibrary**，在环信**sdk3.0**的基础上提供了收发红包、转账和零钱的功能。


## 2. redpacketlibrary目录说明

* libs ：包含了集成红包功能所依赖的jar包。(红包使用了glide库做图片加载，由于已经依赖了easeui这里不重复添加)
* res ：包含了红包SDK和聊天页面中的资源文件。（红包SDK相关以rp开头，聊天页面相关以em开头）
* utils ： 封装了收、发红包以及转账的相关方法。
* widget ：聊天界面中的红包、红包回执以及转账的chatrow(如不使用easeui可继承自己的ChatRow基类)。
* **注意: 由于RedPacketUtil类中使用了环信SDK中相关方法，redpacketlibrary依赖了easeui，如不使用easeui可替换掉相关的方法**。

## 3. 集成步骤

###3.1 添加对红包工程的依赖
* ChatDemo的build.gradle中

```java
 dependencies {
    //增加对redpacketlibrary的依赖
    compile project(':redpacketlibrary')
    compile project(':EaseUI')
    compile fileTree(dir: 'libs', include: '*.jar', exclude: 'android-support-multidex.jar')
}
```

* ChatDemo的setting.gradle中


```java

   include ':EaseUI', ':redpacketlibrary'

```
### 3.2 ChatDemo清单文件中注册红包相关组件

```java

        <uses-sdk
            android:minSdkVersion="9"
            android:targetSdkVersion="19"
            tools:overrideLibrary="com.easemob.redpacketui"
        />
        
    <!--红包相关界面start-->
        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPRedPacketActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan|stateVisible"
            />

        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPDetailActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan"
            />

        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPRecordActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan"
            />

        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPWebViewActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustResize|stateHidden"
            />
        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPChangeActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustResize|stateHidden"
            />

        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPBankCardActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan|stateHidden"
            />

        <activity
            android:name="com.easemob.redpacketui.ui.activity.RPGroupMemberActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan|stateHidden"
            />

        <activity
            android:name="com.alipay.sdk.app.H5PayActivity"
            android:configChanges="orientation|keyboardHidden|navigation|screenSize"
            android:exported="false"
            android:screenOrientation="behind"
            android:windowSoftInputMode="adjustResize|stateHidden"
            />
            
       <activity
            android:name="com.easemob.redpacketui.ui.activity.RPTransferActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan|stateVisible"
            />
            
       <activity
            android:name="com.easemob.redpacketui.ui.activity.RPTransferDetailActivity"
            android:screenOrientation="portrait"
            android:theme="@style/horizontal_slide"
            android:windowSoftInputMode="adjustPan|stateHidden"
            />
      <!--红包相关界面end-->
```

### 3.3 初始化红包上下文和token

* DemoApplication中初始化红包上下文。

```java
    import com.easemob.redpacketsdk.RedPacket;
    
    @Override
    public void onCreate() {
        super.onCreate();
        RedPacket.getInstance().initContext(applicationContext);
        //打开Log开关 正式发布时请关闭
        RedPacket.getInstance().setDebugMode(true);
    }
```

### 3.4 ChatFragment中增加收、发红包和转账的功能

* 需要导入的包

```java

   import com.easemob.redpacketsdk.constant.RPConstant;
   import com.easemob.redpacketui.utils.RedPacketUtil;
   import com.easemob.redpacketui.widget.ChatRowRedPacket;
   import com.easemob.redpacketui.widget.ChatRowRedPacketAck;
   import com.easemob.redpacketui.widget.ChatRowTransfer;

```

* 添加红包、转账相关常量


```java

   private static final int MESSAGE_TYPE_RECV_RED_PACKET = 5;
        
   private static final int MESSAGE_TYPE_SEND_RED_PACKET = 6;
        
   private static final int MESSAGE_TYPE_SEND_RED_PACKET_ACK = 7;
        
   private static final int MESSAGE_TYPE_RECV_RED_PACKET_ACK = 8;

   private static final int MESSAGE_TYPE_RECV_TRANSFER_PACKET = 9;

   private static final int MESSAGE_TYPE_SEND_TRANSFER_PACKET = 10;

   private static final int ITEM_RED_PACKET = 16;
        
   private static final int REQUEST_CODE_SEND_RED_PACKET = 16;

   private static final int REQUEST_CODE_SEND_TRANSFER_PACKET = 17;

   private static final int ITEM_TRANSFER_PACKET = 17;

```
* 添加红包、转账入口


```java
    @Override
    protected void registerExtendMenuItem() {
        //demo这里不覆盖基类已经注册的item,item点击listener沿用基类的
        super.registerExtendMenuItem();
        //聊天室暂时不支持红包功能
        if (chatType != Constant.CHATTYPE_CHATROOM) {
            inputMenu.registerExtendMenuItem(R.string.attach_red_packet, R.drawable.em_chat_red_packet_selector, ITEM_RED_PACKET, extendMenuItemClickListener);
        }
        //只在单聊中支持转账功能
        if (chatType == Constant.CHATTYPE_SINGLE) {
            inputMenu.registerExtendMenuItem(R.string.attach_transfer_money, R.drawable.em_chat_transfer_selector, ITEM_TRANSFER_PACKET, extendMenuItemClickListener);
        }
    }
```

* 添加自定义chatrow(红包、红包回执、转账)到CustomChatRowProvider，详见ChatFragment中的CustomChatRowProvider。

* ContextMenuActivity的onCreate(）中屏蔽红包、转账消息的转发和撤回功能。


```java
   if (type == EMMessage.Type.TXT.ordinal()) {
            if(message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false) ||
                    message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)
                    //屏蔽红包、转账消息的转发功能
                    || message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)
                    || message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)){
                setContentView(R.layout.em_context_menu_for_location);
            }
        }
        
        
    if (isChatroom
                //red packet code : 屏蔽红包、转账消息的撤回功能
                || message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)
                || message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
                //end of red packet code
            View v = (View) findViewById(R.id.forward);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        }
```

* 进入发红包、转账页面


```java
    @Override
    public boolean onExtendMenuItemClick(int itemId, View view) {
        switch (itemId) {
        ...
        case ITEM_RED_PACKET://进入红包页面
            //单聊红包修改进入红包的方法，可以在小额随机红包和普通单聊红包之间切换
            RedPacketUtil.startRandomPacket(new RPRedPacketUtil.RPRandomCallback() {
                    @Override
                    public void onSendPacketSuccess(Intent data) {
                        sendMessage(RedPacketUtil.createRPMessage(getActivity(), data, toChatUsername));
                    }

                    @Override
                    public void switchToNormalPacket() {
                        RedPacketUtil.startRedPacketActivityForResult(ChatFragment.this, chatType, toChatUsername, REQUEST_CODE_SEND_RED_PACKET);
                    }
                },getActivity(),toChatUsername);
            } else {
                RedPacketUtil.startRedPacketActivityForResult(this, chatType, toChatUsername, REQUEST_CODE_SEND_RED_PACKET);
            }
            break;
        case ITEM_TRANSFER_PACKET://进入转账页面
            RedPacketUtil.startTransferActivityForResult(this, toChatUsername, REQUEST_CODE_SEND_TRANSFER_PACKET);
            break;
        default:
            break;
        }
        //不覆盖已有的点击事件
        return false;
    }
```

* 发送红包、转账消息到聊天窗口

```java
   @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ...
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
            ...
            case REQUEST_CODE_SEND_RED_PACKET://发送红包消息
                if (data != null){
                    sendMessage(RedPacketUtils.createRPMessage(getActivity(), data, toChatUsername));
                }
                break;
            case REQUEST_CODE_SEND_TRANSFER_PACKET://发送转账消息
                if (data != null) {
                    sendMessage(RedPacketUtil.createTRMessage(getActivity(), data, toChatUsername));
                }
                break;
            default:
                break;
            }
        }     
    }
    
```

* 领取红包并发送回执消息到聊天窗口

```java
    @Override
    public boolean onMessageBubbleClick(EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)){
            if (RedPacketUtil.isRandomRedPacket(message)){
                RedPacketUtil.openRandomPacket(getActivity(),message);
            } else {
                RedPacketUtil.openRedPacket(getActivity(), chatType, message, toChatUsername, messageList);
            }
            return true;
        } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
            RedPacketUtil.openTransferPacket(getActivity(), message);
            return true;
        }
        return false;
    }
```
* ChatFragment中群红包领取回执的处理(聊天页面)

```java
    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
            String action = cmdMsgBody.action();//获取自定义action
            if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)){
                RedPacketUtils.receiveRedPacketAckMessage(message);
                messageList.refresh();
            }
        }
        super.onCmdMessageReceived(messages);
    }
```

* MainActivity中群红包领取回执的处理(导航页面)

```java
    import com.easemob.redpacketsdk.constant.RPConstant;
    import com.easemob.redpacketui.utils.RedPacketUtils;
    
    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
            String action = cmdMsgBody.action();//获取自定义action
            if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION) ){
                RedPacketUtils.receiveRedPacketAckMessage(message);
            }
        }
        refreshUIWithMessage();
    }
```
### 3.5 群红包领取回执的全局处理


* DemoHelper中

```java
    import com.easemob.redpacketsdk.constant.RPConstant;
    import com.easemob.redpacketui.utils.RedPacketUtils;
    
    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            //获取消息body
            EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
            final String action = cmdMsgBody.action();//获取自定义action
            if(!easeUI.hasForegroundActivies()){
                if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)){
                    RedPacketUtils.receiveRedPacketAckMessage(message);
                    broadcastManager.sendBroadcast(new Intent(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION));
                }
            }
        }
    }
```

* MainActivity中

```java
    private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_CONTACT_CHANAGED);
        intentFilter.addAction(Constant.ACTION_GROUP_CHANAGED);
        intentFilter.addAction(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION);
        broadcastReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
            ...
            if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)){
                if (conversationListFragment != null){
                    conversationListFragment.refresh();
                    }
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }
```

### 3.6 ConversationListFragment中对红包回执消息和转账消息的处理

```java
   import com.easemob.redpacketsdk.constant.RPConstant;

   @Override
    protected void setUpView() {
       ...
            conversationListView.setConversationListHelper(new EaseConversationListHelper() {
                    @Override
                    public String onSetItemSecondaryText(EMMessage lastMessage) {
                        if (lastMessage.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, false)) {
                            String sendNick = lastMessage.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, "");
                            String receiveNick = lastMessage.getStringAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, "");
                            String msg;
                            if (lastMessage.direct() == EMMessage.Direct.RECEIVE) {
                                msg = String.format(getResources().getString(R.string.msg_someone_take_red_packet), receiveNick);
                            } else {
                                if (sendNick.equals(receiveNick)) {
                                    msg = getResources().getString(R.string.msg_take_red_packet);
                                } else {
                                    msg = String.format(getResources().getString(R.string.msg_take_someone_red_packet), sendNick);
                                }
                            }
                            return msg;
                        } else if (lastMessage.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
                               String transferAmount = lastMessage.getStringAttribute(RPConstant.EXTRA_TRANSFER_AMOUNT, "");
                               String msg;
                               if (lastMessage.direct() == EMMessage.Direct.RECEIVE) {
                                   msg =  String.format(getResources().getString(R.string.msg_transfer_to_you), transferAmount);
                               } else {
                                   msg =  String.format(getResources().getString(R.string.msg_transfer_from_you),transferAmount);
                               }
                            return msg;
                        }
                        return null;
                    }
                });
                super.setUpView();
    }
```

### 3.7 添加零钱页的入口

* 在需要添加零钱的页面调用下面的方法


```java

    import com.easemob.redpacketui.utils.RedPacketUtils;

    RedPacketUtils.startChangeActivity(getActivity());

```

### 4.拆红包音效

* 在assets目录下添加open_packet_sound.mp3或者open_packet_sound.wav文件即可(文件大小不要超过1M)。

* **提示: 如果不需要红包相关功能可全局搜索关键字red packet去掉红包相关的代码以及redpacketlibray**。











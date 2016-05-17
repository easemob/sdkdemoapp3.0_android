package com.easemob.chatuidemo.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.EMNotifierEvent;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.TextMessageBody;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.domain.EmojiconExampleGroupData;
import com.easemob.chatuidemo.domain.RobotUser;
import com.easemob.chatuidemo.utils.MoneyUtils;
import com.easemob.chatuidemo.widget.ChatRowMoney;
import com.easemob.chatuidemo.widget.ChatRowReceiveMoney;
import com.easemob.chatuidemo.widget.ChatRowVoiceCall;
import com.easemob.easeui.EaseConstant;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.ui.EaseChatFragment;
import com.easemob.easeui.ui.EaseChatFragment.EaseChatFragmentHelper;
import com.easemob.easeui.utils.EaseCommonUtils;
import com.easemob.easeui.utils.EaseUserUtils;
import com.easemob.easeui.widget.chatrow.EaseChatRow;
import com.easemob.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.easemob.easeui.widget.emojicon.EaseEmojiconMenu;
import com.easemob.exceptions.EaseMobException;
import com.easemob.luckymoneysdk.bean.MoneyInfo;
import com.easemob.luckymoneysdk.constant.LMConstant;
import com.easemob.luckymoneyui.ui.activity.LMMoneyActivity;
import com.easemob.luckymoneyui.utils.LMMoneyManager;
import com.easemob.util.PathUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class ChatFragment extends EaseChatFragment implements EaseChatFragmentHelper{

    //避免和基类定义的常量可能发生的冲突，常量从11开始定义
    private static final int ITEM_VIDEO = 11;
    private static final int ITEM_FILE = 12;
    private static final int ITEM_VOICE_CALL = 13;
    private static final int ITEM_VIDEO_CALL = 14;
    private static final int ITEM_READFIRE = 15;
    private static final int ITEM_SEND_MONEY = 16;

    private static final int REQUEST_CODE_SELECT_VIDEO = 11;
    private static final int REQUEST_CODE_SELECT_FILE = 12;
    private static final int REQUEST_CODE_GROUP_DETAIL = 13;
    private static final int REQUEST_CODE_CONTEXT_MENU = 14;
    private static final int REQUEST_CODE_SEND_MONEY = 15;


    private static final int MESSAGE_TYPE_SENT_VOICE_CALL = 1;
    private static final int MESSAGE_TYPE_RECV_VOICE_CALL = 2;
    private static final int MESSAGE_TYPE_SENT_VIDEO_CALL = 3;
    private static final int MESSAGE_TYPE_RECV_VIDEO_CALL = 4;

    private static final int MESSAGE_TYPE_RECV_MONEY = 5;
    private static final int MESSAGE_TYPE_SEND_MONEY = 6;
    private static final int MESSAGE_TYPE_SEND_LUCKY = 7;
    private static final int MESSAGE_TYPE_RECV_LUCKY = 8;


    /**
     * 是否为环信小助手
     */
    private boolean isRobot;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void setUpView() {
        setChatFragmentHelper(this);
        if (chatType == Constant.CHATTYPE_SINGLE) { 
            Map<String,RobotUser> robotMap = DemoHelper.getInstance().getRobotList();
            if(robotMap!=null && robotMap.containsKey(toChatUsername)){
                isRobot = true;
            }
        }
        super.setUpView();
        ((EaseEmojiconMenu) inputMenu.getEmojiconMenu()).addEmojiconGroup(EmojiconExampleGroupData.getData());
        if (chatType == Constant.CHATTYPE_GROUP) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EMGroup returnGroup = EMGroupManager.getInstance().getGroupFromServer(toChatUsername);
                        EMGroupManager.getInstance().createOrUpdateLocalGroup(returnGroup);
                    } catch (EaseMobException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    protected void registerExtendMenuItem() {
        //demo这里不覆盖基类已经注册的item,item点击listener沿用基类的
        super.registerExtendMenuItem();
        //增加扩展item
        inputMenu.registerExtendMenuItem(R.string.attach_video, R.drawable.em_chat_video_selector, ITEM_VIDEO, extendMenuItemClickListener);
        inputMenu.registerExtendMenuItem(R.string.attach_file, R.drawable.em_chat_file_selector, ITEM_FILE, extendMenuItemClickListener);
        if(chatType == Constant.CHATTYPE_SINGLE){
            inputMenu.registerExtendMenuItem(R.string.attach_voice_call, R.drawable.em_chat_voice_call_selector, ITEM_VOICE_CALL, extendMenuItemClickListener);
            inputMenu.registerExtendMenuItem(R.string.attach_video_call, R.drawable.em_chat_video_call_selector, ITEM_VIDEO_CALL, extendMenuItemClickListener);
            // 阅后即焚开关菜单
            inputMenu.registerExtendMenuItem(R.string.attach_read_fire, R.drawable.ease_read_fire, ITEM_READFIRE, extendMenuItemClickListener);
        }
        //暂时不支持聊天室
        if (chatType != Constant.CHATTYPE_CHATROOM){
            inputMenu.registerExtendMenuItem(R.string.attach_red_packet, R.drawable.em_chat_money_selector, ITEM_SEND_MONEY, extendMenuItemClickListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("lzan13", "ChatFragment - onActivityResult - "+requestCode);
        if (requestCode == REQUEST_CODE_CONTEXT_MENU) {
            switch (resultCode) {
            case ContextMenuActivity.RESULT_CODE_COPY: // 复制消息
                clipboard.setText(((TextMessageBody) contextMenuMessage.getBody()).getMessage());
                break;
            case ContextMenuActivity.RESULT_CODE_DELETE: // 删除消息
                conversation.removeMessage(contextMenuMessage.getMsgId());
                refreshUI();
                break;

            case ContextMenuActivity.RESULT_CODE_FORWARD: // 转发消息
                if(chatType == EaseConstant.CHATTYPE_CHATROOM){
                    Toast.makeText(getActivity(), R.string.chatroom_not_support_forward, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(getActivity(), ForwardMessageActivity.class);
                intent.putExtra("forward_msg_id", contextMenuMessage.getMsgId());
                startActivity(intent);

                break;
            case ContextMenuActivity.RESULT_CODE_REVOKE:
            	// 显示撤回消息操作的 dialog
                final ProgressDialog pd = new ProgressDialog(getActivity());
                pd.setMessage(getString(R.string.revoking));
                pd.show();
                EaseCommonUtils.sendRevokeMessage(getActivity(), contextMenuMessage, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        pd.dismiss();
                        refreshUI();
                    }

                    @Override
                    public void onError(final int i, final String s) {
                        pd.dismiss();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (s.equals("maxtime")) {
                                    Toast.makeText(getActivity(), R.string.revoke_error_maxtime, Toast.LENGTH_LONG).show();
                                } else {
                                	Toast.makeText(getActivity(), getString(R.string.revoke_error) + i + " " + s + "", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
            	break;
            default:
                break;
            }
        }
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
                case REQUEST_CODE_SELECT_VIDEO: //发送选中的视频
                    if (data != null) {
                        int duration = data.getIntExtra("dur", 0);
                        String videoPath = data.getStringExtra("path");
                        File file = new File(PathUtil.getInstance().getImagePath(), "thvideo" + System.currentTimeMillis());
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            Bitmap ThumbBitmap = ThumbnailUtils.createVideoThumbnail(videoPath, 3);
                            ThumbBitmap.compress(CompressFormat.JPEG, 100, fos);
                            fos.close();
                            sendVideoMessage(videoPath, file.getAbsolutePath(), duration);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case REQUEST_CODE_SELECT_FILE: //发送选中的文件
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            sendFileByUri(uri);
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_MONEY://发送红包消息
                    if (data != null) {
                        String greetings = data.getStringExtra(LMConstant.EXTRA_MONEY_GREETING);
                        String moneyID = data.getStringExtra(LMConstant.EXTRA_CHECK_MONEY_ID);
                        EMMessage message = EMMessage.createTxtSendMessage("["+getResources().getString(R.string.hunxin_luckymoney)+"]"+greetings, toChatUsername);
                        message.setAttribute(LMConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, true);
                        message.setAttribute(LMConstant.EXTRA_SPONSOR_NAME, getResources().getString(R.string.hunxin_luckymoney));
                        message.setAttribute(LMConstant.EXTRA_MONEY_GREETING, greetings);
                        message.setAttribute(LMConstant.EXTRA_CHECK_MONEY_ID, moneyID);
                        sendMessage(message);
                    }
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    public void onEvent(EMNotifierEvent event) {
        switch (event.getEvent()) {
            case EventNewCMDMessage:
                EMMessage cmdMessage = (EMMessage) event.getData();
                CmdMessageBody cmdMsgBody = (CmdMessageBody) cmdMessage.getBody();
                final String action = cmdMsgBody.action;//获取自定义action
                if (action.equals(Constant.REFRESH_GROUP_MONEY_ACTION) && cmdMessage.getChatType() == EMMessage.ChatType.GroupChat) {
                    MoneyUtils.receiveMoneyAckMessage(cmdMessage);
                    messageList.refresh();
                }
                break;
        }
        super.onEvent(event);
    }

    /**
     * 刷新UI界面
     */
    public void refreshUI(){
        messageList.refresh();
    }

    /**
     * 为消息对象添加扩展字段
     * params message   需要处理的消息对象
     */
    @Override
    public void onSetMessageAttributes(EMMessage message) {
        if(isRobot){
            //设置消息扩展属性
            message.setAttribute("em_robot_message", isRobot);
        }
        // 根据当前状态是否是阅后即焚状态来设置发送消息的扩展
        if(isReadFire && (message.getType() == EMMessage.Type.TXT
                || message.getType() == EMMessage.Type.IMAGE
                || message.getType() == EMMessage.Type.VOICE)){
            message.setAttribute(EaseConstant.EASE_ATTR_READFIRE, true);
        }

    }

    @Override
    public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
        //设置自定义listview item提供者
        return new CustomChatRowProvider();
    }

    @Override
    public void onEnterToChatDetails() {
        if (chatType == Constant.CHATTYPE_GROUP) {
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            if (group == null) {
                Toast.makeText(getActivity(), R.string.gorup_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(
                    (new Intent(getActivity(), GroupDetailsActivity.class).putExtra("groupId", toChatUsername)),
                    REQUEST_CODE_GROUP_DETAIL);
        }else if(chatType == Constant.CHATTYPE_CHATROOM){
        	startActivityForResult(new Intent(getActivity(), ChatRoomDetailsActivity.class).putExtra("roomId", toChatUsername), REQUEST_CODE_GROUP_DETAIL);
        }
    }

    @Override
    public void onAvatarClick(String username) {
        //头像点击事件
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    @Override
    public boolean onMessageBubbleClick(final EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, false)) {
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
            String moneyId = "";
            String messageDirect;
            //接收者头像url 默认值为none
            String toAvatarUrl = "none";//测试用图片url:http://i.imgur.com/DvpvklR.png
            //接收者昵称 默认值为当前用户ID
            String toNickname = EMChatManager.getInstance().getCurrentUser();
            int cType;
            try {
                moneyId = message.getStringAttribute(LMConstant.EXTRA_CHECK_MONEY_ID);
            } catch (EaseMobException e) {
                e.printStackTrace();
            }
            if (message.direct == EMMessage.Direct.SEND) {
                messageDirect = LMConstant.MESSAGE_DIRECT_SEND;
            } else {
                messageDirect = LMConstant.MESSAGE_DIRECT_RECEIVE;
            }
            if (chatType == Constant.CHATTYPE_SINGLE) {
                cType = LMConstant.CHATTYPE_SINGLE;
            } else {
                cType = LMConstant.CHATTYPE_GROUP;
            }
            EaseUser easeUser = EaseUserUtils.getUserInfo(EMChatManager.getInstance().getCurrentUser());
            if (easeUser != null) {
                toAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
                toNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
            }
            MoneyInfo moneyInfo = new MoneyInfo();
            moneyInfo.moneyID = moneyId;
            moneyInfo.toAvatarUrl = toAvatarUrl;
            moneyInfo.toNickName = toNickname;
            moneyInfo.moneyMsgDirect = messageDirect;
            moneyInfo.chatType = cType;
            LMMoneyManager.getInstance().openMoney(moneyInfo, getActivity(), new LMMoneyManager.LMReceiveMoneyCallBack() {
                @Override
                public void onSuccess(String senderId, String senderNickname) {
                    //领取红包成功 发送消息到聊天窗口
                    String receiverId = EMChatManager.getInstance().getCurrentUser();
                    //设置默认值为id
                    String receiverNickname = receiverId;
                    EaseUser receiverUser = EaseUserUtils.getUserInfo(receiverId);
                    if (receiverUser != null) {
                        receiverNickname = TextUtils.isEmpty(receiverUser.getNick()) ? receiverUser.getUsername() : receiverUser.getNick();
                    }
                    if (chatType == Constant.CHATTYPE_SINGLE) {
                        EMMessage msg = EMMessage.createTxtSendMessage(String.format(getResources().getString(R.string.money_msg_someone_take_money),receiverNickname), toChatUsername);
                        msg.setAttribute(LMConstant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, true);
                        msg.setAttribute(LMConstant.EXTRA_LUCKY_MONEY_RECEIVER, receiverNickname);
                        msg.setAttribute(LMConstant.EXTRA_LUCKY_MONEY_SENDER, senderNickname);
                        sendMessage(msg);
                    } else {
                        MoneyUtils.sendMoneyAckMessage(message, senderId, senderNickname, receiverId, receiverNickname, new EMCallBack() {
                            @Override
                            public void onSuccess() {
                                messageList.refresh();
                            }

                            @Override
                            public void onError(int i, String s) {

                            }

                            @Override
                            public void onProgress(int i, String s) {

                            }
                        });
                    }
                }

                @Override
                public void showLoading() {
                    progressDialog.show();
                }

                @Override
                public void hideLoading() {
                    progressDialog.dismiss();
                }

                @Override
                public void onError(String code, String message) {
                    //错误处理
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onMessageBubbleLongClick(EMMessage message) {
        //消息框长按
        startActivityForResult((new Intent(getActivity(), ContextMenuActivity.class)).putExtra("message",message),
                REQUEST_CODE_CONTEXT_MENU);
    }

    @Override
    public boolean onExtendMenuItemClick(int itemId, View view) {
        switch (itemId) {
            case ITEM_VIDEO: //视频
                Intent intent = new Intent(getActivity(), ImageGridActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
                break;
            case ITEM_FILE: //一般文件
                //demo这里是通过系统api选择文件，实际app中最好是做成qq那种选择发送文件
                selectFileFromLocal();
                break;
            case ITEM_VOICE_CALL: //音频通话
                startVoiceCall();
                break;
            case ITEM_VIDEO_CALL: //视频通话
                startVideoCall();
                break;
            case ITEM_READFIRE:
                setReadFire(true);
                break;
            case ITEM_SEND_MONEY://发送红包
                sendMoney();
                break;
            default:
                break;
        }
        //不覆盖已有的点击事件
        return false;
    }
    /**
     * 选择文件
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) { //19以后这个api不可用，demo这里简单处理成图库选择图片
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    /**
     * 拨打语音电话
     */
    protected void startVoiceCall() {
        if (!EMChatManager.getInstance().isConnected()) {
            Toast.makeText(getActivity(), R.string.not_connect_to_server, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(getActivity(), VoiceCallActivity.class).putExtra("username", toChatUsername)
                    .putExtra("isComingCall", false));
            // voiceCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    /**
     * 拨打视频电话
     */
    protected void startVideoCall() {
        if (!EMChatManager.getInstance().isConnected())
            Toast.makeText(getActivity(), R.string.not_connect_to_server, Toast.LENGTH_SHORT).show();
        else {
            startActivity(new Intent(getActivity(), VideoCallActivity.class).putExtra("username", toChatUsername)
                    .putExtra("isComingCall", false));
            // videoCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    /**
     * 进入红包页面
     */
    protected void sendMoney() {
        Intent intent = new Intent(getActivity(), LMMoneyActivity.class);
        //发送者头像url
        String fromAvatarUrl = "";
        //发送者昵称 设置了昵称就传昵称 否则传id
        String fromNickname = "";
        EaseUser easeUser = EaseUserUtils.getUserInfo(EMChatManager.getInstance().getCurrentUser());
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        MoneyInfo moneyInfo = new MoneyInfo();
        moneyInfo.fromAvatarUrl = fromAvatarUrl;
        moneyInfo.fromNickName = fromNickname;
        //接收者Id或者接收的群Id
        if (chatType == Constant.CHATTYPE_SINGLE) {
            moneyInfo.toUserId = toChatUsername;
            moneyInfo.chatType = LMConstant.CHATTYPE_SINGLE;
        } else if (chatType == Constant.CHATTYPE_GROUP) {
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            moneyInfo.toGroupId = group.getGroupId();
            moneyInfo.groupMemberCount = group.getAffiliationsCount();
            moneyInfo.chatType = LMConstant.CHATTYPE_GROUP;
        }
        intent.putExtra(LMConstant.EXTRA_MONEY_INFO, moneyInfo);
        startActivityForResult(intent, REQUEST_CODE_SEND_MONEY);
    }

    /**
     * chat row provider
     */
    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override
        public int getCustomChatRowTypeCount() {
            //红包、音、视频通话发送、接收共8种
            return 8;
        }

        @Override
        public int getCustomChatRowType(EMMessage message) {
            if(message.getType() == EMMessage.Type.TXT){
                //语音通话类型
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)){
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VOICE_CALL : MESSAGE_TYPE_SENT_VOICE_CALL;
                }else if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)){
                    //视频通话
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VIDEO_CALL : MESSAGE_TYPE_SENT_VIDEO_CALL;
                } else if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, false)) {
                    //发送红包消息
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_MONEY : MESSAGE_TYPE_SEND_MONEY;
                } else if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, false)) {
                    //领取红包消息
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_LUCKY : MESSAGE_TYPE_SEND_LUCKY;
                }
            }
            return 0;
        }

        @Override
        public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            if(message.getType() == EMMessage.Type.TXT){
                // 语音通话,  视频通话
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false) ||
                    message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)){
                    return new ChatRowVoiceCall(getActivity(), message, position, adapter);
                }else if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, false)) {//发送红包消息
                    return new ChatRowMoney(getActivity(), message, position, adapter);
                } else if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, false)) {//领取红包消息
                    return new ChatRowReceiveMoney(getActivity(), message, position, adapter);
                }
            }
            return null;
        }
    }

}

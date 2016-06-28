package com.easemob.redpacketui.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMCmdMessageBody;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.easeui.EaseConstant;
import com.easemob.easeui.domain.EaseUser;
import com.easemob.easeui.utils.EaseUserUtils;
import com.easemob.easeui.widget.EaseChatMessageList;
import com.easemob.exceptions.EaseMobException;
import com.easemob.redpacketsdk.bean.RPUserBean;
import com.easemob.redpacketsdk.bean.RedPacketInfo;
import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.R;
import com.easemob.redpacketui.RedPacketConstant;
import com.easemob.redpacketui.callback.GroupMemberCallback;
import com.easemob.redpacketui.callback.NotifyGroupMemberCallback;
import com.easemob.redpacketui.ui.activity.RPChangeActivity;
import com.easemob.redpacketui.ui.activity.RPRedPacketActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by max on 16/5/24.
 */
public class RedPacketUtil {

    /**
     * 进入发红包页面
     *
     * @param fragment
     * @param chatType
     * @param toChatUsername
     * @param requestCode
     */
    public static void startRedPacketActivityForResult(Fragment fragment, int chatType, final String toChatUsername, int requestCode) {
        //发送者头像url
        String fromAvatarUrl = "none";
        //发送者昵称 设置了昵称就传昵称 否则传id
        String fromNickname = EMChatManager.getInstance().getCurrentUser();
        EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.fromNickName = fromNickname;
        redPacketInfo.imUserId = EMChatManager.getInstance().getCurrentUser();
        //此处使用环信id代替了appUserId 开发者可传入App的appUserId
        redPacketInfo.appUserId = EMChatManager.getInstance().getCurrentUser();
        redPacketInfo.imToken = EMChatManager.getInstance().getAccessToken();
        //接收者Id或者接收的群Id
        if (chatType == EaseConstant.CHATTYPE_SINGLE) {
            redPacketInfo.toUserId = toChatUsername;
            redPacketInfo.chatType = RPConstant.CHATTYPE_SINGLE;
        } else if (chatType == EaseConstant.CHATTYPE_GROUP) {
            //拉取最新群组数据
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
            RPGroupMemberUtil.getInstance().setGroupMemberListener(new NotifyGroupMemberCallback() {
                @Override
                public void getGroupMember(final String groupID, final GroupMemberCallback mCallBack) {
                    EMGroup group = EMGroupManager.getInstance().getGroup(groupID);
                    List<String> members = group.getMembers();
                    List<RPUserBean> userBeanList = new ArrayList<RPUserBean>();
                    EaseUser user;
                    for (int i = 0; i < members.size(); i++) {
                        RPUserBean userBean = new RPUserBean();
                        userBean.userId = members.get(i);
                        if (userBean.userId.equals(EMChatManager.getInstance().getCurrentUser())) {
                            continue;
                        }
                        user = EaseUserUtils.getUserInfo(userBean.userId);
                        if (user != null) {
                            userBean.userAvatar = TextUtils.isEmpty(user.getAvatar()) ? "none" : user.getAvatar();
                            userBean.userNickname = TextUtils.isEmpty(user.getNick()) ? user.getUsername() : user.getNick();
                        } else {
                            userBean.userNickname = userBean.userId;
                            userBean.userAvatar = "none";
                        }
                        userBeanList.add(userBean);
                    }
                    mCallBack.setGroupMember(userBeanList);
                }
            });
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            redPacketInfo.toGroupId = group.getGroupId();
            redPacketInfo.groupMemberCount = group.getAffiliationsCount();
            redPacketInfo.chatType = RPConstant.CHATTYPE_GROUP;
        }
        Intent intent = new Intent(fragment.getContext(), RPRedPacketActivity.class);
        intent.putExtra(RPConstant.EXTRA_MONEY_INFO, redPacketInfo);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 创建一条红包消息
     *
     * @param context        上下文
     * @param data           intent
     * @param toChatUsername 消息接收者id
     * @return
     */
    public static EMMessage createRPMessage(Context context, Intent data, String toChatUsername) {
        String greetings = data.getStringExtra(RedPacketConstant.EXTRA_RED_PACKET_GREETING);
        String moneyID = data.getStringExtra(RedPacketConstant.EXTRA_RED_PACKET_ID);
        String specialReceiveId = data.getStringExtra(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_ID);
        String redPacketType = data.getStringExtra(RedPacketConstant.EXTRA_RED_PACKET_TYPE);
        EMMessage message = EMMessage.createTxtSendMessage("[" + context.getResources().getString(R.string.easemob_red_packet) + "]" + greetings, toChatUsername);
        message.setAttribute(RedPacketConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, true);
        message.setAttribute(RedPacketConstant.EXTRA_SPONSOR_NAME, context.getResources().getString(R.string.easemob_red_packet));
        message.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_GREETING, greetings);
        message.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_ID, moneyID);
        message.setAttribute(RedPacketConstant.MESSAGE_ATTR_RED_PACKET_TYPE, redPacketType);
        message.setAttribute(RedPacketConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, specialReceiveId);
        return message;
    }

    /**
     * 拆红包的方法
     *
     * @param activity       FragmentActivity
     * @param chatType       聊天类型
     * @param message        EMMessage
     * @param toChatUsername 消息接收者id
     * @param messageList
     * @return
     */
    public static void openRedPacket(final FragmentActivity activity, final int chatType, final EMMessage message, final String toChatUsername, final EaseChatMessageList messageList) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setCanceledOnTouchOutside(false);
        String messageDirect;
        //接收者头像url 默认值为none
        String toAvatarUrl = "none";//测试用图片url:http://i.imgur.com/DvpvklR.png
        //接收者昵称 默认值为当前用户ID
        String toNickname = EMChatManager.getInstance().getCurrentUser();
        String currentUserId = toNickname;
        String moneyId = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_ID, "");
        if (message.direct == EMMessage.Direct.SEND) {
            messageDirect = RPConstant.MESSAGE_DIRECT_SEND;
        } else {
            messageDirect = RPConstant.MESSAGE_DIRECT_RECEIVE;
        }
        EaseUser easeUser = EaseUserUtils.getUserInfo(EMChatManager.getInstance().getCurrentUser());
        if (easeUser != null) {
            toAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            toNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        String specialAvatarUrl = "none";
        String specialNickname = "";
        String packetType;
        packetType = message.getStringAttribute(RedPacketConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        String specialReceiveId = message.getStringAttribute(RedPacketConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, "");
        if (!TextUtils.isEmpty(packetType) && packetType.equals(RedPacketConstant.GROUP_RED_PACKET_TYPE_EXCLUSIVE)) {
            EaseUser userInfo = EaseUserUtils.getUserInfo(specialReceiveId);
            if (userInfo != null) {
                specialAvatarUrl = TextUtils.isEmpty(userInfo.getAvatar()) ? "none" : userInfo.getAvatar();
                specialNickname = TextUtils.isEmpty(userInfo.getNick()) ? userInfo.getUsername() : userInfo.getNick();
            } else {
                specialNickname = specialReceiveId;
            }
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.moneyID = moneyId;
        redPacketInfo.toAvatarUrl = toAvatarUrl;
        redPacketInfo.toNickName = toNickname;
        redPacketInfo.moneyMsgDirect = messageDirect;
        redPacketInfo.toUserId = currentUserId;
        redPacketInfo.chatType = chatType;
        redPacketInfo.imUserId = currentUserId;
        //此处使用环信id代替了appUserId 开发者可传入App的appUserId
        redPacketInfo.appUserId = currentUserId;
        redPacketInfo.imToken = EMChatManager.getInstance().getAccessToken();
        if (packetType.equals(RedPacketConstant.GROUP_RED_PACKET_TYPE_EXCLUSIVE)) {
            redPacketInfo.specialAvatarUrl = specialAvatarUrl;
            redPacketInfo.specialNickname = specialNickname;
        }
        RPOpenPacketUtil.getInstance().openRedPacket(redPacketInfo, activity, new RPOpenPacketUtil.RPOpenPacketCallBack() {
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
                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                    EMMessage msg = EMMessage.createTxtSendMessage(String.format(activity.getResources().getString(com.easemob.redpacketui.R.string.money_msg_someone_take_money), receiverNickname), toChatUsername);
                    msg.setAttribute(RedPacketConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                    msg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                    msg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                    try {
                        EMChatManager.getInstance().sendMessage(msg);
                    } catch (EaseMobException e) {
                        e.printStackTrace();
                    }
                } else {
                    sendRedPacketAckMessage(message, senderId, senderNickname, receiverId, receiverNickname, new EMCallBack() {
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
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 进入零钱页面
     *
     * @param context 上下文
     */
    public static void startChangeActivity(Context context) {
        Intent intent = new Intent(context, RPChangeActivity.class);
        String fromNickname = EMChatManager.getInstance().getCurrentUser();
        String fromAvatarUrl = "none";
        EaseUser easeUser = EaseUserUtils.getUserInfo(EMChatManager.getInstance().getCurrentUser());
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromNickName = fromNickname;
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.imUserId = EMChatManager.getInstance().getCurrentUser();
        //此处使用环信id代替了appUserId 开发者可传入App的appUserId
        redPacketInfo.appUserId = EMChatManager.getInstance().getCurrentUser();
        redPacketInfo.imToken = EMChatManager.getInstance().getAccessToken();
        intent.putExtra(RPConstant.EXTRA_MONEY_INFO, redPacketInfo);
        context.startActivity(intent);
    }


    /**
     * 使用cmd消息发送领到红包之后的回执消息
     */
    private static void sendRedPacketAckMessage(final EMMessage message, final String senderId, final String senderNickname, String receiverId, final String receiverNickname, final EMCallBack callBack) {
        //创建透传消息
        final EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
        cmdMsg.setChatType(EMMessage.ChatType.Chat);
        EMCmdMessageBody cmdBody = new EMCmdMessageBody(RedPacketConstant.REFRESH_GROUP_RED_PACKET_ACTION);
        cmdMsg.addBody(cmdBody);
        cmdMsg.setReceipt(senderId);
        //设置扩展属性
        cmdMsg.setAttribute(RedPacketConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
        cmdMsg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
        cmdMsg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
        cmdMsg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
        cmdMsg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_ID, receiverId);
        cmdMsg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_GROUP_ID, message.getTo());
        cmdMsg.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                //保存消息到本地
                EMMessage sendMessage = EMMessage.createTxtSendMessage("content", message.getTo());
                sendMessage.setChatType(EMMessage.ChatType.GroupChat);
                sendMessage.setFrom(message.getFrom());
                sendMessage.setTo(message.getTo());
                sendMessage.setMsgId(UUID.randomUUID().toString());
                sendMessage.setMsgTime(cmdMsg.getMsgTime());
                sendMessage.setUnread(false);//去掉未读的显示
                sendMessage.direct = EMMessage.Direct.SEND;
                sendMessage.setAttribute(RedPacketConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                sendMessage.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                sendMessage.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                sendMessage.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
                EMChatManager.getInstance().saveMessage(sendMessage);
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {

            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
        try {
            EMChatManager.getInstance().sendMessage(cmdMsg);
        } catch (EaseMobException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用cmd消息收取领到红包之后的回执消息
     */
    public static void receiveRedPacketAckMessage(EMMessage message) {
        String senderNickname = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_NAME, "");
        String receiverNickname = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_NAME, "");
        String senderId = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_ID, "");
        String receiverId = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_ID, "");
        String groupId = message.getStringAttribute(RedPacketConstant.EXTRA_RED_PACKET_GROUP_ID, "");
        String currentUser = EMChatManager.getInstance().getCurrentUser();
        //更新UI为 xx领取了你的红包
        if (currentUser.equals(senderId) && !receiverId.equals(senderId)) {//如果不是自己领取的红包更新此类消息UI
            EMMessage msg = EMMessage.createTxtSendMessage("content", groupId);
            msg.setChatType(EMMessage.ChatType.GroupChat);
            msg.setFrom(message.getFrom());
            if (TextUtils.isEmpty(groupId)) {
                msg.setTo(message.getTo());
            } else {
                msg.setTo(groupId);
            }
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setMsgTime(message.getMsgTime());
            msg.direct = EMMessage.Direct.RECEIVE;
            msg.setUnread(false);//去掉未读的显示
            msg.setAttribute(RedPacketConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
            msg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
            msg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
            msg.setAttribute(RedPacketConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
            //保存消息
            EMChatManager.getInstance().saveMessage(msg);
        }
    }
}

package com.easemob.redpacket.utils;

import android.app.ProgressDialog;
import android.content.Context;
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
import com.easemob.redpacket.R;
import com.easemob.redpacketsdk.RPGroupMemberListener;
import com.easemob.redpacketsdk.RPSendPacketCallback;
import com.easemob.redpacketsdk.RPValueCallback;
import com.easemob.redpacketsdk.RedPacket;
import com.easemob.redpacketsdk.bean.RPUserBean;
import com.easemob.redpacketsdk.bean.RedPacketInfo;
import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.utils.RPRedPacketUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedPacketUtil {


    /**
     * 进入红包、转账页面的相关方法
     *
     * @param activity       FragmentActivity
     * @param itemType       转账、单聊红包、群聊红包、小额随机红包
     * @param toChatUsername 接收者id或者群id
     * @param callback       RPSendPacketCallback
     */
    public static void startRedPacket(final FragmentActivity activity, int itemType, final String toChatUsername, RPSendPacketCallback callback) {
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        if (itemType == RPConstant.RP_ITEM_TYPE_GROUP) {
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
            RedPacket.getInstance().setRPGroupMemberListener(new RPGroupMemberListener() {
                @Override
                public void getGroupMember(String groupId, RPValueCallback<List<RPUserBean>> callback) {
                    EMGroup group = EMGroupManager.getInstance().getGroup(groupId);
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
                    callback.onSuccess(userBeanList);
                }
            });
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            redPacketInfo.toGroupId = group.getGroupId();
            redPacketInfo.groupMemberCount = group.getAffiliationsCount();
        } else {
            EaseUser easeToUser = EaseUserUtils.getUserInfo(toChatUsername);
            String toAvatarUrl = "none";
            String toUserName = "";
            if (easeToUser != null) {
                toAvatarUrl = TextUtils.isEmpty(easeToUser.getAvatar()) ? "none" : easeToUser.getAvatar();
                toUserName = TextUtils.isEmpty(easeToUser.getNick()) ? easeToUser.getUsername() : easeToUser.getNick();
            }
            redPacketInfo.toUserId = toChatUsername;
            redPacketInfo.toAvatarUrl = toAvatarUrl;
            redPacketInfo.toNickName = toUserName;
        }
        RPRedPacketUtil.getInstance().startRedPacket(activity, itemType, redPacketInfo, callback);
    }

    /**
     * 拆红包的方法
     *
     * @param activity       FragmentActivity
     * @param chatType       聊天类型
     * @param message        EMMessage
     * @param toChatUsername 消息接收者id
     * @param messageList    EaseChatMessageList
     */
    public static void openRedPacket(final FragmentActivity activity, final int chatType, final EMMessage message, final String toChatUsername, final EaseChatMessageList messageList) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setCanceledOnTouchOutside(false);
        RPRedPacketUtil.getInstance().openRedPacket(wrapperRedPacketInfo(chatType, message), activity, new RPRedPacketUtil.RPOpenPacketCallback() {
            @Override
            public void onSuccess(String senderId, String senderNickname, String myAmount) {
                //领取红包成功 发送消息到聊天窗口
                String receiverId = EMChatManager.getInstance().getCurrentUser();
                //设置默认值为id
                String receiverNickname = receiverId;
                EaseUser receiverUser = EaseUserUtils.getUserInfo(receiverId);
                if (receiverUser != null) {
                    receiverNickname = TextUtils.isEmpty(receiverUser.getNick()) ? receiverUser.getUsername() : receiverUser.getNick();
                }
                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                    if (!isRandomRedPacket(message)) {
                        EMMessage msg = EMMessage.createTxtSendMessage(String.format(activity.getResources().getString(R.string.msg_someone_take_red_packet), receiverNickname), toChatUsername);
                        msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                        msg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                        msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                        try {
                            EMChatManager.getInstance().sendMessage(msg);
                        } catch (EaseMobException e) {
                            e.printStackTrace();
                        }
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
     * 创建红包消息
     *
     * @param context        上下文
     * @param redPacketInfo  RedPacketInfo
     * @param toChatUsername 接收者id
     * @return EMMessage
     */
    public static EMMessage createRPMessage(Context context, RedPacketInfo redPacketInfo, String toChatUsername) {
        EMMessage message = EMMessage.createTxtSendMessage("[" + context.getResources().getString(R.string.easemob_red_packet) + "]" + redPacketInfo.redPacketGreeting, toChatUsername);
        message.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, true);
        message.setAttribute(RPConstant.EXTRA_SPONSOR_NAME, context.getResources().getString(R.string.easemob_red_packet));
        message.setAttribute(RPConstant.EXTRA_RED_PACKET_GREETING, redPacketInfo.redPacketGreeting);
        message.setAttribute(RPConstant.EXTRA_RED_PACKET_ID, redPacketInfo.redPacketId);
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, redPacketInfo.redPacketType);
        message.setAttribute(RPConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, redPacketInfo.toUserId);
        return message;
    }

    /**
     * 封装拆红包所需参数
     *
     * @param chatType 聊天类型
     * @param message  EMMessage
     * @return RedPacketInfo
     */
    private static RedPacketInfo wrapperRedPacketInfo(int chatType, EMMessage message) {
        String redPacketId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_ID, "");
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.redPacketId = redPacketId;
        redPacketInfo.messageDirect = getMessageDirect(message);
        redPacketInfo.chatType = chatType;
        //3.4.0版之前集成过红包的用户，需要增加如下参数的传入对旧版本进行兼容
        String specialReceiveId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, "");
        if (!TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.GROUP_RED_PACKET_TYPE_EXCLUSIVE)) {
            EaseUser userInfo = EaseUserUtils.getUserInfo(specialReceiveId);
            String specialAvatarUrl = null;
            String specialNickname;
            if (userInfo != null) {
                specialAvatarUrl = TextUtils.isEmpty(userInfo.getAvatar()) ? "none" : userInfo.getAvatar();
                specialNickname = TextUtils.isEmpty(userInfo.getNick()) ? userInfo.getUsername() : userInfo.getNick();
            } else {
                specialNickname = specialReceiveId;
            }
            redPacketInfo.specialAvatarUrl = specialAvatarUrl;
            redPacketInfo.specialNickname = specialNickname;
        }
        //兼容end
        return redPacketInfo;
    }

    private static String getMessageDirect(EMMessage message) {
        String messageDirect;
        if (message.direct == EMMessage.Direct.SEND) {
            messageDirect = RPConstant.MESSAGE_DIRECT_SEND;
        } else {
            messageDirect = RPConstant.MESSAGE_DIRECT_RECEIVE;
        }
        return messageDirect;
    }


    /**
     * 使用cmd消息发送领到红包之后的回执消息
     */
    private static void sendRedPacketAckMessage(final EMMessage message, final String senderId, final String senderNickname, String receiverId, final String receiverNickname, final EMCallBack callBack) {
        //创建透传消息
        final EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
        cmdMsg.setChatType(EMMessage.ChatType.Chat);
        EMCmdMessageBody cmdBody = new EMCmdMessageBody(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION);
        cmdMsg.addBody(cmdBody);
        cmdMsg.setReceipt(senderId);
        //设置扩展属性
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
        cmdMsg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
        cmdMsg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
        cmdMsg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
        cmdMsg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_ID, receiverId);
        cmdMsg.setAttribute(RPConstant.EXTRA_RED_PACKET_GROUP_ID, message.getTo());
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
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
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
        String senderNickname = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, "");
        String receiverNickname = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, "");
        String senderId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, "");
        String receiverId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_ID, "");
        String groupId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_GROUP_ID, "");
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
            msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
            //保存消息
            EMChatManager.getInstance().saveMessage(msg);
        }
    }

    /**
     * 判断红包类型是否为小额随机红包
     *
     * @param message EMMessage
     * @return true or false
     */
    public static boolean isRandomRedPacket(EMMessage message) {
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        return !TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.RED_PACKET_TYPE_RANDOM);
    }

}

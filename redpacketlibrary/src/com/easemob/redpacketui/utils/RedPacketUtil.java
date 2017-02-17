package com.easemob.redpacketui.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.easemob.redpacketsdk.RPGroupMemberListener;
import com.easemob.redpacketsdk.RPValueCallback;
import com.easemob.redpacketsdk.RedPacket;
import com.easemob.redpacketsdk.bean.RPUserBean;
import com.easemob.redpacketsdk.bean.RedPacketInfo;
import com.easemob.redpacketsdk.bean.TokenData;
import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.R;
import com.easemob.redpacketui.ui.activity.RPChangeActivity;
import com.easemob.redpacketui.ui.activity.RPRedPacketActivity;
import com.easemob.redpacketui.ui.activity.RPTransferActivity;
import com.easemob.redpacketui.ui.activity.RPTransferDetailActivity;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseChatMessageList;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedPacketUtil {

    /**
     * 进入发红包页面
     *
     * @param fragment       Fragment
     * @param chatType       聊天类型
     * @param toChatUsername 消息接收者Id
     * @param requestCode    请求码
     */
    public static void startRedPacketActivityForResult(Fragment fragment, int chatType, final String toChatUsername, int requestCode) {
        //发送者头像url
        String fromAvatarUrl = "none";
        //发送者昵称 设置了昵称就传昵称 否则传id
        String fromNickname = EMClient.getInstance().getCurrentUser();
        EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.fromNickName = fromNickname;
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
                        EMClient.getInstance().groupManager().getGroupFromServer(toChatUsername);
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            RedPacket.getInstance().setRPGroupMemberListener(new RPGroupMemberListener() {
                @Override
                public void getGroupMember(String groupID, RPValueCallback<List<RPUserBean>> rpValueCallback) {
                    EMGroup group = EMClient.getInstance().groupManager().getGroup(groupID);
                    List<String> members = group.getMembers();
                    List<RPUserBean> userBeanList = new ArrayList<RPUserBean>();
                    EaseUser user;
                    for (int i = 0; i < members.size(); i++) {
                        RPUserBean userBean = new RPUserBean();
                        userBean.userId = members.get(i);
                        if (userBean.userId.equals(EMClient.getInstance().getCurrentUser())) {
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
                    rpValueCallback.onSuccess(userBeanList);
                }
            });
            EMGroup group = EMClient.getInstance().groupManager().getGroup(toChatUsername);
            redPacketInfo.toGroupId = group.getGroupId();
            redPacketInfo.groupMemberCount = group.getMemberCount();
            redPacketInfo.chatType = RPConstant.CHATTYPE_GROUP;
        }
        Intent intent = new Intent(fragment.getContext(), RPRedPacketActivity.class);
        intent.putExtra(RPConstant.EXTRA_RED_PACKET_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_TOKEN_DATA, getTokenData());
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 小额随机红包
     *
     * @param callBack       RPRandomCallback
     * @param toChatUsername 消息接收者Id
     */
    public static void startRandomPacket(RPRedPacketUtil.RPRandomCallback callBack, final FragmentActivity activity, final String toChatUsername) {
        //发送者头像url
        String fromAvatarUrl = "none";
        //发送者昵称 设置了昵称就传昵称 否则传id
        String fromNickname = EMClient.getInstance().getCurrentUser();
        EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }

        EaseUser easeToUser = EaseUserUtils.getUserInfo(toChatUsername);
        String toAvatarUrl = "none";
        String toUserName = "";
        if (easeToUser != null) {
            toAvatarUrl = TextUtils.isEmpty(easeToUser.getAvatar()) ? "none" : easeToUser.getAvatar();
            toUserName = TextUtils.isEmpty(easeToUser.getNick()) ? easeToUser.getUsername() : easeToUser.getNick();
        }

        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.chatType = RPConstant.CHATTYPE_SINGLE;
        //发送者头像url和昵称
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.fromNickName = fromNickname;
        //接收者Id、头像url和昵称
        redPacketInfo.toUserId = toChatUsername;
        redPacketInfo.toNickName = toUserName;
        redPacketInfo.toAvatarUrl = toAvatarUrl;
        RPRedPacketUtil.getInstance().enterRandomRedPacket(redPacketInfo, getTokenData(), activity, callBack);
    }

    private static TokenData getTokenData() {
        TokenData tokenData = new TokenData();
        tokenData.imUserId = EMClient.getInstance().getCurrentUser();
        //此处使用环信id代替了appUserId 开发者可传入App的appUserId
        tokenData.appUserId = EMClient.getInstance().getCurrentUser();
        tokenData.imToken = EMClient.getInstance().getOptions().getAccessToken();
        return tokenData;
    }

    /**
     * 进入转账页面
     *
     * @param fragment       Fragment
     * @param toChatUsername 消息接收者Id
     * @param requestCode    请求码
     */
    public static void startTransferActivityForResult(Fragment fragment, final String toChatUsername, int requestCode) {
        //发送者头像url
        String fromAvatarUrl = "none";
        //发送者昵称 设置了昵称就传昵称 否则传id
        String fromNickname = EMClient.getInstance().getCurrentUser();
        EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        String toAvatarUrl = "none";
        String toUserName = "";
        EaseUser easeToUser = EaseUserUtils.getUserInfo(toChatUsername);
        if (easeToUser != null) {
            toAvatarUrl = TextUtils.isEmpty(easeToUser.getAvatar()) ? "none" : easeToUser.getAvatar();
            toUserName = TextUtils.isEmpty(easeToUser.getNick()) ? easeToUser.getUsername() : easeToUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.fromNickName = fromNickname;
        //接收者Id
        redPacketInfo.toUserId = toChatUsername;
        redPacketInfo.toNickName = toUserName;
        redPacketInfo.toAvatarUrl = toAvatarUrl;

        Intent intent = new Intent(fragment.getContext(), RPTransferActivity.class);
        intent.putExtra(RPConstant.EXTRA_RED_PACKET_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_TOKEN_DATA, getTokenData());
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 创建一条红包消息
     *
     * @param context        上下文
     * @param data           intent
     * @param toChatUsername 消息接收者id
     * @return 封装好的红包消息
     */
    public static EMMessage createRPMessage(Context context, Intent data, String toChatUsername) {
        String greetings = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_GREETING);
        String redPacketId = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_ID);
        String receiverId = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_RECEIVER_ID);
        String redPacketType = data.getStringExtra(RPConstant.EXTRA_RED_PACKET_TYPE);
        EMMessage message = EMMessage.createTxtSendMessage("[" + context.getResources().getString(R.string.easemob_red_packet) + "]" + greetings, toChatUsername);
        message.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, true);
        message.setAttribute(RPConstant.EXTRA_SPONSOR_NAME, context.getResources().getString(R.string.easemob_red_packet));
        message.setAttribute(RPConstant.EXTRA_RED_PACKET_GREETING, greetings);
        message.setAttribute(RPConstant.EXTRA_RED_PACKET_ID, redPacketId);
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, redPacketType);
        message.setAttribute(RPConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, receiverId);
        return message;
    }

    /**
     * 创建转账消息
     *
     * @param context        上下文
     * @param data           Intent
     * @param toChatUsername 消息接收者id
     * @return 封账好的转账消息
     */
    public static EMMessage createTRMessage(Context context, Intent data, String toChatUsername) {
        String transferAmount = data.getStringExtra(RPConstant.EXTRA_TRANSFER_AMOUNT);
        String transferTime = data.getStringExtra(RPConstant.EXTRA_TRANSFER_PACKET_TIME);
        EMMessage message = EMMessage.createTxtSendMessage(String.format(context.getResources().getString(R.string.easemob_transfer_packet), transferAmount), toChatUsername);
        message.setAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, true);
        message.setAttribute(RPConstant.EXTRA_TRANSFER_AMOUNT, transferAmount);
        message.setAttribute(RPConstant.EXTRA_TRANSFER_PACKET_TIME, transferTime);
        return message;
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
        String messageDirect;
        //接收者头像url 默认值为none
        String toAvatarUrl = "none";//测试用图片url:http://i.imgur.com/DvpvklR.png
        //接收者昵称 默认值为当前用户ID
        String toNickname = EMClient.getInstance().getCurrentUser();
        String currentUserId = toNickname;
        String redPacketId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_ID, "");
        if (message.direct() == EMMessage.Direct.SEND) {
            messageDirect = RPConstant.MESSAGE_DIRECT_SEND;
        } else {
            messageDirect = RPConstant.MESSAGE_DIRECT_RECEIVE;
        }
        EaseUser easeUser = EaseUserUtils.getUserInfo(EMClient.getInstance().getCurrentUser());
        if (easeUser != null) {
            toAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            toNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        String specialAvatarUrl = "none";
        String specialNickname = "";
        String packetType;
        packetType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        String specialReceiveId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_SPECIAL_RECEIVER_ID, "");
        if (!TextUtils.isEmpty(packetType) && packetType.equals(RPConstant.GROUP_RED_PACKET_TYPE_EXCLUSIVE)) {
            EaseUser userInfo = EaseUserUtils.getUserInfo(specialReceiveId);
            if (userInfo != null) {
                specialAvatarUrl = TextUtils.isEmpty(userInfo.getAvatar()) ? "none" : userInfo.getAvatar();
                specialNickname = TextUtils.isEmpty(userInfo.getNick()) ? userInfo.getUsername() : userInfo.getNick();
            } else {
                specialNickname = specialReceiveId;
            }
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.redPacketId = redPacketId;
        redPacketInfo.toAvatarUrl = toAvatarUrl;
        redPacketInfo.toNickName = toNickname;
        redPacketInfo.moneyMsgDirect = messageDirect;
        redPacketInfo.toUserId = currentUserId;
        redPacketInfo.chatType = chatType;
        if (packetType.equals(RPConstant.GROUP_RED_PACKET_TYPE_EXCLUSIVE)) {
            redPacketInfo.specialAvatarUrl = specialAvatarUrl;
            redPacketInfo.specialNickname = specialNickname;
        }
        RPRedPacketUtil.getInstance().openRedPacket(redPacketInfo, getTokenData(), activity, new RPRedPacketUtil.RPOpenPacketCallback() {
            @Override
            public void onSuccess(String senderId, String senderNickname, String myAmount) {
                //领取红包成功 发送消息到聊天窗
                String receiverId = EMClient.getInstance().getCurrentUser();
                //设置默认值为id
                String receiverNickname = receiverId;
                EaseUser receiverUser = EaseUserUtils.getUserInfo(receiverId);
                if (receiverUser != null) {
                    receiverNickname = TextUtils.isEmpty(receiverUser.getNick()) ? receiverUser.getUsername() : receiverUser.getNick();
                }
                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                    EMMessage msg = EMMessage.createTxtSendMessage(String.format(activity.getResources().getString(R.string.msg_someone_take_red_packet), receiverNickname), toChatUsername);
                    msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                    msg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                    msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                    EMClient.getInstance().chatManager().sendMessage(msg);
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
     * 打开小额随机红包的方法
     *
     * @param activity FragmentActivity
     * @param message  EMMessage
     */
    public static void openRandomPacket(final FragmentActivity activity, final EMMessage message) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setCanceledOnTouchOutside(false);
        String messageDirect;//消息的方向
        //接收者头像url
        String toAvatarUrl = "";
        //接收者昵称
        String toNickname = "";
        String redPacketId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_ID, "");
        if (message.direct() == EMMessage.Direct.SEND) {
            messageDirect = RPConstant.MESSAGE_DIRECT_SEND;
        } else {
            messageDirect = RPConstant.MESSAGE_DIRECT_RECEIVE;
        }
        //小额随机红包由于UI展示的需要，区别于普通单聊红包，需要传入红包接收者的id
        String randomPacketReceiverId = message.getTo();
        EaseUser easeUser = EaseUserUtils.getUserInfo(randomPacketReceiverId);
        if (easeUser != null) {
            toAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            toNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.redPacketId = redPacketId;
        redPacketInfo.toUserId = randomPacketReceiverId;
        redPacketInfo.toNickName = toNickname;
        redPacketInfo.toAvatarUrl = toAvatarUrl;
        redPacketInfo.moneyMsgDirect = messageDirect;
        redPacketInfo.chatType = RPConstant.CHATTYPE_SINGLE;//必须传这个值为1(单聊)
        RPRedPacketUtil.getInstance().openRedPacket(redPacketInfo, getTokenData(), activity, new RPRedPacketUtil.RPOpenPacketCallback() {
            @Override
            public void onSuccess(String senderId, String senderNickname, String myAmount) {
                //小额随机红包无回执消息
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
     * 进入转账详情
     *
     * @param context 上下文
     * @param message EMMessage
     */
    public static void openTransferPacket(Context context, EMMessage message) {
        String fromNickname = EMClient.getInstance().getCurrentUser();
        String fromAvatarUrl = "none";
        EaseUser easeUser = EaseUserUtils.getUserInfo(EMClient.getInstance().getCurrentUser());
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        String messageDirect;
        String transferAmount = message.getStringAttribute(RPConstant.EXTRA_TRANSFER_AMOUNT, "");
        String time = message.getStringAttribute(RPConstant.EXTRA_TRANSFER_PACKET_TIME, "");
        if (message.direct() == EMMessage.Direct.SEND) {
            messageDirect = RPConstant.MESSAGE_DIRECT_SEND;
        } else {
            messageDirect = RPConstant.MESSAGE_DIRECT_RECEIVE;
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.moneyMsgDirect = messageDirect;
        redPacketInfo.redPacketAmount = transferAmount;
        redPacketInfo.fromNickName = fromNickname;
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        redPacketInfo.transferTime = time;
        Intent intent = new Intent(context, RPTransferDetailActivity.class);
        intent.putExtra(RPConstant.EXTRA_RED_PACKET_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_TOKEN_DATA, getTokenData());
        context.startActivity(intent);
    }

    /**
     * 进入零钱页面
     *
     * @param context 上下文
     */
    public static void startChangeActivity(Context context) {
        Intent intent = new Intent(context, RPChangeActivity.class);
        String fromNickname = EMClient.getInstance().getCurrentUser();
        String fromAvatarUrl = "none";
        EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
        if (easeUser != null) {
            fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
            fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
        }
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        redPacketInfo.fromNickName = fromNickname;
        redPacketInfo.fromAvatarUrl = fromAvatarUrl;
        intent.putExtra(RPConstant.EXTRA_RED_PACKET_INFO, redPacketInfo);
        intent.putExtra(RPConstant.EXTRA_TOKEN_DATA, getTokenData());
        context.startActivity(intent);
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
        cmdMsg.setTo(senderId);
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
                sendMessage.setDirection(EMMessage.Direct.SEND);
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
                sendMessage.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
                EMClient.getInstance().chatManager().saveMessage(sendMessage);
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {

            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(cmdMsg);
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
        String currentUser = EMClient.getInstance().getCurrentUser();
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
            msg.setDirection(EMMessage.Direct.RECEIVE);
            msg.setUnread(false);//去掉未读的显示
            msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, senderNickname);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, receiverNickname);
            msg.setAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, senderId);
            //保存消息
            EMClient.getInstance().chatManager().saveMessage(msg);
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
        return !TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.RANDOM_PACKET_TYPE);
    }

    /**
     * 判断红包类型是否为广告红包
     *
     * @param message EMMessage
     * @return true or false
     */
    public static boolean isADRedPacket(EMMessage message) {
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        return !TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.AD_RED_PACKET_TYPE);
    }
}

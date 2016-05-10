package com.easemob.chatuidemo.utils;

import android.util.Log;

import com.easemob.EMCallBack;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMMessage;
import com.easemob.chatuidemo.Constant;
import com.easemob.exceptions.EaseMobException;

import java.util.UUID;

/**
 * Created by max on 16/5/10.
 */
public class MoneyUtils {

    public static void sendMoneyMessage(final EMMessage message, final EMCallBack callBack) {
        try {
            //获取扩展属性值
            final String senderNickname = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER);
            final String senderId = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER_ID);
            final String receiverNickname = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER);
            String receiverId = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER_ID);
            //创建透传消息
            final EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
            cmdMsg.setChatType(EMMessage.ChatType.GroupChat);
            CmdMessageBody cmdBody = new CmdMessageBody(Constant.REFRESH_GROUP_MONEY_ACTION);
            cmdMsg.addBody(cmdBody);
            //设置cmd消息接收者为发红包的人
            cmdMsg.setReceipt(message.getTo());
            //设置扩展属性
            cmdMsg.setAttribute(Constant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, true);
            cmdMsg.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER, senderNickname);
            cmdMsg.setAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER, receiverNickname);
            cmdMsg.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER_ID, senderId);
            cmdMsg.setAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER_ID, receiverId);
            EMChatManager.getInstance().sendMessage(cmdMsg, new EMCallBack() {
                @Override
                public void onSuccess() {
                    //更新UI 为你领取了xx的红包
                    Log.d("tag", senderId);
                    EMMessage sendMessage = EMMessage.createTxtSendMessage("content", message.getTo());
                    sendMessage.setChatType(EMMessage.ChatType.GroupChat);
                    sendMessage.setFrom(message.getFrom());
                    sendMessage.setTo(message.getTo());
                    sendMessage.setMsgId(UUID.randomUUID().toString());
                    sendMessage.direct = EMMessage.Direct.SEND;
                    sendMessage.setAttribute(Constant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, true);
                    sendMessage.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER, senderNickname);
                    sendMessage.setAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER, receiverNickname);
                    sendMessage.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER_ID, senderId);
                    EMChatManager.getInstance().saveMessage(sendMessage);
                    callBack.onSuccess();
                }

                @Override
                public void onProgress(int progress, String status) {
                }

                @Override
                public void onError(int code, String error) {
                    callBack.onError(code, error);
                }
            });
        } catch (EaseMobException e) {
            e.printStackTrace();
        }
    }

    public static void receiveMoneyMessage(EMMessage message) {
        try {
            String senderNickname = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER);
            String receiverNickname = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER);
            String senderId = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER_ID);
            String receiverId = message.getStringAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER_ID);
            String currentUser = EMChatManager.getInstance().getCurrentUser();
            //更新UI 为xx领取了你的红包
            if (currentUser.equals(senderId) && !receiverId.equals(senderId)) {//如果不是自己领取的红包更新此类消息UI
                EMMessage msg = EMMessage.createTxtSendMessage("content", message.getTo());
                msg.setChatType(EMMessage.ChatType.GroupChat);
                msg.setFrom(message.getFrom());
                msg.setTo(message.getTo());
                msg.setMsgId(UUID.randomUUID().toString());
                msg.direct = EMMessage.Direct.RECEIVE;
                msg.setAttribute(Constant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, true);
                msg.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER, senderNickname);
                msg.setAttribute(Constant.EXTRA_LUCKY_MONEY_SENDER_ID, senderId);
                msg.setAttribute(Constant.EXTRA_LUCKY_MONEY_RECEIVER, receiverNickname);
                //保存消息
                EMChatManager.getInstance().saveMessage(msg);
            }
        } catch (EaseMobException e) {
            e.printStackTrace();
        }
    }
}

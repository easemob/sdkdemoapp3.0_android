package com.easemob.luckymoneyui.widget;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.easemob.luckymoneyui.R;
import com.easemob.luckymoneyui.RedPacketConstant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.exceptions.HyphenateException;

public class ChatRowReceiveMoney extends EaseChatRow {

    private TextView mTvMessage;

    public ChatRowReceiveMoney(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflatView() {
        if (message.getBooleanAttribute(RedPacketConstant.MESSAGE_ATTR_IS_OPEN_MONEY_MESSAGE, false)) {
            inflater.inflate(message.direct() == EMMessage.Direct.RECEIVE ?
                    R.layout.em_row_money_message : R.layout.em_row_money_message, this);
        }
    }

    @Override
    protected void onFindViewById() {
        mTvMessage = (TextView) findViewById(R.id.ease_tv_money_msg);
    }

    @Override
    protected void onSetUpView() {
        try {
            String currentUser = EMClient.getInstance().getCurrentUser();
            String fromUser = message.getStringAttribute(RedPacketConstant.EXTRA_LUCKY_MONEY_SENDER_NAME);//红包发送者
            String toUser = message.getStringAttribute(RedPacketConstant.EXTRA_LUCKY_MONEY_RECEIVER_NAME);//红包接收者
            String senderId;
            if (message.direct() == EMMessage.Direct.SEND) {
                if (message.getChatType().equals(EMMessage.ChatType.GroupChat)) {
                    senderId = message.getStringAttribute(RedPacketConstant.EXTRA_LUCKY_MONEY_SENDER_ID);
                    if (senderId.equals(currentUser)) {
                        mTvMessage.setText(R.string.money_msg_take_money);
                    } else {
                        mTvMessage.setText(String.format(getResources().getString(R.string.money_msg_take_someone_money), fromUser));
                    }
                } else {
                    mTvMessage.setText(String.format(getResources().getString(R.string.money_msg_take_someone_money), fromUser));
                }
            } else {
                mTvMessage.setText(String.format(getResources().getString(R.string.money_msg_someone_take_money), toUser));
            }
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onUpdateView() {

    }

    @Override
    protected void onBubbleClick() {
    }

}

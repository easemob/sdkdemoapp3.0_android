package com.easemob.redpacket.widget;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.easemob.redpacket.R;
import com.easemob.redpacketsdk.constant.RPConstant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;

public class ChatRowRedPacketAck extends EaseChatRow {

    private TextView mTvMessage;

    public ChatRowRedPacketAck(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflateView() {
        if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, false)) {
            inflater.inflate(message.direct() == EMMessage.Direct.RECEIVE ?
                    R.layout.em_row_red_packet_ack_message : R.layout.em_row_red_packet_ack_message, this);
        }
    }

    @Override
    protected void onFindViewById() {
        mTvMessage = (TextView) findViewById(R.id.ease_tv_money_msg);
    }

    @Override
    protected void onSetUpView() {
        String currentUser = EMClient.getInstance().getCurrentUser();
        String fromUser = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME, "");//红包发送者
        String toUser = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, "");//红包接收者
        String senderId;
        if (message.direct() == EMMessage.Direct.SEND) {
            if (message.getChatType().equals(EMMessage.ChatType.GroupChat)) {
                senderId = message.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_ID, "");
                if (senderId.equals(currentUser)) {
                    mTvMessage.setText(R.string.msg_take_red_packet);
                } else {
                    mTvMessage.setText(String.format(getResources().getString(R.string.msg_take_someone_red_packet), fromUser));
                }
            } else {
                mTvMessage.setText(String.format(getResources().getString(R.string.msg_take_someone_red_packet), fromUser));
            }
        } else {
            mTvMessage.setText(String.format(getResources().getString(R.string.msg_someone_take_red_packet), toUser));
        }
    }

    @Override
    protected void onUpdateView() {

    }

    @Override
    protected void onBubbleClick() {
    }

}

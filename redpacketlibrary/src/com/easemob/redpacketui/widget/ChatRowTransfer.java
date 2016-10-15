package com.easemob.redpacketui.widget;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.R;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;

public class ChatRowTransfer extends EaseChatRow {

    private TextView mTvTransfer;

    public ChatRowTransfer(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflateView() {
        if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
            inflater.inflate(message.direct() == EMMessage.Direct.RECEIVE ?
                    R.layout.em_row_received_transfer : R.layout.em_row_sent_transfer, this);
        }
    }

    @Override
    protected void onFindViewById() {
        mTvTransfer = (TextView) findViewById(R.id.tv_transfer_received);
    }

    @Override
    protected void onSetUpView() {
        String transferAmount = message.getStringAttribute(RPConstant.EXTRA_TRANSFER_AMOUNT, "");
        mTvTransfer.setText(String.format("%s元", transferAmount));
    }

    @Override
    protected void onUpdateView() {

    }

    @Override
    protected void onBubbleClick() {
    }

}

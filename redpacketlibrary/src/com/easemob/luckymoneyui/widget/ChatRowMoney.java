package com.easemob.luckymoneyui.widget;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.easemob.luckymoneyui.R;
import com.easemob.luckymoneyui.RedPacketConstant;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.exceptions.HyphenateException;

public class ChatRowMoney extends EaseChatRow {

    private TextView mTvGreeting;
    private TextView mTvSponsorName;

    public ChatRowMoney(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflatView() {
        if (message.getBooleanAttribute(RedPacketConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, false)) {
            inflater.inflate(message.direct() == EMMessage.Direct.RECEIVE ?
                    R.layout.em_row_received_money : R.layout.em_row_sent_money, this);
        }
    }

    @Override
    protected void onFindViewById() {
        mTvGreeting = (TextView) findViewById(R.id.tv_money_greeting);
        mTvSponsorName = (TextView) findViewById(R.id.tv_sponsor_name);
    }

    @Override
    protected void onSetUpView() {
        try {
            String sponsorName = message.getStringAttribute(RedPacketConstant.EXTRA_SPONSOR_NAME);
            String greetings = message.getStringAttribute(RedPacketConstant.EXTRA_MONEY_GREETING);
            mTvGreeting.setText(greetings);
            mTvSponsorName.setText(sponsorName);
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

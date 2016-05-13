package com.easemob.chatuidemo.widget;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.easemob.chat.EMMessage;
import com.easemob.chatuidemo.R;
import com.easemob.easeui.widget.chatrow.EaseChatRow;
import com.easemob.exceptions.EaseMobException;
import com.easemob.luckymoneysdk.constant.LMConstant;

public class ChatRowMoney extends EaseChatRow {

    private TextView mTvGreeting;
    private TextView mTvSponsorName;

    public ChatRowMoney(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflatView() {
        if (message.getBooleanAttribute(LMConstant.MESSAGE_ATTR_IS_MONEY_MESSAGE, false)) {
            inflater.inflate(message.direct == EMMessage.Direct.RECEIVE ?
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
            String sponsorName = message.getStringAttribute(LMConstant.EXTRA_SPONSOR_NAME);
            String greetings = message.getStringAttribute(LMConstant.EXTRA_MONEY_GREETING);
            mTvGreeting.setText(greetings);
            mTvSponsorName.setText(sponsorName);
        } catch (EaseMobException e) {
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

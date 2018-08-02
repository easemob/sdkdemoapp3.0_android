package com.hyphenate.chatuidemo.widget;

import android.content.Context;
import android.widget.BaseAdapter;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.easeui.widget.presenter.EaseChatRowPresenter;

/**
 * Created by zhangsong on 17-10-12.
 */

public class ChatRowLivePresenter extends EaseChatRowPresenter {
    @Override
    protected EaseChatRow onCreateChatRow(Context cxt, EMMessage message, int position, BaseAdapter adapter) {
        return new ChatRowConferenceInvite(cxt, message, position, adapter);
    }

    @Override
    public void onBubbleClick(EMMessage message) {
        super.onBubbleClick(message);

        String confId = message.getStringAttribute("em_conference_id", "");
        String confPassword = message.getStringAttribute("em_conference_password","");
        int type = message.getIntAttribute("em_conference_type", 0);
        DemoHelper.getInstance().goLive(confId, confPassword, message.getFrom());
    }

}

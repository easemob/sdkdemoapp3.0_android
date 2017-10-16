package com.easemob.redpacket.widget;

import android.content.Context;
import android.widget.BaseAdapter;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.easeui.widget.presenter.EaseChatRowPresenter;

/**
 * Created by zhangsong on 17-10-12.
 */

public class ChatRowRedPacketPresenter extends EaseChatRowPresenter {
    @Override
    protected EaseChatRow onCreateChatRow(Context cxt, EMMessage message, int position, BaseAdapter adapter) {
        return new ChatRowRedPacket(cxt, message, position, adapter);
    }
}

package com.hyphenate.chatuidemo.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.db.InviteMessgeDao;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.model.EaseDingMessageHelper;
import com.hyphenate.easeui.ui.EaseConversationListFragment;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.widget.EaseSwipeMenuLayout;
import com.hyphenate.util.NetUtils;

import org.json.JSONObject;

public class ConversationListFragment extends EaseConversationListFragment{

    private TextView errorText;
    private View itemView;

    @Override
    protected void initView() {
        super.initView();
        View errorView = (LinearLayout) View.inflate(getActivity(),R.layout.em_chat_neterror_item, null);
        errorItemContainer.addView(errorView);
        errorText = (TextView) errorView.findViewById(R.id.tv_connect_errormsg);
    }

    @Override
    protected void setUpView() {
        super.setUpView();
        // register context menu
//        registerForContextMenu(conversationListView);
        super.setUpView();
    }

    /***
     * item点击和侧滑菜单点击
     * @param type
     * @param conversation
     * @param rootView
     */
    @Override
    public void viewClick(int type, EMConversation conversation, View rootView) {
        super.viewClick(type, conversation, rootView);
        itemView = rootView;
        switch (type){
            case DEFAULT:
                //点击跳转聊天界面
                String username = conversation.conversationId();
                // start chat acitivity
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                if(conversation.isGroup()){
                    if(conversation.getType() == EMConversationType.ChatRoom){
                        // it's group chat
                        intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_CHATROOM);
                    }else{
                        intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_GROUP);
                    }

                }
                // it's single chat
                intent.putExtra(Constant.EXTRA_USER_ID, username);
                startActivity(intent);

                break;
            case TOP_ITEM:
                //会话置顶
                int top = EaseCommonUtils.getExtTop(conversation);
                if(top == 1){
                    setConversationExtTop(conversation,0);
                }else {
                    setConversationExtTop(conversation,1);
                }

                refresh();
                break;
            case UNREAD_ITEM:
                //会话设置已读、未读
                if(conversation.getUnreadMsgCount() > 0){
                    conversation.markAllMessagesAsRead();
                }else{
                    EMMessage message = conversation.getLastMessage();
                    message.setUnread(true);
                    EMClient.getInstance().chatManager().updateMessage(message);
                }
                refresh();

                // update unread count
                ((MainActivity) getActivity()).updateUnreadLabel();

                break;
            case REMOVE_ITEM:
                //删除会话和消息
                if (conversation == null) {
                    return ;
                }
                if(conversation.getType() == EMConversationType.GroupChat){
                    EaseAtMessageHelper.get().removeAtMeGroup(conversation.conversationId());
                }
                try {
                    // delete conversation
                    EMClient.getInstance().chatManager().deleteConversation(conversation.conversationId(), true);
                    InviteMessgeDao inviteMessgeDao = new InviteMessgeDao(getActivity());
                    inviteMessgeDao.deleteMessage(conversation.conversationId());
                    // To delete the native stored adked users in this conversation.
                    EaseDingMessageHelper.get().delete(conversation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                refresh();

                // update unread count
                ((MainActivity) getActivity()).updateUnreadLabel();

                break;
        }
        //关闭侧滑显示
        ((EaseSwipeMenuLayout)rootView).quickClose();
    }

    /**
     * 在会话ext里设置置顶标识和置顶时间戳
     * @param conversation
     * @param i
     */
    protected void setConversationExtTop(EMConversation conversation, int i){
        try {
            JSONObject ext = new JSONObject();
            ext.put("top", i);
            ext.put("topTime",System.currentTimeMillis());
            conversation.setExtField(ext.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    protected void onConnectionDisconnected() {
        super.onConnectionDisconnected();
        if (NetUtils.hasNetwork(getActivity())){
            errorText.setText(R.string.can_not_connect_chat_server_connection);
        } else {
            errorText.setText(R.string.the_current_network);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(itemView != null) {
            ((EaseSwipeMenuLayout) itemView).quickClose();
        }
    }

    //    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
//        getActivity().getMenuInflater().inflate(R.menu.em_delete_message, menu);
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        boolean deleteMessage = false;
//        if (item.getItemId() == R.id.delete_message) {
//            deleteMessage = true;
//        } else if (item.getItemId() == R.id.delete_conversation) {
//            deleteMessage = false;
//        }
//        EMConversation tobeDeleteCons = conversationListView.getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
//        if (tobeDeleteCons == null) {
//            return true;
//        }
//        if(tobeDeleteCons.getType() == EMConversationType.GroupChat){
//            EaseAtMessageHelper.get().removeAtMeGroup(tobeDeleteCons.conversationId());
//        }
//        try {
//            // delete conversation
//            EMClient.getInstance().chatManager().deleteConversation(tobeDeleteCons.conversationId(), deleteMessage);
//            InviteMessgeDao inviteMessgeDao = new InviteMessgeDao(getActivity());
//            inviteMessgeDao.deleteMessage(tobeDeleteCons.conversationId());
//            // To delete the native stored adked users in this conversation.
//            if (deleteMessage) {
//                EaseDingMessageHelper.get().delete(tobeDeleteCons);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        refresh();
//
//        // update unread count
//        ((MainActivity) getActivity()).updateUnreadLabel();
//        return true;
//    }

}

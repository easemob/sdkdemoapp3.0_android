package com.hyphenate.chatuidemo.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.redpacketsdk.constant.RPConstant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseConversationExtUtils;
import com.hyphenate.easeui.ui.EaseConversationListFragment;
import com.hyphenate.easeui.widget.EaseConversationList.EaseConversationListHelper;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.util.NetUtils;
import java.util.ArrayList;
import java.util.List;

public class ConversationListFragment extends EaseConversationListFragment {

    private Activity activity;

    private TextView errorText;
    private AlertDialog.Builder alertDialogBuilder;
    private AlertDialog conversationMenuDialog;

    private View afficheNotifyView;
    private EMConversation afficheConversation;

    @Override protected void initView() {
        super.initView();
        View errorView =
                (LinearLayout) View.inflate(getActivity(), R.layout.em_chat_neterror_item, null);
        errorItemContainer.addView(errorView);
        errorText = (TextView) errorView.findViewById(R.id.tv_connect_errormsg);

        activity = getActivity();

        // 添加公告按钮
        titleBar = (EaseTitleBar) getView().findViewById(R.id.title_bar);
        View view = LayoutInflater.from(activity).inflate(R.layout.em_widget_affiche, null);
        afficheNotifyView = view.findViewById(R.id.text_affiche_notify);
        titleBar.addView(view);
        view.findViewById(R.id.layout_affiche).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(activity, "公告", Toast.LENGTH_LONG).show();
                startActivity(new Intent(activity, AfficheActivity.class));
            }
        });
    }

    @Override protected void setUpView() {
        //red packet code : 红包回执消息在会话列表最后一条消息的展示
        conversationListView.setConversationListHelper(new EaseConversationListHelper() {
            @Override public String onSetItemSecondaryText(EMMessage lastMessage) {
                if (lastMessage.getBooleanAttribute(
                        RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, false)) {
                    String sendNick =
                            lastMessage.getStringAttribute(RPConstant.EXTRA_RED_PACKET_SENDER_NAME,
                                    "");
                    String receiveNick = lastMessage.getStringAttribute(
                            RPConstant.EXTRA_RED_PACKET_RECEIVER_NAME, "");
                    String msg;
                    if (lastMessage.direct() == EMMessage.Direct.RECEIVE) {
                        msg = String.format(
                                getResources().getString(R.string.msg_someone_take_red_packet),
                                receiveNick);
                    } else {
                        if (sendNick.equals(receiveNick)) {
                            msg = getResources().getString(R.string.msg_take_red_packet);
                        } else {
                            msg = String.format(
                                    getResources().getString(R.string.msg_take_someone_red_packet),
                                    sendNick);
                        }
                    }
                    return msg;
                } else if (lastMessage.getBooleanAttribute(
                        RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
                    String transferAmount =
                            lastMessage.getStringAttribute(RPConstant.EXTRA_TRANSFER_AMOUNT, "");
                    String msg;
                    if (lastMessage.direct() == EMMessage.Direct.RECEIVE) {
                        msg = String.format(getResources().getString(R.string.msg_transfer_to_you),
                                transferAmount);
                    } else {
                        msg = String.format(
                                getResources().getString(R.string.msg_transfer_from_you),
                                transferAmount);
                    }
                    return msg;
                }
                return null;
            }
        });
        //end of red packet code
        // 初始化会话列表点击监听
        initConversationListItemClickListener();
        super.setUpView();
    }

    @Override protected void onConnectionDisconnected() {
        super.onConnectionDisconnected();
        if (NetUtils.hasNetwork(getActivity())) {
            errorText.setText(R.string.can_not_connect_chat_server_connection);
        } else {
            errorText.setText(R.string.the_current_network);
        }
    }

    /**
     * 会话列表点击监听，包含点击和长按事件的监听
     * 这个在之前 easeui 有定义接口，但是并未使用，这里直接拿来用了，同时添加了一个长按回调方法
     * 定义在{@liink EaseConversationListFragment#EaseConversationListItemClickListener}
     */
    private void initConversationListItemClickListener() {
        this.setConversationListItemClickListener(new EaseConversationListItemClickListener() {
            /**
             * 会话列表点击事件
             * @param conversation -- clicked item
             */
            @Override public void onListItemClicked(EMConversation conversation) {
                String username = conversation.conversationId();
                if (username.equals(EMClient.getInstance().getCurrentUser())) {
                    Toast.makeText(getActivity(), R.string.Cant_chat_with_yourself,
                            Toast.LENGTH_SHORT).show();
                } else {
                    // start chat acitivity
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    if (conversation.isGroup()) {
                        if (conversation.getType() == EMConversationType.ChatRoom) {
                            // it's group chat
                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_CHATROOM);
                        } else {
                            intent.putExtra(Constant.EXTRA_CHAT_TYPE, Constant.CHATTYPE_GROUP);
                        }
                    }
                    // it's single chat
                    intent.putExtra(Constant.EXTRA_USER_ID, username);
                    startActivity(intent);
                }
            }

            /**
             * 会话列表长按事件
             *
             * @param conversation 当前长按的会话
             */
            @Override public boolean onListItemLongClicked(EMConversation conversation) {
                itemLongClick(conversation);
                return true;
            }
        });
    }

    /**
     * 会话列表长按事件
     *
     * @param conversation 当前长按的会话
     */
    public void itemLongClick(final EMConversation conversation) {
        final boolean isTop = EaseConversationExtUtils.getConversationTop(conversation);
        // 根据当前会话不同的状态来显示不同的长按菜单
        List<String> menuList = new ArrayList<String>();
        if (isTop) {
            menuList.add(getResources().getString(R.string.conversation_cancel_top));
        } else {
            menuList.add(getResources().getString(R.string.conversation_top));
        }

        menuList.add(getResources().getString(R.string.delete_conversation));
        menuList.add(getResources().getString(R.string.delete_conversation_messages));

        String[] menus = new String[menuList.size()];
        menuList.toArray(menus);

        // 创建并显示 ListView 的长按弹出菜单，并设置弹出菜单 Item的点击监听
        alertDialogBuilder = new AlertDialog.Builder(activity);
        // 弹出框标题
        // alertDialogBuilder.setTitle(R.string.ml_dialog_title_conversation);
        alertDialogBuilder.setItems(menus, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // 根据当前状态设置会话是否置顶
                        if (isTop) {
                            EaseConversationExtUtils.setConversationTop(conversation, false);
                        } else {
                            EaseConversationExtUtils.setConversationTop(conversation, true);
                        }
                        refresh();
                        break;
                    case 1:
                        // 删除会话，但是不删除消息
                        EMClient.getInstance()
                                .chatManager()
                                .deleteConversation(conversation.conversationId(), false);
                        refresh();
                        // update unread count
                        ((MainActivity) getActivity()).updateUnreadLabel();
                        break;
                    case 2:
                        // 删除会话，同时清空消息
                        EMClient.getInstance()
                                .chatManager()
                                .deleteConversation(conversation.conversationId(), true);
                        refresh();
                        // update unread count
                        ((MainActivity) getActivity()).updateUnreadLabel();
                        break;
                }
            }
        });
        conversationMenuDialog = alertDialogBuilder.create();
        conversationMenuDialog.show();
    }

    /**
     * 刷新公告
     */
    public void refreshAffiche() {
        afficheConversation = EMClient.getInstance()
                .chatManager()
                .getConversation(Constant.AFFICHE_CONVERSATION_ID);
        if (afficheConversation == null) {
            return;
        }
        if (afficheConversation.getUnreadMsgCount() > 0) {
            afficheNotifyView.setVisibility(View.VISIBLE);
        } else {
            afficheNotifyView.setVisibility(View.GONE);
        }
    }

    @Override public void onResume() {
        refresh();
        refreshAffiche();
        super.onResume();
    }
}

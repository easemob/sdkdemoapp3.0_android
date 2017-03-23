package com.hyphenate.chatuidemo.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.utils.RPRedPacketUtil;
import com.easemob.redpacketui.utils.RedPacketUtil;
import com.easemob.redpacketui.widget.ChatRowRandomPacket;
import com.easemob.redpacketui.widget.ChatRowRedPacket;
import com.easemob.redpacketui.widget.ChatRowRedPacketAck;
import com.easemob.redpacketui.widget.ChatRowTransfer;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.domain.EmojiconExampleGroupData;
import com.hyphenate.chatuidemo.domain.RobotUser;
import com.hyphenate.chatuidemo.widget.ChatRowVoiceCall;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.ui.EaseBaiduMapActivity;
import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.easeui.ui.EaseChatFragment.EaseChatFragmentHelper;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseMessageUtils;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.hyphenate.easeui.widget.emojicon.EaseEmojiconMenu;
import com.hyphenate.util.EasyUtils;
import com.hyphenate.util.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class ChatFragment extends EaseChatFragment implements EaseChatFragmentHelper {

    // constant start from 11 to avoid conflict with constant in base class
    private static final int ITEM_LOCATION = 3;
    private static final int ITEM_VIDEO = 11;
    private static final int ITEM_FILE = 12;
    private static final int ITEM_VOICE_CALL = 13;
    private static final int ITEM_VIDEO_CALL = 14;
    private static final int ITEM_BURN = 15;


    private static final int REQUEST_CODE_SELECT_VIDEO = 11;
    private static final int REQUEST_CODE_SELECT_FILE = 12;
    private static final int REQUEST_CODE_GROUP_DETAIL = 13;
    private static final int REQUEST_CODE_CONTEXT_MENU = 14;
    private static final int REQUEST_CODE_SELECT_AT_USER = 15;
    private static final int REQUEST_CODE_CLOSE_MAP = 20;

    private static final int MESSAGE_TYPE_SENT_VOICE_CALL = 1;
    private static final int MESSAGE_TYPE_RECV_VOICE_CALL = 2;
    private static final int MESSAGE_TYPE_SENT_VIDEO_CALL = 3;
    private static final int MESSAGE_TYPE_RECV_VIDEO_CALL = 4;

    //red packet code : 红包功能使用的常量
    private static final int MESSAGE_TYPE_RECV_RED_PACKET = 5;
    private static final int MESSAGE_TYPE_SEND_RED_PACKET = 6;
    private static final int MESSAGE_TYPE_SEND_RED_PACKET_ACK = 7;
    private static final int MESSAGE_TYPE_RECV_RED_PACKET_ACK = 8;
    private static final int MESSAGE_TYPE_RECV_TRANSFER_PACKET = 9;
    private static final int MESSAGE_TYPE_SEND_TRANSFER_PACKET = 10;
    private static final int MESSAGE_TYPE_RECV_RANDOM = 11;
    private static final int MESSAGE_TYPE_SEND_RANDOM = 12;
    private static final int REQUEST_CODE_SEND_RED_PACKET = 16;
    private static final int ITEM_RED_PACKET = 16;
    private static final int REQUEST_CODE_SEND_TRANSFER_PACKET = 17;
    private static final int ITEM_TRANSFER_PACKET = 17;
    //end of red packet code

    /**
     * if it is chatBot
     */
    private boolean isRobot;

    // 是否是阅后即焚
    private boolean isBurn = false;

    // 进度对话框
    private ProgressDialog progressDialog;
    private BroadcastReceiver broadcastReceiver;
    private LocalBroadcastManager broadcastManager;



    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override protected void setUpView() {
        setChatFragmentListener(this);
        registerBroadcastReceiver();
        if (chatType == Constant.CHATTYPE_SINGLE) {
            Map<String, RobotUser> robotMap = DemoHelper.getInstance().getRobotList();
            if (robotMap != null && robotMap.containsKey(toChatUsername)) {
                isRobot = true;
            }
        }
        super.setUpView();

        // set click listener
        titleBar.setLeftLayoutClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                if (EasyUtils.isSingleActivity(getActivity())) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                }
                onBackPressed();
            }
        });
        ((EaseEmojiconMenu) inputMenu.getEmojiconMenu()).addEmojiconGroup(EmojiconExampleGroupData.getData());
        if (chatType == EaseConstant.CHATTYPE_GROUP) {
            inputMenu.getPrimaryMenu().getEditText().addTextChangedListener(new TextWatcher() {

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (count == 1 && "@".equals(String.valueOf(s.charAt(start)))) {
                        startActivityForResult(new Intent(getActivity(), PickAtUserActivity.class).
                                putExtra("groupId", toChatUsername), REQUEST_CODE_SELECT_AT_USER);
                    }
                }

                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override public void afterTextChanged(Editable s) {

                }
            });
        }
    }

    @Override protected void registerExtendMenuItem() {
        //use the menu in base class
        super.registerExtendMenuItem();
        //extend menu items
        inputMenu.registerExtendMenuItem(R.string.attach_video, R.drawable.em_chat_video_selector, ITEM_VIDEO, extendMenuItemClickListener);
        inputMenu.registerExtendMenuItem(R.string.attach_file, R.drawable.em_chat_file_selector, ITEM_FILE, extendMenuItemClickListener);
        if (chatType == Constant.CHATTYPE_SINGLE) {
            inputMenu.registerExtendMenuItem(R.string.attach_voice_call, R.drawable.em_chat_voice_call_selector, ITEM_VOICE_CALL,
                    extendMenuItemClickListener);
            inputMenu.registerExtendMenuItem(R.string.attach_video_call, R.drawable.em_chat_video_call_selector, ITEM_VIDEO_CALL,
                    extendMenuItemClickListener);
            inputMenu.registerExtendMenuItem(R.string.attach_burn, R.drawable.ic_launcher, ITEM_BURN, extendMenuItemClickListener);
        }
        //聊天室暂时不支持红包功能
        //red packet code : 注册红包菜单选项
        if (chatType != Constant.CHATTYPE_CHATROOM) {
            inputMenu.registerExtendMenuItem(R.string.attach_red_packet, R.drawable.em_chat_red_packet_selector, ITEM_RED_PACKET,
                    extendMenuItemClickListener);
        }
        //red packet code : 注册转账菜单选项
        if (chatType == Constant.CHATTYPE_SINGLE) {
            inputMenu.registerExtendMenuItem(R.string.attach_transfer_money, R.drawable.em_chat_transfer_selector, ITEM_TRANSFER_PACKET,
                    extendMenuItemClickListener);
        }
        //end of red packet code
    }

    private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_GROUP_NOTIFY);
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(Constant.ACTION_GROUP_NOTIFY)){
                    messageList.refresh();
                }
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver(){
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
    }

    /**
     * 撤回消息，将已经发送成功的消息进行撤回
     *
     * @param message 需要撤回的消息
     */
    private void recallMessage(final EMMessage message) {
        // 显示撤回消息操作的 dialog
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("正在撤回 请稍候~~");
        progressDialog.show();
        EaseMessageUtils.sendRecallMessage(message, new EMCallBack() {
            @Override public void onSuccess() {
                // 关闭进度对话框
                progressDialog.dismiss();
                // 设置扩展为撤回消息类型，是为了区分消息的显示
                message.setAttribute(EaseConstant.REVOKE_FLAG, true);
                // 更新消息
                EMClient.getInstance().chatManager().updateMessage(message);
                // 撤回成功，刷新 UI
                messageList.refresh();
            }

            /**
             * 撤回消息失败
             * @param i 失败的错误码
             * @param s 失败的错误信息
             */
            @Override public void onError(final int i, final String s) {
                progressDialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        // 弹出错误提示
                        if (s.equals(EaseConstant.ERROR_S_RECALL_TIME)) {
                            Toast.makeText(getActivity(), getString(R.string.recall_failed_max_time), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.recall_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override public void onProgress(int i, String s) {

            }
        });
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONTEXT_MENU) {
            switch (resultCode) {
                case ContextMenuActivity.RESULT_CODE_COPY: // copy
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, ((EMTextMessageBody) contextMenuMessage.getBody()).getMessage()));
                    break;
                case ContextMenuActivity.RESULT_CODE_DELETE: // delete
                    conversation.removeMessage(contextMenuMessage.getMsgId());
                    messageList.refresh();
                    break;

                case ContextMenuActivity.RESULT_CODE_FORWARD: // forward
                    Intent intent = new Intent(getActivity(), ForwardMessageActivity.class);
                    intent.putExtra("forward_msg_id", contextMenuMessage.getMsgId());
                    startActivity(intent);

                    break;

                case ContextMenuActivity.RESULT_CODE_SEND_LOCATION:
                    startActivityForResult(new Intent(getActivity(), EaseBaiduMapActivity.class), REQUEST_CODE_MAP);
                    break;

                case ContextMenuActivity.RESULT_CODE_REAL_TIME_LOCATION:

                    EMMessage message = EMMessage.createTxtSendMessage("发起了位置共享", toChatUsername);
                    message.setAttribute("shareLocation", true);
                    sendMessage(message);

                    startActivityForResult(new Intent(getActivity(), EaseBaiduMapActivity.class).putExtra("realtimelocation", true)
                            .putExtra("direct", 0)
                            .putExtra("username", toChatUsername).putExtra("chattype",message.getChatType()),REQUEST_CODE_CLOSE_MAP);
                    break;

                case ContextMenuActivity.RESULT_CODE_RECALL: // recall
                    recallMessage(contextMenuMessage);
                    break;

                default:
                    break;
            }
        }
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_VIDEO: //send the video
                    if (data != null) {
                        int duration = data.getIntExtra("dur", 0);
                        String videoPath = data.getStringExtra("path");
                        File file = new File(PathUtil.getInstance().getImagePath(), "thvideo" + System.currentTimeMillis());
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            Bitmap ThumbBitmap = ThumbnailUtils.createVideoThumbnail(videoPath, 3);
                            ThumbBitmap.compress(CompressFormat.JPEG, 100, fos);
                            fos.close();
                            sendVideoMessage(videoPath, file.getAbsolutePath(), duration);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case REQUEST_CODE_SELECT_FILE: //send the file
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            sendFileByUri(uri);
                        }
                    }
                    break;
                case REQUEST_CODE_SELECT_AT_USER:
                    if (data != null) {
                        String username = data.getStringExtra("username");
                        inputAtUsername(username, false);
                    }
                    break;
                //red packet code : 发送红包消息到聊天界面
                case REQUEST_CODE_SEND_RED_PACKET:
                    if (data != null) {
                        sendMessage(RedPacketUtil.createRPMessage(getActivity(), data, toChatUsername));
                    }
                    break;
                case REQUEST_CODE_SEND_TRANSFER_PACKET://发送转账消息
                    if (data != null) {
                        sendMessage(RedPacketUtil.createTRMessage(getActivity(), data, toChatUsername));
                    }
                    break;
                case REQUEST_CODE_CLOSE_MAP:

                    EMMessage message = EMMessage.createTxtSendMessage("停止了位置共享", toChatUsername);
                    message.setAttribute("shareLocation", false);
                    sendMessage(message);
                    break;
                //end of red packet code
                default:
                    break;
            }
        }
    }

    /**
     * 启动阅后即焚模式
     */
    private void setupBurn(){
        if (!isBurn) {
            isBurn = true;
            titleBar.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
            Toast.makeText(getActivity(), R.string.attach_burn, Toast.LENGTH_SHORT).show();
        }else{
            isBurn = false;
            Toast.makeText(getActivity(), R.string.attach_burn_cancel, Toast.LENGTH_SHORT).show();
            titleBar.setBackgroundColor(getResources().getColor(R.color.top_bar_normal_bg));
        }
    }

    @Override public void onSetMessageAttributes(EMMessage message) {
        if (isRobot) {
            //set message extension
            message.setAttribute("em_robot_message", isRobot);
        }
        if (isBurn) {
            message.setAttribute(EaseConstant.MESSAGE_ATTR_BURN, isBurn);
        }
    }

    @Override public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
        return new CustomChatRowProvider();
    }

    @Override public void onEnterToChatDetails() {
        if (chatType == Constant.CHATTYPE_GROUP) {
            EMGroup group = EMClient.getInstance().groupManager().getGroup(toChatUsername);
            if (group == null) {
                Toast.makeText(getActivity(), R.string.gorup_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult((new Intent(getActivity(), GroupDetailsActivity.class).putExtra("groupId", toChatUsername)),
                    REQUEST_CODE_GROUP_DETAIL);
        } else if (chatType == Constant.CHATTYPE_CHATROOM) {
            startActivityForResult(new Intent(getActivity(), ChatRoomDetailsActivity.class).putExtra("roomId", toChatUsername),
                    REQUEST_CODE_GROUP_DETAIL);
        }
    }

    @Override public void onAvatarClick(String username) {
        //handling when user click avatar
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    @Override public void onAvatarLongClick(String username) {
        inputAtUsername(username);
    }

    @Override public boolean onMessageBubbleClick(EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        //red packet code : 拆红包页面
        if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)) {
            if (RedPacketUtil.isRandomRedPacket(message)) {
                RedPacketUtil.openRandomPacket(getActivity(), message);
            } else {
                RedPacketUtil.openRedPacket(getActivity(), chatType, message, toChatUsername, messageList);
            }
            return true;
        } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
            RedPacketUtil.openTransferPacket(getActivity(), message);
            return true;
        } else if (message.getBooleanAttribute("shareLocation", false)) {

            int direct = message.direct() != EMMessage.Direct.RECEIVE ? 0 : 1;

            startActivityForResult(new Intent(getActivity(), EaseBaiduMapActivity.class).putExtra("realtimelocation", true)
                    .putExtra("direct", direct)
                    .putExtra("username", toChatUsername).putExtra("chattype",message.getChatType()), REQUEST_CODE_CLOSE_MAP);
        }
        //end of red packet code
        return false;
    }

    @Override public void onCmdMessageReceived(List<EMMessage> messages) {
        //red packet code : 处理红包回执透传消息
        for (EMMessage message : messages) {
            EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
            String action = cmdMsgBody.action();//获取自定义action
            if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)) {
                RedPacketUtil.receiveRedPacketAckMessage(message);
                messageList.refresh();
            } else if (action.equals("shareLocation")) {

                if (EaseBaiduMapActivity.instance != null) {
                    Message msg = EaseBaiduMapActivity.instance.handler.obtainMessage();
                    msg.obj = message;
                    msg.sendToTarget();
                }
            } else if (action.equals(EaseConstant.REVOKE_FLAG)) { // 判断是不是撤回消息的透传
                    // 收到透传的CMD消息后，调用撤回消息方法进行处理
                    boolean result = EaseMessageUtils.receiveRecallMessage(message);
                    // 撤回消息之后，判断是否当前聊天界面，用来刷新界面

                String conversationId = "";
                if(message.getChatType() == EMMessage.ChatType.Chat){
                    conversationId = message.getFrom();
                }else if(message.getChatType() == EMMessage.ChatType.GroupChat){
                    conversationId = message.getTo();
                }
                if (toChatUsername.equals(conversationId) && result) {
                    messageList.refresh();
                }

            } else if (action.equals(EaseConstant.MESSAGE_ATTR_BURN_ACTION)) {
                EaseMessageUtils.receiveBurnCMDMessage(message);
                messageList.refresh();
            }
        }
        //end of red packet code
        super.onCmdMessageReceived(messages);
    }

    @Override public void onMessageBubbleLongClick(EMMessage message) {
        // no message forward when in chat room
        startActivityForResult((new Intent(getActivity(), ContextMenuActivity.class)).putExtra("message", message)
                .putExtra("ischatroom", chatType == EaseConstant.CHATTYPE_CHATROOM), REQUEST_CODE_CONTEXT_MENU);
    }

    @Override public boolean onExtendMenuItemClick(int itemId, View view) {
        switch (itemId) {
            case ITEM_LOCATION:
                startActivityForResult(new Intent(getActivity(), ContextMenuActivity.class), REQUEST_CODE_CONTEXT_MENU);
                break;
            case ITEM_VIDEO:
                if (isBurn) {
                    Toast.makeText(getActivity(), "阅后即焚模式，无法进行此操作", Toast.LENGTH_LONG).show();
                    break;
                }
                Intent intent = new Intent(getActivity(), ImageGridActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
                break;
            case ITEM_FILE: //file
                if (isBurn) {
                    Toast.makeText(getActivity(), "阅后即焚模式，无法进行此操作", Toast.LENGTH_LONG).show();
                    break;
                }
                selectFileFromLocal();
                break;
            case ITEM_VOICE_CALL:
                startVoiceCall();
                break;
            case ITEM_VIDEO_CALL:
                startVideoCall();
                break;
            case ITEM_BURN:
                // 阅后即焚
                setupBurn();
                break;
            //red packet code : 进入发红包页面
            case ITEM_RED_PACKET:
                if (isBurn) {
                    Toast.makeText(getActivity(), "阅后即焚模式，无法进行此操作", Toast.LENGTH_LONG).show();
                    break;
                }
                if (chatType == Constant.CHATTYPE_SINGLE) {
                    //单聊红包修改进入红包的方法，可以在小额随机红包和普通单聊红包之间切换
                    RedPacketUtil.startRandomPacket(new RPRedPacketUtil.RPRandomCallback() {
                        @Override public void onSendPacketSuccess(Intent data) {
                            sendMessage(RedPacketUtil.createRPMessage(getActivity(), data, toChatUsername));
                        }

                        @Override public void switchToNormalPacket() {
                            RedPacketUtil.startRedPacketActivityForResult(ChatFragment.this, chatType, toChatUsername, REQUEST_CODE_SEND_RED_PACKET);
                        }
                    }, getActivity(), toChatUsername);
                } else {
                    RedPacketUtil.startRedPacketActivityForResult(this, chatType, toChatUsername, REQUEST_CODE_SEND_RED_PACKET);
                }
                break;
            case ITEM_TRANSFER_PACKET://进入转账页面
                if (isBurn) {
                    Toast.makeText(getActivity(), "阅后即焚模式，无法进行此操作", Toast.LENGTH_LONG).show();
                    break;
                }
                RedPacketUtil.startTransferActivityForResult(this, toChatUsername, REQUEST_CODE_SEND_TRANSFER_PACKET);
                break;
            //end of red packet code
            default:
                break;
        }
        //keep exist extend menu
        return false;
    }

    /**
     * select file
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) { //api 19 and later, we can't use this way, demo just select from images
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    /**
     * make a voice call
     */
    protected void startVoiceCall() {
        if (!EMClient.getInstance().isConnected()) {
            Toast.makeText(getActivity(), R.string.not_connect_to_server, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(getActivity(), VoiceCallActivity.class).putExtra("username", toChatUsername).putExtra("isComingCall", false));
            // voiceCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    /**
     * make a video call
     */
    protected void startVideoCall() {
        if (!EMClient.getInstance().isConnected()) {
            Toast.makeText(getActivity(), R.string.not_connect_to_server, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(getActivity(), VideoCallActivity.class).putExtra("username", toChatUsername).putExtra("isComingCall", false));
            // videoCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    /**
     * chat row provider
     */
    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override public int getCustomChatRowTypeCount() {
            //here the number is the message type in EMMessage::Type
            //which is used to count the number of different chat row
            return 12;
        }

        @Override public int getCustomChatRowType(EMMessage message) {
            if (message.getType() == EMMessage.Type.TXT) {
                //voice call
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)) {
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VOICE_CALL : MESSAGE_TYPE_SENT_VOICE_CALL;
                } else if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)) {
                    //video call
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VIDEO_CALL : MESSAGE_TYPE_SENT_VIDEO_CALL;
                }
                //red packet code : 红包消息、红包回执消息以及转账消息的chatrow type
                else if (RedPacketUtil.isRandomRedPacket(message)) {
                    //小额随机红包
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_RANDOM : MESSAGE_TYPE_SEND_RANDOM;
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)) {
                    //发送红包消息
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_RED_PACKET : MESSAGE_TYPE_SEND_RED_PACKET;
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, false)) {
                    //领取红包消息
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_RED_PACKET_ACK : MESSAGE_TYPE_SEND_RED_PACKET_ACK;
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {
                    //转账消息
                    return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_TRANSFER_PACKET : MESSAGE_TYPE_SEND_TRANSFER_PACKET;
                }
                //end of red packet code
            }
            return 0;
        }

        @Override public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            if (message.getType() == EMMessage.Type.TXT) {
                // voice call or video call
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false) || message.getBooleanAttribute(
                        Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)) {
                    return new ChatRowVoiceCall(getActivity(), message, position, adapter);
                }
                //red packet code : 红包消息、红包回执消息以及转账消息的chat row
                else if (RedPacketUtil.isRandomRedPacket(message)) {//小额随机红包
                    return new ChatRowRandomPacket(getActivity(), message, position, adapter);
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)) {//红包消息
                    return new ChatRowRedPacket(getActivity(), message, position, adapter);
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, false)) {//红包回执消息
                    return new ChatRowRedPacketAck(getActivity(), message, position, adapter);
                } else if (message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_TRANSFER_PACKET_MESSAGE, false)) {//转账消息
                    return new ChatRowTransfer(getActivity(), message, position, adapter);
                }
                //end of red packet code
            }
            return null;
        }
    }
}

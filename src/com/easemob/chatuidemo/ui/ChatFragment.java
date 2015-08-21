package com.easemob.chatuidemo.ui;

import java.util.Map;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.TextMessageBody;
import com.easemob.chat.EMMessage.Type;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoSDKHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.domain.RobotUser;
import com.easemob.easeui.controller.EaseSDKHelper;
import com.easemob.easeui.ui.EaseChatFragment;

public class ChatFragment extends EaseChatFragment{
    //避免和基类定义的常量可能发生的冲突，常量从11开始定义
    private static final int ITEM_VIDEO = 11;
    static final int ITEM_FILE = 12;
    static final int ITEM_VOICE_CALL = 13;
    static final int ITEM_VIDEO_CALL = 14;
    
    private static final int REQUEST_CODE_SELECT_VIDEO = 11;
    private static final int REQUEST_CODE_SELECT_FILE = 12;
    private static final int REQUEST_CODE_GROUP_DETAIL = 13;
    private static final int REQUEST_CODE_CONTEXT_MENU = 14;
    
    
    
    /**
     * 是否为环信小助手
     */
    protected boolean isRobot;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void setUpView() {
        super.setUpView();
        if (chatType == Constant.CHATTYPE_SINGLE) { 
            Map<String,RobotUser> robotMap = ((DemoSDKHelper)EaseSDKHelper.getInstance()).getRobotList();
            if(robotMap!=null && robotMap.containsKey(toChatUsername)){
                isRobot = true;
            }
        }
    }
    
    @Override
    protected void registerExtendMenuItem() {
        //demo这里不覆盖基类已经注册的item,item点击listener沿用基类的
        super.registerExtendMenuItem();
        //增加扩展item
        inputMenu.registerExtendMenuItem(R.string.attach_video, R.drawable.em_chat_video_selector, ITEM_VIDEO, extendMenuItemClickListener);
        inputMenu.registerExtendMenuItem(R.string.attach_file, R.drawable.em_chat_file_selector, ITEM_FILE, extendMenuItemClickListener);
        if(chatType == Constant.CHATTYPE_SINGLE){
            inputMenu.registerExtendMenuItem(R.string.attach_voice_call, R.drawable.em_chat_voice_call_selector, ITEM_VOICE_CALL, extendMenuItemClickListener);
            inputMenu.registerExtendMenuItem(R.string.attach_video_call, R.drawable.em_chat_video_call_selector, ITEM_VIDEO_CALL, extendMenuItemClickListener);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONTEXT_MENU) {
            switch (resultCode) {
            case ContextMenuActivity.RESULT_CODE_COPY: // 复制消息
                clipboard.setText(((TextMessageBody) contextMenuMessage.getBody()).getMessage());
                break;
            case ContextMenuActivity.RESULT_CODE_DELETE: // 删除消息|
                conversation.removeMessage(contextMenuMessage.getMsgId());
                messageList.refresh();
                break;

            case ContextMenuActivity.RESULT_CODE_FORWARD: // 转发消息
                Intent intent = new Intent(getActivity(), ForwardMessageActivity.class);
                intent.putExtra("forward_msg_id", contextMenuMessage.getMsgId());
                startActivity(intent);
                
                break;

            default:
                break;
            }
        }
        
    }
    
    @Override
    protected void onSetMessageAttributes(Map<String, Object> attrsMap, Type type) {
        super.onSetMessageAttributes(attrsMap, type);
        //设置消息扩展属性
        attrsMap.put("em_robot_message", isRobot);
    }
  

    @Override
    protected void onEnterToChatDetails() {
        super.onEnterToChatDetails();
        if (chatType == Constant.CHATTYPE_GROUP) {
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            if (group == null) {
                Toast.makeText(getActivity(), R.string.gorup_not_found, 0).show();
                return;
            }
            startActivityForResult(
                    (new Intent(getActivity(), GroupDetailsActivity.class).putExtra("groupId", toChatUsername)),
                    REQUEST_CODE_GROUP_DETAIL);
        }
    }

    @Override
    protected void onAvatarClick(String username) {
        super.onAvatarClick(username);
        //头像点击事件
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }
    
    @Override
    protected boolean onMessageBubbleClick(EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        return super.onMessageBubbleClick(message);
    }

    @Override
    protected void onMessageBubbleLongClick(EMMessage message) {
        super.onMessageBubbleLongClick(message);
        //消息框长按
        startActivityForResult((new Intent(getActivity(), ContextMenuActivity.class)).putExtra("message",message),
                REQUEST_CODE_CONTEXT_MENU);
    }

    @Override
    protected void onExtendMenuItemClick(int itemId, View view) {
        super.onExtendMenuItemClick(itemId, view);
        switch (itemId) {
        case ITEM_VIDEO: //视频
            Intent intent = new Intent(getActivity(), ImageGridActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
            break;
        case ITEM_FILE: //一般文件
            //demo这里是通过系统api选择文件，实际app中最好是做成qq那种选择发送文件
            selectFileFromLocal();
            break;
        case ITEM_VOICE_CALL: //音频通话
            startVoiceCall();
            break;
        case ITEM_VIDEO_CALL: //视频通话
            startVideoCall();
            break;

        default:
            break;
        }
    }
    
    /**
     * 选择文件
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }
    
    /**
     * 拨打语音电话
     */
    protected void startVoiceCall() {
        if (!EMChatManager.getInstance().isConnected()) {
            Toast.makeText(getActivity(), R.string.not_connect_to_server, 0).show();
        } else {
            startActivity(new Intent(getActivity(), VoiceCallActivity.class).putExtra("username", toChatUsername)
                    .putExtra("isComingCall", false));
            // voiceCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }
    
    /**
     * 拨打视频电话
     */
    protected void startVideoCall() {
        if (!EMChatManager.getInstance().isConnected())
            Toast.makeText(getActivity(), R.string.not_connect_to_server, 0).show();
        else {
            startActivity(new Intent(getActivity(), VideoCallActivity.class).putExtra("username", toChatUsername)
                    .putExtra("isComingCall", false));
            // videoCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }
    
}

package com.easemob.chatuidemo.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.TextMessageBody;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.domain.EmojiconExampleGroupData;
import com.easemob.chatuidemo.domain.RobotUser;
import com.easemob.chatuidemo.widget.ChatRowVoiceCall;
import com.easemob.easeui.EaseConstant;
import com.easemob.easeui.ui.EaseChatFragment;
import com.easemob.easeui.ui.EaseChatFragment.EaseChatFragmentHelper;
import com.easemob.easeui.utils.EaseCommonUtils;
import com.easemob.easeui.widget.chatrow.EaseChatRow;
import com.easemob.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.easemob.easeui.widget.emojicon.EaseEmojiconMenu;
import com.easemob.util.PathUtil;

public class ChatFragment extends EaseChatFragment implements EaseChatFragmentHelper{

    //避免和基类定义的常量可能发生的冲突，常量从11开始定义
    private static final int ITEM_VIDEO = 11;
    private static final int ITEM_FILE = 12;
    private static final int ITEM_VOICE_CALL = 13;
    private static final int ITEM_VIDEO_CALL = 14;
    private static final int ITEM_READFIRE = 15;
    
    private static final int REQUEST_CODE_SELECT_VIDEO = 11;
    private static final int REQUEST_CODE_SELECT_FILE = 12;
    private static final int REQUEST_CODE_GROUP_DETAIL = 13;
    private static final int REQUEST_CODE_CONTEXT_MENU = 14;
    
    private static final int MESSAGE_TYPE_SENT_VOICE_CALL = 1;
    private static final int MESSAGE_TYPE_RECV_VOICE_CALL = 2;
    private static final int MESSAGE_TYPE_SENT_VIDEO_CALL = 3; 
    private static final int MESSAGE_TYPE_RECV_VIDEO_CALL = 4;
     
    /**
     * 是否为环信小助手
     */
    private boolean isRobot;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void setUpView() {
        setChatFragmentHelper(this);
        if (chatType == Constant.CHATTYPE_SINGLE) { 
            Map<String,RobotUser> robotMap = DemoHelper.getInstance().getRobotList();
            if(robotMap!=null && robotMap.containsKey(toChatUsername)){
                isRobot = true;
            }
        }
        super.setUpView();
        ((EaseEmojiconMenu)inputMenu.getEmojiconMenu()).addEmojiconGroup(EmojiconExampleGroupData.getData());
        
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
            // 阅后即焚开关菜单
            inputMenu.registerExtendMenuItem(R.string.attach_read_fire, R.drawable.ease_read_fire, ITEM_READFIRE, extendMenuItemClickListener);
        }
        
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("lzan13", "ChatFragment - onActivityResult - "+requestCode);
        if (requestCode == REQUEST_CODE_CONTEXT_MENU) {
            switch (resultCode) {
            case ContextMenuActivity.RESULT_CODE_COPY: // 复制消息
                clipboard.setText(((TextMessageBody) contextMenuMessage.getBody()).getMessage());
                break;
            case ContextMenuActivity.RESULT_CODE_DELETE: // 删除消息
                conversation.removeMessage(contextMenuMessage.getMsgId());
                refreshUI();
                break;

            case ContextMenuActivity.RESULT_CODE_FORWARD: // 转发消息
                if(chatType == EaseConstant.CHATTYPE_CHATROOM){
                    Toast.makeText(getActivity(), R.string.chatroom_not_support_forward, 1).show();
                    return;
                }
                Intent intent = new Intent(getActivity(), ForwardMessageActivity.class);
                intent.putExtra("forward_msg_id", contextMenuMessage.getMsgId());
                startActivity(intent);
                
                break;
            case ContextMenuActivity.RESULT_CODE_REVOKE:
            	// 显示撤回消息操作的 dialog
                final ProgressDialog pd = new ProgressDialog(getActivity());
                pd.setMessage(getString(R.string.revoking));
                pd.show();
                EaseCommonUtils.sendRevokeMessage(getActivity(), contextMenuMessage, new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        pd.dismiss();
                        refreshUI();
                    }

                    @Override
                    public void onError(final int i, final String s) {
                        pd.dismiss();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (s.equals("maxtime")) {
                                    Toast.makeText(getActivity(), R.string.revoke_error_maxtime, Toast.LENGTH_LONG).show();
                                } else {
                                	Toast.makeText(getActivity(), getString(R.string.revoke_error) + i + " " + s + "", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
            	break;
            default:
                break;
            }
        }
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
            case REQUEST_CODE_SELECT_VIDEO: //发送选中的视频
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
            case REQUEST_CODE_SELECT_FILE: //发送选中的文件
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        sendFileByUri(uri);
                    }
                }
                break;
            default:
                break;
            }
        }
        
    }
    
    /**
     * 刷新UI界面
     */
    public void refreshUI(){
        messageList.refresh();
    }

    /**
     * 为消息对象添加扩展字段
     * params message   需要处理的消息对象
     */
    @Override
    public void onSetMessageAttributes(EMMessage message) {
        if(isRobot){
            //设置消息扩展属性
            message.setAttribute("em_robot_message", isRobot);
        }
        // 根据当前状态是否是阅后即焚状态来设置发送消息的扩展
        if(isReadFire && (message.getType() == EMMessage.Type.TXT
                || message.getType() == EMMessage.Type.IMAGE
                || message.getType() == EMMessage.Type.VOICE)){
            message.setAttribute(EaseConstant.EASE_ATTR_READFIRE, true);
        }
        
    }
    
    @Override
    public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
        //设置自定义listview item提供者
        return new CustomChatRowProvider();
    }
  
    @Override
    public void onEnterToChatDetails() {
        if (chatType == Constant.CHATTYPE_GROUP) {
            EMGroup group = EMGroupManager.getInstance().getGroup(toChatUsername);
            if (group == null) {
                Toast.makeText(getActivity(), R.string.gorup_not_found, 0).show();
                return;
            }
            startActivityForResult(
                    (new Intent(getActivity(), GroupDetailsActivity.class).putExtra("groupId", toChatUsername)),
                    REQUEST_CODE_GROUP_DETAIL);
        }else if(chatType == Constant.CHATTYPE_CHATROOM){
        	startActivityForResult(new Intent(getActivity(), ChatRoomDetailsActivity.class).putExtra("roomId", toChatUsername), REQUEST_CODE_GROUP_DETAIL);
        }
    }

    @Override
    public void onAvatarClick(String username) {
        //头像点击事件
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }
    
    @Override
    public boolean onMessageBubbleClick(EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        return false;
    }

    @Override
    public void onMessageBubbleLongClick(EMMessage message) {
        //消息框长按
        startActivityForResult((new Intent(getActivity(), ContextMenuActivity.class)).putExtra("message",message),
                REQUEST_CODE_CONTEXT_MENU);
    }

    @Override
    public boolean onExtendMenuItemClick(int itemId, View view) {
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
        case ITEM_READFIRE:
            setReadFire(true);
            break;
        default:
            break;
        }
        //不覆盖已有的点击事件
        return false;
    }
    /**
     * 选择文件
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) { //19以后这个api不可用，demo这里简单处理成图库选择图片
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
    
    /**
     * chat row provider 
     *
     */
    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override
        public int getCustomChatRowTypeCount() {
            //音、视频通话发送、接收共4种
            return 4;
        }

        @Override
        public int getCustomChatRowType(EMMessage message) {
            if(message.getType() == EMMessage.Type.TXT){
                //语音通话类型
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)){
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VOICE_CALL : MESSAGE_TYPE_SENT_VOICE_CALL;
                }else if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)){
                    //视频通话
                    return message.direct == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_VIDEO_CALL : MESSAGE_TYPE_SENT_VIDEO_CALL;
                }
            }
            return 0;
        }

        @Override
        public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            if(message.getType() == EMMessage.Type.TXT){
                // 语音通话,  视频通话
                if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false) ||
                    message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)){
                    return new ChatRowVoiceCall(getActivity(), message, position, adapter);
                }
            }
            return null;
        }
    }
    
}

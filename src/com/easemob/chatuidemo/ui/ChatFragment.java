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
import com.easemob.easeui.ui.EaseChatFragment.EaseChatFragmentListener;
import com.easemob.easeui.utils.EaseCommonUtils;
import com.easemob.easeui.widget.chatrow.EaseChatRow;
import com.easemob.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.easemob.easeui.widget.emojicon.EaseEmojiconMenu;
import com.easemob.util.PathUtil;

public class ChatFragment extends EaseChatFragment implements EaseChatFragmentListener{

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
    // 跳转到At用户选择列表
    private static final int REQUEST_CODE_AT_MEMBER= 15;
    
    private static final int MESSAGE_TYPE_SENT_VOICE_CALL = 1;
    private static final int MESSAGE_TYPE_RECV_VOICE_CALL = 2;
    private static final int MESSAGE_TYPE_SENT_VIDEO_CALL = 3; 
    private static final int MESSAGE_TYPE_RECV_VIDEO_CALL = 4;
     
    /**
     * 是否为环信小助手
     */
    private boolean isRobot;
    
    // 临时保存要 @ 的群成员
    private List<String> atMembers;
    private EditText mMessageEditText;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void setUpView() {
        setChatFragmentListener(this);
        if (chatType == Constant.CHATTYPE_SINGLE) { 
            Map<String,RobotUser> robotMap = DemoHelper.getInstance().getRobotList();
            if(robotMap!=null && robotMap.containsKey(toChatUsername)){
                isRobot = true;
            }
        }
        super.setUpView();
        ((EaseEmojiconMenu)inputMenu.getEmojiconMenu()).addEmojiconGroup(EmojiconExampleGroupData.getData());
        
        // 判断下当前聊天模式，只有为群聊才检测输入框内容
        if(chatType == Constant.CHATTYPE_GROUP){
            atMembers = new ArrayList<String>();
            mMessageEditText = (EditText) getView().findViewById(R.id.et_sendmessage);
            // 设置输入框的内容变化简体呢
            setEditTextContentListener();
            // 设置输入框点击监听，主要是为了设置光标位置
            setEditTextOnClickListener();
            // 设置输入框按键监听，主要为了监听删除按键
            setEditTextOnKeyListener();
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
            case REQUEST_CODE_AT_MEMBER: // 选择要 @ 的群成员加入到list中并设置特殊状态显示
                String username = data.getStringExtra(EaseConstant.EXTRA_USER_ID);
                atMembers.add(username);
                // 获取光标位置
                int index = mMessageEditText.getSelectionStart();
                // 获取输入框内容，然后根据光标位置将 @成员插入到输入框
                Editable editable = mMessageEditText.getEditableText();
                if (index < 0 || index >= editable.length()){
                    editable.append(username + " ");
                } else {
                    editable.insert(index, username + " ");
                }
                // 将被@成员的名字用蓝色高亮表示
                Spannable span = new SpannableString(mMessageEditText.getText());
                // 设置被@成员名字颜色
//                span.setSpan(new ForegroundColorSpan(getActivity().getResources().getColor(R.color.holo_blue_bright)), 
//                        index - 1, 
//                        index + username.length(), 
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // 设置被@群成员名字背景颜色
                span.setSpan(new BackgroundColorSpan(getActivity().getResources().getColor(R.color.holo_blue_bright)),
                        index - 1,
                        index + username.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mMessageEditText.setText(span);
                // 设置当前光标在输入框最后
                mMessageEditText.setSelection(span.length());
                
                break;
            default:
                break;
            }
        }
        
    }
    
    /**
     * 设置输入框内容的监听 在调用此方法的地方要加一个判断，只有当聊天为群聊时才需要监视
     */
    private void setEditTextContentListener(){
        mMessageEditText.addTextChangedListener(new TextWatcher() {

            /**
             * 输入框内容改变之前
             * params s         输入框内容改变前的内容
             * params start     输入框内容开始变化的索引位置，从0开始计数
             * params count     输入框内容将要减少的变化的字符数
             * params after     输入框内容将要增加的文本的长度，
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("melove", String.format("beforeTextChanged s-%s, start-%d, count-%d, after-%d", s, start, count, after));
            }
            
            /**
             * 输入框内容改变
             * params s         输入框内容改变后的内容
             * params start     输入框内容开始变化的索引位置，从0开始计数
             * params before    输入框内容减少的文本的长度
             * params count     输入框内容增加的字符数量
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("melove", String.format("onTextChanged s-%s, start-%d, before-%d, count-%d", s, start, before, count));
                // 当新增内容长度为1时采取判断增加的字符是否为@符号
                if(count == 1){
                    String str = String.valueOf(s.charAt(start));
                    if(str.equals("@")){
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), PickAtMemberActivity.class);
                        // 这里将群组id传递过去，为了在@用户列表选择界面显示群成员
                        intent.putExtra("groupId", conversation.getUserName());
                        // 跳转Activity 并定义请求码为@类型
                        startActivityForResult(intent, REQUEST_CODE_AT_MEMBER);
                    }
                }
            }
            
            /**
             * 输入框内容改变之后
             * params s 输入框最终的内容
             */
            @Override
            public void afterTextChanged(Editable s) {
                Log.d("melove", "afterTextChanged s-" + s);
            }
        });
    }
    
    /**
     * 设置聊天内容输入框的点击监听，主要是为了判断 在输入内容有@ 成员的情况下光标的设置
     */
    private void setEditTextOnClickListener(){
        mMessageEditText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取当前点击后的光标位置
                int selectionStart = ((EditText) v).getSelectionStart();
                int position = 0;
                String tempContent = mMessageEditText.getText().toString();
                for(int i=0; i<atMembers.size(); i++){
                    // 从当前位置开始搜索被@的成员的位置
                    position = tempContent.indexOf(atMembers.get(i), position);
                    // 判断当前点击处是否在被 @用户的中间
                    if(position != -1
                            && selectionStart > position - 1 
                            && selectionStart <= (position + atMembers.get(i).length())){
                        // 设置光标位置为被@的成员的末尾
                        mMessageEditText.setSelection(position + atMembers.get(i).length() + 1);
                    }else{
                        // 如果当前点击的被@成员不是当前搜索到的，设置搜索位置为当前成员后，继续搜索下一个
                        position += atMembers.get(i).length();
                    }
                }
            }
        });
    }
    
    /**
     * 设置聊天输入框键盘按键监听，主要是为了监听在输入内容有@某成员的情况下，点击了删除按钮的判断
     */
    private void setEditTextOnKeyListener(){
        mMessageEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN){
                    // 获取当前光标位置
                    int selectionStart = mMessageEditText.getSelectionStart();
                    int position = 0;
                    String tempContent = mMessageEditText.getText().toString();
                    for(int i=0; i<atMembers.size(); i++){
                        // 搜索被@成员在输入框的位置
                        position = tempContent.indexOf(atMembers.get(i), position);
                        // 判断点击删除按键时光标是否正好在被 @用户的末尾
                        if(position != -1 
                                && selectionStart > position 
                                && selectionStart <= (position + atMembers.get(i).length() + 1)){
                            // 删除输入框内被@ 的成员 
                            Editable editable = mMessageEditText.getText();
                            editable.delete(position - 1, position + atMembers.get(i).length());
                            // 同时删除集合中保存的群成员
                            atMembers.remove(i);
                            return true;
                        }else{
                            // 如果当前搜索到的群成员不符合删除监听的条件，则设置搜索位置为当前成员后，继续搜索下一个
                            position += atMembers.get(i).length();
                        }
                    }
                }
                return false;
            }
        });
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
        if(isReadFire){
        	message.setAttribute(EaseConstant.EASE_ATTR_READFIRE, true);
        }
        // 判断是否存在需要 @ 的群成员
        if(atMembers != null && atMembers.size() > 0){
            /*
             * -------------------------------------------------------
             * "ext":{
             *   "ease_group_at_members": ["lz0", "lz1"] // 这里表示@ 类型的扩展
             * }
             */
            JSONArray atJson = new JSONArray(atMembers);
            // 设置消息的扩展为@群成员类型
            message.setAttribute(EaseConstant.EASE_ATTR_GROUP_AT_MEMBERS, atJson);
            // 设置万扩展消息之后要清除atMembers，为下一次@别人做准备
            atMembers.clear();
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

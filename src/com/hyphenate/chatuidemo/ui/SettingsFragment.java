/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.chatuidemo.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.redpacketui.utils.RedPacketUtil;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chatuidemo.Constant;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.DemoModel;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.utils.PreferenceManager;
import com.hyphenate.easeui.widget.EaseSwitchButton;
import com.hyphenate.util.EMLog;

/**
 * settings screen
 * 
 * 
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class SettingsFragment extends Fragment implements OnClickListener {

	/**
	 * new message notification
	 */
	private RelativeLayout rl_switch_notification;
	/**
	 * sound
	 */
	private RelativeLayout rl_switch_sound;
	/**
	 * vibration
	 */
	private RelativeLayout rl_switch_vibrate;
	/**
	 * speaker
	 */
	private RelativeLayout rl_switch_speaker;


	/**
	 * line between sound and vibration
	 */
	private TextView textview1, textview2;

	private LinearLayout blacklistContainer;
	
	private LinearLayout userProfileContainer;
	
	/**
	 * logout
	 */
	private Button logoutBtn;

	private RelativeLayout rl_switch_chatroom_leave;
	
    private RelativeLayout rl_switch_delete_msg_when_exit_group;
    private RelativeLayout rl_switch_auto_accept_group_invitation;
    private RelativeLayout rl_switch_adaptive_video_encode;
	private RelativeLayout rl_custom_appkey;
    private RelativeLayout rl_custom_server;
	RelativeLayout rl_push_settings;
	private LinearLayout   ll_call_option;

	/**
	 * Diagnose
	 */
	private LinearLayout llDiagnose;
	/**
	 * display name for APNs
	 */
	private LinearLayout pushNick;
	
    private EaseSwitchButton notifySwitch;
    private EaseSwitchButton soundSwitch;
    private EaseSwitchButton vibrateSwitch;
    private EaseSwitchButton speakerSwitch;
    private EaseSwitchButton ownerLeaveSwitch;
    private EaseSwitchButton switch_delete_msg_when_exit_group;
    private EaseSwitchButton switch_auto_accept_group_invitation;
    private EaseSwitchButton switch_adaptive_video_encode;
	private EaseSwitchButton customServerSwitch;
	private EaseSwitchButton customAppkeySwitch;
    private DemoModel settingsModel;
    private EMOptions chatOptions;
	private EditText edit_custom_appkey;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.em_fragment_conversation_settings, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false))
            return;
		rl_switch_notification = (RelativeLayout) getView().findViewById(R.id.rl_switch_notification);
		rl_switch_sound = (RelativeLayout) getView().findViewById(R.id.rl_switch_sound);
		rl_switch_vibrate = (RelativeLayout) getView().findViewById(R.id.rl_switch_vibrate);
		rl_switch_speaker = (RelativeLayout) getView().findViewById(R.id.rl_switch_speaker);
		rl_switch_chatroom_leave = (RelativeLayout) getView().findViewById(R.id.rl_switch_chatroom_owner_leave);
		rl_switch_delete_msg_when_exit_group = (RelativeLayout) getView().findViewById(R.id.rl_switch_delete_msg_when_exit_group);
		rl_switch_auto_accept_group_invitation = (RelativeLayout) getView().findViewById(R.id.rl_switch_auto_accept_group_invitation);
		rl_switch_adaptive_video_encode = (RelativeLayout) getView().findViewById(R.id.rl_switch_adaptive_video_encode);
		rl_custom_appkey = (RelativeLayout) getView().findViewById(R.id.rl_custom_appkey);
		rl_custom_server = (RelativeLayout) getView().findViewById(R.id.rl_custom_server);
		rl_push_settings = (RelativeLayout) getView().findViewById(R.id.rl_push_settings);

		ll_call_option = (LinearLayout) getView().findViewById(R.id.ll_call_option);
		
		notifySwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_notification);
		soundSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_sound);
		vibrateSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_vibrate);
		speakerSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_speaker);
		ownerLeaveSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_owner_leave);
		switch_delete_msg_when_exit_group = (EaseSwitchButton) getView().findViewById(R.id.switch_delete_msg_when_exit_group);
		switch_auto_accept_group_invitation = (EaseSwitchButton) getView().findViewById(R.id.switch_auto_accept_group_invitation);
		switch_adaptive_video_encode = (EaseSwitchButton) getView().findViewById(R.id.switch_adaptive_video_encode);
		LinearLayout llChange = (LinearLayout) getView().findViewById(R.id.ll_change);
		logoutBtn = (Button) getView().findViewById(R.id.btn_logout);
		if(!TextUtils.isEmpty(EMClient.getInstance().getCurrentUser())){
			logoutBtn.setText(getString(R.string.button_logout) + "(" + EMClient.getInstance().getCurrentUser() + ")");
		}
		customServerSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_custom_server);
		customAppkeySwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_custom_appkey);

		textview1 = (TextView) getView().findViewById(R.id.textview1);
		textview2 = (TextView) getView().findViewById(R.id.textview2);
		
		blacklistContainer = (LinearLayout) getView().findViewById(R.id.ll_black_list);
		userProfileContainer = (LinearLayout) getView().findViewById(R.id.ll_user_profile);
		llDiagnose=(LinearLayout) getView().findViewById(R.id.ll_diagnose);
		pushNick=(LinearLayout) getView().findViewById(R.id.ll_set_push_nick);
		edit_custom_appkey = (EditText) getView().findViewById(R.id.edit_custom_appkey);

		settingsModel = DemoHelper.getInstance().getModel();
		chatOptions = EMClient.getInstance().getOptions();
		
		blacklistContainer.setOnClickListener(this);
		userProfileContainer.setOnClickListener(this);
		rl_switch_notification.setOnClickListener(this);
		rl_switch_sound.setOnClickListener(this);
		rl_switch_vibrate.setOnClickListener(this);
		rl_switch_speaker.setOnClickListener(this);
		customAppkeySwitch.setOnClickListener(this);
		customServerSwitch.setOnClickListener(this);
		rl_custom_server.setOnClickListener(this);
		logoutBtn.setOnClickListener(this);
		llDiagnose.setOnClickListener(this);
		pushNick.setOnClickListener(this);
		rl_switch_chatroom_leave.setOnClickListener(this);
		rl_switch_delete_msg_when_exit_group.setOnClickListener(this);
		rl_switch_auto_accept_group_invitation.setOnClickListener(this);
		rl_switch_adaptive_video_encode.setOnClickListener(this);
		rl_push_settings.setOnClickListener(this);
		ll_call_option.setOnClickListener(this);
		llChange.setOnClickListener(this);

		// the vibrate and sound notification are allowed or not?
		if (settingsModel.getSettingMsgNotification()) {
			notifySwitch.openSwitch();
		} else {
		    notifySwitch.closeSwitch();
		}

		// sound notification is switched on or not?
		if (settingsModel.getSettingMsgSound()) {
		    soundSwitch.openSwitch();
		} else {
		    soundSwitch.closeSwitch();
		}

		// vibrate notification is switched on or not?
		if (settingsModel.getSettingMsgVibrate()) {
		    vibrateSwitch.openSwitch();
		} else {
		    vibrateSwitch.closeSwitch();
		}

		// the speaker is switched on or not?
		if (settingsModel.getSettingMsgSpeaker()) {
		    speakerSwitch.openSwitch();
		} else {
		    speakerSwitch.closeSwitch();
		}

		// if allow owner leave
		if(settingsModel.isChatroomOwnerLeaveAllowed()){
		    ownerLeaveSwitch.openSwitch();
		}else{
		    ownerLeaveSwitch.closeSwitch();
		}
		
		// delete messages when exit group?
		if(settingsModel.isDeleteMessagesAsExitGroup()){
		    switch_delete_msg_when_exit_group.openSwitch();
		} else {
		    switch_delete_msg_when_exit_group.closeSwitch();
		}
		
		if (settingsModel.isAutoAcceptGroupInvitation()) {
		    switch_auto_accept_group_invitation.openSwitch();
		} else {
		    switch_auto_accept_group_invitation.closeSwitch();
		}
		
		if (settingsModel.isAdaptiveVideoEncode()) {
            switch_adaptive_video_encode.openSwitch();
			EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(false);
        } else {
            switch_adaptive_video_encode.closeSwitch();
			EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(true);
        }

		if(settingsModel.isCustomServerEnable()){
			customServerSwitch.openSwitch();
		}else{
			customServerSwitch.closeSwitch();
        }

		if (settingsModel.isCustomAppkeyEnabled()) {
			customAppkeySwitch.openSwitch();
		} else {
			customAppkeySwitch.closeSwitch();
		}
		edit_custom_appkey.setEnabled(settingsModel.isCustomAppkeyEnabled());

		edit_custom_appkey.setText(settingsModel.getCutomAppkey());
		edit_custom_appkey.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				PreferenceManager.getInstance().setCustomAppkey(s.toString());
			}
		});
	}

	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			//red packet code : 进入零钱页面
			case R.id.ll_change:
				RedPacketUtil.startChangeActivity(getActivity());
				break;
			//end of red packet code
			case R.id.rl_switch_notification:
				if (notifySwitch.isSwitchOpen()) {
					notifySwitch.closeSwitch();
					rl_switch_sound.setVisibility(View.GONE);
					rl_switch_vibrate.setVisibility(View.GONE);
					textview1.setVisibility(View.GONE);
					textview2.setVisibility(View.GONE);
					settingsModel.setSettingMsgNotification(false);
				} else {
					notifySwitch.openSwitch();
					rl_switch_sound.setVisibility(View.VISIBLE);
					rl_switch_vibrate.setVisibility(View.VISIBLE);
					textview1.setVisibility(View.VISIBLE);
					textview2.setVisibility(View.VISIBLE);
					settingsModel.setSettingMsgNotification(true);
				}
				break;
			case R.id.rl_switch_sound:
				if (soundSwitch.isSwitchOpen()) {
					soundSwitch.closeSwitch();
					settingsModel.setSettingMsgSound(false);
				} else {
					soundSwitch.openSwitch();
					settingsModel.setSettingMsgSound(true);
				}
				break;
			case R.id.rl_switch_vibrate:
				if (vibrateSwitch.isSwitchOpen()) {
					vibrateSwitch.closeSwitch();
					settingsModel.setSettingMsgVibrate(false);
				} else {
					vibrateSwitch.openSwitch();
					settingsModel.setSettingMsgVibrate(true);
				}
				break;
			case R.id.rl_switch_speaker:
				if (speakerSwitch.isSwitchOpen()) {
					speakerSwitch.closeSwitch();
					settingsModel.setSettingMsgSpeaker(false);
				} else {
					speakerSwitch.openSwitch();
					settingsModel.setSettingMsgVibrate(true);
				}
				break;
			case R.id.rl_switch_chatroom_owner_leave:
				if(ownerLeaveSwitch.isSwitchOpen()){
					ownerLeaveSwitch.closeSwitch();
					settingsModel.allowChatroomOwnerLeave(false);
					chatOptions.allowChatroomOwnerLeave(false);
				}else{
					ownerLeaveSwitch.openSwitch();
					settingsModel.allowChatroomOwnerLeave(true);
					chatOptions.allowChatroomOwnerLeave(true);
				}
				break;
			case R.id.rl_switch_delete_msg_when_exit_group:
				if(switch_delete_msg_when_exit_group.isSwitchOpen()){
					switch_delete_msg_when_exit_group.closeSwitch();
					settingsModel.setDeleteMessagesAsExitGroup(false);
					chatOptions.setDeleteMessagesAsExitGroup(false);
				}else{
					switch_delete_msg_when_exit_group.openSwitch();
					settingsModel.setDeleteMessagesAsExitGroup(true);
					chatOptions.setDeleteMessagesAsExitGroup(true);
				}
				break;
			case R.id.rl_switch_auto_accept_group_invitation:
				if(switch_auto_accept_group_invitation.isSwitchOpen()){
					switch_auto_accept_group_invitation.closeSwitch();
					settingsModel.setAutoAcceptGroupInvitation(false);
					chatOptions.setAutoAcceptGroupInvitation(false);
				}else{
					switch_auto_accept_group_invitation.openSwitch();
					settingsModel.setAutoAcceptGroupInvitation(true);
					chatOptions.setAutoAcceptGroupInvitation(true);
				}
				break;
			case R.id.rl_switch_adaptive_video_encode:
				EMLog.d("switch", "" + !switch_adaptive_video_encode.isSwitchOpen());
				if (switch_adaptive_video_encode.isSwitchOpen()){
					switch_adaptive_video_encode.closeSwitch();
					settingsModel.setAdaptiveVideoEncode(false);
					EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(true);

				}else{
					switch_adaptive_video_encode.openSwitch();
					settingsModel.setAdaptiveVideoEncode(true);
					EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(false);
				}
				break;
			case R.id.btn_logout:
				logout();
				break;
			case R.id.ll_black_list:
				startActivity(new Intent(getActivity(), BlacklistActivity.class));
				break;
			case R.id.ll_diagnose:
				startActivity(new Intent(getActivity(), DiagnoseActivity.class));
				break;
			case R.id.ll_set_push_nick:
				startActivity(new Intent(getActivity(), OfflinePushNickActivity.class));
				break;
			case R.id.ll_call_option:
				startActivity(new Intent(getActivity(), CallOptionActivity.class));
				break;
			case R.id.ll_user_profile:
				startActivity(new Intent(getActivity(), UserProfileActivity.class).putExtra("setting", true)
						.putExtra("username", EMClient.getInstance().getCurrentUser()));
				break;
			case R.id.switch_custom_server:
				if(customServerSwitch.isSwitchOpen()){
					customServerSwitch.closeSwitch();
					settingsModel.enableCustomServer(false);
				}else{
					customServerSwitch.openSwitch();
					settingsModel.enableCustomServer(true);
				}
				break;
			case R.id.switch_custom_appkey:
				if(customAppkeySwitch.isSwitchOpen()){
					customAppkeySwitch.closeSwitch();
					settingsModel.enableCustomAppkey(false);
				}else{
					customAppkeySwitch.openSwitch();
					settingsModel.enableCustomAppkey(true);
				}
				edit_custom_appkey.setEnabled(customAppkeySwitch.isSwitchOpen());
				break;
			case R.id.rl_custom_server:
				startActivity(new Intent(getActivity(), SetServersActivity.class));
				break;
			case R.id.rl_push_settings:
				startActivity(new Intent(getActivity(), OfflinePushSettingsActivity.class));
				break;
			default:
				break;
		}
		
	}

	void logout() {
		final ProgressDialog pd = new ProgressDialog(getActivity());
		String st = getResources().getString(R.string.Are_logged_out);
		pd.setMessage(st);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		DemoHelper.getInstance().logout(false,new EMCallBack() {
			
			@Override
			public void onSuccess() {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						pd.dismiss();
						// show login screen
						((MainActivity) getActivity()).finish();
						startActivity(new Intent(getActivity(), LoginActivity.class));
						
					}
				});
			}
			
			@Override
			public void onProgress(int progress, String status) {
				
			}
			
			@Override
			public void onError(int code, String message) {
				getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						pd.dismiss();
						Toast.makeText(getActivity(), "unbind devicetokens failed", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
        if(((MainActivity)getActivity()).isConflict){
        	outState.putBoolean("isConflict", true);
        }else if(((MainActivity)getActivity()).getCurrentAccountRemoved()){
        	outState.putBoolean(Constant.ACCOUNT_REMOVED, true);
        }
    }
}

/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
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
package com.easemob.chatuidemo.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMChatOptions;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHelper;
import com.easemob.chatuidemo.DemoModel;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.utils.PreferenceManager;
import com.easemob.easeui.widget.EaseSwitchButton;

/**
 * 设置界面
 * 
 * 
 */
public class SettingsFragment extends Fragment implements OnClickListener {

	/**
	 * 设置新消息通知布局
	 */
	private RelativeLayout rl_switch_notification;
	/**
	 * 设置声音布局
	 */
	private RelativeLayout rl_switch_sound;
	/**
	 * 设置震动布局
	 */
	private RelativeLayout rl_switch_vibrate;
	/**
	 * 设置扬声器布局
	 */
	private RelativeLayout rl_switch_speaker;


	/**
	 * 声音和震动中间的那条线
	 */
	private TextView textview1, textview2;

	private LinearLayout blacklistContainer;
	
	private LinearLayout userProfileContainer;
	
	/**
	 * 退出按钮
	 */
	private Button logoutBtn;

	private RelativeLayout rl_switch_chatroom_leave;
	
 
	/**
	 * 诊断
	 */
	private LinearLayout llDiagnose;
	/**
	 * iOS离线推送昵称
	 */
	private LinearLayout pushNick;
	
    private EaseSwitchButton notifiSwitch;
    private EaseSwitchButton soundSwitch;
    private EaseSwitchButton vibrateSwitch;
    private EaseSwitchButton speakerSwitch;
    private EaseSwitchButton ownerLeaveSwitch;
    private DemoModel settingsModel;
    private EMChatOptions chatOptions;
	
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

		
		notifiSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_notification);
		soundSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_sound);
		vibrateSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_vibrate);
		speakerSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_speaker);
		ownerLeaveSwitch = (EaseSwitchButton) getView().findViewById(R.id.switch_owner_leave);
		
		logoutBtn = (Button) getView().findViewById(R.id.btn_logout);
		if(!TextUtils.isEmpty(EMChatManager.getInstance().getCurrentUser())){
			logoutBtn.setText(getString(R.string.button_logout) + "(" + EMChatManager.getInstance().getCurrentUser() + ")");
		}

		textview1 = (TextView) getView().findViewById(R.id.textview1);
		textview2 = (TextView) getView().findViewById(R.id.textview2);
		
		blacklistContainer = (LinearLayout) getView().findViewById(R.id.ll_black_list);
		userProfileContainer = (LinearLayout) getView().findViewById(R.id.ll_user_profile);
		llDiagnose=(LinearLayout) getView().findViewById(R.id.ll_diagnose);
		pushNick=(LinearLayout) getView().findViewById(R.id.ll_set_push_nick);
		
		settingsModel = DemoHelper.getInstance().getModel();
		chatOptions = EMChatManager.getInstance().getChatOptions();
		
		blacklistContainer.setOnClickListener(this);
		userProfileContainer.setOnClickListener(this);
		rl_switch_notification.setOnClickListener(this);
		rl_switch_sound.setOnClickListener(this);
		rl_switch_vibrate.setOnClickListener(this);
		rl_switch_speaker.setOnClickListener(this);
		logoutBtn.setOnClickListener(this);
		llDiagnose.setOnClickListener(this);
		pushNick.setOnClickListener(this);
		rl_switch_chatroom_leave.setOnClickListener(this);
		
		
		// 震动和声音总开关，来消息时，是否允许此开关打开
		// the vibrate and sound notification are allowed or not?
		if (settingsModel.getSettingMsgNotification()) {
			notifiSwitch.openSwitch();
		} else {
		    notifiSwitch.closeSwitch();
		}
		
		// 是否打开声音
		// sound notification is switched on or not?
		if (settingsModel.getSettingMsgSound()) {
		    soundSwitch.openSwitch();
		} else {
		    soundSwitch.closeSwitch();
		}
		
		// 是否打开震动
		// vibrate notification is switched on or not?
		if (settingsModel.getSettingMsgVibrate()) {
		    vibrateSwitch.openSwitch();
		} else {
		    vibrateSwitch.closeSwitch();
		}

		// 是否打开扬声器
		// the speaker is switched on or not?
		if (settingsModel.getSettingMsgSpeaker()) {
		    speakerSwitch.openSwitch();
		} else {
		    speakerSwitch.closeSwitch();
		}

		// 是否允许聊天室owner leave
		if(settingsModel.isChatroomOwnerLeaveAllowed()){
		    ownerLeaveSwitch.openSwitch();
		}else{
		    ownerLeaveSwitch.closeSwitch();
		}
	}

	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_switch_notification:
			if (notifiSwitch.isSwitchOpen()) {
			    notifiSwitch.closeSwitch();
				rl_switch_sound.setVisibility(View.GONE);
				rl_switch_vibrate.setVisibility(View.GONE);
				textview1.setVisibility(View.GONE);
				textview2.setVisibility(View.GONE);

				settingsModel.setSettingMsgNotification(false);
			} else {
			    notifiSwitch.openSwitch();
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
		case R.id.btn_logout: //退出登陆
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
		case R.id.ll_user_profile:
			startActivity(new Intent(getActivity(), UserProfileActivity.class).putExtra("setting", true)
			        .putExtra("username", EMChatManager.getInstance().getCurrentUser()));
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
		DemoHelper.getInstance().logout(true,new EMCallBack() {
			
			@Override
			public void onSuccess() {
				getActivity().runOnUiThread(new Runnable() {
					public void run() {
						pd.dismiss();
						// 重新显示登陆页面
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

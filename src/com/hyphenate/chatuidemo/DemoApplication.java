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
package com.hyphenate.chatuidemo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.easemob.redpacketsdk.RPInitRedPacketCallback;
import com.easemob.redpacketsdk.RPValueCallback;
import com.easemob.redpacketsdk.RedPacket;
import com.easemob.redpacketsdk.bean.RedPacketInfo;
import com.easemob.redpacketsdk.bean.TokenData;
import com.easemob.redpacketsdk.constant.RPConstant;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseUserUtils;
// ============== fabric start
//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;
// ============== fabric end

public class DemoApplication extends Application {

	public static Context applicationContext;
	private static DemoApplication instance;
	// login user name
	public final String PREF_USERNAME = "username";
	
	/**
	 * nickname for current user, the nickname instead of ID be shown when user receive notification from APNs
	 */
	public static String currentUserNick = "";

	@Override
	public void onCreate() {
		MultiDex.install(this);
		super.onCreate();
// ============== fabric start
//		Fabric.with(this, new Crashlytics());
// ============== fabric end
        applicationContext = this;
        instance = this;
        
        //init demo helper
        DemoHelper.getInstance().init(applicationContext);
		//red packet code : 初始化红包SDK，开启日志输出开关
		RedPacket.getInstance().initRedPacket(applicationContext, RPConstant.AUTH_METHOD_EASEMOB, new RPInitRedPacketCallback() {

			@Override
			public void initTokenData(RPValueCallback<TokenData> callback) {
				TokenData tokenData = new TokenData();
				tokenData.imUserId = EMClient.getInstance().getCurrentUser();
				//此处使用环信id代替了appUserId 开发者可传入App的appUserId
				tokenData.appUserId = EMClient.getInstance().getCurrentUser();
				tokenData.imToken = EMClient.getInstance().getAccessToken();
				//同步或异步获取TokenData 获取成功后回调onSuccess()方法
				callback.onSuccess(tokenData);
			}

			@Override
			public RedPacketInfo initCurrentUserSync() {
				//这里需要同步设置当前用户id、昵称和头像url
				String fromAvatarUrl = "";
				String fromNickname = EMClient.getInstance().getCurrentUser();
				EaseUser easeUser = EaseUserUtils.getUserInfo(fromNickname);
				if (easeUser != null) {
					fromAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
					fromNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
				}
				RedPacketInfo redPacketInfo = new RedPacketInfo();
				redPacketInfo.fromUserId = EMClient.getInstance().getCurrentUser();
				redPacketInfo.fromAvatarUrl = fromAvatarUrl;
				redPacketInfo.fromNickName = fromNickname;
				return redPacketInfo;
			}
		});
		RedPacket.getInstance().setDebugMode(true);
		//end of red packet code
	}

	public static DemoApplication getInstance() {
		return instance;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}

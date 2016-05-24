package com.hyphenate.chatuidemo.ui;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.util.EMLog;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
/**
 * 诊断界面；在此上传错误日志
 * 
 * @author lyuzhao
 * 
 */
public class DiagnoseActivity extends BaseActivity implements OnClickListener {
	private TextView currentVersion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_activity_diagnose);

		currentVersion = (TextView) findViewById(R.id.tv_version);
		String strVersion = "";
		try {
			strVersion = getVersionName();
		} catch (Exception e) {
		}
		if (!TextUtils.isEmpty(strVersion))
			currentVersion.setText("V" + strVersion);
		else{
			String st = getResources().getString(R.string.Not_Set);
			currentVersion.setText(st);}
	}

	public void back(View view) {
		finish();
	}

	private String getVersionName() throws Exception {
		// 获取packagemanager的实例
		return EMClient.getInstance().getChatConfig().getVersion();
	}

	@Override
	public void onClick(View v) {
	}

}

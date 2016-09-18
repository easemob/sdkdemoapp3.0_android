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

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.easemob.chat.EMMessage;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.R;
import com.easemob.easeui.EaseConstant;
import com.easemob.redpacketsdk.constant.RPConstant;

public class ContextMenuActivity extends BaseActivity {
	public static final int RESULT_CODE_COPY = 1;
	public static final int RESULT_CODE_DELETE = 2;
	public static final int RESULT_CODE_FORWARD = 3;
	public static final int RESULT_CODE_REVOKE = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EMMessage message = getIntent().getParcelableExtra("message");

		int type = message.getType().ordinal();
		if(message.getBooleanAttribute(EaseConstant.EASE_ATTR_READFIRE, false)){
		    setContentView(R.layout.em_context_menu_for_common);
		}else{
    		if (type == EMMessage.Type.TXT.ordinal()) {
    		    if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL, false)
    		            || message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL, false)
						//red packet code : 屏蔽红包消息的转发功能
						|| message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)
						//end of red packet code
						) {
    		        setContentView(R.layout.em_context_menu_for_delete);
    		    } else if (message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)) {
    		        setContentView(R.layout.em_context_menu_for_image);
    		    } else {
    		        setContentView(R.layout.em_context_menu_for_text);
    		    }
    		} else if (type == EMMessage.Type.LOCATION.ordinal()) {
    		    setContentView(R.layout.em_context_menu_for_location);
    		} else if (type == EMMessage.Type.IMAGE.ordinal()) {
    		    setContentView(R.layout.em_context_menu_for_image);
    		} else if (type == EMMessage.Type.VOICE.ordinal()) {
    		    setContentView(R.layout.em_context_menu_for_voice);
    		} else if (type == EMMessage.Type.VIDEO.ordinal()) {
    		    setContentView(R.layout.em_context_menu_for_video);
    		} else if (type == EMMessage.Type.FILE.ordinal()) {
    		    setContentView(R.layout.em_context_menu_for_location);
    		}
		}
		// 这里根据消息是发送方还是接收放判断是否显示撤回菜单项
		if (message.direct == EMMessage.Direct.RECEIVE || message.getChatType() == EMMessage.ChatType.ChatRoom
				//red packet code : 屏蔽红包消息的撤回功能
				|| message.getBooleanAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, false)) {
			    //end of red packet code
			View view = findViewById(R.id.text_revoke);
			if(view != null){
			    view.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		finish();
		return true;
	}

	public void copy(View view) {
		setResult(RESULT_CODE_COPY);
		finish();
	}

	public void delete(View view) {
		setResult(RESULT_CODE_DELETE);
		finish();
	}

	public void forward(View view) {
		setResult(RESULT_CODE_FORWARD);
		finish();
	}

	// 添加撤回菜单项点击事件
	public void revoke(View view) {
		setResult(RESULT_CODE_REVOKE);
		finish();
	}

}

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
package com.hyphenate.chatuidemo.adapter;

import java.util.List;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.db.InviteMessgeDao;
import com.hyphenate.chatuidemo.domain.InviteMessage;
import com.hyphenate.chatuidemo.domain.InviteMessage.InviteMesageStatus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NewFriendsMsgAdapter extends ArrayAdapter<InviteMessage> {

	private Context context;
	private InviteMessgeDao messgeDao;

	public NewFriendsMsgAdapter(Context context, int textViewResourceId, List<InviteMessage> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		messgeDao = new InviteMessgeDao(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(context, R.layout.em_row_invite_msg, null);
			holder.avator = (ImageView) convertView.findViewById(R.id.avatar);
			holder.reason = (TextView) convertView.findViewById(R.id.message);
			holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.agree = (Button) convertView.findViewById(R.id.agree);
			holder.status = (Button) convertView.findViewById(R.id.user_state);
			holder.groupContainer = (LinearLayout) convertView.findViewById(R.id.ll_group);
			holder.groupname = (TextView) convertView.findViewById(R.id.tv_groupName);
			// holder.time = (TextView) convertView.findViewById(R.id.time);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		String str1 = context.getResources().getString(R.string.Has_agreed_to_your_friend_request);
		String str2 = context.getResources().getString(R.string.agree);
		
		String str3 = context.getResources().getString(R.string.Request_to_add_you_as_a_friend);
		String str4 = context.getResources().getString(R.string.Apply_to_the_group_of);
		String str5 = context.getResources().getString(R.string.Has_agreed_to);
		String str6 = context.getResources().getString(R.string.Has_refused_to);
		
		String str7 = context.getResources().getString(R.string.refuse);
		
		final InviteMessage msg = getItem(position);
		if (msg != null) {
		    
		    holder.agree.setVisibility(View.INVISIBLE);
		    
			if(msg.getGroupId() != null){ // 显示群聊提示
				holder.groupContainer.setVisibility(View.VISIBLE);
				holder.groupname.setText(msg.getGroupName());
			} else{
				holder.groupContainer.setVisibility(View.GONE);
			}
			
			holder.reason.setText(msg.getReason());
			holder.name.setText(msg.getFrom());
			// holder.time.setText(DateUtils.getTimestampString(new
			// Date(msg.getTime())));
			if (msg.getStatus() == InviteMesageStatus.BEAGREED) {
				holder.status.setVisibility(View.INVISIBLE);
				holder.reason.setText(str1);
			} else if (msg.getStatus() == InviteMesageStatus.BEINVITEED || msg.getStatus() == InviteMesageStatus.BEAPPLYED) {
			    holder.agree.setVisibility(View.VISIBLE);
                holder.agree.setEnabled(true);
                holder.agree.setBackgroundResource(android.R.drawable.btn_default);
                holder.agree.setText(str2);
			    
				holder.status.setVisibility(View.VISIBLE);
				holder.status.setEnabled(true);
				holder.status.setBackgroundResource(android.R.drawable.btn_default);
				holder.status.setText(str7);
				if(msg.getStatus() == InviteMesageStatus.BEINVITEED){
					if (msg.getReason() == null) {
						// 如果没写理由
						holder.reason.setText(str3);
					}
				}else{ //入群申请
					if (TextUtils.isEmpty(msg.getReason())) {
						holder.reason.setText(str4 + msg.getGroupName());
					}
				}
				
				// 设置点击事件
                holder.agree.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 同意别人发的好友请求
                        acceptInvitation(holder.agree, holder.status, msg);
                    }
                });
				holder.status.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// 拒绝别人发的好友请求
					    refuseInvitation(holder.agree, holder.status, msg);
					}
				});
			} else if (msg.getStatus() == InviteMesageStatus.AGREED) {
				holder.status.setText(str5);
				holder.status.setBackgroundDrawable(null);
				holder.status.setEnabled(false);
			} else if(msg.getStatus() == InviteMesageStatus.REFUSED){
				holder.status.setText(str6);
				holder.status.setBackgroundDrawable(null);
				holder.status.setEnabled(false);
			}

			// 设置用户头像
		}

		return convertView;
	}

	/**
	 * 同意好友请求或者群申请
	 * 
	 * @param button
	 * @param username
	 */
	private void acceptInvitation(final Button buttonAgree, final Button buttonRefuse, final InviteMessage msg) {
		final ProgressDialog pd = new ProgressDialog(context);
		String str1 = context.getResources().getString(R.string.Are_agree_with);
		final String str2 = context.getResources().getString(R.string.Has_agreed_to);
		final String str3 = context.getResources().getString(R.string.Agree_with_failure);
		pd.setMessage(str1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();

		new Thread(new Runnable() {
			public void run() {
				// 调用sdk的同意方法
				try {
					if(msg.getGroupId() == null) {//同意好友请求
						EMClient.getInstance().contactManager().acceptInvitation(msg.getFrom());
					} else { //同意加群申请
						EMClient.getInstance().groupManager().acceptApplication(msg.getFrom(), msg.getGroupId());
					}
					msg.setStatus(InviteMesageStatus.AGREED);
                    // 更新db
                    ContentValues values = new ContentValues();
                    values.put(InviteMessgeDao.COLUMN_NAME_STATUS, msg.getStatus().ordinal());
                    messgeDao.updateMessage(msg.getId(), values);
					((Activity) context).runOnUiThread(new Runnable() {

						@Override
						public void run() {
							pd.dismiss();
							buttonAgree.setText(str2);
							buttonAgree.setBackgroundDrawable(null);
							buttonAgree.setEnabled(false);
							
							buttonRefuse.setVisibility(View.INVISIBLE);
						}
					});
				} catch (final Exception e) {
					((Activity) context).runOnUiThread(new Runnable() {

						@Override
						public void run() {
							pd.dismiss();
							Toast.makeText(context, str3 + e.getMessage(), 1).show();
						}
					});

				}
			}
		}).start();
	}
	
	/**
     * 拒绝好友请求或者群申请
     * 
     * @param button
     * @param username
     */
    private void refuseInvitation(final Button buttonAgree, final Button buttonRefuse, final InviteMessage msg) {
        final ProgressDialog pd = new ProgressDialog(context);
        String str1 = context.getResources().getString(R.string.Are_refuse_with);
        final String str2 = context.getResources().getString(R.string.Has_refused_to);
        final String str3 = context.getResources().getString(R.string.Refuse_with_failure);
        pd.setMessage(str1);
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        new Thread(new Runnable() {
            public void run() {
                // 调用sdk的拒绝方法
                try {
                    if(msg.getGroupId() == null) {//拒绝好友请求
                        EMClient.getInstance().contactManager().declineInvitation(msg.getFrom());
                    } else { //同意加群申请
                        EMClient.getInstance().groupManager().declineApplication(msg.getFrom(), msg.getGroupId(), "");
                    }
                    msg.setStatus(InviteMesageStatus.REFUSED);
                    // 更新db
                    ContentValues values = new ContentValues();
                    values.put(InviteMessgeDao.COLUMN_NAME_STATUS, msg.getStatus().ordinal());
                    messgeDao.updateMessage(msg.getId(), values);
                    ((Activity) context).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            pd.dismiss();
                            buttonRefuse.setText(str2);
                            buttonRefuse.setBackgroundDrawable(null);
                            buttonRefuse.setEnabled(false);

                            buttonAgree.setVisibility(View.INVISIBLE);
                        }
                    });
                } catch (final Exception e) {
                    ((Activity) context).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            pd.dismiss();
                            Toast.makeText(context, str3 + e.getMessage(), 1).show();
                        }
                    });

                }
            }
        }).start();
    }

	private static class ViewHolder {
		ImageView avator;
		TextView name;
		TextView reason;
        Button agree;
		Button status;
		LinearLayout groupContainer;
		TextView groupname;
		// TextView time;
	}

}

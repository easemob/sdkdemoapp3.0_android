package com.hyphenate.chatuidemo.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.hyphenate.chat.EMCallConference;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallManager.EMCallType;
import com.hyphenate.chat.EMCallStream;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.media.EMLocalSurfaceView;
import com.hyphenate.media.EMOppositeSurfaceView;
import com.hyphenate.util.EMLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by linan on 16/5/30.
 */
public class ConferenceActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String TAG = "ConferenceActivity";
    
    private LayoutInflater inflater;
    SubscribeAdapter subscribableAdapter = null;
    EMCallConference currentConference;
    boolean isOwner = false;
    List<String> conferenceMembers = Collections.synchronizedList(new ArrayList<String>());
    List<String> publishedNames = Collections.synchronizedList(new ArrayList<String>());
    List<Pair<String, EMCallStream>> canSubNames = Collections.synchronizedList(new ArrayList<Pair<String, EMCallStream>>());
    List<EMOppositeSurfaceView> oppositeList = new ArrayList<EMOppositeSurfaceView>();

    static void joinConference(String conferenceId, String password, String creator) {
        Intent intent = new Intent(EMClient.getInstance().getContext(), ConferenceActivity.class);
        intent.putExtra("conferenceId", conferenceId);
        intent.putExtra("password", password);
        intent.putExtra("creator", creator);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        EMClient.getInstance().getContext().startActivity(intent);
    }
    
    public static void queryJoinConference(final String conferenceId, final String password, final String creator) {
        Intent intent = new Intent(EMClient.getInstance().getContext(), ConfirmJoinConferenceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conferenceId", conferenceId);
        intent.putExtra("password", password);
        intent.putExtra("creator", creator);
        EMClient.getInstance().getContext().startActivity(intent);
    }

    EMCallManager.EMConferenceListener listener = new EMCallManager.EMConferenceListener() {
        @Override
        public void onConferenceMemberEntered(String callId, String enteredName) {
//            if (callId == null || !callId.equals(callSession)) {
//                EMLog.d(TAG, "onConferenceMemberEntered not match:" + callId == null ? "is empty" : callId);
//            } else {
//                EMLog.d(TAG, "onConferenceMemberEntered: " + callId + " enterName:" + enteredName);
//            }
//            conferenceMembers.add(enteredName);

            refreshList(callId);
}

        @Override
        public void onConferenceMemberExited(String callId, String exitedName) {
//            if (callId == null || !callId.equals(callSession)) {
//                EMLog.d(TAG, "onConferenceMemberExited not match:" + callId == null ? "is empty" : callId);
//            } else {
//                EMLog.d(TAG, "onConferenceMemberExited: " + callId + " exitedName:" + exitedName);
//            }
//            conferenceMembers.remove(exitedName);
            
            refreshList(callId);

        }

        @Override
        public void onConferenceMemberPublished(final String callId, final String publishedName) {
//            if (callId == null || !callId.equals(currentCallId)) {
//                EMLog.d(TAG, "onConferenceMemberPublished not match:" + callId == null ? "is empty" : callId);
//            } else {
//                EMLog.d(TAG, "onConferenceMemberPublished: " + callId + " publishedName:" + publishedName);
//            }
//            if (!publishedNames.contains(publishedName)) {
//                publishedNames.add(publishedName);
//            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ConferenceActivity.this, publishedName + " has joined conference", Toast.LENGTH_SHORT).show();
                }
            });

            refreshList(callId);
        }

        @Override
        public void onConferenceMembersUpdated(String callId) {
            if (callId == null || !callId.equals(currentConference.getCallId())) {
                EMLog.d(TAG, "onConferenceMembersUpdated not match:" + callId == null ? "is empty" : callId);
            } else {
                EMLog.d(TAG, "onConferenceMembersUpdated: " + callId);
            }

        }

        @Override
        public void onConferenceClosed(String callId) {
            EMLog.d(TAG, "onConferenceClosed, callId:" + callId);
            runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    Toast.makeText(ConferenceActivity.this, "会议结束", Toast.LENGTH_SHORT).show();
                    ConferenceActivity.this.finish();
                }
            });
        }
    };

    public void refreshList(String callId) {
        
        EMLog.d(TAG, "refreshList:" + callId);
        final List<EMCallStream> streams = currentConference.getSubscribableStreams();
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                canSubNames.clear();
                List<Pair<String, EMCallStream>> list = new ArrayList<Pair<String, EMCallStream>>();
                for (EMCallStream stream : streams) {
                    EMLog.d(TAG, "onConferenceMemberPublished stream.Username" + stream.getUserName());
                    list.add(new Pair<String, EMCallStream>(stream.getUserName(), stream));
                }
                canSubNames.addAll(list);
                
                if (subscribableAdapter != null) {
                    subscribableAdapter.notifyDataSetChanged();
                }                
            }
        });
    }


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        
        setContentView(R.layout.em_activity_conference);
        this.inflater = LayoutInflater.from(this);
        
        if (bundle != null) {
            return;
        }

        EMClient.getInstance().callManager().addConferenceListener(listener);

        WindowManager winManager=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        int cellWidth = (winManager.getDefaultDisplay().getWidth() - 4 - 2) / 2;

        // set 1 local view
        EMLocalSurfaceView localView = new EMLocalSurfaceView(this);
        localView.setLayoutParams(new AbsListView.LayoutParams(cellWidth,
                cellWidth));
        EMClient.getInstance().callManager().setLocalView(localView);
        
        // set 5 opposite view
        List<View> views =  new ArrayList<View>();
        // ========================== old begin
        /*
        for (int i = 0; i < 5; i++) {
            EMOppositeSurfaceView oppositeView = new EMOppositeSurfaceView(this);
            oppositeView.setLayoutParams(new AbsListView.LayoutParams(200, 200));
            views.add(oppositeView);
        }
        */

        // ========================== split
        for (int i = 0; i < 5; i++) {
            View oppositeView = inflater.inflate(R.layout.em_conference_opposite_cell, null);
            oppositeView.setLayoutParams(new AbsListView.LayoutParams(cellWidth, cellWidth));
            views.add(oppositeView);
            EMOppositeSurfaceView surfaceView = (EMOppositeSurfaceView) oppositeView.findViewById(R.id.opposite_surface);
            oppositeView.setOnClickListener(this);
            oppositeList.add(surfaceView);
        }
        // ========================== new end
        EMClient.getInstance().callManager().setOppositeViews(oppositeList);
        
        GridView gridView = (GridView)findViewById(R.id.grid_view_conf);
        views.add(0, localView);
        SurfaceAdapter surfaceAdapter = new SurfaceAdapter(this, views);
        gridView.setAdapter(surfaceAdapter);
        gridView.setOnItemClickListener(this);
        
        //==================== test
        canSubNames.add(new Pair<String, EMCallStream>("refreshing...", null));
        //==================== test

        ListView listView = (ListView)findViewById(R.id.list_view_conf_members);
        subscribableAdapter = new SubscribeAdapter(this, canSubNames);
        listView.setAdapter(subscribableAdapter);
        listView.setOnItemClickListener(this);

        Intent intent = getIntent();
        final String conferenceId = intent.getStringExtra("conferenceId");
        final String password = intent.getStringExtra("password");
        final String creator = intent.getStringExtra("creator");

        final String[] members = intent.getStringArrayExtra("invite.members");

        if (conferenceId == null || conferenceId.isEmpty()) {
            isOwner = true;
            createConference(members, "12345678");
        } else {
            isOwner = false;
            joinConference(conferenceId, password);
        }
    }


    private void inviteJoinConference(String member, String conferenceId, String password) throws JSONException {
        final EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
        message.setReceipt(member);
        JSONObject json = new JSONObject();
        json.put("em_conference_call", true);
        json.put("conferenceId", conferenceId);
        json.put("creator", EMClient.getInstance().getCurrentUser());
        json.put("password", password);
        EMCmdMessageBody body = new EMCmdMessageBody(json.toString());
        message.addBody(body);
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    void createConference(final String[] members, final String password) {

        if (members == null || members.length == 0) {
            Toast.makeText(this, "conference members should not be null or empty", Toast.LENGTH_SHORT);
            finish();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    currentConference = EMClient.getInstance().callManager().createAndJoinConference(EMCallType.VIDEO, password);
                    
                    for (int i = 0; i < members.length; i++) {
                        inviteJoinConference(members[i], currentConference.getCallId(), password);
                    }

                    EMClient.getInstance().callManager().asyncPublishConferenceStream(currentConference.getCallId());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void joinConference(final String conferenceId, final String password) {
        if (conferenceId == null || conferenceId.isEmpty()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentConference = EMClient.getInstance().callManager().joinConference(conferenceId, password);
                    
//                    EMClient.getInstance().callManager().asyncPublishConferenceStream(callSession.getCallId());
                    
//                    EMClient.getInstance().callManager().asyncPublishConferenceStream(conferenceId);
                    
                    refreshList(currentConference.getCallId());

                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        EMClient.getInstance().callManager().removeConferenceListener(listener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isOwner) {
                        EMClient.getInstance().callManager().deleteConference(currentConference.getCallId());
                    } else {
                        EMClient.getInstance().callManager().leaveConference(currentConference.getCallId());
                    }
                    
                    if (listener != null) {
                        EMClient.getInstance().callManager().removeConferenceListener(listener);
                        listener = null;
                    }
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        super.onDestroy();
    }

    /**
     * surface adapter
     * @author linan
     *
     */
    class SurfaceAdapter extends ArrayAdapter<View> {

        public SurfaceAdapter(Context context, List<View> surfaceViews) {
            super(context, 0, surfaceViews);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getItem(position);
            }
            return convertView;
        }
    }

    class SubscribeClassHolder {
        View nameText;
        EMCallStream callStream;
        EMOppositeSurfaceView oppositeView;
    }

    /**
     * subscribable list 
     * @author linan
     *
     */
    class SubscribeAdapter extends ArrayAdapter<Pair<String, EMCallStream>> {

        public SubscribeAdapter(Context context, List<Pair<String, EMCallStream>> canSubNames) {
            super(context, 0, canSubNames);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.em_listview_row_conference_item, null);
            }
            final TextView nameText = (TextView) convertView.findViewById(R.id.name);

            synchronized (canSubNames) {
                if (position <= canSubNames.size()) {
                    nameText.setText(canSubNames.get(position).first);
                }
                if (convertView.getTag() == null) {
                }
                convertView.setTag(new SubscribeClassHolder());
                SubscribeClassHolder obj = (SubscribeClassHolder)convertView.getTag();
                obj.nameText = nameText;
                obj.callStream = canSubNames.get(position).second;
                if (position < oppositeList.size()) {
                    obj.oppositeView = oppositeList.get(position);
                }
            }
            return convertView;
        }
    }

    public void onClick(final View v) {
        View memberListBg = findViewById(R.id.list_view_conf_members_bg);
        if (v.getId() == R.id.em_conference_opposite_cell) {
            if (memberListBg.getVisibility() == View.INVISIBLE) {
                memberListBg.setTag(v);
                memberListBg.setVisibility(View.VISIBLE);
            }
        } else if (v.getId() == R.id.list_view_conf_members_bg) {
            memberListBg.setVisibility(View.INVISIBLE);
        } else {
            final RelativeLayout oppositeView = (RelativeLayout)memberListBg.getTag();
            final EMOppositeSurfaceView oppositeSurfaceView = (EMOppositeSurfaceView)oppositeView.findViewById(R.id.opposite_surface);
            if (oppositeView == null) {
                return;
            }
            memberListBg.setTag(null);
            memberListBg.setVisibility(View.INVISIBLE);

            final SubscribeClassHolder holder = (SubscribeClassHolder) v.getTag();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        if (holder == null || holder.callStream == null) {
                            return;
                        }
                        EMLog.d(TAG, "asyncSubscribeConferenceStream callId:" + currentConference.getCallId());
                        EMCallStream callStream = holder.callStream;
                        EMClient.getInstance().callManager().asyncSubscribeConferenceStream(currentConference.getCallId(), callStream, oppositeSurfaceView);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView streamText = (TextView)oppositeView.findViewById(R.id.stream_name);
                                streamText.setText(holder.callStream.getUserName());
                            }
                        });  
                        refreshList(currentConference.getCallId());
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        View memberListBg = findViewById(R.id.list_view_conf_members_bg);
        final EMOppositeSurfaceView oppositeView = (EMOppositeSurfaceView)memberListBg.getTag();
        if (oppositeView == null) {
            return;
        }
        memberListBg.setTag(null);

        
        final SubscribeClassHolder holder = (SubscribeClassHolder) view.getTag();
        
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (holder == null || holder.callStream == null) {
                        return;
                    }
                    EMLog.d(TAG, "asyncSubscribeConferenceStream callId:" + currentConference.getCallId());
                    EMCallStream callStream = holder.callStream;
                    EMClient.getInstance().callManager().asyncSubscribeConferenceStream(currentConference.getCallId(), callStream, oppositeView);
                    TextView streamText = (TextView)oppositeView.findViewById(R.id.stream_name);
                    streamText.setText(callStream.getUserName());
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
}
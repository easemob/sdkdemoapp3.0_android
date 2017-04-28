package com.hyphenate.chatuidemo.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMPushConfigs;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.widget.EaseTitleBar;
import com.hyphenate.exceptions.HyphenateException;

/**
 * Created by wei on 2016/12/6.
 */

public class OfflinePushSettingsActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener{
    private CheckBox noDisturbOn, noDisturbOff, noDisturbInNight;
    private Status status = Status.OFF;

    EMPushConfigs mPushConfigs;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_offline_push_settings);

        EaseTitleBar titleBar = (EaseTitleBar) findViewById(R.id.title_bar);
        noDisturbOn = (CheckBox) findViewById(R.id.cb_no_disturb_on);
        noDisturbOff = (CheckBox) findViewById(R.id.cb_no_disturb_off);
        noDisturbInNight = (CheckBox) findViewById(R.id.cb_no_disturb_only_night);
        Button saveButton = (Button) findViewById(R.id.btn_save);

        noDisturbOn.setOnCheckedChangeListener(this);
        noDisturbOff.setOnCheckedChangeListener(this);
        noDisturbInNight.setOnCheckedChangeListener(this);

        titleBar.setLeftLayoutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog savingPd = new ProgressDialog(OfflinePushSettingsActivity.this);
                savingPd.setMessage(getString(R.string.push_saving_settings));
                savingPd.setCanceledOnTouchOutside(false);
                savingPd.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(status == Status.ON){
                                EMClient.getInstance().pushManager().disableOfflinePush(0, 24);
                            }else if(status == Status.OFF){
                                EMClient.getInstance().pushManager().enableOfflinePush();
                            }else{
                                EMClient.getInstance().pushManager().disableOfflinePush(22, 7);
                            }
                            finish();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    savingPd.dismiss();
                                    Toast.makeText(OfflinePushSettingsActivity.this, R.string.push_save_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        mPushConfigs = EMClient.getInstance().pushManager().getPushConfigs();
        if(mPushConfigs == null){
            final ProgressDialog loadingPd = new ProgressDialog(this);
            loadingPd.setMessage("loading");
            loadingPd.setCanceledOnTouchOutside(false);
            loadingPd.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mPushConfigs = EMClient.getInstance().pushManager().getPushConfigsFromServer();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingPd.dismiss();
                                processPushConfigs();
                            }
                        });
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingPd.dismiss();
                                Toast.makeText(OfflinePushSettingsActivity.this, "loading failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        }else{
            processPushConfigs();
        }


    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_no_disturb_on:
                if(isChecked){
                    noDisturbOff.setChecked(false);
                    noDisturbInNight.setChecked(false);
                    status = Status.ON;
                }
                break;
            case R.id.cb_no_disturb_off:
                if(isChecked){
                    noDisturbOn.setChecked(false);
                    noDisturbInNight.setChecked(false);
                    status = Status.OFF;
                }
                break;
            case R.id.cb_no_disturb_only_night:
                if(isChecked){
                    noDisturbOn.setChecked(false);
                    noDisturbOff.setChecked(false);
                    status = Status.ON_IN_NIGHT;
                }
                break;
        }
    }

    private void processPushConfigs(){
        if(mPushConfigs == null)
            return;
        if(mPushConfigs.isNoDisturbOn()){
            status = status.ON;
            noDisturbOn.setChecked(true);
            if(mPushConfigs.getNoDisturbStartHour() > 0){
                status = Status.ON_IN_NIGHT;
                noDisturbInNight.setChecked(true);
            }
        }else{
            status = Status.OFF;
            noDisturbOff.setChecked(true);
        }
    }

    private enum Status {
        ON,
        OFF,
        ON_IN_NIGHT
    }

}

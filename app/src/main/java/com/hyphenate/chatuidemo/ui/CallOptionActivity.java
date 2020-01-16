package com.hyphenate.chatuidemo.ui;

import android.hardware.Camera;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.hyphenate.EMConferenceListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.utils.PreferenceManager;
import com.hyphenate.easeui.widget.EaseSwitchButton;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by linan on 16/11/29.
 */
public class CallOptionActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.em_activity_call_option);

        // EMClient.getInstance().callManager().getOptions().xxx set initial values resident at DemoHelper

        // min video kbps
        EditText editMinBitRate = (EditText)findViewById(R.id.edit_min_bit_rate);
        editMinBitRate.setText("" + PreferenceManager.getInstance().getCallMinVideoKbps());
        editMinBitRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    EMClient.getInstance().callManager().getCallOptions().setMinVideoKbps(new Integer(s.toString()).intValue());
                    PreferenceManager.getInstance().setCallMinVideoKbps(new Integer(s.toString()).intValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // max video kbps
        EditText editMaxBitRate = (EditText)findViewById(R.id.edit_max_bit_rate);
        editMaxBitRate.setText("" + PreferenceManager.getInstance().getCallMaxVideoKbps());
        editMaxBitRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    EMClient.getInstance().callManager().getCallOptions().setMaxVideoKbps(new Integer(s.toString()).intValue());
                    PreferenceManager.getInstance().setCallMaxVideoKbps(new Integer(s.toString()).intValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // max frame rate
        EditText editMaxFrameRate = (EditText)findViewById(R.id.edit_max_frame_rate);
        editMaxFrameRate.setText("" + PreferenceManager.getInstance().getCallMaxFrameRate());
        editMaxFrameRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    EMClient.getInstance().callManager().getCallOptions().setMaxVideoFrameRate(new Integer(s.toString()).intValue());
                    PreferenceManager.getInstance().setCallMaxFrameRate(new Integer(s.toString()).intValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // audio sample rate
        initAudioSampleRateSpinner(R.id.spinner_audio_sample_rate);

        /**
         * Back camera and front camera share the same API: EMCallOptions.setVideoResolution(w, h);
         */
        initCameraResolutionSpinner(Camera.CameraInfo.CAMERA_FACING_BACK, R.id.spinner_video_resolution_back);
        initCameraResolutionSpinner(Camera.CameraInfo.CAMERA_FACING_FRONT, R.id.spinner_video_resolution_front);

        // external audioInput
        RelativeLayout rlSwitcheExternalAudioInput = (RelativeLayout)findViewById(R.id.rl_switch_external_audioInput_resolution);
        rlSwitcheExternalAudioInput.setOnClickListener(this);
        EaseSwitchButton swOnExternalAudioInput = (EaseSwitchButton)findViewById(R.id.switch_external_audioInput_resolution);
        if (PreferenceManager.getInstance().isExternalAudioInputResolution()) {
            swOnExternalAudioInput.openSwitch();
        } else {
            swOnExternalAudioInput.closeSwitch();
        }

        //watermark
        RelativeLayout rlSwitcheWatermark = (RelativeLayout)findViewById(R.id.rl_switch_water_mark);
        rlSwitcheWatermark.setOnClickListener(this);
        EaseSwitchButton swOnWatermark = (EaseSwitchButton)findViewById(R.id.switch_water_mark);
        if (PreferenceManager.getInstance().isWatermarkResolution()) {
            swOnWatermark.openSwitch();
        } else {
            swOnWatermark.closeSwitch();
        }

        // fixed sample rate
        RelativeLayout rlSwitchSampleRate = (RelativeLayout)findViewById(R.id.rl_switch_fix_video_resolution);
        rlSwitchSampleRate.setOnClickListener(this);
        EaseSwitchButton swFixedSampleRate = (EaseSwitchButton)findViewById(R.id.switch_fix_video_resolution);
        if (PreferenceManager.getInstance().isCallFixedVideoResolution()) {
            swFixedSampleRate.openSwitch();
        } else {
            swFixedSampleRate.closeSwitch();
        }

        // offline call push
        RelativeLayout rlSwitchOfflineCallPush = (RelativeLayout)findViewById(R.id.rl_switch_offline_call_push);
        rlSwitchOfflineCallPush.setOnClickListener(this);
        EaseSwitchButton swOfflineCallPush = (EaseSwitchButton)findViewById(R.id.switch_offline_call_push);
        if (PreferenceManager.getInstance().isPushCall()) {
            swOfflineCallPush.openSwitch();
        } else {
            swOfflineCallPush.closeSwitch();
        }

        // record on server
        RelativeLayout rlSwitchRecordOnServer = (RelativeLayout)findViewById(R.id.rl_switch_record_on_server);
        rlSwitchRecordOnServer.setOnClickListener(this);
        EaseSwitchButton swRecordOnServer = (EaseSwitchButton)findViewById(R.id.switch_record_on_server);
        if (PreferenceManager.getInstance().isRecordOnServer()) {
            swRecordOnServer.openSwitch();
        } else {
            swRecordOnServer.closeSwitch();
        }

        // merge stream
        RelativeLayout rlSwitchMergeStream = (RelativeLayout)findViewById(R.id.rl_switch_merge_stream);
        rlSwitchMergeStream.setOnClickListener(this);
        EaseSwitchButton swMergeStream = (EaseSwitchButton)findViewById(R.id.switch_merge_stream);
        if (PreferenceManager.getInstance().isMergeStream()) {
            swMergeStream.openSwitch();
        } else {
            swMergeStream.closeSwitch();
        }
    }

    void initCameraResolutionSpinner(final int cameraId, final int spinnerId) {
        // for simulator which doesn't has camera, open will fail
        Camera mCameraDevice = null;
        try {
            mCameraDevice = android.hardware.Camera.open(cameraId);
            Camera.Parameters parameters = mCameraDevice.getParameters();

            final List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            List<String> strSizes = new ArrayList<String>();
            strSizes.add("Not Set");

            for (Camera.Size size : sizes) {
                String str = "" + size.width + "x" + size.height;
                strSizes.add(str);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strSizes);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
            final Spinner spinnerVideoResolution = (Spinner) findViewById(spinnerId);
            spinnerVideoResolution.setAdapter(adapter);

            // update selection
            int selection = 0;
            String resolution = cameraId == Camera.CameraInfo.CAMERA_FACING_BACK ?
                    PreferenceManager.getInstance().getCallBackCameraResolution() :
                    PreferenceManager.getInstance().getCallFrontCameraResolution();
            if (resolution.equals("") || resolution.equals("Not Set")) {
                selection = 0;
            } else {
                for (int i = 1; i < strSizes.size(); i++) {
                    if (resolution.equals(strSizes.get(i))) {
                        selection = i;
                        break;
                    }
                }
            }
            if (selection < strSizes.size()) {
                spinnerVideoResolution.setSelection(selection);
            }

            /**
             * Spinner onItemSelected is obscure
             * setSelection will trigger onItemSelected
             * if spinner.setSelection(newValue)'s newValue == spinner.getSelectionPosition(), it will not trigger onItemSelected
             *
             * The target we want to archive are:
             * 1. select one spinner, clear another
             * 2. set common text
             * 3. if another spinner is already at position 0, ignore it
             * 4. Use disableOnce AtomicBoolean to record whether is spinner.setSelected(x) triggered action, which should be ignored
             */
            AtomicBoolean disableOnce = new AtomicBoolean(false);
            spinnerVideoResolution.setTag(disableOnce);
            spinnerVideoResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    AtomicBoolean disable = (AtomicBoolean)spinnerVideoResolution.getTag();
                    if (disable.get() == true) {
                        disable.set(false);
                        return;
                    }

                    if (position == 0) {
                        TextView textVideoResolution = (TextView)findViewById(R.id.text_video_resolution);
                        textVideoResolution.setText("Set video resolution:" + " Not Set");

                        PreferenceManager.getInstance().setCallBackCameraResolution("");
                        PreferenceManager.getInstance().setCallFrontCameraResolution("");
                        return;
                    }
                    Camera.Size size = sizes.get(position - 1);
                    if (size != null) {
                        EMClient.getInstance().callManager().getCallOptions().setVideoResolution(size.width, size.height);
                        TextView textVideoResolution = (TextView)findViewById(R.id.text_video_resolution);
                        textVideoResolution.setText("Set video resolution:" + size.width + "x" + size.height);

                        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            PreferenceManager.getInstance().setCallBackCameraResolution(size.width + "x" + size.height);
                            PreferenceManager.getInstance().setCallFrontCameraResolution("");
                            Spinner frontSpinner = (Spinner) findViewById(R.id.spinner_video_resolution_front);
                            if (frontSpinner.getSelectedItemPosition() != 0) {
                                AtomicBoolean disableOnce = (AtomicBoolean) frontSpinner.getTag();
                                disableOnce.set(true);
                                frontSpinner.setSelection(0);
                            }

                        } else {
                            PreferenceManager.getInstance().setCallFrontCameraResolution(size.width + "x" + size.height);
                            PreferenceManager.getInstance().setCallBackCameraResolution("");
                            Spinner backSpinner = (Spinner) findViewById(R.id.spinner_video_resolution_back);
                            if (backSpinner.getSelectedItemPosition() != 0) {
                                AtomicBoolean disableOnce = (AtomicBoolean) backSpinner.getTag();
                                disableOnce.set(true);
                                backSpinner.setSelection(0);
                            }
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice.release();
            }
        }
    }

    void initAudioSampleRateSpinner(int spinnerId) {
        final List<String> sampleRateList = new ArrayList<String>();
        // Notice: some of devices may not support 48KHz, but 16KHz is widely accepted
        sampleRateList.add("Not set(prefer 16KHz)");
        sampleRateList.add("8000Hz");
        sampleRateList.add("11025Hz");
        sampleRateList.add("22050Hz");
        sampleRateList.add("16000Hz");
        sampleRateList.add("44100Hz");
        sampleRateList.add("48000Hz");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sampleRateList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        Spinner spinnerAudioSampleRate = (Spinner) findViewById(spinnerId);
        spinnerAudioSampleRate.setAdapter(adapter);

        spinnerAudioSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 // Not set
                 if (position == 0) {
                     return;
                 }
                 String audioSampleRate = sampleRateList.get(position);
                 if (audioSampleRate != null) {
                     try {
                         String data = audioSampleRate.substring(0, audioSampleRate.length() - 2);
                         int hz = new Integer(data).intValue();
                         PreferenceManager.getInstance().setCallAudioSampleRate(hz);

                         boolean externalAudioflag  = PreferenceManager.getInstance().isExternalAudioInputResolution();
                         EMClient.getInstance().callManager().getCallOptions().setExternalAudioParam(externalAudioflag,hz,1);
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                 }
             }

             @Override
             public void onNothingSelected(AdapterView<?> parent) {}
         });

        // update selection
        int selection = 0;
        int audioSampleRate = PreferenceManager.getInstance().getCallAudioSampleRate();
        if (audioSampleRate == -1) {
            selection = 0;
        } else {
            String selText = "" + audioSampleRate + "Hz";
            for (int i = 1; i < sampleRateList.size(); i++) {
                if (selText.equals(sampleRateList.get(i))) {
                    selection = i;
                    break;
                }
            }
        }
        if (selection < sampleRateList.size()) {
            spinnerAudioSampleRate.setSelection(selection);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_switch_fix_video_resolution:
                EaseSwitchButton swFixedVideoResolution = (EaseSwitchButton)findViewById(R.id.switch_fix_video_resolution);
                if (swFixedVideoResolution.isSwitchOpen()) {
                    EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(false);
                    swFixedVideoResolution.closeSwitch();
                    PreferenceManager.getInstance().setCallFixedVideoResolution(false);

                } else {
                    EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(true);
                    swFixedVideoResolution.openSwitch();
                    PreferenceManager.getInstance().setCallFixedVideoResolution(true);
                }
                break;
            case R.id.rl_switch_offline_call_push:
                EaseSwitchButton swOfflineCallPush = (EaseSwitchButton)findViewById(R.id.switch_offline_call_push);
                if (swOfflineCallPush.isSwitchOpen()) {
                    EMClient.getInstance().callManager().getCallOptions().setIsSendPushIfOffline(false);
                    swOfflineCallPush.closeSwitch();
                    PreferenceManager.getInstance().setPushCall(false);
                } else {
                    EMClient.getInstance().callManager().getCallOptions().setIsSendPushIfOffline(true);
                    swOfflineCallPush.openSwitch();
                    PreferenceManager.getInstance().setPushCall(true);
                }
                break;
            case R.id.rl_switch_record_on_server:
                EaseSwitchButton swRecordOnServer = (EaseSwitchButton)findViewById(R.id.switch_record_on_server);
                if (swRecordOnServer.isSwitchOpen()) {
                    swRecordOnServer.closeSwitch();
                    PreferenceManager.getInstance().setRecordOnServer(false);
                } else {
                    swRecordOnServer.openSwitch();
                    PreferenceManager.getInstance().setRecordOnServer(true);
                }
                break;
            case R.id.rl_switch_merge_stream:
                EaseSwitchButton swMergeStream = (EaseSwitchButton)findViewById(R.id.switch_merge_stream);
                if (swMergeStream.isSwitchOpen()) {
                    swMergeStream.closeSwitch();
                    PreferenceManager.getInstance().setMergeStream(false);
                } else {
                    swMergeStream.openSwitch();
                    PreferenceManager.getInstance().setMergeStream(true);
                }
                break;
            case R.id.rl_switch_external_audioInput_resolution:
                 EaseSwitchButton swExternalAudioInputResolution = (EaseSwitchButton)findViewById(R.id.switch_external_audioInput_resolution);
                 if(swExternalAudioInputResolution.isSwitchOpen()) {
                     swExternalAudioInputResolution.closeSwitch();
                     PreferenceManager.getInstance().setExternalAudioInputResolution(false);
                     int hz = PreferenceManager.getInstance().getCallAudioSampleRate();
                     if(hz == -1){
                         hz = 16000;
                     }
                     EMClient.getInstance().callManager().getCallOptions().setExternalAudioParam(false,hz,1);
                 }else {
                     swExternalAudioInputResolution.openSwitch();
                     PreferenceManager.getInstance().setExternalAudioInputResolution(true);

                     int hz = PreferenceManager.getInstance().getCallAudioSampleRate();
                     if(hz == -1){
                         hz = 16000;
                     }
                     EMClient.getInstance().callManager().getCallOptions().setExternalAudioParam(true,hz,1);
                 }
            case R.id.rl_switch_water_mark:
                 EaseSwitchButton swWater_mark = (EaseSwitchButton)findViewById(R.id.switch_water_mark);
                if (swWater_mark.isSwitchOpen()) {
                    swWater_mark.closeSwitch();
                    PreferenceManager.getInstance().setWatermarkResolution(false);
                } else {
                    swWater_mark.openSwitch();
                    PreferenceManager.getInstance().setWatermarkResolution(true);
                }
            default:
                break;
        }
    }

}

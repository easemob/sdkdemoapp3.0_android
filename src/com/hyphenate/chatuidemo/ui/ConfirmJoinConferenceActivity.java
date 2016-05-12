package com.hyphenate.chatuidemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hyphenate.chatuidemo.R;

public class ConfirmJoinConferenceActivity extends Activity implements OnClickListener {
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();
        setContentView(R.layout.em_activity_confirm);
        Button title = (Button)findViewById(R.id.title);
        title.setText(String.format(getString(R.string.em_invite_join_conference), intent.getStringExtra("creator")));
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.join:
                intent.setClass(this, ConferenceActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.decline:
                finish();
                break;
        }
    }
}

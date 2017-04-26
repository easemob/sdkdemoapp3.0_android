package com.hyphenate.chatuidemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.hyphenate.chatuidemo.R;

public class NamePasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_password);
    }

    public void back(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onOk(View view) {
        String username = ((EditText)findViewById(R.id.username)).getText().toString().trim();
        String password = ((EditText)findViewById(R.id.password)).getText().toString().trim();
        setResult(RESULT_OK, new Intent().putExtra("username", username).putExtra("password", password));
        finish();
    }

    public void onCancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

}

package com.hyphenate.chatuidemo.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMCheckType;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseCommonUtils;

/**
 * Created by zhangsong on 17-11-15.
 */

public class ServiceCheckActivity extends BaseActivity {
    private static final String TAG = "ServiceCheckActivity";

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText serviceCheckResultView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.em_activity_service_check);

        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);
        serviceCheckResultView = (EditText) findViewById(R.id.et_service_check);

        if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            usernameEditText.setText(DemoHelper.getInstance().getCurrentUsernName());
        }

        // Set the username EditText not editable.
        if (EMClient.getInstance().isLoggedInBefore()) {
            usernameEditText.setEnabled(false);
            usernameEditText.setFocusable(false);
            usernameEditText.setKeyListener(null);

            // Will replace to the correct password in sdk.
            passwordEditText.setText("password");
            passwordEditText.setEnabled(false);
            passwordEditText.setFocusable(false);
            passwordEditText.setKeyListener(null);
            return;// Do not set the TextChangedListener
        }

        // if user changed, clear the password
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordEditText.setText(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void back(View view) {
        super.back(view);
        hideSoftKeyboard();
    }

    public void serviceCheck(View v) {
        hideSoftKeyboard();

        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }

        final String currentUsername = usernameEditText.getText().toString().trim();
        String currentPassword = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        final StringBuilder builder = new StringBuilder();

        builder.append(getString(R.string.check_service_start));
        showResultOnUiThread(builder.toString());

        EMClient.getInstance().check(currentUsername, currentPassword, new EMClient.CheckResultListener() {
            @Override
            public void onResult(int type, int result, String desc) {
                switch (type) {
                    case EMCheckType.ACCOUNT_VALIDATION: // Account validation.
                        if (result != EMError.EM_NO_ERROR) {
                            updateResultOnUiThread(builder, R.string.check_result_account_validate_fail, result, desc);
                        }
                        break;
                    case EMCheckType.GET_DNS_LIST_FROM_SERVER: // Get dns list from server.
                        if (result == EMError.EM_NO_ERROR) {
                            updateResultOnUiThread(builder, R.string.check_result_get_dns_list_success, 0, null);
                        } else {
                            updateResultOnUiThread(builder, R.string.check_result_get_dns_list_fail, result, desc);
                        }
                        break;
                    case EMCheckType.GET_TOKEN_FROM_SERVER: // Get token from server.
                        if (result == EMError.EM_NO_ERROR) {
                            updateResultOnUiThread(builder, R.string.check_result_get_token_success, 0, null);
                        } else {
                            updateResultOnUiThread(builder, R.string.check_result_get_token_fail, result, desc);
                        }
                        break;
                    case EMCheckType.DO_LOGIN: // User login
                        if (result == EMError.EM_NO_ERROR) {
                            updateResultOnUiThread(builder, R.string.check_result_login_success, 0, null);
                        } else {
                            updateResultOnUiThread(builder, R.string.check_result_login_fail, result, desc);
                        }
                        break;
                    case EMCheckType.DO_LOGOUT: // User logout
                        if (result == EMError.EM_NO_ERROR) {
                            updateResultOnUiThread(builder, R.string.check_result_logout_success, 0, null);
                        } else {
                            updateResultOnUiThread(builder, R.string.check_result_logout_fail, result, desc);
                        }
                        break;
                }
            }
        });
    }

    private void updateResultOnUiThread(final StringBuilder builder, @StringRes int resId, int result, String desc) {
        builder.append(String.format(getString(resId), result == EMError.EM_NO_ERROR ? "" : ", error code: " + result,
                TextUtils.isEmpty(desc) ? "" : ", desc: " + desc))
                .append("\n");

        showResultOnUiThread(builder.toString());
    }

    private void showResultOnUiThread(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                serviceCheckResultView.setText(result);
            }
        });
    }
}

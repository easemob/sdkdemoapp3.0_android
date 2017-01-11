package com.hyphenate.chatuidemo.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.R;

import java.io.File;

/**
 * Created by linan on 17/2/10.
 */

public class MailLogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_mail_log);
    }


    void test() {
        Intent data=new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:linan@easemob.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, "log");
        data.putExtra(Intent.EXTRA_TEXT, "log test");

        String logPath = "";
        try {
            logPath = EMClient.getInstance().compressLogs();
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MailLogActivity.this, "compress logs failed", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        File f = new File(logPath);
        File storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (f.exists() && f.canRead()) {
            try {
                storage.mkdirs();
                File temp = File.createTempFile("hyphenate", ".log.gz", storage);
                if (!temp.canWrite()) {
                    return;
                }
                boolean result = f.renameTo(temp);
                if (result == false) {
                    return;
                }
                data.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + temp.getAbsolutePath()));
                startActivity(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMail(View v) {

        if (true) {
            test();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                String logPath = "";
                try {
                    logPath = EMClient.getInstance().compressLogs();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MailLogActivity.this, "compress logs failed", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                final String finalLogPath = logPath;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EditText editTo = (EditText)findViewById(R.id.et_mail_to);
                        EditText editTitle = (EditText) findViewById(R.id.et_mail_title);
                        EditText editContent = (EditText) findViewById(R.id.et_mail_content);

                        if (editTo.getText().toString().isEmpty()) {
                            Toast.makeText(MailLogActivity.this, "'mail to' can not be empty", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (editTitle.getText().toString().isEmpty()) {
                            Toast.makeText(MailLogActivity.this, "'title' can not be empty", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (editContent.getText().toString().isEmpty()) {
                            Toast.makeText(MailLogActivity.this, "'content' can not be empty", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (finalLogPath == null || finalLogPath.isEmpty()) {
                            Toast.makeText(MailLogActivity.this, "logPath can not be empty", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String[] to = editTo.toString().split(";");
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_EMAIL, to);
                        intent.putExtra(Intent.EXTRA_SUBJECT, editTitle.getText().toString());
                        intent.putExtra(Intent.EXTRA_TEXT, editContent.getText().toString());
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(finalLogPath));
                        startActivity(Intent.createChooser(intent, "Send mail...").setType("message/rfc822"));
                    }
                });

            }
        }).start();


    }

}

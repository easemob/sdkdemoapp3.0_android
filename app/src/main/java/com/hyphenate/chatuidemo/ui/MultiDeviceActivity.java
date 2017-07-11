package com.hyphenate.chatuidemo.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMDeviceInfo;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

public class MultiDeviceActivity extends BaseActivity {

    private static final int REQUEST_CODE_USERNAME_PASSWORD = 0;

    ListView listView;
    List<EMDeviceInfo> deviceInfos;
    String username;
    String password;
    private ProgressBar loadingPB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_device);

        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new MultiDeviceAdapter(this, 0, new ArrayList<EMDeviceInfo>()));
        loadingPB = (ProgressBar)findViewById(R.id.progressBar);

        registerForContextMenu(listView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startActivityForResult(new Intent(this, NamePasswordActivity.class), REQUEST_CODE_USERNAME_PASSWORD);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        menu.setHeaderTitle("Multi-device context menu");
        inflater.inflate(R.menu.em_multi_device_menu_item, menu);

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (deviceInfos != null && menuInfo.position < deviceInfos.size()) {
            final EMDeviceInfo deviceInfo = deviceInfos.get(menuInfo.position);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingPB.setVisibility(View.VISIBLE);
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EMClient.getInstance().kickDevice(username, password, deviceInfo.getResource());
                        updateList(username, password);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingPB.setVisibility(View.INVISIBLE);
                            }
                        });
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MultiDeviceActivity.this, "kick device failed", Toast.LENGTH_LONG).show();
                                loadingPB.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                }
            }).start();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }
        if (resultCode == RESULT_OK) {
            username = data.getStringExtra("username");
            password = data.getStringExtra("password");
            updateList(username, password);
        }
    }

    void updateList(final String username, final String password) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingPB.setVisibility(View.VISIBLE);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    deviceInfos = EMClient.getInstance().getLoggedInDevicesFromServer(username, password);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setAdapter(new MultiDeviceAdapter(MultiDeviceActivity.this, 0, deviceInfos));
                            loadingPB.setVisibility(View.INVISIBLE);
                        }
                    });

                } catch (HyphenateException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MultiDeviceActivity.this, "get logged in devices failed", Toast.LENGTH_LONG).show();
                            loadingPB.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        }).start();
    }

    private class MultiDeviceAdapter extends ArrayAdapter<EMDeviceInfo> {
        private LayoutInflater inflater;

        public MultiDeviceAdapter(Context context, int res, List<EMDeviceInfo> deviceInfos) {
            super(context, res, deviceInfos);
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.em_multi_dev_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.multi_device_name)).setText(getItem(position).getDeviceName());
            convertView.setTag(getItem(position).getDeviceName());
            return convertView;
        }
    }


}

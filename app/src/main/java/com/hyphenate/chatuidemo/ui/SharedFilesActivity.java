package com.hyphenate.chatuidemo.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMucSharedFile;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.util.FileUtils;
import com.hyphenate.util.PathUtil;
import com.hyphenate.util.TextFormater;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SharedFilesActivity extends BaseActivity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int pageSize = 10;
    private int pageNum = 1;

    private String groupId;
    EMGroup group;
    private List<EMMucSharedFile> fileList;

    private FilesAdapter adapter;
    private boolean hasMoreData;
    private boolean isLoading;
    private ProgressBar loadmorePB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_files);


        groupId = getIntent().getStringExtra("groupId");
        group = EMClient.getInstance().groupManager().getGroup(groupId);

        fileList = new ArrayList<>();

        listView = (ListView) findViewById(R.id.list_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        loadmorePB = (ProgressBar) findViewById(R.id.pb_load_more);
        registerForContextMenu(listView);

        showFileList(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showFile(fileList.get(position));
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                int lasPos = view.getLastVisiblePosition();
                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                        && hasMoreData
                        && !isLoading
                        && lasPos == fileList.size() -1){
                    showFileList(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showFileList(true);
            }
        });

    }

    private void showFileList(final boolean isRefresh) {
        isLoading = true;
        if(isRefresh){
            pageNum = 1;
            swipeRefreshLayout.setRefreshing(true);
        }else{
            pageNum++;
            loadmorePB.setVisibility(View.VISIBLE);
        }
        EMClient.getInstance().groupManager().asyncFetchGroupSharedFileList(groupId, pageNum, pageSize, new EMValueCallBack<List<EMMucSharedFile>>() {
            @Override
            public void onSuccess(final List<EMMucSharedFile> value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isRefresh) {
                            swipeRefreshLayout.setRefreshing(false);
                        }else{
                            loadmorePB.setVisibility(View.INVISIBLE);
                        }
                        isLoading = false;
                        if(isRefresh)
                            fileList.clear();
                        fileList.addAll(value);
                        if(value.size() == pageSize){
                            hasMoreData = true;
                        }else{
                            hasMoreData = false;
                        }
                        if(adapter == null){
                            adapter = new FilesAdapter(SharedFilesActivity.this, 1, fileList);
                            listView.setAdapter(adapter);
                        }else{
                            adapter.notifyDataSetChanged();
                        }

                    }
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(SharedFilesActivity.this, "Load files fail", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * If local file doesn't exits, download it first.
     * else show file directly
     * @param file
     */
    private void showFile(EMMucSharedFile file){
        final File localFile = new File(PathUtil.getInstance().getFilePath(), file.getFileName());
        if(localFile.exists()){
            openFile(localFile);
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Downloading...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();
        EMClient.getInstance().groupManager().asyncDownloadGroupSharedFile(
                groupId,
                file.getFileId(),
                localFile.getAbsolutePath(),
                new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                openFile(localFile);
                            }
                        });
                    }

                    @Override
                    public void onError(int code, final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                Toast.makeText(SharedFilesActivity.this, "Download file fails, " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }
                }
        );
    }

    private void openFile(File file){
        if(file != null && file.exists()){
            FileUtils.openFile(file, this);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add("Delete File");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        final int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

        EMClient.getInstance().groupManager().asyncDeleteGroupSharedFile(
                groupId,
                fileList.get(position).getFileId(),
                new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                fileList.remove(position);
                                adapter.notifyDataSetChanged();

                            }
                        });
                    }

                    @Override
                    public void onError(int code, final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                Toast.makeText(SharedFilesActivity.this, "Delete file fails, " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }
                }
        );

        return super.onContextItemSelected(item);
    }

    /**
     * upload file button clicked
     * @param view
     */
    public void uploadFile(View view){
        selectFileFromLocal();
    }

    /**
     * select file
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) { //api 19 and later, we can't use this way, demo just select from images
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE_SELECT_FILE){
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        uploadFileWithUri(uri);
                    }
                }
            }
        }
    }

    private void uploadFileWithUri(Uri uri) {
        String filePath = getFilePath(uri);
        if (filePath == null) {
            Toast.makeText(this, "only support upload image when android os >= 4.4", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, R.string.File_does_not_exist, Toast.LENGTH_SHORT).show();
            return;
        }
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);
        pd.setMessage("Uploading...");
        pd.show();
        EMClient.getInstance().groupManager().asyncUploadGroupSharedFile(groupId, filePath, new EMCallBack() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        if(adapter != null){
                            fileList.clear();
                            fileList.addAll(group.getShareFileList());
                            adapter.notifyDataSetChanged();
                            Toast.makeText(SharedFilesActivity.this, "Upload success", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }

            @Override
            public void onError(int code, final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(SharedFilesActivity.this, "Upload fail, " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onProgress(int progress, String status) {
            }
        });
    }

    @Nullable
    private String getFilePath(Uri uri) {
        String filePath = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }
        if (filePath == null) {
            return null;
        }
        return filePath;
    }

    private static class FilesAdapter extends ArrayAdapter<EMMucSharedFile> {
        private Context mContext;
        private LayoutInflater inflater;
        List<EMMucSharedFile> list;

        public FilesAdapter(@NonNull Context context, int resource, @NonNull List<EMMucSharedFile> objects) {
            super(context, resource, objects);
            mContext = context;
            this.inflater = LayoutInflater.from(context);
            list = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(mContext, R.layout.em_shared_file_row, null);
                holder.tv_file_name = (TextView) convertView.findViewById(R.id.tv_file_name);
                holder.tv_file_size = (TextView) convertView.findViewById(R.id.tv_file_size);
                holder.tv_update_time = (TextView) convertView.findViewById(R.id.tv_update_time);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.tv_file_name.setText(getItem(position).getFileName());
            holder.tv_file_size.setText(TextFormater.getDataSize(getItem(position).getFileSize()));
            holder.tv_update_time.setText(new Date(getItem(position).getFileUpdateTime()).toString());

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView tv_file_name;
        TextView tv_file_size;
        TextView tv_update_time;
    }

}

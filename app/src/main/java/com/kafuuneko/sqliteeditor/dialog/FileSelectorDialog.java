package com.kafuuneko.sqliteeditor.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kafuuneko.sqliteeditor.R;
import com.kafuuneko.sqliteeditor.util.FileOperation;

import java.io.File;
import java.util.Stack;

public class FileSelectorDialog {
    private Builder mBuilder;

    private ImageButton mBtnBack = null;
    private ListView mFileList = null;
    private TextView mTvPath = null;
    private TextView mTvInputTip = null;
    private EditText mInputData = null;
    private AlertDialog mAlertDialog = null;


    private FileSelectorDialog(Builder builder) {
        mBuilder = builder;
    }

    /**
     * 初始化文件选择器对话框视图
     */
    private View initView() {
        View view = LayoutInflater.from(mBuilder.mContext).inflate(R.layout.dialog_file_selector, null);

        mFileList = view.findViewById(R.id.lvFile);
        mBtnBack = view.findViewById(R.id.btnBack);
        mTvPath = view.findViewById(R.id.tvPath);
        mTvInputTip = view.findViewById(R.id.tvInputTip);
        mInputData = view.findViewById(R.id.etInputData);

        if (mBuilder.getFileNameTip() != null) {
            mTvInputTip.setText(mBuilder.getFileNameTip());
        }

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBuilder.backPath()) {
                    Toast.makeText(mBuilder.mContext, R.string.blocking_access, Toast.LENGTH_SHORT).show();
                } else {
                    mFileList.setAdapter(new FileListAdapter(mBuilder.mContext, mBuilder.getFile().listFiles()));
                    mTvPath.setText(mBuilder.mFile.getPath());
                }
            }
        });

        mFileList.setAdapter(new FileListAdapter(mBuilder.mContext, mBuilder.getFile().listFiles()));
        mTvPath.setText(mBuilder.mFile.getPath());
        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileListAdapter adapter = ((FileListAdapter) mFileList.getAdapter());
                File file = adapter.getItem(position);

                if (file.isDirectory()) {
                    mBuilder.openFile(file);
                    mFileList.setAdapter(new FileListAdapter(mBuilder.mContext, mBuilder.getFile().listFiles()));
                    mTvPath.setText(mBuilder.mFile.getPath());
                } else if (file.isFile()) {
                    //如果模式是打开文件且非用户输入文件名，则完成操作
                    if (mBuilder.checkMode(Builder.MODE_OPEN_FILE) && !mBuilder.checkMode(Builder.MODE_INOUT_NAME)) {
                        mAlertDialog.cancel();
                        mAlertDialog = null;
                        mBuilder.mFileSelectorListener.SelectComplete(file);
                    }
                }

            }
        });

        return view;
    }

    /**
     * 弹出文件选择对话框
     */
    public void show() {
        //判断模式是否冲突
        if (mBuilder.checkMode(Builder.MODE_OPEN_DIR) && mBuilder.checkMode(Builder.MODE_OPEN_FILE)) {
            Log.e("FileSelectorDialog.show", "模式配置错误");
            return;
        }

        if (mAlertDialog != null) {
            Log.e("FileSelectorDialog.show", "禁止重复弹出");
            return;
        }

        AlertDialog.Builder AlertDialog_Builder = new AlertDialog.Builder(mBuilder.mContext);
        AlertDialog_Builder.setView(initView());
        AlertDialog_Builder.setTitle(mBuilder.getTitle());
        AlertDialog_Builder.setNegativeButton(R.string.cancel, null);

        if (mBuilder.checkMode(Builder.MODE_OPEN_DIR)) {
            show_dir(AlertDialog_Builder);
        } else if (mBuilder.checkMode(Builder.MODE_OPEN_FILE)) {
            show_file(AlertDialog_Builder);
        }

        mAlertDialog = AlertDialog_Builder.create();

        mAlertDialog.show();

    }

    /**
     * show函数分支
     * 若mode有MODE_OPEN_FILE属性则执行此函数
     */
    private void show_file(AlertDialog.Builder AlertDialog_Builder) {
        if (mBuilder.checkMode(Builder.MODE_INOUT_NAME)) {
            AlertDialog_Builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String path = mBuilder.getFile().getPath() + "/" + mInputData.getText().toString();
                    mBuilder.mFileSelectorListener.SelectComplete(new File(path));
                }
            });
        } else {
            mTvInputTip.setVisibility(View.GONE);
            mInputData.setVisibility(View.GONE);
        }
    }

    /**
     * show函数分支
     * 若mode有MODE_OPEN_DIR属性则执行此函数
     */
    private void show_dir(AlertDialog.Builder AlertDialog_Builder) {
        if (mBuilder.checkMode(Builder.MODE_INOUT_NAME)) {
            AlertDialog_Builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String path = mBuilder.getFile().getPath() + "/" + mInputData.getText().toString();
                    mBuilder.mFileSelectorListener.SelectComplete(new File(path));
                }
            });
        } else {
            mTvInputTip.setVisibility(View.GONE);
            mInputData.setVisibility(View.GONE);
            AlertDialog_Builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String path = mBuilder.getFile().getPath();
                    mBuilder.mFileSelectorListener.SelectComplete(new File(path));
                }
            });
        }
    }

    /**
     * 文件选择器对话框构造器
     */
    public static class Builder {
        public final static int MODE_OPEN_FILE = 1 << 0;
        public final static int MODE_OPEN_DIR = 1 << 1;
        public final static int MODE_INOUT_NAME = 1 << 2;

        private Context mContext;
        private int mSelectorMode;
        private FileSelectorListener mFileSelectorListener = null;
        private Stack<String> mPath = null;
        private File mFile;
        private String mTitle;
        private String mFileNameTip;
        private int mCheckPermissionRequestCode;

        public Builder(Context context, String rootPath, int checkPermissionRequestCode) {
            mContext = context;
            mSelectorMode |= MODE_OPEN_FILE;

            mPath = new Stack<>();
            for (String name : rootPath.split("/")) mPath.push(name);

            mFile = new File(getPath());

            mCheckPermissionRequestCode = checkPermissionRequestCode;

        }

        public Builder setSelectorMode(int mSelectorMode) {
            this.mSelectorMode = mSelectorMode;
            return this;
        }

        public boolean checkMode(int mode) {
            return (mSelectorMode & mode) == mode;
        }

        public Builder setFileSelectorListener(FileSelectorListener fileSelectorListener) {
            mFileSelectorListener = fileSelectorListener;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setFileNameTip(String mFileNameTip) {
            this.mFileNameTip = mFileNameTip;
            return this;
        }

        public String getFileNameTip() {
            return mFileNameTip;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getPath() {
            String path = new String();
            if (mPath == null || mPath.size() == 0) return "/";
            for (String name : mPath) {
                path += "/" + name;
            }
            return path;
        }

        /*
         * 退回上一级目录
         * */
        private boolean backPath() {
            if (mPath == null || mPath.size() == 0) return false;

            String temp = mPath.pop();
            File tempFile = new File(getPath());

            if (tempFile.listFiles() != null) {
                mFile = tempFile;
                return true;
            } else {
                mPath.push(temp);
                return false;
            }
        }

        private void openFile(File file) {
            mPath = new Stack<>();
            for (String name : file.getPath().split("/")) mPath.push(name);
            mFile = file;

        }

        private File getFile() {
            return mFile;
        }

        /**
         * 构造选择器
         *
         * @return 如果无权限则开始请求权限并返回null
         */
        public FileSelectorDialog build() {
            if (!FileOperation.checkPermission(mContext, mCheckPermissionRequestCode)) {
                return null;
            }
            return new FileSelectorDialog(this);
        }

    }

    /**
     * 文件列表Adapter
     */
    private static class FileListAdapter extends BaseAdapter {
        private Context mContext;
        private File[] mFileList;

        public FileListAdapter(Context context, File[] fileList) {
            mContext = context;
            mFileList = fileList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.dialog_file_selector_item, null);
            }

            ImageView tvFileIcon = view.findViewById(R.id.tvFileIcon);
            TextView tvFileName = view.findViewById(R.id.tvFileName);

            if (getItem(position).isDirectory()) {
                tvFileIcon.setImageResource(R.drawable.ic_dir);
            } else if (getItem(position).isFile()) {
                tvFileIcon.setImageResource(R.drawable.ic_file);
            }

            tvFileName.setText(getItem(position).getName());

            return view;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            if (mFileList == null) {
                return 0;
            }
            return mFileList.length;
        }

        @Override
        public File getItem(int position) {
            return mFileList[position];
        }
    }

    public interface FileSelectorListener {

        void SelectComplete(File file);

    }
}

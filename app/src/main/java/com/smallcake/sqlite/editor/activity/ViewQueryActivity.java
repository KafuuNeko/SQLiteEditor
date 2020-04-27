package com.smallcake.sqlite.editor.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rmondjone.locktableview.DisplayUtil;
import com.rmondjone.locktableview.LockTableView;
import com.rmondjone.xrecyclerview.XRecyclerView;
import com.smallcake.sqlite.editor.R;
import com.smallcake.sqlite.editor.sqlite.OpenDatabase;
import com.smallcake.sqlite.editor.util.StrOperation;

import java.util.ArrayList;


/**
 * 查询SQL Activity
 * 与 TableDataEdit 类似，但少了编辑功能
 * 此Activity仅提供查询服务
 */
public class ViewQueryActivity extends AppCompatActivity {
    //每页或每次加载数量
    private static final int MaxRowNumber = 50;

    private int mTriggerSource = 0;

    private String mQuerySql;

    private Handler handler;

    private Toolbar mToolBar;
    private LinearLayout mContentView;

    //页操作按钮
    private ImageButton mBtnNext, mBtnPrevious, mBtnToStart, mBtnToEnd;
    private TextView mTotalNumber, mTvPageTip;
    private TextView mLoadingTip;

    //所有数据
    private ArrayList<ArrayList<String>> mAllData;

    //显示出的数据
    private ArrayList<ArrayList<String>> mTableData;

    private int mStartPos = 0;
    private int mMaxPos = 0;
    private int mOffsetPos = 0;

    private LockTableView mLockTableView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_query);

        handler = new Handler();

        mTriggerSource = getIntent().getIntExtra("trigger_source", -1);

        mQuerySql = getIntent().getStringExtra("query_sql");
        if (mQuerySql == null) {
            finish();
        } else {
            findView();
            initToolBar();
            //开启线程加载数据
            new Thread(mLoadThread).start();
        }

    }

    //加载数据线程
    private Runnable mLoadThread = new Runnable() {
        @Override
        public void run() {
            try {
                mAllData = OpenDatabase.getQueryTableData(mQuerySql);
                mMaxPos = mAllData.size();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mMaxPos == 0 || mAllData.get(0).size() <= 1) {
                            sendResultBroadcast(2, "result is null");
                            finish();
                        } else {
                            --mMaxPos;
                            initView();
                            mLoadingTip.setVisibility(View.GONE);
                            sendResultBroadcast(0, "success");

                        }
                    }
                });


            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendResultBroadcast(1, e.toString());
                        finish();
                    }
                });
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendResultBroadcast(int code, String msg) {
        Intent intent = new Intent();
        intent.setAction(OpenDatabase.BROADCAST_QUERY_STATUS);
        intent.putExtra("trigger_source", mTriggerSource);
        intent.putExtra("code", code);
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 查找此Activity所有的View
     */
    private void findView() {
        mToolBar = findViewById(R.id.mToolBar);

        mContentView = findViewById(R.id.mContentView);

        mBtnNext = findViewById(R.id.mBtnNext);
        mBtnPrevious = findViewById(R.id.mBtnPrevious);
        mBtnToStart = findViewById(R.id.mBtnToStart);
        mBtnToEnd = findViewById(R.id.mBtnToEnd);

        mTotalNumber = findViewById(R.id.mTotalNumber);

        mTvPageTip = findViewById(R.id.mTvPageTip);

        mLoadingTip = findViewById(R.id.mLoadingTip);
    }

    /**
     * 初始化/配置此Activity所有的View
     */
    private void initView() {

        /*
         * 初始化页面操作按钮点击事件
         * */

        //下一页
        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartPos + MaxRowNumber < mMaxPos) {
                    mStartPos += MaxRowNumber;
                    showTableData();
                }
            }
        });

        //上一页
        mBtnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartPos != 0) {
                    mStartPos -= MaxRowNumber;
                    if (mStartPos < 0) {
                        mStartPos = 0;
                    }

                    showTableData();
                }
            }
        });

        //跳到尾页
        mBtnToEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMaxPos - MaxRowNumber > 0) {
                    mStartPos = mMaxPos - MaxRowNumber;
                    showTableData();
                }
            }
        });

        //跳到首页
        mBtnToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartPos = 0;
                showTableData();
            }
        });

        initDisplayOpinion();
        showTableData();//显示表格数据
    }

    private void initToolBar() {
        /*
         * 配置ToolBar
         * */
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (getIntent().getStringExtra("title") == null) {
            getSupportActionBar().setTitle(R.string.query_sql);
        } else {
            getSupportActionBar().setTitle(getIntent().getStringExtra("title"));
        }
    }

    /**
     * 更新页面提示内容
     */
    private void updatePageTips() {
        mTotalNumber.setText(mMaxPos + "");
        int max_page = mMaxPos / MaxRowNumber;
        if (mMaxPos % MaxRowNumber > 0) {
            ++max_page;
        }

        int current_page = (mStartPos) / MaxRowNumber;
        if ((mStartPos) % MaxRowNumber > 0) {
            ++current_page;
        }
        ++current_page;

        mTvPageTip.setText(current_page + "/" + max_page);
    }

    private void initDisplayOpinion() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        DisplayUtil.density = dm.density;
        DisplayUtil.densityDPI = dm.densityDpi;
        DisplayUtil.screenWidthPx = dm.widthPixels;
        DisplayUtil.screenhightPx = dm.heightPixels;
        DisplayUtil.screenWidthDip = DisplayUtil.px2dip(getApplicationContext(), dm.widthPixels);
        DisplayUtil.screenHightDip = DisplayUtil.px2dip(getApplicationContext(), dm.heightPixels);
    }

    private ArrayList<ArrayList<String>> makeTableData(int startPos, int number, boolean head) {
        ArrayList<ArrayList<String>> result = new ArrayList<>();

        if (head) {
            result.add(mAllData.get(0));
        }

        for (int pos = startPos + 1; pos <= startPos + number; pos++) {
            if (mAllData.size() > pos) {

                ArrayList<String> row = new ArrayList<>();
                for (int i = 0; i < mAllData.get(pos).size(); i++) {
                    row.add(StrOperation.limitStringLength(mAllData.get(pos).get(i), true));
                }

                result.add(row);
            }
        }

        return result;
    }

    /**
     * 显示表格
     */
    private void showTableData() {
        mOffsetPos = 0;
        updatePageTips();
        mTableData = makeTableData(mStartPos, MaxRowNumber, true);
        mLockTableView = new LockTableView(this, mContentView, mTableData);
        mLockTableView.setLockFristRow(true); //是否锁定第一行
        mLockTableView.setCellPadding(1);//设置单元格内边距(dp)
        mLockTableView.setMaxColumnWidth(500);
        mLockTableView.setOnItemClickListenter(new LockTableView.OnItemClickListenter() {
            @Override
            public void onItemClick(View item, int position) {
                TableRowClick(position);
            }
        });

        mLockTableView.show();

        //拉到底部自动加载
        mLockTableView.getTableScrollView().setLoadingMoreEnabled(mStartPos + MaxRowNumber < mMaxPos);
        mLockTableView.getTableScrollView().setPullRefreshEnabled(false);
        mLockTableView.getTableScrollView().setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {

            }

            @Override
            public void onLoadMore() {
                loadMore();
            }
        });

    }

    private void TableRowClick(int pos) {
        final int itemPos = (mStartPos - mOffsetPos) + pos;

        final ArrayList<String> column = new ArrayList<>();
        //舍弃第一行第一列的数据
        for (int i = 1; i < mTableData.get(0).size(); i++) {
            column.add(mTableData.get(0).get(i));
        }

        final CharSequence[] item_list = new CharSequence[column.size()];
        for (int i = 0; i < column.size(); i++) {
            item_list[i] = column.get(i);
        }

        (new AlertDialog.Builder(this))
                .setItems(item_list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String data = mAllData.get(itemPos).get(which + 1);
                        new AlertDialog.Builder(ViewQueryActivity.this)
                                .setTitle(item_list[which])
                                .setMessage(data)
                                .setPositiveButton(R.string.cancel, null)
                                .setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager cm = (ClipboardManager) getSystemService(ViewQueryActivity.CLIPBOARD_SERVICE);
                                        ClipData mClipData = ClipData.newPlainText("Label", data);
                                        cm.setPrimaryClip(mClipData);
                                        Toast.makeText(ViewQueryActivity.this, R.string.copy_success, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .create().show();
                    }
                })
                .setTitle(R.string.select_column)
                .setPositiveButton(R.string.cancel, null)
                .create().show();
    }

    private void loadMore() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mStartPos += MaxRowNumber;
                mOffsetPos += MaxRowNumber;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTableData.addAll(makeTableData(mStartPos, MaxRowNumber, false));
                        mLockTableView.setTableDatas(mTableData);
                        mLockTableView.getTableScrollView().loadMoreComplete();
                        mLockTableView.getTableScrollView().setNoMore(!(mStartPos + MaxRowNumber < mMaxPos));
                        updatePageTips();
                    }
                });
            }
        }).start();
    }
}

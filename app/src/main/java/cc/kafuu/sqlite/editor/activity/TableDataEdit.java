package cc.kafuu.sqlite.editor.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
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
import cc.kafuu.sqlite.editor.R;
import cc.kafuu.sqlite.editor.dialog.AddRowDialog;
import cc.kafuu.sqlite.editor.sqlite.OpenDatabase;
import cc.kafuu.sqlite.editor.util.StrOperation;

import java.util.ArrayList;
import java.util.List;

public class TableDataEdit extends AppCompatActivity {
    private static final int MaxRowNumber = 50;

    private Handler handler;

    private LinearLayout mContentView;
    private String mTableName;
    private int mStartPos = 0;
    private int mMaxPos = 0;
    private int DynamicLoading = 0;
    private int mLastPos = 0;
    private ArrayList<ArrayList<String>> mShowTableData = null;
    private List<Long> mRowId = null;

    private Toolbar mToolBar;
    private ImageButton mBtnNext, mBtnPrevious, mBtnToStart, mBtnToEnd;
    private TextView mTotalNumber, mTvPageTip;
    private LockTableView mLockTableView = null;
    private ImageButton mBtnAdd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_table_data_edit);

        handler = new Handler();

        mTableName = getIntent().getStringExtra("table_name");
        if (mTableName == null || OpenDatabase.DB == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();

        mStartPos = 0;

        initDisplayOpinion();

        loadTableData();
        showTableData();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 初始化视图
     */
    private void initView() {
        mToolBar = findViewById(R.id.mToolBar);

        mContentView = findViewById(R.id.mContentView);

        mBtnNext = findViewById(R.id.mBtnNext);
        mBtnPrevious = findViewById(R.id.mBtnPrevious);
        mBtnToStart = findViewById(R.id.mBtnToStart);
        mBtnToEnd = findViewById(R.id.mBtnToEnd);

        mTotalNumber = findViewById(R.id.mTotalNumber);

        mTvPageTip = findViewById(R.id.mTvPageTip);

        mBtnAdd = findViewById(R.id.mBtnAdd);

        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(mTableName);

        mBtnToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartPos = 0;
                loadTableData();
                showTableData();
            }
        });

        mBtnToEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMaxPos - MaxRowNumber > 0) {
                    mStartPos = mMaxPos - MaxRowNumber;
                    loadTableData();
                    showTableData();
                }
            }
        });

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartPos + MaxRowNumber < mMaxPos) {
                    mStartPos += MaxRowNumber;
                    loadTableData();
                    showTableData();
                }

            }
        });

        mBtnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartPos != 0) {
                    mStartPos -= MaxRowNumber;
                    if (mStartPos < 0) {
                        mStartPos = 0;
                    }

                    loadTableData();
                    showTableData();
                }

            }
        });

        //添加数据
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRowData();
            }
        });
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

    /**
     * 更新页面提示内容
     */
    private void updatePageTips() {
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

    /**
     * 加载表格数据
     */
    private void loadTableData() {
        mRowId = new ArrayList<>();
        mShowTableData = new ArrayList<>();

        DynamicLoading = 0;
        mMaxPos = OpenDatabase.getTableDataCount(mTableName);
        mTotalNumber.setText(mMaxPos + "");
        mShowTableData = OpenDatabase.getTableData(mTableName, mStartPos, MaxRowNumber, mRowId);
        mLastPos = mStartPos;
        updatePageTips();
    }

    /**
     * 显示表格
     */
    private void showTableData() {
        mLockTableView = new LockTableView(this, mContentView, mShowTableData);
        mLockTableView.setLockFristRow(true); //是否锁定第一行
        mLockTableView.setCellPadding(1);//设置单元格内边距(dp)
        mLockTableView.setMaxColumnWidth(500);
        mLockTableView.setTextViewSize(12);
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

    /**
     * 表格行被用户点击
     */
    private void TableRowClick(final int position) {
        //database_pos是数据在此数据表中的顺序位置
        final long database_pos = (mStartPos - (DynamicLoading * MaxRowNumber)) + position;

        new AlertDialog.Builder(TableDataEdit.this)
                .setItems(R.array.edit_table_data, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                updateRowData(position);
                                break;

                            case 1:
                                deleteRow(position);
                                break;
                        }
                    }
                })
                .setTitle(getResources().getString(R.string.select_row_tips).replace("%row%", database_pos + ""))
                .create().show();
    }

    /**
     * 删除数据行
     */
    private void deleteRow(final int pos) {
        //database_pos是数据在此数据表中的顺序位置
        final long database_pos = (mStartPos - (DynamicLoading * MaxRowNumber)) + pos;

        new AlertDialog.Builder(TableDataEdit.this)
                .setTitle(R.string.warning)
                .setMessage(getResources().getString(R.string.delete_row_warning).replace("%row_id", database_pos + ""))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OpenDatabase.deleteRow(mTableName, mRowId.get(pos - 1));
                        mStartPos = mLastPos;
                        loadTableData();
                        showTableData();
                    }
                })
                .create().show();
    }

    /**
     * 加载更多数据
     */
    private void loadMore() {
        mMaxPos = OpenDatabase.getTableDataCount(mTableName);
        mTotalNumber.setText(mMaxPos + "");

        new Thread(new Runnable() {
            @Override
            public void run() {
                mStartPos += MaxRowNumber;
                mLastPos = mStartPos;
                OpenDatabase.getTableData(mTableName, mShowTableData, mStartPos, MaxRowNumber, mRowId);
                DynamicLoading++;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLockTableView.setTableDatas(mShowTableData);
                        mLockTableView.getTableScrollView().loadMoreComplete();
                        mLockTableView.getTableScrollView().setNoMore(!(mStartPos + MaxRowNumber < mMaxPos));
                        updatePageTips();
                    }
                });
            }
        }).start();
    }

    /**
     * 修改数据
     */
    private void updateRowData(final int position) {
        final long rowId = mRowId.get(position - 1);
        final ArrayList<String> column = new ArrayList<>();
        //舍弃第一行第一列的数据
        for (int i = 1; i < mShowTableData.get(0).size(); i++) {
            column.add(mShowTableData.get(0).get(i));
        }

        final CharSequence[] item_list = new CharSequence[column.size()];
        for (int i = 0; i < column.size(); i++) {
            item_list[i] = column.get(i);
        }

        (new AlertDialog.Builder(this))
                .setItems(item_list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final int columnIndex = which;
                        final String columnName = item_list[columnIndex].toString();

                        final EditText iData = new EditText(TableDataEdit.this);
                        final String currentData = OpenDatabase.readRowData(mTableName, rowId, columnName);
                        iData.setText(currentData);
                        new AlertDialog.Builder(TableDataEdit.this)
                                .setTitle(columnName)
                                .setView(iData)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (OpenDatabase.updateRowData(mTableName, rowId, columnName, iData.getText().toString())) {
                                            mShowTableData.get(position).set(columnIndex + 1, StrOperation.limitStringLength(iData.getText().toString(), true));
                                            mLockTableView.setTableDatas(mShowTableData);
                                        }

                                    }
                                })
                                .setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                        ClipData mClipData = ClipData.newPlainText("Label", currentData);
                                        cm.setPrimaryClip(mClipData);
                                        Toast.makeText(TableDataEdit.this, R.string.copy_success, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .create().show();

                    }
                })
                .setTitle(R.string.select_column)
                .setPositiveButton(R.string.cancel, null)
                .create().show();
    }

    /**
     * 添加数据
     */
    private void addRowData() {

        final ArrayList<String> column = new ArrayList<>();
        //舍弃第一行第一列的数据
        for (int i = 1; i < mShowTableData.get(0).size(); i++) {
            column.add(mShowTableData.get(0).get(i));
        }

        (new AddRowDialog(this, column)).show(new AddRowDialog.CompleteListener() {
            @Override
            public boolean complete(String sql, String[] data_list) {

                try {
                    Log.d("sql", sql);
                    OpenDatabase.DataBase.execSQL(sql, data_list);
                    mMaxPos = OpenDatabase.getTableDataCount(mTableName);
                    mStartPos = mMaxPos - MaxRowNumber;
                    if (mStartPos < 0) {
                        mStartPos = 0;
                    }
                    loadTableData();
                    showTableData();
                    return true;
                } catch (Exception e) {
                    Toast.makeText(TableDataEdit.this, e.toString(), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }, mTableName);
    }

}

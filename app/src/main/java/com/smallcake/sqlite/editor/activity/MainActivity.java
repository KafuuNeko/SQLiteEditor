package com.smallcake.sqlite.editor.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.smallcake.sqlite.editor.R;
import com.smallcake.sqlite.editor.adapter.MainPagerAdapter;
import com.smallcake.sqlite.editor.dialog.CreateTableDialog;
import com.smallcake.sqlite.editor.dialog.FileSelectorDialog;
import com.smallcake.sqlite.editor.fragment.MainViewFragment;
import com.smallcake.sqlite.editor.fragment.MainTablesFragment;
import com.smallcake.sqlite.editor.listener.ViewsListOnClick;
import com.smallcake.sqlite.editor.sqlite.OpenDatabase;
import com.smallcake.sqlite.editor.util.FileOperation;
import com.smallcake.sqlite.editor.util.UApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int CheckPermissionRequestCode_Associated = 0x1;
    private static final int CheckPermissionRequestCode_OpenOrCloseDatabase = 0x2;
    private static final int CheckPermissionRequestCode_CreateDatabase = 0x3;
    private static final int CheckPermissionRequestCode_BulkImport = 0x4;

    private String mFilePath = null;

    private DrawerLayout mDrawer;
    private Toolbar mToolbar;
    private NavigationView mSideNav;
    private ViewPager mPager;
    private BottomNavigationView mBottomNav;
    private TextView mDBFilePath;

    private ImageButton mBtnAdd;

    private List<Fragment> mPagerFragment = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        initView();

        initBroadcast();

        checkApplication();

        associatedOpen(getIntent());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean result = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                result = false;
                break;
            }
        }

        if (result) {

            switch (requestCode) {
                case CheckPermissionRequestCode_Associated:
                    File openFile = new File(mFilePath);
                    if (openFile.exists()) {
                        OpenDatabase.openDatabase(this, openFile.getPath());
                    }
                    break;

                case CheckPermissionRequestCode_OpenOrCloseDatabase:
                    openOrCloseDatabase();
                    break;

                case CheckPermissionRequestCode_CreateDatabase:
                    createDatabase();
                    break;

                case CheckPermissionRequestCode_BulkImport:
                    bulkImportFile();
                    break;
            }

        }
    }

    @Override
    protected void onDestroy() {
        cancelBroadcast();
        OpenDatabase.closeDatabase(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        associatedOpen(intent);
    }

    /**
     * 验证软件资源是否被非法篡改
     */
    private void checkApplication() {
        //验证资源
        String desc = getResources().getString(R.string.describe);
        if (!desc.contains("短毛猫") || !desc.contains("66492422@qq.com")) {
            Toast.makeText(this, "Illegal modification of resources is prohibited", Toast.LENGTH_SHORT).show();
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }


    /**
     * 关联打开
     */
    private void associatedOpen(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            mFilePath = uri.getPath();
            if (mFilePath != null && FileOperation.checkPermission(this, CheckPermissionRequestCode_Associated)) {
                File openFile = new File(mFilePath);
                if (openFile.exists()) {
                    OpenDatabase.closeDatabase(this);
                    OpenDatabase.openDatabase(this, openFile.getPath());
                }
            }
        }
    }

    /**
     * 数据库状态广播接收器
     * 用于接收数据库打开与关闭事件，并作出相应的响应
     */
    private BroadcastReceiver mDBBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case OpenDatabase.BROADCAST_OPEN_DATABASE:
                    mSideNav.getMenu().getItem(0).setTitle(R.string.close_database);
                    mDBFilePath.setText(intent.getStringExtra("file_path"));
                    break;
                case OpenDatabase.BROADCAST_CLOSE_DATABASE:
                    mSideNav.getMenu().getItem(0).setTitle(R.string.open_database);
                    mDBFilePath.setText(R.string.unopened_database_tip);
                    break;

                case OpenDatabase.BROADCAST_QUERY_STATUS:
                    if (intent.getIntExtra("trigger_source", -1) != ViewsListOnClick.Trigger_Source)
                        break;

                    String message = intent.getStringExtra("message");
                    if (intent.getIntExtra("code", -1) != 0 && message != null) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(message)
                                .setPositiveButton(R.string.confirm, null)
                                .create().show();
                    }
                    break;
            }
        }
    };

    /**
     * 查找所有视图
     */
    private void findView() {
        mDrawer = findViewById(R.id.drawer);
        mToolbar = findViewById(R.id.mToolBar);
        mSideNav = findViewById(R.id.mSideNav);
        mPager = findViewById(R.id.mViewPager);
        mBottomNav = findViewById(R.id.mBottomNavigation);
        mBtnAdd = findViewById(R.id.mBtnAdd);
        mDBFilePath = mSideNav.getHeaderView(0).findViewById(R.id.dbFilePath);

        ((TextView) mSideNav.getHeaderView(0).findViewById(R.id.tvApplicationInfo)).setText(getResources().getString(R.string.describe).replace("%version", UApplication.getAppVersion(getApplicationContext())));

    }

    /**
     * 配置视图
     */
    private void initView() {
        /*
         * Toolbar And Drawer
         * */
        mToolbar.setTitle(R.string.app_name);
        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.openDrawerContentDescRes, R.string.closeDrawerContentDescRes);
        toggle.syncState();

        mDrawer.addDrawerListener(toggle);

        /*
         * SlidNav
         * */
        mSideNav.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.openOrClose:
                        openOrCloseDatabase();
                        break;

                    case R.id.createDatabase:
                        createDatabase();
                        break;

                    case R.id.execSql:
                        execSql();
                        break;

                    case R.id.querySql:
                        querySql();
                        break;

                    case R.id.bulkImport:
                        bulkImport();
                        break;

                    case R.id.refreshData:
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                        break;

                }

                if (mDrawer.isDrawerOpen(Gravity.START)) {
                    mDrawer.closeDrawer(Gravity.START);
                }

                return false;
            }
        });

        /*
         * Pager Fragment
         * */
        mPagerFragment.add(new MainTablesFragment());
        mPagerFragment.add(new MainViewFragment());

        /*
         * PagerView
         * */
        mPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager(), mPagerFragment));
        mPager.setOffscreenPageLimit(mPagerFragment.size());
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mBottomNav.getMenu().getItem(i).setChecked(true);
                switch (i) {
                    case 0:
                        getSupportActionBar().setTitle(R.string.table);
                        break;

                    case 1:
                        getSupportActionBar().setTitle(R.string.view);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        /*
         * Bottom
         * 底部导航操作
         * */
        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.navTable:
                        getSupportActionBar().setTitle(R.string.table);
                        mPager.setCurrentItem(0);
                        break;

                    case R.id.navView:
                        getSupportActionBar().setTitle(R.string.view);
                        mPager.setCurrentItem(1);
                        break;
                }
                return false;
            }
        });

        /*
         * 添加表或视图按钮被点击
         * */
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTableOrView();
            }
        });

        /*
         * 其他项目
         * */
        getSupportActionBar().setTitle(R.string.table);

    }

    /**
     * 初始化本地广播接收器
     */
    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OpenDatabase.BROADCAST_OPEN_DATABASE);
        intentFilter.addAction(OpenDatabase.BROADCAST_CLOSE_DATABASE);
        intentFilter.addAction(OpenDatabase.BROADCAST_QUERY_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mDBBroadcast, intentFilter);
    }

    /**
     * 取消本地广播接收器
     */
    private void cancelBroadcast() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDBBroadcast);
    }

    /**
     * 打开或关闭数据库
     * 具体操作由OpenDatabase.DB是否为空来判断
     */
    private void openOrCloseDatabase() {

        if (OpenDatabase.DB == null) {
            //若当前未打开数据库
            FileSelectorDialog dialog = new FileSelectorDialog.Builder(this, FileOperation.getSDPath(), CheckPermissionRequestCode_OpenOrCloseDatabase)
                    .setTitle(getResources().getString(R.string.please_select_database))
                    .setSelectorMode(FileSelectorDialog.Builder.MODE_OPEN_FILE)
                    .setFileSelectorListener(new FileSelectorDialog.FileSelectorListener() {
                        @Override
                        public void SelectComplete(File file) {
                            OpenDatabase.openDatabase(MainActivity.this, file.getPath());
                        }
                    }).build();
            if (dialog != null) dialog.show();
        } else {
            //数据库以被打开
            OpenDatabase.closeDatabase(MainActivity.this);
        }

    }

    /**
     * 创建一个数据库
     */
    private void createDatabase() {
        FileSelectorDialog dialog = new FileSelectorDialog.Builder(this, FileOperation.getSDPath(), CheckPermissionRequestCode_CreateDatabase)
                .setTitle(getResources().getString(R.string.save_in))
                .setFileNameTip(getResources().getString(R.string.file_name_))
                .setSelectorMode(FileSelectorDialog.Builder.MODE_OPEN_FILE | FileSelectorDialog.Builder.MODE_INOUT_NAME)
                .setFileSelectorListener(new FileSelectorDialog.FileSelectorListener() {
                    @Override
                    public void SelectComplete(File file) {
                        createDatabase(file);
                    }
                }).build();
        if (dialog != null) dialog.show();
    }

    private void createDatabase(File file)
    {
        if (file.exists()) {
            Toast.makeText(MainActivity.this, R.string.file_creation_failed_1, Toast.LENGTH_SHORT).show();
        } else {
            try {
                if (file.getName().indexOf('.') == -1) {
                    file = new File(file.getPath() + ".db");
                }
                if (file.createNewFile()) {
                    final String tempDatabasePath = file.getPath();
                    Toast.makeText(MainActivity.this, R.string.file_creation_success, Toast.LENGTH_SHORT).show();
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.whether_to_open_a_new_database)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    OpenDatabase.closeDatabase(MainActivity.this);
                                    OpenDatabase.openDatabase(MainActivity.this, tempDatabasePath);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.file_creation_failed_2, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.file_creation_failed_3).replace("%e", e.toString()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 执行SQL
     */
    private void execSql() {
        if (OpenDatabase.DB == null) {
            Toast.makeText(MainActivity.this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(this, ExecSqlActivity.class));
        }
    }

    /**
     * 查询语句
     */
    private void querySql() {
        if (OpenDatabase.DB == null) {
            Toast.makeText(MainActivity.this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(this, QuerySqlActivity.class));
        }
    }

    /**
     * 批量导入
     */
    private void bulkImport() {
        if (OpenDatabase.DB == null) {
            Toast.makeText(MainActivity.this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setItems(R.array.import_operation, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which)
                            {
                                case 0:
                                    startActivity(new Intent(MainActivity.this, BatchImportActivity.class));
                                    break;

                                case 1:
                                    bulkImportFile();
                                    break;
                            }
                        }
                    })
                    .create().show();
        }
    }

    private void bulkImportFile()
    {
        final List<String> tables = OpenDatabase.getTables();
        if (tables == null && tables.size() == 0)
        {
            Toast.makeText(this, R.string.tables_is_null, Toast.LENGTH_SHORT).show();
            return;
        }
        final CharSequence[] item_list = new CharSequence[tables.size()];
        for (int i = 0; i < tables.size(); ++i)
        {
            item_list[i] = tables.get(i);
        }

        new AlertDialog.Builder(MainActivity.this)
                .setItems(item_list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText et_separator = new EditText(MainActivity.this);
                        et_separator.setText(BatchImportActivity.SEPARATOR);

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.bulk_import_separator)
                                .setView(et_separator)
                                .setNegativeButton(R.string.bulk_import, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        BatchImportActivity.SEPARATOR = et_separator.getText().toString();
                                        bulkImportFile(tables.get(which), BatchImportActivity.SEPARATOR);
                                    }
                                })
                                .setPositiveButton(R.string.cancel, null)
                                .create().show();
                    }
                })
                .setTitle(R.string.table)
                .create().show();

    }

    private void bulkImportFile(final String table, final String separator)
    {
        FileSelectorDialog dialog = new FileSelectorDialog.Builder(this, FileOperation.getSDPath(), CheckPermissionRequestCode_OpenOrCloseDatabase)
                .setTitle(getResources().getString(R.string.bulk_import))
                .setSelectorMode(FileSelectorDialog.Builder.MODE_OPEN_FILE)
                .setFileSelectorListener(new FileSelectorDialog.FileSelectorListener() {
                    @Override
                    public void SelectComplete(File file) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_load,  null);
                                AlertDialog loadDialog = new AlertDialog.Builder(MainActivity.this)
                                        .setView(view)
                                        .setCancelable(false)
                                        .create();
                                loadDialog.show();

                                loadDialog.cancel();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            }
                        }).start();

                    }
                }).build();
        if (dialog != null) dialog.show();
    }

    /**
     * 添加表或视图
     */
    private void addTableOrView() {
        if (OpenDatabase.DB == null) {
            Toast.makeText(MainActivity.this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mPager.getCurrentItem() == 0) {
            //创建表
            (new CreateTableDialog(this)).show();
        } else {
            //创建视图
            createView(null, null);
        }
    }

    /**
     * 创建视图
     */
    private void createView(String name, String sql) {
        final EditText query_name = new EditText(this);
        final EditText query_sql = new EditText(this);

        query_name.setInputType(InputType.TYPE_CLASS_TEXT);

        query_name.setText(name);
        query_sql.setText(sql);
        //弹出第一个对话框，要求用户输入视图名称
        new AlertDialog.Builder(this)
                .setView(query_name)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //用户输入视图名称，并点击确定钮后触发这事件
                        //判断用户是否输入视图名，如果未输入，则让用户重新输入
                        if (query_name.getText().toString().length() == 0) {
                            Toast.makeText(MainActivity.this, R.string.please_enter_view_name, Toast.LENGTH_SHORT).show();
                            createView(query_name.getText().toString(), query_sql.getText().toString());
                        } else {
                            //用户以输入视图名
                            //弹出第二个对话框，要求用户输入视图语句
                            new AlertDialog.Builder(MainActivity.this)
                                    .setView(query_sql)
                                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //用户输入视图语句后点击确认钮触发这个事件
                                            //判断用户是否已输入的视图，若未输入，则弹出提示并让用户输入
                                            if (query_name.getText().toString().length() == 0) {
                                                Toast.makeText(MainActivity.this, R.string.please_enter_view_sql, Toast.LENGTH_SHORT).show();
                                                //重新创建对话框，保留用户输入的数据
                                                createView(query_name.getText().toString(), query_sql.getText().toString());
                                            } else {
                                                //用户输入完成，开始合成并执行语句
                                                try {
                                                    OpenDatabase.DataBase.execSQL("CREATE VIEW " + query_name.getText().toString() + " AS " + query_sql.getText().toString());
                                                    Toast.makeText(MainActivity.this, R.string.create_view_success, Toast.LENGTH_SHORT).show();
                                                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                                                } catch (Exception e) {
                                                    //执行语句异常，弹出异常提示，并等待用户点击确认后保留数据重新开始输入
                                                    new AlertDialog.Builder(MainActivity.this)
                                                            .setMessage(e.toString())
                                                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    createView(query_name.getText().toString(), query_sql.getText().toString());
                                                                }
                                                            }).create().show();//异常提示对话框弹出
                                                }
                                            }
                                        }
                                    })
                                    .setTitle(R.string.please_enter_view_sql)
                                    .setNegativeButton(R.string.cancel, null).create().show();//输入视图语句对话框弹出
                        }
                    }
                })
                .setTitle(R.string.please_enter_view_name)
                .setNegativeButton(R.string.cancel, null).create().show();//输入视图名称对话框弹出
    }

}

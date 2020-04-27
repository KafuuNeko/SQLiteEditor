package com.smallcake.sqlite.editor.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.smallcake.sqlite.editor.R;
import com.smallcake.sqlite.editor.sqlite.OpenDatabase;

import java.util.Date;

public class QuerySqlActivity extends AppCompatActivity {
    private final static int Trigger_Source = 0x2;
    private static String mSaveSql = "";

    private TextView mTvQueryStatus;
    private ImageButton mBtnQuerySql;
    private EditText mEtSql;
    private Toolbar mToolBar;

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (OpenDatabase.DB == null) {
            Toast.makeText(this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_query_sql);

        initView();
        initGesture();
        initBroadcast();
    }

    @Override
    protected void onDestroy() {
        cancelBroadcast();
        super.onDestroy();
    }


    private void initView() {
        mTvQueryStatus = findViewById(R.id.mTvQueryStatus);
        mBtnQuerySql = findViewById(R.id.mBtnQuerySql);
        mEtSql = findViewById(R.id.mEtSql);
        mToolBar = findViewById(R.id.mToolBar);

        mEtSql.setText(mSaveSql);

        mEtSql.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mSaveSql = mEtSql.getText().toString();
            }
        });

        mToolBar.setTitle(R.string.query_sql);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTvQueryStatus.setText(R.string.click_the_blue_button_on_the_right_to_execute);
        mTvQueryStatus.setTextColor(Color.GREEN);

        mBtnQuerySql.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEtSql.getText().toString().length() > 0) {
                    Intent intent = new Intent(QuerySqlActivity.this, ViewQueryActivity.class);
                    intent.putExtra("query_sql", mEtSql.getText().toString());
                    intent.putExtra("trigger_source", Trigger_Source);
                    startActivity(intent);
                }

            }
        });
    }

    private void initGesture() {
        mGestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getX() - e1.getX() > 60 && velocityX > 1000) {
                    finish();
                }
                return false;
            }
        });

        mEtSql.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OpenDatabase.BROADCAST_QUERY_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mQueryStatus, filter);
    }

    private void cancelBroadcast() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mQueryStatus);
    }

    private BroadcastReceiver mQueryStatus = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra("trigger_source", -1) != Trigger_Source) return;

            int code = intent.getIntExtra("code", -1);
            String msg = intent.getStringExtra("message");
            if (msg == null) {
                msg = "null";
            }

            if (code == 0) {
                mTvQueryStatus.setText(new Date().toString() + getResources().getString(R.string.query_complete));
                mTvQueryStatus.setTextColor(Color.GREEN);
            } else {
                mTvQueryStatus.setText(msg);
                mTvQueryStatus.setTextColor(Color.RED);
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
}

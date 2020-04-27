package com.smallcake.sqlite.editor.activity;

import android.content.Intent;
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

public class ExecSqlActivity extends AppCompatActivity {
    private static String mSaveSql = "";

    private TextView mTvExecStatus;
    private ImageButton mBtnExecSql;
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

        setContentView(R.layout.activity_exec_sql);

        initView();
        initGesture();
    }

    private void initView() {
        mTvExecStatus = findViewById(R.id.tvExecStatus);
        mBtnExecSql = findViewById(R.id.mBtnExecSql);
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

        mToolBar.setTitle(R.string.exec_sql);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTvExecStatus.setText(R.string.click_the_blue_button_on_the_right_to_execute);
        mTvExecStatus.setTextColor(Color.GREEN);

        mBtnExecSql.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    OpenDatabase.DataBase.execSQL(mEtSql.getText().toString());
                    mTvExecStatus.setText(new Date().toString() + getResources().getString(R.string.exec_complete));
                    mTvExecStatus.setTextColor(Color.GREEN);
                    //发送数据更新广播
                    LocalBroadcastManager.getInstance(ExecSqlActivity.this).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                } catch (Exception e) {
                    mTvExecStatus.setText(e.toString());
                    mTvExecStatus.setTextColor(Color.RED);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}

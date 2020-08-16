package com.kafuuneko.sqliteeditor.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.kafuuneko.sqliteeditor.R;
import com.kafuuneko.sqliteeditor.sqlite.OpenDatabase;

import java.util.List;

public class BatchImportActivity extends AppCompatActivity {
    private static String mSaveImportData = "";
    public static String SEPARATOR = "-----";

    private Spinner mTable;
    private ImageButton mBtnImport;
    private EditText mImportData;
    private Toolbar mToolBar;
    private EditText mEdSeparator;

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (OpenDatabase.DB == null) {
            Toast.makeText(this, R.string.unopened_database_tip, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_bulk_import);

        initView();
        initGesture();
    }

    private void initView() {
        mBtnImport = findViewById(R.id.mBtnImport);
        mImportData = findViewById(R.id.mImportData);
        mToolBar = findViewById(R.id.mToolBar);
        mTable = findViewById(R.id.mTable);
        mEdSeparator = findViewById(R.id.mEdSeparator);

        mImportData.setText(mSaveImportData);
        mEdSeparator.setText(SEPARATOR);

        mImportData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mSaveImportData = mImportData.getText().toString();
            }
        });

        mEdSeparator.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SEPARATOR = mEdSeparator.getText().toString();
            }
        });

        mToolBar.setTitle(R.string.bulk_import);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        mBtnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String select_table = (String) mTable.getSelectedItem();
                int row_number = OpenDatabase.BatchImport(select_table, mImportData.getText().toString(), mEdSeparator.getText().toString());
                if (row_number == -1) {
                    Toast.makeText(BatchImportActivity.this, R.string.bulk_import_fail, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BatchImportActivity.this, getResources().getString(R.string.bulk_import_success).replace("%count%", row_number + ""), Toast.LENGTH_SHORT).show();
                }
            }
        });

        List<String> tables = OpenDatabase.getTables();
        if (tables == null || tables.size() == 0) {
            Toast.makeText(BatchImportActivity.this, R.string.tables_is_null, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            SpinnerAdapter adapter = new ArrayAdapter<>(BatchImportActivity.this, android.R.layout.simple_list_item_1, tables);
            mTable.setAdapter(adapter);
        }

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

        mImportData.setOnTouchListener(new View.OnTouchListener() {
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

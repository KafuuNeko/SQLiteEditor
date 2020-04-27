package com.smallcake.sqlite.editor.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.smallcake.sqlite.editor.R;
import com.smallcake.sqlite.editor.listener.TablesListOnClick;
import com.smallcake.sqlite.editor.sqlite.OpenDatabase;

import java.util.List;

public class MainTablesFragment extends Fragment {
    private View mRootView = null;
    private ListView mTablesList = null;
    private TextView mTvTip = null;

    private Context mContext = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView != null) {
            ViewGroup parent = (ViewGroup) container.getParent();
            parent.removeView(mRootView);
        } else {
            mRootView = inflater.inflate(R.layout.fragment_main_tables, null);
        }

        mContext = getContext();

        initView();

        initBroadcast();

        if (OpenDatabase.DB != null) {
            eventOpenDatabase();
        } else {
            eventCloseDatabase();
        }
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelBroadcast();
        mRootView = null;
    }

    private void initView() {
        mTablesList = mRootView.findViewById(R.id.lvTablesList);
        mTvTip = mRootView.findViewById(R.id.tvTip);
        mTablesList.setOnItemClickListener(new TablesListOnClick(getContext(), mTablesList));
    }

    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OpenDatabase.BROADCAST_OPEN_DATABASE);
        filter.addAction(OpenDatabase.BROADCAST_CLOSE_DATABASE);
        filter.addAction(OpenDatabase.BROADCAST_DATA_UPDATE);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mDBroadcast, filter);
    }

    private void cancelBroadcast() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mDBroadcast);
    }

    private BroadcastReceiver mDBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case OpenDatabase.BROADCAST_OPEN_DATABASE:
                    eventOpenDatabase();
                    break;

                case OpenDatabase.BROADCAST_CLOSE_DATABASE:
                    eventCloseDatabase();
                    break;

                case OpenDatabase.BROADCAST_DATA_UPDATE:
                    refreshTablesList();
                    break;
            }
        }
    };

    private void refreshTablesList() {
        List<String> tables = OpenDatabase.getTables();
        if (tables != null && tables.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, tables);
            mTablesList.setAdapter(adapter);
            mTvTip.setText(null);
        } else {
            mTablesList.setAdapter(null);
            mTvTip.setText(R.string.tables_is_null);
        }
    }

    private void eventOpenDatabase() {
        refreshTablesList();
    }

    private void eventCloseDatabase() {
        mTablesList.setAdapter(null);
        mTvTip.setText(R.string.unopened_database_tip);
    }
}

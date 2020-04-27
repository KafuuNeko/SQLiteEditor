package com.smallcake.sqlite.editor.listener;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.smallcake.sqlite.editor.R;
import com.smallcake.sqlite.editor.activity.TableDataEdit;
import com.smallcake.sqlite.editor.activity.ViewQueryActivity;
import com.smallcake.sqlite.editor.sqlite.OpenDatabase;

public class ViewsListOnClick implements AdapterView.OnItemClickListener {
    public static final int Trigger_Source = 0x1;
    private Context mContext;
    private ListView mListView;

    public ViewsListOnClick(Context context, ListView listView) {
        mContext = context;
        mListView = listView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BaseAdapter adapter = (BaseAdapter) mListView.getAdapter();
        final String operationView = (String) adapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(operationView);
        builder.setItems(R.array.views_operation, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                switch (which) {
                    case 0:
                        //查看视图
                        intent = new Intent(mContext, ViewQueryActivity.class);
                        intent.putExtra("title", operationView);
                        intent.putExtra("query_sql", "SELECT * FROM [" + operationView + "]");
                        intent.putExtra("trigger_source", Trigger_Source);
                        mContext.startActivity(intent);
                        break;

                    case 1:
                        //删除视图
                        new AlertDialog.Builder(mContext)
                                .setMessage(mContext.getResources().getString(R.string.delete_view_warning).replace("%view_name", operationView))
                                .setTitle(R.string.warning)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            OpenDatabase.DataBase.execSQL("DROP VIEW [" + operationView + "]");
                                            Toast.makeText(mContext, R.string.exec_success, Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            new AlertDialog.Builder(mContext).setMessage(e.toString()).setPositiveButton(R.string.confirm, null)
                                                    .create().show();
                                        }
                                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                                    }
                                }).create().show();
                        break;

                    case 2:
                        //视图结构
                        final String createSql = OpenDatabase.getViewCreateSql(operationView);
                        new AlertDialog.Builder(mContext)
                                .setMessage(createSql)
                                .setTitle(operationView)
                                .setPositiveButton(R.string.cancel, null)
                                .setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(mContext.CLIPBOARD_SERVICE);
                                        ClipData mClipData = ClipData.newPlainText("Label", createSql);
                                        cm.setPrimaryClip(mClipData);
                                        Toast.makeText(mContext, R.string.copy_success, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .create().show();
                        break;
                }
            }
        });

        builder.create().show();

    }
}

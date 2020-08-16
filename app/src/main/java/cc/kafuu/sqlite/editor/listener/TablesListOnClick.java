package cc.kafuu.sqlite.editor.listener;

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

import cc.kafuu.sqlite.editor.R;
import cc.kafuu.sqlite.editor.activity.TableDataEdit;
import cc.kafuu.sqlite.editor.sqlite.OpenDatabase;

public class TablesListOnClick implements AdapterView.OnItemClickListener {
    private Context mContext;
    private ListView mListView;

    public TablesListOnClick(Context context, ListView listView) {
        mContext = context;
        mListView = listView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

        final BaseAdapter adapter = (BaseAdapter) mListView.getAdapter();
        final String operationTable = (String) adapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(operationTable);
        builder.setItems(R.array.tables_operation, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                switch (which) {
                    case 0:
                        //编辑数据
                        intent = new Intent(mContext, TableDataEdit.class);
                        intent.putExtra("table_name", operationTable);
                        mContext.startActivity(intent);
                        break;

                    case 1:
                        //查看结构

                        final String createSql = OpenDatabase.getTableCreateSql(operationTable);
                        new AlertDialog.Builder(mContext)
                                .setMessage(createSql)
                                .setTitle(operationTable)
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

                    case 2:
                        //删除此表
                        new AlertDialog.Builder(mContext)
                                .setMessage(mContext.getResources().getString(R.string.delete_table_warning).replace("%table_name", operationTable))
                                .setTitle(R.string.warning)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            OpenDatabase.DataBase.execSQL("DROP TABLE [" + operationTable + "]");
                                            Toast.makeText(mContext, R.string.exec_success, Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            new AlertDialog.Builder(mContext).setMessage(e.toString()).setPositiveButton(R.string.confirm, null)
                                                    .create().show();
                                        }
                                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                                    }
                                }).create().show();
                        break;

                    case 3:
                        //清空数据
                        new AlertDialog.Builder(mContext)
                                .setMessage(mContext.getResources().getString(R.string.clear_table_data_warning).replace("%table_name", operationTable))
                                .setTitle(R.string.warning)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            OpenDatabase.DataBase.execSQL("DELETE FROM [" + operationTable + "]");
                                            Toast.makeText(mContext, R.string.exec_success, Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            new AlertDialog.Builder(mContext).setMessage(e.toString()).setPositiveButton(R.string.confirm, null)
                                                    .create().show();
                                        }

                                    }
                                }).create().show();
                        break;
                }
            }
        });

        builder.create().show();


    }
}

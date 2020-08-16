package com.kafuuneko.sqliteeditor.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.kafuuneko.sqliteeditor.R;
import com.kafuuneko.sqliteeditor.util.StrOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加新行对话框类
 */
public class AddRowDialog implements AdapterView.OnItemClickListener {

    private Context mContext;
    private List<ColumnData> mColumns = new ArrayList<>();
    private ListView listView = null;

    public AddRowDialog(Context context, List<String> columns) {
        mContext = context;
        for (String columnsName : columns) {
            mColumns.add(new ColumnData(columnsName));
        }

    }

    public void show(final CompleteListener listener, final String tableName) {
        if (mColumns.size() == 0) return;

        listView = new ListView(mContext);
        listView.setAdapter(new ColumnsAdapter());
        listView.setOnItemClickListener(this);

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.add_new_row)
                .setView(listView)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sql_columns = "";
                        String sql_data = "";

                        String[] data_list = new String[mColumns.size()];

                        for (int i = 0; i < mColumns.size(); i++) {
                            if (sql_columns.length() == 0) {
                                sql_columns += mColumns.get(i).columnName;
                                sql_data += "?";
                            } else {
                                sql_columns += "," + mColumns.get(i).columnName;
                                sql_data += ",?";
                            }
                            data_list[i] = mColumns.get(i).columnData;
                        }
                        String sql = "INSERT INTO " + tableName + "(" + sql_columns + ") VALUES(" + sql_data + ")";
                        if (!listener.complete(sql, data_list)) {
                            show(listener, tableName);
                        }

                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

        final EditText dataView = new EditText(mContext);
        dataView.setText(mColumns.get(position).columnData);

        new AlertDialog.Builder(mContext)
                .setView(dataView)
                .setTitle(mColumns.get(position).columnName)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mColumns.get(position).columnData = dataView.getText().toString();
                        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }


    public interface CompleteListener {
        boolean complete(String sql, String[] data_list);
    }

    private class ColumnsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mColumns.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ColumnData getItem(int position) {
            return mColumns.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
            }


            ((TextView) view).setText(StrOperation.limitStringLength(getItem(position).toString(), true));

            return view;
        }
    }

    private class ColumnData {
        public String columnName;
        public String columnData;

        public ColumnData(String columnName) {
            this.columnName = columnName;
            this.columnData = null;
        }

        public ColumnData(String columnName, String columnData) {
            this.columnName = columnName;
            this.columnData = columnData;
        }

        @Override
        public String toString() {
            return columnName + ":" + (columnData == null ? "null" : columnData);
        }
    }

}

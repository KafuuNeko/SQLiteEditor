package com.smallcake.sqlite.editor.sqlite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smallcake.sqlite.editor.util.StrOperation;

import java.util.ArrayList;
import java.util.List;

public class OpenDatabase extends SQLiteOpenHelper {
    public static OpenDatabase DB = null;
    public static SQLiteDatabase DataBase = null;

    //数据库打开事件
    public static final String BROADCAST_OPEN_DATABASE = "database.open";

    //数据库关闭事件
    public static final String BROADCAST_CLOSE_DATABASE = "database.close";

    //数据库刷新事件
    public static final String BROADCAST_DATA_UPDATE = "database.data.update";

    //数据库执行Query状态事件
    public static final String BROADCAST_QUERY_STATUS = "database.query.sql.status";

    /**
     * OpenDatabase 动态内容 开始
     */

    private Context mContext;

    private OpenDatabase(Context context, String path) {
        super(context, path, null, 1);
        mContext = context;
        DataBase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * OpenDatabase 动态内容 结束
     * */

    /**
     * 尝试打开指定SQLite数据库
     */
    public static void openDatabase(Context context, String path) {
        closeDatabase(context);

        DB = new OpenDatabase(context, path);

        Intent intent = new Intent(BROADCAST_OPEN_DATABASE);
        intent.putExtra("file_path", path);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * 尝试关闭已打开带Database
     * 若Database未打开，则不进行任何操作
     */
    public static void closeDatabase(Context context) {
        if (DB != null) {
            DataBase.close();
            DB.close();
            DB = null;
            DataBase = null;
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BROADCAST_CLOSE_DATABASE));
        }
    }


    /**
     * 获取所有表
     */
    public static List<String> getTables() {
        if (DB == null) return null;

        List<String> tables = new ArrayList<>();
        try {
            Cursor cursor = DataBase.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                //过滤系统表
                if (!name.equals("android_metadata") && !name.equals("sqlite_sequence")) {
                    tables.add(name);
                }

            }
            cursor.close();

        } catch (Exception e) {

        }
        return tables;
    }

    /**
     * 获取所有视图
     */
    public static List<String> getViews() {
        if (DB == null) return null;

        List<String> tables = new ArrayList<>();
        try {
            Cursor cursor = DataBase.rawQuery("SELECT name FROM sqlite_master WHERE type='view' ORDER BY name", null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                tables.add(name);
            }
            cursor.close();

        } catch (Exception e) {

        }
        return tables;
    }

    /**
     * 获取表格创建语句
     */
    public static String getTableCreateSql(String table) {
        //SELECT sql FROM sqlite_master WHERE type='table' AND name = 'table_name'
        if (DB == null) return null;
        String sql = "";
        Cursor cursor = DataBase.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name = ?", new String[]{table});
        while (cursor.moveToNext()) {
            sql += cursor.getString(0);
        }
        cursor.close();
        return sql;
    }

    /**
     * 获取视图创建语句
     */
    public static String getViewCreateSql(String view) {
        //SELECT sql FROM sqlite_master WHERE type='table' AND name = 'table_name'
        if (DB == null) return null;
        String sql = "";
        Cursor cursor = DataBase.rawQuery("SELECT sql FROM sqlite_master WHERE type='view' AND name = ?", new String[]{view});
        while (cursor.moveToNext()) {
            sql += cursor.getString(0);
        }
        cursor.close();
        return sql;
    }

    /**
     * 制取表格数据
     */
    public static ArrayList<ArrayList<String>> getTableData(String table, int start, int number, List<Long> rowid) {
        if (DB == null) return null;

        ArrayList<ArrayList<String>> result = new ArrayList<>();

        Cursor cursor = DataBase.rawQuery("SELECT rowid,* FROM [" + table + "] LIMIT " + start + "," + number, null);

        //制作第一列数据
        ArrayList<String> first_row = new ArrayList<>();
        first_row.add("序号");
        for (int ci = 1; ci < cursor.getColumnCount(); ci++) {
            first_row.add(cursor.getColumnName(ci));
        }
        result.add(first_row);

        //制作数据
        ArrayList<String> row;
        int current_index = start;
        while (cursor.moveToNext()) {
            row = new ArrayList<>();
            row.add("" + (++current_index));
            rowid.add(cursor.getLong(0));

            for (int ci = 1; ci < cursor.getColumnCount(); ci++) {
                String data = cursor.getString(ci);
                data = StrOperation.limitStringLength(data, true);
                row.add(data);
            }
            result.add(row);
        }

        cursor.close();
        return result;
    }

    /**
     * 制取表格数据
     * 无表头
     */
    public static void getTableData(String table, ArrayList<ArrayList<String>> list, int start, int number, List<Long> rowid) {
        if (DB == null) return;

        Cursor cursor = DataBase.rawQuery("SELECT rowid,* FROM [" + table + "] LIMIT " + start + "," + number, null);

        //制作数据
        ArrayList<String> row;
        int current_index = start;
        while (cursor.moveToNext()) {
            row = new ArrayList<>();
            row.add("" + (++current_index));
            rowid.add(cursor.getLong(0));
            for (int ci = 1; ci < cursor.getColumnCount(); ci++) {
                String data = cursor.getString(ci);
                data = StrOperation.limitStringLength(data, true);
                row.add(data);
            }
            list.add(row);
        }

        cursor.close();

    }


    /**
     * 获取指定表数据行数
     */
    public static int getTableDataCount(String table) {
        if (DB == null) return 0;

        Cursor cursor = DataBase.rawQuery("SELECT COUNT(*) FROM [" + table + "]", null);

        int result = 0;
        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        return result;

    }

    /**
     * 提供query sql 制取表格数据，带表头
     */
    public static ArrayList<ArrayList<String>> getQueryTableData(String query_sql) throws Exception {
        if (DB == null) return null;

        ArrayList<ArrayList<String>> result = new ArrayList<>();

        Cursor cursor = DataBase.rawQuery(query_sql, null);

        //制作第一列数据
        ArrayList<String> first_row = new ArrayList<>();
        first_row.add("序号");
        for (int ci = 0; ci < cursor.getColumnCount(); ci++) {
            first_row.add(cursor.getColumnName(ci));
        }
        result.add(first_row);

        //制作数据
        ArrayList<String> row;
        int current_index = 0;
        while (cursor.moveToNext()) {
            row = new ArrayList<>();
            row.add("" + (++current_index));
            for (int ci = 0; ci < cursor.getColumnCount(); ci++) {
                String data = cursor.getString(ci);
                //data = StrOperation.limitStringLength(data);
                row.add(data);
            }
            result.add(row);
        }

        cursor.close();
        return result;
    }

    /**
     * 提供rowid删除数据表中指定带记录
     */
    public static void deleteRow(String table_name, long rowid) {
        //String sql = "DELETE FROM [" + table_name + "] WHERE rowid IN(SELECT rowid FROM [" + table_name + "] LIMIT 1 OFFSET " + index + ")";
        String sql = "DELETE FROM [" + table_name + "] WHERE rowid=" + rowid;
        DataBase.execSQL(sql);
    }

    /**
     * 提供rowid更新数据表中指定记录
     */
    public static boolean updateRowData(String table, long rowid, String column, String data) {
        if (DB == null) return false;
        //String sql = "UPDATE ["+ table +"] SET " + column + "=? WHERE rowid IN(SELECT rowid FROM [" + table + "] LIMIT 1 OFFSET " + row + ")";

        String sql = "UPDATE [" + table + "] SET " + column + "=? WHERE rowid=" + rowid;

        Log.d("updateRowData", sql);
        try {
            DataBase.execSQL(sql, new String[]{data});
            return true;
        } catch (Exception e) {
            Log.e("updateRowData", e.toString());
            return false;
        }
    }

    /**
     * 提供rowid获取数据表中指定记录数据
     */
    public static String readRowData(String table, long rowid, String column) {
        if (DB == null) return null;
        String result = null;
        try {
            //String sql = "SELECT "+column+" FROM [" + table + "] LIMIT 1 OFFSET " + row;
            String sql = "SELECT " + column + " FROM [" + table + "] WHERE rowid=" + rowid;
            Log.d("readRowData", sql);
            Cursor cursor = DataBase.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
            cursor.close();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static int BatchImport(String table_name, String import_data, String col_split) {

        int result_count = 0;

        DataBase.beginTransaction();

        try {
            String[] rows = import_data.split("\n");

            for (String row_data : rows) {
                String[] cols = row_data.split(col_split);

                String values_part = null;
                for (int i = 0; i < cols.length; ++i) {
                    values_part = (values_part == null) ? "?" : (values_part + ",?");
                }

                DataBase.execSQL("INSERT INTO " + table_name + " VALUES(" + values_part + ")", cols);
                ++result_count;

            }

            DataBase.setTransactionSuccessful();
        } catch (Exception e) {
            result_count = -1;

        } finally {
            DataBase.endTransaction();

        }

        return result_count;
    }

}

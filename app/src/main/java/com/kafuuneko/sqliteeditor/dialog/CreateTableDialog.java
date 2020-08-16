package com.kafuuneko.sqliteeditor.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.kafuuneko.sqliteeditor.R;
import com.kafuuneko.sqliteeditor.sqlite.OpenDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建表对话框类
 */
public class CreateTableDialog implements AdapterView.OnItemClickListener {
    private Context mContext;

    private GridView mTableStruct;
    private EditText mTableName;
    private String mTableNameStr;
    List<String> mTableStructItem = new ArrayList<>();

    public CreateTableDialog(Context context) {
        mContext = context;

        mTableStructItem.add(context.getResources().getString(R.string.field_name));
        mTableStructItem.add(context.getResources().getString(R.string.field_struct));
    }

    public void show() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_create_table, null);

        mTableStruct = view.findViewById(R.id.tableStruct);
        ImageButton btnAdd = view.findViewById(R.id.btnAddField);
        mTableStruct.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mTableStructItem));
        mTableStruct.setOnItemClickListener(this);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update_field(0);
            }
        });

        mTableName = view.findViewById(R.id.tableName);
        mTableName.setText(mTableNameStr);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mTableNameStr = mTableName.getText().toString();
                String sql = "CREATE TABLE " + mTableName.getText();
                String sql_field = null;
                for (int i = 2; i < mTableStructItem.size(); i += 2) {
                    if (sql_field == null) {
                        sql_field = mTableStructItem.get(i) + " " + mTableStructItem.get(i + 1);
                    } else {
                        sql_field += "," + mTableStructItem.get(i) + " " + mTableStructItem.get(i + 1);
                    }
                }

                sql += "(" + sql_field + ")";
                try {
                    OpenDatabase.DataBase.execSQL(sql);
                    Toast.makeText(mContext, R.string.create_table_success, Toast.LENGTH_SHORT).show();
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(OpenDatabase.BROADCAST_DATA_UPDATE));
                } catch (Exception e) {
                    AlertDialog.Builder error = new AlertDialog.Builder(mContext);
                    error.setMessage(e.toString());
                    error.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            show();
                        }
                    });
                    error.create().show();

                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.create().show();

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        update_field(position);
    }

    private void update_field(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View dialog_view = LayoutInflater.from(mContext).inflate(R.layout.dialog_field_struct, null);

        final EditText fieldName = dialog_view.findViewById(R.id.fieldName);
        final MultiAutoCompleteTextView fieldStruct = dialog_view.findViewById(R.id.fieldStruct);
        final int startPos = GridRowPosition(position, 2);

        fieldStruct.setTokenizer(new CommaTokenizer());

        ArrayList<String> autoComplete = new ArrayList<>();

        autoComplete.add("INT");
        autoComplete.add("BIGINT");
        autoComplete.add("BLOB");
        autoComplete.add("BOOLEAN");
        autoComplete.add("CHAR");
        autoComplete.add("DATE");
        autoComplete.add("DATETIME");
        autoComplete.add("DECIMAL");
        autoComplete.add("DOUBLE");
        autoComplete.add("INTEGER");
        autoComplete.add("NONE");
        autoComplete.add("NUMERIC");
        autoComplete.add("REAL");
        autoComplete.add("STRING");
        autoComplete.add("TEXT");
        autoComplete.add("TIME");
        autoComplete.add("VARCHAR");
        autoComplete.add("PRIMARY");
        autoComplete.add("KEY");
        autoComplete.add("AUTOINCREMENT");

        fieldStruct.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, autoComplete));

        builder.setView(dialog_view);

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (position < 2) {
                    mTableStructItem.add(fieldName.getText().toString());
                    mTableStructItem.add(fieldStruct.getText().toString());
                } else {
                    mTableStructItem.set(startPos, fieldName.getText().toString());
                    mTableStructItem.set(startPos + 1, fieldStruct.getText().toString());
                }
                mTableStruct.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mTableStructItem));
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        if (position >= 2) {

            fieldName.setText((String) mTableStruct.getAdapter().getItem(startPos));
            fieldStruct.setText((String) mTableStruct.getAdapter().getItem(startPos + 1));

            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mTableStructItem.remove(startPos);
                    mTableStructItem.remove(startPos);
                    mTableStruct.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mTableStructItem));
                }
            });

            builder.setTitle(R.string.edit_field);
        } else {
            builder.setTitle(R.string.add_field);
        }

        builder.create().show();
    }

    public static int GridRowPosition(int pos, int rowSize) {
        //pos与rowSize都是从0开始
        if (pos == 0) {
            return 0;
        }

        return pos - pos % rowSize;
    }


    private static class CommaTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;

            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (text.charAt(i) == ' ') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            if (i > 0 && text.charAt(i - 1) == ' ') {
                return text;
            } else {
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + " ");
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                            Object.class, sp, 0);
                    return sp;
                } else {
                    return text + " ";
                }
            }
        }
    }

}

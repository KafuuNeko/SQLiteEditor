package com.smallcake.sqlite.editor.util;

public class StrOperation {

    /**
     * 限制显示文本长度
     */
    public static String limitStringLength(String str, boolean replaceNewline) {
        if (str == null) return null;
        if (str.length() > 100) {
            str = str.substring(0, 100);
            str += "...";
        }
        if (replaceNewline) {
            str = str.replace('\n', ' ');
        }
        return str;
    }

}

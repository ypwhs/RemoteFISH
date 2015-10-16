package com.yangpeiwen.remotefish;

/**
 * Created by ypw
 * on 2015-09-04 下午2:24.
 */

public class Common {

    public static boolean find(String text, String w) {
        // 从text里找w，有则返回真
        return text.contains(w);
    }

    public static String zhongjian(String text, String textl, String textr) {
        // ==================================================================
        // 函数名：zhongjian
        // 作者：ypw
        // 功能：取中间文本,这是对于不用考虑起始位置的情况的zhongjian函数重写
        // 输入参数：text,textl(左边的text),textr(右边的text)
        // 返回值：String
        // ==================================================================
        return zhongjian(text, textl, textr, 0);
    }
    public static String zhongjian(String text, String textl, String textr, int start) {
        // ==================================================================
        // 函数名：zhongjian
        // 作者：ypw
        // 功能：取中间文本,比如
        // zhongjian("abc123efg","abc","efg",0)返回123
        // 输入参数：text,textl(左边的text),textr(右边的text),start(起始寻找位置)
        // 返回值：String
        // ==================================================================
        int left = text.indexOf(textl, start);
        int right = text.indexOf(textr, left + textl.length());
        String zhongjianString = "";
        try{
            zhongjianString = text.substring(left + textl.length(), right);
        }catch (Exception ignore){}
        return zhongjianString;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv.toUpperCase());
        }
        return stringBuilder.toString();
    }


}

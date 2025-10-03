package org.noear.snack4.codec.util;

import org.noear.snack4.codec.DEFAULTS;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 时间解析工具
 * 
 * @author noear 2025/10/3 created
 */
public class DateUtil {
    public static final String FORMAT_29 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";//XXX: +08:00
    public static final String FORMAT_27 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX'Z'";
    public static final String FORMAT_25 = "yyyy-MM-dd'T'HH:mm:ss+HH:mm";
    public static final String FORMAT_24_ISO08601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String FORMAT_23_a = "yyyy-MM-dd HH:mm:ss,SSS";
    public static final String FORMAT_23_b = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FORMAT_23_t = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String FORMAT_22 = "yyyyMMddHHmmssSSSZ";//z: +0000
    public static final String FORMAT_19_ISO = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String FORMAT_19_a = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_19_b = "yyyy/MM/dd HH:mm:ss";
    public static final String FORMAT_19_c = "yyyy.MM.dd HH:mm:ss";
    public static final String FORMAT_18 = "HH:mm:ss.SSS+HH:mm";
    public static final String FORMAT_17 = "yyyyMMddHHmmssSSS";
    public static final String FORMAT_16_a = "yyyy-MM-dd HH:mm";
    public static final String FORMAT_16_b = "yyyy/MM/dd HH:mm";
    public static final String FORMAT_16_c = "yyyy.MM.dd HH:mm";
    public static final String FORMAT_14 = "yyyyMMddHHmmss";
    public static final String FORMAT_12 = "HH:mm:ss.SSS";
    public static final String FORMAT_10_a = "yyyy-MM-dd";
    public static final String FORMAT_10_b = "yyyy/MM/dd";
    public static final String FORMAT_10_c = "yyyy.MM.dd";
    public static final String FORMAT_9 = "HH时mm分ss秒";
    public static final String FORMAT_8_a = "HH:mm:ss";
    public static final String FORMAT_8_b = "yyyyMMdd";

    /**
     * 解析时间
     */
    public static Date parse(String val) throws ParseException {
        if (val == null) {
            return null;
        }

        final int len = val.length();

        if (len == 0) {
            return null;
        }

        String ft = null;

        if (len == 29) {
            if (val.charAt(26) == ':' && val.charAt(28) == '0') {
                ft = FORMAT_29;
            }
        } else if (len == 27 && val.charAt(4) == '-') {
            ft = FORMAT_27;
        } else if (len == 25 && val.charAt(4) == '-') {
            ft = FORMAT_25;
        } else if (len == 24 && val.charAt(4) == '-') {
            if (val.charAt(10) == 'T') {
                ft = FORMAT_24_ISO08601;
            }
        } else if (len <= 23 && len >= 20 && val.charAt(4) == '-') {
            if (val.charAt(10) == 'T') {
                ft = FORMAT_23_t;
            } else if (val.charAt(19) == ',') {
                ft = FORMAT_23_a;
            } else {
                ft = FORMAT_23_b;
            }
        } else if (len == 22) {
            ft = FORMAT_22;
        } else if (len == 19) {
            if (val.charAt(10) == 'T') {
                ft = FORMAT_19_ISO;
            } else {
                char c1 = val.charAt(4);
                if (c1 == '/') {
                    ft = FORMAT_19_b;
                } else if (c1 == '.') {
                    ft = FORMAT_19_c;
                } else if (c1 == '-') {
                    ft = FORMAT_19_a;
                }
            }
        } else if (len == 18 && val.charAt(2) == ':') {
            ft = FORMAT_18;
        } else if (len == 17) {
            ft = FORMAT_17;
        } else if (len == 16) {
            char c1 = val.charAt(4);
            if (c1 == '/') {
                ft = FORMAT_16_b;
            } else if (c1 == '.') {
                ft = FORMAT_16_c;
            } else if (c1 == '-') {
                ft = FORMAT_16_a;
            }
        } else if (len == 14) {
            ft = FORMAT_14;
        } else if (len == 12 && val.charAt(2) == ':') {
            ft = FORMAT_12;
        } else if (len == 10) {
            char c1 = val.charAt(4);
            if (c1 == '/') {
                ft = FORMAT_10_b;
            } else if (c1 == '.') {
                ft = FORMAT_10_c;
            } else if (c1 == '-') {
                ft = FORMAT_10_a;
            }
        } else if (len == 9) {
            char c1 = val.charAt(4);
            if (c1 == '/') {
                ft = FORMAT_10_b; //兼容：yyyy/d/m
            } else if (c1 == '.') {
                ft = FORMAT_10_c;
            } else if (c1 == '-') {
                ft = FORMAT_10_a;
            } else {
                ft = FORMAT_9;
            }
        } else if (len == 8) {
            char c1 = val.charAt(4);
            if (c1 == '/') {
                ft = FORMAT_10_b; //兼容：yyyy/d/m
            } else if (c1 == '.') {
                ft = FORMAT_10_c;
            } else if (c1 == '-') {
                ft = FORMAT_10_a;
            } else {
                if (val.charAt(2) == ':') {
                    ft = FORMAT_8_a;
                } else {
                    ft = FORMAT_8_b;
                }
            }
        }

        if (ft != null) {
            DateFormat df = new SimpleDateFormat(ft, DEFAULTS.DEF_LOCALE);
            df.setTimeZone(DEFAULTS.DEF_TIME_ZONE);
            return df.parse(val);
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isDigit(val.charAt(i)) == false) {
                    return null;
                }
            }

            return new Date(Long.parseLong(val));
        }
    }


    /**
     * 格式化时间
     */
    public static String format(Date date, String dateFormat) {
        return format(date, dateFormat, null);
    }

    /**
     * 格式化时间
     */
    public static String format(Date date, String dateFormat, TimeZone timeZone) {
        DateFormat df = new SimpleDateFormat(dateFormat, DEFAULTS.DEF_LOCALE);
        if (timeZone != null) {
            df.setTimeZone(timeZone);
        }

        return df.format(date);
    }
}
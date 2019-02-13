package com.aic.xj.app.util;

public class StringUtil {
    public static boolean isNullOrWhiteSpace(String value) {
        if (value == null)
            return true;
        for (int index = 0; index < value.length(); ++index) {
            if (!Character.isWhitespace(value.charAt(index)))
                return false;
        }
        return true;
    }

    public static boolean isIpAddress(String value) {
        String[] ips = value.split("\\.");
        if (ips.length == 4) {
            for (String ip : ips) {
                try {
                    int result = Integer.parseInt(ip);
                    if (result <= 0 && result > 255) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static String concat(String... strings) {
        if (strings.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string);
            }
            return sb.toString();
        } else
            return null;
    }

    public static String join(char c, String... strings) {
        if (strings.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string);
                sb.append(c);
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } else
            return null;
    }
}

package ch.psi.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String utilities.
 */
public class Str {

    public static String trimLeft(String str) {
        return str.replaceAll("^\\s+", "");
    }

    public static String trimRight(String str) {
        return str.replaceAll("\\s+$", "");
    }
    
    public static String replaceLast(String str, String token,String replacement) {
        int index = str.lastIndexOf(token);
        if (index<0){
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, index));
        sb.append(replacement);
        sb.append(str.substring(index + token.length()));
        return sb.toString();
    }

    public static String removeMultipleSpaces(String str) {
        return str.replaceAll("\\s+", " ");
    }

    //Taken from https://community.oracle.com/thread/2080607
    public static String toTitleCase(String str) {
        if ((str == null) || str.isEmpty()) {
            return str;
        }

        StringBuffer result = new StringBuffer();
        char prevChar = ' ';
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                result.append(' ');
            } else if (prevChar == ' ' || prevChar == '_') {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c) && !Character.isUpperCase(prevChar)) {
                /*
                 * Insert space before start of word if camel case
                 */
                result.append(' ');
                result.append(Character.toUpperCase(c));
            } else {
                result.append(c);
            }

            prevChar = c;
        }

        return result.toString();
    }

    public static String capitalizeFirst(String str) {
        return new StringBuilder().append(Character.toUpperCase(str.charAt(0))).append(str.substring(1)).toString();
    }

    public static String toHtml(String str) {
        str = str.replace("\n", "<br>");
        str = str.replace("&", "&amp;");
        str = str.replace("<", "&lt;");
        str = str.replace(">", "&gt;");
        return str;
    }

    public static int count(String string, String substring) {
        int ret = 0;
        int pos = 0;
        while ((pos = string.indexOf(substring, pos)) != -1) {
            pos++;
            ret++;
        }
        return ret;
    }

    /**
     * Provides a string representation of numbers, arrays, lists and maps.
     */
    public static String toString(Object obj) {
        return toString(obj, -1);
    }

    /**
     * Provides a string representation of objects, arrays, lists and maps, limiting the size of
     * arrays to maxElements.
     */
    public static String toString(Object obj, int maxElements) {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        if (obj != null) {
            if (obj instanceof List) {
                obj = ((List) obj).toArray();
            } else if (obj instanceof Map) {
                ArrayList list = new ArrayList();
                for (Object key : ((Map) obj).keySet()) {
                    list.add(String.valueOf(key) + "=" + toString(((Map) obj).get(key), maxElements));
                }
                obj = list.toArray();
                sj = new StringJoiner(", ", "{", "}");
            }
        }
        if ((obj == null) || (!obj.getClass().isArray())) {
            return String.valueOf(obj);
        }

        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
            sj.add(toString(Array.get(obj, i), maxElements));
            if (maxElements >= 0) {
                if (i < (length - 1)) {
                    if (i >= (maxElements - 1)) {
                        sj.add("...");
                        break;
                    }
                }
            }
        }
        return sj.toString();
    }

    static boolean isDoubleQuotes(String str) {
        boolean doubleQuotes = true;
        int countSingle = count(str, "'");
        int countDouble = count(str, "\"");
        if ((countSingle > 0) && ((countSingle % 2) == 0)) {
            int indexSingle = str.indexOf("'");
            int indexDouble = str.indexOf("\"");
            if ((indexDouble < 0) || (indexSingle < indexDouble)) {
                doubleQuotes = false;
            }
        }
        return doubleQuotes;
    }

    /**
     * Calls splitIgnoringQuotes checking if single or double quotes are used
     */
    public static String[] splitIgnoringQuotes(String str, String separator) {
        return splitIgnoringQuotes(str, separator, isDoubleQuotes(str));
    }

    /**
     * Split strings ignoring separators in quotes
     */
    public static String[] splitIgnoringQuotes(String str, String separator, boolean doubleQuotes) {
        if (doubleQuotes) {
            return str.split(separator + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        } else {
            return str.split(separator + "(?=(?:[^']*'[^']*')*[^']*$)", -1);
        }
    }

    public static String removeQuotes(String str) {
        String quote = isDoubleQuotes(str) ? "\"" : "'";
        int count = count(str, quote);
        if ((count > 0) && ((count % 2) == 0)) {
            str = str.substring(str.indexOf(quote) + 1, str.lastIndexOf(quote));
        }
        return str;
    }

    public static String[] splitIgnoringQuotesAndMultSpaces(String str) {
        ArrayList<String> args = new ArrayList<>();
        //Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str); //Won't accept empty strings
        Matcher m = Pattern.compile("([^\"]\\S*|\".*?\")\\s*").matcher(str);
        while (m.find()) {
            args.add(m.group(1));
        }
        return args.toArray(new String[0]);
    }    
    
    /**
     * Split strings ignoring separators in brackets - parenthesis, curly and square. Don't support nested brackets. 
     */
    public static String[] splitIgnoringBrackets(String str, String separator) {
        return str.split(separator + "(?![^\\{\\(\\[]*[\\]\\)\\}])", -1);
    }

    
    public static String[] split(String str, String[] separators) {
        for (String sep: separators){
            if (str.contains(sep)){
                if (Arr.containsEqual(new String[]{".","|","?"}, sep)){                
                    sep = "\\"+sep;
                }
                return str.split(sep);
            }
        }
        return new String[]{str};
    }
    
    
    public static String[] trim(String[] array) {
        for (int i=0; i<array.length; i++){
            array[i] = array[i].trim();
        }
        return array;
    }
}

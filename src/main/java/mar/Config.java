package mar;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public static String suffix = "3g2|3gp|7z|aac|abw|aif|aifc|aiff|apk|arc|au|avi|azw|bat|bin|bmp|bz|bz2|cmd|cmx|cod|com|csh|css|csv|dll|doc|docx|ear|eot|epub|exe|flac|flv|gif|gz|ico|ics|ief|jar|jfif|jpe|jpeg|jpg|less|m3u|mid|midi|mjs|mkv|mov|mp2|mp3|mp4|mpa|mpe|mpeg|mpg|mpkg|mpp|mpv2|odp|ods|odt|oga|ogg|ogv|ogx|otf|pbm|pdf|pgm|png|pnm|ppm|ppt|pptx|ra|ram|rar|ras|rgb|rmi|rtf|scss|sh|snd|svg|swf|tar|tif|tiff|ttf|vsd|war|wav|weba|webm|webp|wmv|woff|woff2|xbm|xls|xlsx|xpm|xul|xwd|zip";

    public static String host = "gh0st.cn";

    public static String[] scope = new String[]{
            "request",
            "request method",
            "request uri",
            "request header",
            "request body",
            "response",
            "response status",
            "response header",
            "response body"
    };


    public static String[] responseScope = new String[]{
            "response",
            "response status",
            "response header",
            "response body"
    };

    public static String[] relationship = new String[]{
            "Matches",
            "Does not match"
    };

    public static String[] ruleFields = {
            "Loaded", "Name", "C-Scope", "Relationship", "Condition", "C-Regex", "M-Scope", "Match", "Replace", "M-Regex"
    };

    public static Object[][] ruleTemplate = new Object[][]{
            {
                    false, "New Name", "request", "Matches", "Host: 127.0.0.1", false, "response", "isAdmin=0", "isAdmin=1", false
            }
    };

    public static String scopeOptions = "Suite|Target|Proxy|Scanner|Intruder|Repeater|Logger|Sequencer|Decoder|Comparer|Extensions|Organizer|Recorded login replayer";

    public static Map<String, Object[][]> globalRules = new HashMap<>();
}

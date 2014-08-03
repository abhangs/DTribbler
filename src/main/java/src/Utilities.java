package src;


import com.sun.org.glassfish.external.statistics.Statistic;

import java.util.ArrayList;

public class Utilities {

    private static final String userListPrefix = "user_list";
    private static final String serverStatusPrefix = "server_status";
    private static final String serverAllKeysPrefix = "server_all_keys";


    public static String getUserListPrefix() {
        return userListPrefix;
    }

    public static String getServerStatusPrefix() {
        return serverStatusPrefix;
    }


    public static String getServerAllKeysPrefix() {
        return serverAllKeysPrefix;
    }
}

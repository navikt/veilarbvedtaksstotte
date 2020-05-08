package no.nav.veilarbvedtaksstotte.utils;

public class UrlUtils {

    private UrlUtils() {}

    public static String lagClusterUrl(String appName) {
        return joinPaths(clusterUrlForApplication(appName), appName);
    }

    public static String lagClusterUrl(String appName, boolean withAppContext) {
        return withAppContext ? lagClusterUrl(appName): clusterUrlForApplication(appName);
    }
}

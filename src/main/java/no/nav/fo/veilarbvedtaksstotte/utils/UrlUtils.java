package no.nav.fo.veilarbvedtaksstotte.utils;

import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static no.nav.apiapp.util.UrlUtils.joinPaths;

public class UrlUtils {

    private UrlUtils() {}

    public static String lagClusterUrl(String appName) {
        return joinPaths(clusterUrlForApplication(appName), appName);
    }

    public static String lagClusterUrl(String appName, boolean withAppContext) {
        return withAppContext ? lagClusterUrl(appName): clusterUrlForApplication(appName);
    }
}

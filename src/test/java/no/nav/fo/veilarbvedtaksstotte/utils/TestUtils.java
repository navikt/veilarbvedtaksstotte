package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.fasit.FasitUtils;

public class TestUtils {

    private TestUtils(){}

    public static String lagFssUrl(String appName){
        return String.format("https://%s-%s.nais.preprod.local/%s/",
                appName, FasitUtils.getDefaultEnvironment(), appName);
    }

}
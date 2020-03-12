package no.nav.veilarbvedtaksstotte.utils;

import lombok.SneakyThrows;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    private static final String DEFAULT_ENVIRONMENT = "q0";

    private TestUtils(){}

    public static String lagFssUrl(String appName){
        return String.format("https://%s-%s.nais.preprod.local/%s/",
                appName, DEFAULT_ENVIRONMENT, appName);
    }

    public static String lagFssUrl (String appName, boolean withAppContext) {
        return withAppContext ? lagFssUrl(appName) : String.format("https://%s-%s.nais.preprod.local/",
                appName, DEFAULT_ENVIRONMENT);

    }

    @SneakyThrows
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return new String(Files.readAllBytes(resPath), StandardCharsets.UTF_8);
    }

}

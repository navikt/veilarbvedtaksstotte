package no.nav.veilarbvedtaksstotte.utils;

import lombok.SneakyThrows;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    private TestUtils(){}

    @SneakyThrows
    public static String readTestResourceFile(String fileName) {
        URL fileUrl = TestUtils.class.getClassLoader().getResource(fileName);
        Path resPath = Paths.get(fileUrl.toURI());
        return new String(Files.readAllBytes(resPath), StandardCharsets.UTF_8);
    }

}

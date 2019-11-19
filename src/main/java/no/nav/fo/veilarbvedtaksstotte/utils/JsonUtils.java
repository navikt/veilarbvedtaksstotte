package no.nav.fo.veilarbvedtaksstotte.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;

public class JsonUtils {

    private JsonUtils(){}

    public static String createErrorStr(String errorMsg) {
        return createJsonStr("feilmelding", errorMsg);
    }

    public static String createNoDataStr(String noDataMsg) {
        return createJsonStr("ingenData", noDataMsg);
    }

    public static String createJsonStr(String fieldName, String value) {
        ObjectNode error = JsonNodeFactory.instance.objectNode();
        error.put(fieldName, value);
        return error.toString();
    }

    @SneakyThrows
    public static String toJson(Object object) {
        String jsonStr = new ObjectMapper().writeValueAsString(object);
        return "null".equals(jsonStr) ? null : jsonStr;
    }

}

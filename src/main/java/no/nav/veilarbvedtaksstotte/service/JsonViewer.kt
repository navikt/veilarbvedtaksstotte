package no.nav.veilarbvedtaksstotte.service

import com.google.gson.*
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbvedtaksstotte.utils.SecureLog
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*


@Slf4j
class JsonViewer() {
    companion object {

        val log: Logger = LoggerFactory.getLogger(JsonViewer::class.java)

        fun jsonToHtml(json: String?): String {
            val inputJson = json;
            if (json == null) {
                return "";
            }
            val gson = Gson();
            val jsonObject = gson.fromJson(
                inputJson?.substring(inputJson.indexOf("{"), inputJson.lastIndexOf("}") + 1),
                JsonObject::class.java
            )
            return "<div>" + objToHtml(jsonObject) + "</div>";
        }

        private fun objToHtml(jsonObject: JsonObject?): String {
            if (jsonObject == null) {
                return ""
            }
            var output = "";

            val keySet = jsonObject.keySet()
            for (key in keySet) {
                try {
                    val value: JsonElement = jsonObject.get(key)

                    if (isNotAllowedKey(key)) continue;

                    if (key.equals("ingenData")) {
                        output += "<b>Ingen registrerte data:</b> $value";
                    }

                    if (value.isJsonPrimitive) {
                        if (isLink(key)) {
                            output += getLink(key, value.asString)
                        }
                        output += "<div class='json-key-wrapper'>";
                        output += "<span class='json-key'>";
                        output += prettifyKey(key) + ": ";
                        output += "</span>";
                        output += "<span>";
                        output += scalarToString(value.asJsonPrimitive);
                        output += "</span>";
                        output += "</div>"
                    } else if (value is JsonArray) {
                        output += "<div class='json-array-wrapper'>";
                        output += "<h3 class='json-key'>" + prettifyKey(key) + "</h3>";
                        if (value.size() > 0) {
                            output += "<ul class='json-array'>";
                            value.forEach { arrValue ->
                                run {
                                    output += "<li>" + objToHtml(arrValue as JsonObject) + "</li>";
                                }
                            }
                            output += "</ul>";
                        } else {
                            output += "<p class='json-array-empty'>Ingen oppføringer</p>"
                        }
                        output += "</div>";
                    } else if (value is JsonObject) {
                        output += "<div>";
                        output += "<h3 class='json-key'>" + prettifyKey(key) + "</h3>";
                        output += "<div class='json-obj'>"
                        output += objToHtml(value.asJsonObject)
                        output += "</div>";
                        output += "</div>";
                    }
                } catch (e: Exception) {
                    SecureLog.secureLog.warn("Can't parse input json string " + jsonObject, e)
                }
            }

            return output;

        }

        private fun isNotAllowedKey(key: String): Boolean {
            val notAllowedList = listOf("spmId", "besvarelseId", "id", "sporsmalId", "konseptId", "styrk08", "type");
            return notAllowedList.stream().anyMatch { key.contentEquals(it, true) };
        }

        private fun isLink(key: String): Boolean {
            return key.contains("lenke", true);
        }

        private fun scalarToString(value: JsonPrimitive): String {
            if (value.isBoolean) {
                if (value.asBoolean) return "Ja" else return "Nei";
            }
            return fixDateFormat(value.asString);
        }

        private fun getLink(key: String, value: String): String {
            val valueSanitized = sanitazeValue(value)
            var output = "";
            output += "<div class='json-key-wrapper'>";
            output += "<span class='json-key'>";
            output += prettifyKey(key) + ": ";
            output += "</span>";
            output += "<a href='$valueSanitized' target='_blank' rel='noopener noreferrer'>";
            output += valueSanitized;
            output += "</a></div>";
            return output;
        }

        private fun sanitazeValue(value: String): String {
            return Jsoup.parse(value).text();
        }

        private fun prettifyKey(key: String): String {
            val translatedKey = translateKeysToNorwegian(sanitazeValue(key));
            return StringUtils.capitalize(
                StringUtils.replace(
                    StringUtils.lowerCase(
                        StringUtils.join(
                            StringUtils.splitByCharacterTypeCamelCase(translatedKey),
                            StringUtils.SPACE
                        )
                    ), "_", " "
                )
            );
        }

        private fun translateKeysToNorwegian(key: String): String {
            val dictionary: HashMap<String, String> = hashMapOf(
                "forerkort" to "førerkort",
                "sprak" to "språk",
                "onsket" to "ønsket",
                "sporsmal" to "spørsmål",
                "Maneder" to "Måneder",
                "utloper" to "utløper"
            );

            for (dictionaryKey in dictionary.keys) {
                if (key.contains(dictionaryKey)) {
                    return key.replace(dictionaryKey, dictionary.get(dictionaryKey).toString());
                }
            }
            return key;
        }

        private fun fixDateFormat(value: String): String {
            try {
                val locale = Locale.forLanguageTag("no");
                if (value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}\$"))) {
                    return LocalDate.parse(value)
                        .format(DateTimeFormatter.ofPattern("dd. MMM YYYY").withLocale(locale))
                } else if (value.matches(Regex("^\\d{4}-\\d{2}\$"))) {
                    val yearMonth = YearMonth.parse(value);
                    return LocalDate.of(yearMonth.year, yearMonth.month, 1)
                        .format(DateTimeFormatter.ofPattern("MMM YYYY").withLocale(locale))
                } else if (value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}[T]\\d{2}(.)*\$"))) {
                    return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
                        .format(DateTimeFormatter.ofPattern("dd. MMM YYYY 'kl.' HH:mm").withLocale(locale))
                }
                return prettifyKey(value);
            } catch (e: Exception) {
                SecureLog.secureLog.warn("Can't parse date: " + value + " during pdf generation")
                return prettifyKey(value);
            }
        }
    }


}
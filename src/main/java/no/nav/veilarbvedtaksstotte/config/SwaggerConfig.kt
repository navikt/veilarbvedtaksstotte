package no.nav.veilarbvedtaksstotte.config

import io.swagger.v3.oas.models.OpenAPI
import no.nav.veilarbvedtaksstotte.annotations.EksterntEndepunkt
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {


    /**
     * Lager en gruppering kalt "Eksterne endepunkter (for konsumenter)" i Swagger-doc.
     * Grupperingen inneholder alle endepunkter som er annotert med [EksterntEndepunkt].
     */
    @Bean
    fun externalApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("Eksterne endepunkter (for konsumenter)")
            .addOperationCustomizer { operation, handlerMethod ->
                if (handlerMethod.method.getAnnotation(EksterntEndepunkt::class.java) != null) {
                    operation
                } else {
                    null
                }
            }
            .addOpenApiCustomizer(removeEmptyTags())
            .build()
    }

    /**
     * Lager en gruppering kalt "Interne endepunkter" i Swagger-doc.
     * Grupperingen inneholder alle endepunkter.
     */
    @Bean
    fun internalApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("Interne endepunkter")
            .addOpenApiCustomizer(removeEmptyTags())
            .build()
    }

    /**
     * For å unngå at vi får tomme "seksjoner" i Swagger-doc, fjerner vi tags som ikke er i bruk.
     * Klasser annotert med [io.swagger.v3.oas.annotations.tags.Tag] vil få en egen seksjon i Swagger-doc.
     * Siden vi per dags dato blander eksterne/interne endepunkter i samme controller, må vi eksplisitt gjøre denne filtreringen.
     *
     * Denne trenger vi foreløpig kun å bruke i [externalApi], siden [internalApi] uansett inneholder alle endepunkter.
     */
    @Bean
    fun removeEmptyTags(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi: OpenAPI ->
            val usedTags = openApi.paths.values
                .flatMap { pathItem -> pathItem.readOperations().flatMap { it.tags } }
                .toSet()
            openApi.tags = openApi.tags?.filter { it.name in usedTags }
        }
    }
}
package no.nav.veilarbvedtaksstotte.annotations


/**
 * Markerer et endepunkt som et eksternt endepunkt.
 * Det vil si, endepunkt kan brukes både internt i egne applikasjoner og av eksterne konsumenter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EksterntEndepunkt
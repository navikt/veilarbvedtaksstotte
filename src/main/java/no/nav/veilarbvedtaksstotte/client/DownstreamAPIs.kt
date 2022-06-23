package no.nav.veilarbvedtaksstotte.client

import no.nav.veilarbvedtaksstotte.utils.DownstreamApi

object DownstreamAPIs {
    @JvmStatic
    val veilarbveileder:(String) -> DownstreamApi =  {DownstreamApi(it, "pto", "veilarbveileder")}
    @JvmStatic
    val veilarbperson:(String) -> DownstreamApi = {DownstreamApi(it, "pto", "veilarbperson")}
    @JvmStatic
    val arena:(String) -> DownstreamApi = {DownstreamApi(it, "pto", "veilarbarena")}
    @JvmStatic
    val saf:(String) -> DownstreamApi = {DownstreamApi(it, "teamdokumenthandtering", "saf")}
}
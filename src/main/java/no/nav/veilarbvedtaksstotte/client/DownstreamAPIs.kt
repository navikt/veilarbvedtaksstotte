package no.nav.veilarbvedtaksstotte.client

import no.nav.veilarbvedtaksstotte.utils.DownstreamApi

object DownstreamAPIs {
    @JvmStatic
    val veilarbveileder: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbveileder") }
    @JvmStatic
    val veilarbperson: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbperson") }
    @JvmStatic
    val veilarbvedtakinfo: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbvedtakinfo") }
    @JvmStatic
    val arena: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbarena") }
    @JvmStatic
    val dokarkiv: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "dokarkiv-q1" else "dokarkiv") }
    @JvmStatic
    val saf: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", "saf") }
}
package no.nav.veilarbvedtaksstotte.client

import no.nav.veilarbvedtaksstotte.utils.DownstreamApi

object DownstreamAPIs {
    @JvmStatic
    val veilarbveileder: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbveileder", null) }
    @JvmStatic
    val veilarbperson: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbperson", null) }
    @JvmStatic
    val veilarbvedtakinfo: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbvedtakinfo", null) }
    @JvmStatic
    val veilarbarena: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbarena", null) }
    @JvmStatic
    val veilarboppfolging: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarboppfolging", null) }
    @JvmStatic
    val dokarkiv: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "dokarkiv-q1" else "dokarkiv", null) }
    @JvmStatic
    val saf: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", "saf", if(it == "dev-fss") "saf-q1" else "saf") }
    @JvmStatic
    val regoppslag: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "regoppslag-q1" else "regoppslag", null) }
    @JvmStatic
    val pdl: (String) -> DownstreamApi = { DownstreamApi(it, "pdl", "pdl-api", null) }

}

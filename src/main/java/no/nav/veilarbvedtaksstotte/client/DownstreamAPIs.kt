package no.nav.veilarbvedtaksstotte.client

import no.nav.veilarbvedtaksstotte.utils.DownstreamApi

object DownstreamAPIs {
    @JvmStatic
    val veilarbveileder: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbveileder") }
    @JvmStatic
    val veilarbperson: (String) -> DownstreamApi = { DownstreamApi(it, if(it == "prod-fss") "pto" else "obo", "veilarbperson") }
    @JvmStatic
    val veilarbarena: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarbarena") }
    @JvmStatic
    val veilarboppfolging: (String) -> DownstreamApi = { DownstreamApi(it, "pto", "veilarboppfolging") }
    @JvmStatic
    val dokarkiv: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "dokarkiv" else "dokarkiv") }
    @JvmStatic
    val saf: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "saf-q1" else "saf") }
    @JvmStatic
    val regoppslag: (String) -> DownstreamApi = { DownstreamApi(it, "teamdokumenthandtering", if(it == "dev-fss") "regoppslag" else "regoppslag") }
    @JvmStatic
    val pdl: (String) -> DownstreamApi = { DownstreamApi(it, "pdl", "pdl-api") }
    @JvmStatic
    val aiaBackend: (String) -> DownstreamApi = { DownstreamApi(it, "paw","aia-backend") }
}

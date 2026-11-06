package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId

fun BrukerIdenter.alleIdenterListe() : List<EksternBrukerId> {
    return this.let {
        buildList {
            add(it.fnr)
            add(it.aktorId)
            addAll(it.historiskeFnr)
            addAll(it.historiskeAktorId)
        }
    }
}
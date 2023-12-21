package no.nav.veilarbvedtaksstotte.client.person.dto

sealed class CvDto {
    data class CVMedInnhold(val cvInnhold: CvInnhold) : CvDto()
    data class CvMedError(val cvErrorStatus: CvErrorStatus) : CvDto()
}
package no.nav.veilarbvedtaksstotte.client.dokarkiv.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

public class JournalpostGraphqlResponse extends GraphqlResponse<JournalpostGraphqlResponse.JournalpostReponseData> {
    @Data
    @Accessors(chain = true)
    public static class JournalpostReponseData {
        // Get-er for Kotlin
        public Journalpost getJournalpost() {
            return journalpost;
        }

        private Journalpost journalpost;
    }
}

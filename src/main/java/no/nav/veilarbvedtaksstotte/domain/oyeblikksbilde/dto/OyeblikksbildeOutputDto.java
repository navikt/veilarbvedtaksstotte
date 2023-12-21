package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.service.JsonViewer;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeOutputDto {
    public long vedtakId;
    public OyeblikksbildeType oyeblikksbildeType;
    public String json;
    public boolean journalfort;
    public String htmlView;

    public static OyeblikksbildeOutputDto from(Oyeblikksbilde oyeblikksbilde) {
        String htmlView = JsonViewer.Companion.jsonToHtml(oyeblikksbilde.json);

        return new OyeblikksbildeOutputDto(oyeblikksbilde.vedtakId,
                oyeblikksbilde.oyeblikksbildeType,
                oyeblikksbilde.getJson(),
                oyeblikksbilde.isJournalfort(),
                htmlView);
    }
}

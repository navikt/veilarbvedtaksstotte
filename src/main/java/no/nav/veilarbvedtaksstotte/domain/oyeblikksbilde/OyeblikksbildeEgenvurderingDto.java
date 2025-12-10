package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OyeblikksbildeEgenvurderingDto {
    public EgenvurderingData data;
    public boolean journalfort;

    public OyeblikksbildeEgenvurderingDto setData(EgenvurderingData data) {
        this.data = data;
        return this;
    }

    public OyeblikksbildeEgenvurderingDto setJournalfort(boolean journalfort) {
        this.journalfort = journalfort;
        return this;
    }

    public EgenvurderingData getData() {
        return data;
    }

    public boolean isJournalfort() {
        return journalfort;
    }
}

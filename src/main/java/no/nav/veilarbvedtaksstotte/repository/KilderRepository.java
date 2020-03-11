package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.Kilde;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class KilderRepository {

    public final static String KILDE_TABLE                  = "KILDE";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String TEKST                       = "TEKST";

    private final JdbcTemplate db;

    @Inject
    public KilderRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagKilder(List<String> kilder, long vedtakId) {
        kilder.forEach((opplysning) -> insertKilde(opplysning, vedtakId));
    }

    public List<Kilde> hentKilderForVedtak(long vedtakId) {
        return SqlUtils.select(db, KILDE_TABLE, KilderRepository::mapKilder)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public List<Kilde> hentKilderForAlleVedtak(List<Vedtak> vedtakListe) {
        if (vedtakListe.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> vedtakIder = vedtakListe.stream().map(Vedtak::getId).collect(Collectors.toList());
        return SqlUtils.select(db, KILDE_TABLE, KilderRepository::mapKilder)
                .where(WhereClause.in(VEDTAK_ID, vedtakIder))
                .column("*")
                .executeToList();
    }

    public void slettKilder(long vedtakId) {
        SqlUtils.delete(db, KILDE_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();
    }

    private void insertKilde(String tekst, long vedtakId) {
        SqlUtils.insert(db, KILDE_TABLE)
                .value(VEDTAK_ID, vedtakId)
                .value(TEKST, tekst)
                .execute();
    }

    @SneakyThrows
    private static Kilde mapKilder(ResultSet rs) {
        return new Kilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setTekst(rs.getString(TEKST));
    }

}


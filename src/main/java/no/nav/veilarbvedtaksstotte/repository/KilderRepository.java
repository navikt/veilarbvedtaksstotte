package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Kilde;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.DbUtils.toPostgresArray;

@Repository
public class KilderRepository {

    public final static String KILDE_TABLE                  = "KILDE";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String TEKST                       = "TEKST";

    private final JdbcTemplate db;

    @Autowired
    public KilderRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagKilder(List<String> kilder, long vedtakId) {
        kilder.forEach((opplysning) -> insertKilde(opplysning, vedtakId));
    }

    public List<Kilde> hentKilderForVedtak(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = %d", KILDE_TABLE, VEDTAK_ID, vedtakId);
        return db.query(sql, KilderRepository::mapKilder);
    }

    public List<Kilde> hentKilderForAlleVedtak(List<Vedtak> vedtakListe) {
        if (vedtakListe.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> vedtakIder = vedtakListe.stream().map(Vedtak::getId).collect(Collectors.toList());
        String sql = format("SELECT * FROM %s WHERE %s = SOME(?::bigint[])", KILDE_TABLE, VEDTAK_ID);
        List<String> strVedtakIder = vedtakIder.stream().map(String::valueOf).collect(Collectors.toList());

        return db.query(sql, KilderRepository::mapKilder, toPostgresArray(strVedtakIder));
    }

    public void slettKilder(long vedtakId) {
        db.update(format("DELETE FROM %s WHERE %s = ?", KILDE_TABLE, VEDTAK_ID), vedtakId);
    }

    private void insertKilde(String tekst, long vedtakId) {
        String sql = format("INSERT INTO %s(%s, %s) values(?,?)", KILDE_TABLE, VEDTAK_ID, TEKST);
        db.update(sql, vedtakId, tekst);
    }

    @SneakyThrows
    private static Kilde mapKilder(ResultSet rs, int row) {
        return new Kilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setTekst(rs.getString(TEKST));
    }

}


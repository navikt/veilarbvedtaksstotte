package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.EnhetTilgang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;

@Repository
public class TilgangskontrollRepository {

    public final static String TILGANGSKONTROLL_ENHET_TABLE = "TILGANGSKONTROLL_ENHET";
    private final static String ENHET_ID                    = "ENHET_ID";
    private final static String CREATED_AT                  = "CREATED_AT";

    private final JdbcTemplate db;

    @Autowired
    public TilgangskontrollRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void leggTilTilgang(EnhetId enhetId) {
        String sql = format("INSERT INTO %s(%s) values(?)", TILGANGSKONTROLL_ENHET_TABLE, ENHET_ID);
        try {
            db.update(sql, enhetId.get());
        } catch (DuplicateKeyException ignored) {}
    }

    public boolean harEnhetTilgang(EnhetId enhetId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? LIMIT 1", TILGANGSKONTROLL_ENHET_TABLE, ENHET_ID);
        List<EnhetTilgang> tilgang = db.query(sql, TilgangskontrollRepository::mapEnhetTilgang, enhetId.get());
        return !tilgang.isEmpty();
    }

    public List<EnhetTilgang> hentAlleTilganger() {
        String sql = format("SELECT * FROM %s", TILGANGSKONTROLL_ENHET_TABLE);
        return db.query(sql, TilgangskontrollRepository::mapEnhetTilgang);
    }

    public void fjernTilgang(EnhetId enhetId) {
        db.update(format("DELETE FROM %s WHERE %s = ?", TILGANGSKONTROLL_ENHET_TABLE, ENHET_ID), enhetId.get());
    }

    @SneakyThrows
    private static EnhetTilgang mapEnhetTilgang(ResultSet rs, int row) {
        return new EnhetTilgang()
                .setEnhetId(EnhetId.of(rs.getString(ENHET_ID)))
                .setCreatedAt(rs.getTimestamp(CREATED_AT).toLocalDateTime());
    }


}

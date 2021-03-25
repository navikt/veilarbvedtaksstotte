package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.domain.UtrulletEnhet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;

@Repository
public class UtrullingRepository {

    public final static String UTRULLING_TABLE              = "UTRULLING";
    private final static String ENHET_ID                    = "ENHET_ID";
    private final static String CREATED_AT                  = "CREATED_AT";

    private final JdbcTemplate db;

    @Autowired
    public UtrullingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void leggTilUtrulling(EnhetId enhetId) {
        String sql = format("INSERT INTO %s(%s) values(?)", UTRULLING_TABLE, ENHET_ID);
        try {
            db.update(sql, enhetId.get());
        } catch (DuplicateKeyException ignored) {}
    }

    public boolean erUtrullet(EnhetId enhetId) {
        String sql = format("SELECT * FROM %s WHERE %s = ? LIMIT 1", UTRULLING_TABLE, ENHET_ID);
        List<UtrulletEnhet> tilgang = db.query(sql, UtrullingRepository::mapUtrulletEnhet, enhetId.get());
        return !tilgang.isEmpty();
    }

    public List<UtrulletEnhet> hentAlleUtrullinger() {
        String sql = format("SELECT * FROM %s", UTRULLING_TABLE);
        return db.query(sql, UtrullingRepository::mapUtrulletEnhet);
    }

    public void fjernUtrulling(EnhetId enhetId) {
        db.update(format("DELETE FROM %s WHERE %s = ?", UTRULLING_TABLE, ENHET_ID), enhetId.get());
    }

    @SneakyThrows
    private static UtrulletEnhet mapUtrulletEnhet(ResultSet rs, int row) {
        return new UtrulletEnhet()
                .setEnhetId(EnhetId.of(rs.getString(ENHET_ID)))
                .setCreatedAt(rs.getTimestamp(CREATED_AT).toLocalDateTime());
    }


}

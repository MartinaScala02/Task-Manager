package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.AuditLog;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object (DAO) per la gestione della tabella 'AuditLog'.
 * <p>
 * Questa classe gestisce la scrittura dei log di sistema nel database.
 * È progettata per lavorare in sinergia con altri DAO all'interno di transazioni condivise.
 * </p>
 */
public class DAOAuditLog {
    private static DAOAuditLog instance = new DAOAuditLog();

    /**
     * Costruttore privato (Singleton).
     */
    private DAOAuditLog() {}

    /**
     * Restituisce l'unica istanza di DAOAuditLog.
     * @return L'istanza Singleton.
     */
    public static DAOAuditLog getInstance() { return instance; }

    // NOTA: Riceve la 'conn' come parametro per usare la stessa transazione del Task
    /**
     * Inserisce un record di log nel database utilizzando una connessione esistente.
     * <p>
     * Questo metodo accetta un oggetto {@link Connection} aperto esternamente (solitamente
     * da {@code DAOTasks}) per garantire che l'inserimento del log avvenga nella
     * stessa transazione dell'operazione principale.
     * </p>
     *
     * @param log  L'oggetto AuditLog da inserire.
     * @param conn La connessione al database (già aperta) da utilizzare.
     * @throws SQLException In caso di errore SQL.
     */
    public void insert(AuditLog log, Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        String sql = String.format("INSERT INTO AuditLog (operazione, id_task, id_utente, dettagli) VALUES ('%s', %d, %d, '%s')",
                log.getOperazione(), log.getIdTask(), log.getIdUtente(), log.getDettagli().replace("'", "\\'"));
        st.executeUpdate(sql);
        st.close();
    }
}
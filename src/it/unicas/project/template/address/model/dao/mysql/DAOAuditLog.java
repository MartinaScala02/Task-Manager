package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.AuditLog;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DAOAuditLog {
    private static DAOAuditLog instance = new DAOAuditLog();
    private DAOAuditLog() {}
    public static DAOAuditLog getInstance() { return instance; }

    // NOTA: Riceve la 'conn' come parametro per usare la stessa transazione del Task
    public void insert(AuditLog log, Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        String sql = String.format("INSERT INTO AuditLog (operazione, id_task, id_utente, dettagli) VALUES ('%s', %d, %d, '%s')",
                log.getOperazione(), log.getIdTask(), log.getIdUtente(), log.getDettagli().replace("'", "\\'"));
        st.executeUpdate(sql);
        st.close();
    }
}
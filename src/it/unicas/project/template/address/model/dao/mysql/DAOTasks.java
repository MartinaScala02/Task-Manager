package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.AuditLog;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// Import Date
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DAOTasks implements DAO<Tasks> {

    private DAOTasks(){}
    private static DAOTasks dao = null;
    private static Logger logger = Logger.getLogger(DAOTasks.class.getName());

    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOTasks();
        }
        return dao;
    }

    // --------------------------------------------------------------------------------
    // INSERT - CON TRANSAZIONE E AUDIT
    // --------------------------------------------------------------------------------
    @Override
    public void insert(Tasks t) throws DAOException {
        verifyObject(t);
        Statement st = null;
        Connection conn = null;

        try {
            // 1. Usiamo il metodo originale per ottenere lo statement
            st = DAOMySQLSettings.getStatement();

            // 2. TRUCCO: Recuperiamo la connessione dallo statement per gestire la transazione
            conn = st.getConnection();
            conn.setAutoCommit(false); // Inizio Transazione

            // Preparazione Query
            int completatoInt = (t.getCompletamento() != null && t.getCompletamento()) ? 1 : 0;
            String idCatVal = (t.getIdCategoria() != null && t.getIdCategoria() > 0) ? t.getIdCategoria().toString() : "NULL";
            String query = "INSERT INTO Tasks (titolo, descrizione, scadenza, priorità, completamento, idUtente, idCategoria) VALUES ('"
                    + t.getTitolo().replace("'", "\\'") + "', '"
                    + (t.getDescrizione() != null ? t.getDescrizione().replace("'", "\\'") : "") + "', '"
                    + t.getScadenza() + "', '"
                    + t.getPriorita() + "', "
                    + completatoInt + ", "
                    + t.getIdUtente() + ", "
                    + idCatVal + ")";

            // Esecuzione
            st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                t.setIdTask(rs.getInt(1));
            }
            rs.close();

            // 3. Audit Log (Usiamo la stessa connessione 'conn')
            AuditLog log = new AuditLog("INSERT", t.getIdTask(), t.getIdUtente(), "Creato task: " + t.getTitolo());
            DAOAuditLog.getInstance().insert(log, conn);

            // 4. Conferma tutto
            conn.commit();

        } catch (SQLException e) {
            // Rollback in caso di errore
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw new DAOException("Errore Insert: " + e.getMessage());
        } finally {
            // Pulizia: Chiudiamo statement E connessione manualmente per sicurezza
            DAOMySQLSettings.closeStatement(st);
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // --------------------------------------------------------------------------------
    // UPDATE - CON TRANSAZIONE E AUDIT
    // --------------------------------------------------------------------------------
    @Override
    public void update(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() == null) throw new DAOException("ID mancante");
        verifyObject(t);

        Statement st = null;
        Connection conn = null;

        try {
            st = DAOMySQLSettings.getStatement();
            conn = st.getConnection();
            conn.setAutoCommit(false);

            int completatoInt = (t.getCompletamento() != null && t.getCompletamento()) ? 1 : 0;
            String idCatVal = (t.getIdCategoria() != null && t.getIdCategoria() > 0) ? t.getIdCategoria().toString() : "NULL";
            String query = "UPDATE Tasks SET "
                    + "titolo = '" + t.getTitolo().replace("'", "\\'") + "', "
                    + "descrizione = '" + (t.getDescrizione() != null ? t.getDescrizione().replace("'", "\\'") : "") + "', "
                    + "scadenza = '" + t.getScadenza() + "', "
                    + "priorità = '" + t.getPriorita() + "', "
                    + "idCategoria = " + idCatVal + ", "
                    + "completamento = " + completatoInt
                    + " WHERE idTask = " + t.getIdTask();

            st.executeUpdate(query);

            AuditLog log = new AuditLog("UPDATE", t.getIdTask(), t.getIdUtente(), "Modificato task.");
            DAOAuditLog.getInstance().insert(log, conn);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new DAOException("Errore Update: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // --------------------------------------------------------------------------------
    // DELETE - CON TRANSAZIONE E AUDIT
    // --------------------------------------------------------------------------------
    @Override
    public void delete(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() == null) throw new DAOException("ID mancante");

        Statement st = null;
        Connection conn = null;

        try {
            st = DAOMySQLSettings.getStatement();
            conn = st.getConnection();
            conn.setAutoCommit(false);

            String query = "DELETE FROM Tasks WHERE idTask = " + t.getIdTask();
            st.executeUpdate(query);

            AuditLog log = new AuditLog("DELETE", t.getIdTask(), t.getIdUtente(), "Eliminato task.");
            DAOAuditLog.getInstance().insert(log, conn);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new DAOException("Errore Delete: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // --------------------------------------------------------------------------------
    // SELECT - STANDARD (Nessuna modifica necessaria qui)
    // --------------------------------------------------------------------------------
    @Override
    public List<Tasks> select(Tasks t) throws DAOException {
        ArrayList<Tasks> lista = new ArrayList<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT * FROM Tasks WHERE 1=1 ";

            if (t != null) {
                if (t.getTitolo() != null && !t.getTitolo().isEmpty()) {
                    String k = t.getTitolo().replace("'", "\\'");
                    sql += " AND (titolo LIKE '%" + k + "%' OR descrizione LIKE '%" + k + "%')";
                }
                if (t.getPriorita() != null && !t.getPriorita().isEmpty()) sql += " AND priorità = '" + t.getPriorita() + "'";
                if (t.getIdCategoria() != null && t.getIdCategoria() > 0) sql += " AND idCategoria = " + t.getIdCategoria();
                if (t.getCompletamento() != null) sql += " AND completamento = " + (t.getCompletamento() ? 1 : 0);
                if (t.getIdUtente() != null && t.getIdUtente() > 0) sql += " AND idUtente = " + t.getIdUtente();
                if (t.getScadenza() != null && !t.getScadenza().isEmpty()) sql += " AND scadenza = '" + t.getScadenza() + "'";
            }

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                lista.add(new Tasks(
                        rs.getString("titolo"), rs.getString("descrizione"), rs.getString("scadenza"),
                        rs.getString("priorità"), rs.getString("data_creazione"), rs.getBoolean("completamento"),
                        rs.getInt("idTask"), rs.getInt("idUtente"), rs.getInt("idCategoria")
                ));
            }
        } catch (SQLException sq) {
            throw new DAOException("Errore Select: " + sq.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    private void verifyObject(Tasks t) throws DAOException {
        if (t == null) throw new DAOException("Task nullo");
        if (t.getTitolo() == null || t.getTitolo().isEmpty()) throw new DAOException("Titolo obbligatorio");
        if (t.getIdUtente() == null || t.getIdUtente() <= 0) throw new DAOException("Utente obbligatorio");
        if (t.getScadenza() != null && !t.getScadenza().isEmpty()) {
            try {
                LocalDate d = null;
                if (t.getScadenza().matches("\\d{4}-\\d{2}-\\d{2}")) d = LocalDate.parse(t.getScadenza());
                else d = LocalDate.parse(t.getScadenza(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException e) {
                throw new DAOException("Data non valida: " + t.getScadenza());
            }
        }
    }
}
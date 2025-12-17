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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Data Access Object (DAO) per la gestione della tabella 'Tasks' su database MySQL.
 * Gestisce le operazioni CRUD e assicura l'integrità dei dati tramite transazioni.
 * * <p><b>FIX:</b> Questa versione gestisce correttamente i valori NULL per le date di scadenza.</p>
 */
public class DAOTasks implements DAO<Tasks> {

    private DAOTasks(){}
    private static DAOTasks dao = null;
    private static Logger logger = Logger.getLogger(DAOTasks.class.getName());

    /**
     * Restituisce l'unica istanza (Singleton) della classe DAOTasks.
     * @return L'istanza singleton di DAOTasks.
     */
    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOTasks();
        }
        return dao;
    }

    /**
     * Inserisce un nuovo Task nel database gestendo la transazione e l'audit log.
     * @param t Il task da inserire.
     * @throws DAOException Se i dati non sono validi o in caso di errore SQL.
     */
    @Override
    public void insert(Tasks t) throws DAOException {
        verifyObject(t);
        Statement st = null;
        Connection conn = null;

        try {
            st = DAOMySQLSettings.getStatement();
            conn = st.getConnection();
            conn.setAutoCommit(false);

            int completatoInt = (t.getCompletamento() != null && t.getCompletamento()) ? 1 : 0;
            String idCatVal = (t.getIdCategoria() != null && t.getIdCategoria() > 0) ? t.getIdCategoria().toString() : "NULL";

            // --- FIX SCADENZA ---
            String scadenzaVal = (t.getScadenza() != null && !t.getScadenza().isEmpty() && !t.getScadenza().equalsIgnoreCase("null"))
                    ? "'" + t.getScadenza() + "'"
                    : "NULL";

            String query = "INSERT INTO Tasks (titolo, descrizione, scadenza, priorità, completamento, idUtente, idCategoria) VALUES ('"
                    + t.getTitolo().replace("'", "\\'") + "', '"
                    + (t.getDescrizione() != null ? t.getDescrizione().replace("'", "\\'") : "") + "', "
                    + scadenzaVal + ", '" // Nota: niente apici intorno a scadenzaVal perché sono già inclusi se serve
                    + t.getPriorita() + "', "
                    + completatoInt + ", "
                    + t.getIdUtente() + ", "
                    + idCatVal + ")";

            st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                t.setIdTask(rs.getInt(1));
            }
            rs.close();

            AuditLog log = new AuditLog("INSERT", t.getIdTask(), t.getIdUtente(), "Creato task: " + t.getTitolo());
            DAOAuditLog.getInstance().insert(log, conn);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new DAOException("Errore Insert: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Aggiorna un Task esistente. Corregge il bug del salvataggio completamento per task senza data.
     * @param t Il task aggiornato.
     * @throws DAOException In caso di errore SQL o ID mancante.
     */
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

            // --- FIX SCADENZA: Se vuota o null, usa la parola NULL di SQL senza apici ---
            String scadenzaVal = (t.getScadenza() != null && !t.getScadenza().isEmpty() && !t.getScadenza().equalsIgnoreCase("null"))
                    ? "'" + t.getScadenza() + "'"
                    : "NULL";

            String query = "UPDATE Tasks SET "
                    + "titolo = '" + t.getTitolo().replace("'", "\\'") + "', "
                    + "descrizione = '" + (t.getDescrizione() != null ? t.getDescrizione().replace("'", "\\'") : "") + "', "
                    + "scadenza = " + scadenzaVal + ", " // Corretto: rimosso l'apice singolo fisso
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

    /**
     * Elimina un task e registra l'operazione nel log.
     * @param t Task da eliminare.
     * @throws DAOException Errore durante la cancellazione.
     */
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

    /**
     * Recupera i task filtrati in base ai parametri forniti.
     * @param t Task filtro.
     * @return Lista di task trovati.
     * @throws DAOException Errore di lettura.
     */
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

    /**
     * Valida la struttura del task prima di procedere con le operazioni DB.
     * @param t Task da verificare.
     * @throws DAOException Se i dati sono inconsistenti.
     */
    private void verifyObject(Tasks t) throws DAOException {
        if (t == null) throw new DAOException("Task nullo");
        if (t.getTitolo() == null || t.getTitolo().isEmpty()) throw new DAOException("Titolo obbligatorio");
        if (t.getIdUtente() == null || t.getIdUtente() <= 0) throw new DAOException("Utente obbligatorio");

        // La verifica della data scatta solo se la data è presente
        if (t.getScadenza() != null && !t.getScadenza().isEmpty() && !t.getScadenza().equalsIgnoreCase("null")) {
            try {
                if (t.getScadenza().matches("\\d{4}-\\d{2}-\\d{2}")) {
                    LocalDate.parse(t.getScadenza());
                } else {
                    LocalDate.parse(t.getScadenza(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }
            } catch (DateTimeParseException e) {
                throw new DAOException("Data non valida: " + t.getScadenza());
            }
        }
    }
}
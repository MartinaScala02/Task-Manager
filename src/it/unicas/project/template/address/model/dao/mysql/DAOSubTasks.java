package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) per la gestione della tabella 'SubTasks'.
 * <p>
 * Gestisce la persistenza dei sotto-task (checklist) collegati ai task principali.
 * Implementa il pattern Singleton per garantire un'unica istanza di accesso.
 * </p>
 */
public class DAOSubTasks implements DAO<SubTasks> {

    private DAOSubTasks(){}

    private static DAOSubTasks dao = null;
    private static Logger logger = null;

    /**
     * Restituisce l'istanza Singleton del DAO.
     * @return L'unica istanza di DAOSubTasks.
     */
    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOSubTasks();
            logger = Logger.getLogger(DAOSubTasks.class.getName());
        }
        return dao;
    }

    /**
     * Seleziona i sotto-task dal database.
     * <p>
     * Se l'oggetto passato ha impostato l'{@code idTask}, la query filtrerà
     * solo i sotto-task appartenenti a quel task specifico (funzionalità chiave per le checklist).
     * </p>
     *
     * @param s Oggetto SubTasks usato come filtro (solitamente contiene l'idTask padre).
     * @return Una lista di SubTasks che soddisfano i criteri.
     * @throws DAOException In caso di errori SQL.
     */
    @Override
    public List<SubTasks> select(SubTasks s) throws DAOException {
        ArrayList<SubTasks> lista = new ArrayList<>();
        Statement st = null;

        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT * FROM SubTasks WHERE 1=1 ";

            if (s != null) {
                if (s.getTitolo() != null && !s.getTitolo().isEmpty()) {
                    sql += " AND titolo LIKE '" + s.getTitolo().replace("'", "\\'") + "%'";
                }

                // per filtrare le subtask di uno specifico padre
                if (s.getIdTask() != null && s.getIdTask() > 0) {
                    sql += " AND idTask = " + s.getIdTask();
                }
            }

            logger.info("SQL Select: " + sql);

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                lista.add(new SubTasks(
                        rs.getString("titolo"),
                        rs.getString("descrizione"),
                        rs.getBoolean("completamento"),
                        rs.getInt("idSubTask"),
                        rs.getInt("idTask")
                ));
            }

        } catch (SQLException sq) {
            throw new DAOException("Errore nella select: " + sq.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    /**
     * Elimina un sotto-task dal database.
     *
     * @param s Il sotto-task da eliminare (richiesto ID valido).
     * @throws DAOException Se l'ID è mancante o invalido.
     */
    @Override
    public void delete(SubTasks s) throws DAOException {
        if (s == null || s.getIdSubTask() == null || s.getIdSubTask() <= 0) {
            throw new DAOException("Impossibile eliminare: idSubTask mancante.");
        }

        String query = "DELETE FROM SubTasks WHERE idSubTask = " + s.getIdSubTask();
        logger.info("SQL Delete: " + query);

        executeUpdate(query);
    }

    /**
     * Inserisce un nuovo sotto-task nel database.
     * <p>
     * Recupera automaticamente la chiave generata (AUTO_INCREMENT) e la imposta nell'oggetto.
     * </p>
     *
     * @param s Il sotto-task da inserire. Deve avere un idTask valido.
     * @throws DAOException Se i dati sono incompleti o la query fallisce.
     */
    @Override
    public void insert(SubTasks s) throws DAOException {
        verifyObject(s);

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            int completatoInt = (s.getCompletamento() != null && s.getCompletamento()) ? 1 : 0;

            String titoloSafe = s.getTitolo().replace("'", "\\'");
            String descrizioneSafe = (s.getDescrizione() != null) ? s.getDescrizione().replace("'", "\\'") : "";

            String query = "INSERT INTO SubTasks (titolo, descrizione, completamento, idTask) VALUES ('"
                    + titoloSafe + "', '"
                    + descrizioneSafe + "', "
                    + completatoInt + ", "
                    + s.getIdTask() + ")";

            logger.info("SQL Insert: " + query);

            st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                int idGenerato = rs.getInt(1);
                s.setIdSubTask(idGenerato);
            } else {
                logger.warning("Nessuna chiave generata per la SubTask inserita.");
            }

            if (rs != null) rs.close();

        } catch (SQLException e) {
            throw new DAOException("Errore Database durante l'insert: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    /**
     * Aggiorna un sotto-task esistente.
     *
     * @param s Il sotto-task con i dati aggiornati (richiede ID valido).
     * @throws DAOException Se l'ID è mancante o invalido.
     */
    @Override
    public void update(SubTasks s) throws DAOException {
        if (s == null || s.getIdSubTask() == null || s.getIdSubTask() <= 0) {
            throw new DAOException("Impossibile aggiornare: idSubTask non valido.");
        }

        int completatoInt = (s.getCompletamento() != null && s.getCompletamento()) ? 1 : 0;

        String titoloSafe = s.getTitolo().replace("'", "\\'");
       // String descrizioneSafe = (s.getDescrizione() != null) ? s.getDescrizione().replace("'", "\\'") : "";

        String query = "UPDATE SubTasks SET "
                + "titolo = '" + titoloSafe + "', "
                + /*"descrizione = '" + descrizioneSafe + "', "
                +*/ "completamento = " + completatoInt
                + " WHERE idSubTask = " + s.getIdSubTask();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    /**
     * Verifica la validità dei dati prima dell'inserimento.
     * Controlla che titolo e idTask siano presenti.
     *
     * @param s Il sotto-task da validare.
     * @throws DAOException Se i dati sono mancanti.
     */
    private void verifyObject(SubTasks s) throws DAOException {
        if (s == null) throw new DAOException("SubTask è null");
        if (s.getTitolo() == null || s.getTitolo().isEmpty()) {
            throw new DAOException("Il titolo del Subtask è obbligatorio");
        }
        if (s.getIdTask() == null || s.getIdTask() <= 0) {
            throw new DAOException("SubTask deve essere associato a un Task (idTask mancante)");
        }
    }

    /**
     * Metodo helper per eseguire query di aggiornamento (INSERT, UPDATE, DELETE).
     *
     * @param query La stringa SQL da eseguire.
     * @throws DAOException In caso di errore SQL.
     */
    private void executeUpdate(String query) throws DAOException {
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            st.executeUpdate(query);
        } catch (SQLException e) {
            throw new DAOException("Errore Database: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }
}
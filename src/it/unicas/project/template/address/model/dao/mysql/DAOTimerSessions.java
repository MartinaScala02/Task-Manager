package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.TimerSessions;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) per la gestione della tabella 'TimerSessions'.
 * <p>
 * Gestisce il ciclo di vita delle sessioni di timer (Start, Stop, Update, Delete)
 * e la conversione tra i tipi SQL (Timestamp) e Java (LocalDateTime).
 * </p>
 */
public class DAOTimerSessions implements DAO<TimerSessions> {

    private static DAOTimerSessions instance = null;
    private static Logger logger = null;

    private DAOTimerSessions() {}

    /**
     * Restituisce l'istanza Singleton della classe.
     * @return L'istanza di DAOTimerSessions.
     */
    public static DAOTimerSessions getInstance() {
        if (instance == null) {
            instance = new DAOTimerSessions();
            logger = Logger.getLogger(DAOTimerSessions.class.getName());
        }
        return instance;
    }


    /**
     * Recupera la lista delle sessioni dal database.
     * <p>
     * Converte automaticamente i {@link java.sql.Timestamp} del database in {@link java.time.LocalDateTime}.
     * </p>
     *
     * @param t Oggetto filtro (opzionale). Filtra per idTask o per nome (LIKE).
     * @return Una lista di oggetti TimerSessions.
     * @throws DAOException In caso di errore SQL.
     */
    @Override
    public List<TimerSessions> select(TimerSessions t) throws DAOException {
        ArrayList<TimerSessions> lista = new ArrayList<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT * FROM TimerSessions WHERE 1=1 ";

            if (t != null) {
                if (t.getIdTask() > 0) {
                    sql += " AND idTask = " + t.getIdTask();
                }
                if (t.getNome() != null && !t.getNome().isEmpty()) {
                    sql += " AND nome LIKE '%" + t.getNome().replace("'", "\\'") + "%'";
                }
            }
            sql += " ORDER BY inizio DESC";

            logger.info("SQL Select TimerSession: " + sql);
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Timestamp tsInizio = rs.getTimestamp("inizio");
                Timestamp tsFine = rs.getTimestamp("fine");
                LocalDateTime inizio = (tsInizio != null) ? tsInizio.toLocalDateTime() : null;
                LocalDateTime fine = (tsFine != null) ? tsFine.toLocalDateTime() : null;

                lista.add(new TimerSessions(
                        rs.getString("nome"),
                        inizio,
                        fine,
                        rs.getLong("durata"),
                        rs.getInt("idSession"),
                        rs.getInt("idTask")
                ));
            }
        } catch (SQLException e) {
            throw new DAOException("Errore select TimerSession: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    /**
     * Avvia una nuova sessione di timer (INSERT).
     * <p>
     * Utilizza la funzione SQL {@code NOW()} per registrare l'orario di inizio lato server.
     * </p>
     *
     * @param t La sessione da inserire (richiede idTask valido).
     * @throws DAOException Se i dati sono invalidi o la query fallisce.
     */
    @Override
    public void insert(TimerSessions t) throws DAOException {
        if (t == null || t.getIdTask() <= 0) throw new DAOException("Dati sessione non validi");

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String nomeSafe = (t.getNome() != null) ? t.getNome().replace("'", "\\'") : "";

            // Usiamo NOW() per default se l'inizio è null
            String inizioVal = "NOW()";
            String query = "INSERT INTO TimerSessions (idTask, nome, inizio) VALUES ("
                    + t.getIdTask() + ", '"
                    + nomeSafe + "', "
                    + inizioVal + ")";

            logger.info("SQL Insert (Start Timer): " + query);
            st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                t.setIdSession(rs.getInt(1));
            }
            if (rs != null) rs.close();

        } catch (SQLException e) {
            throw new DAOException("Errore insert TimerSession: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    /**
     * Elimina una sessione dal database.
     *
     * @param t La sessione da eliminare (richiede idSession).
     * @throws DAOException In caso di errore SQL.
     */
    @Override
    public void delete(TimerSessions t) throws DAOException {
        if (t == null || t.getIdSession() <= 0) return;

        String query = "DELETE FROM TimerSessions WHERE idSession = " + t.getIdSession();
        logger.info("SQL Delete TimerSession: " + query);

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            st.executeUpdate(query);
        } catch (SQLException e) {
            throw new DAOException("Errore delete TimerSession: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    /**
     * Aggiorna i dati di una sessione esistente.
     * <p>
     * Ricalcola la durata usando la funzione SQL {@code TIMESTAMPDIFF} se vengono fornite nuove date di inizio/fine.
     * </p>
     *
     * @param t La sessione con i dati aggiornati.
     * @throws DAOException Se l'ID sessione è mancante.
     */
    @Override
    public void update(TimerSessions t) throws DAOException {
        if (t == null || t.getIdSession() <= 0) throw new DAOException("ID Sessione mancante per update");

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            String nomeSafe = (t.getNome() != null) ? t.getNome().replace("'", "\\'") : "";

            // FORMATTER PER LE DATE
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String startStr = (t.getInizio() != null) ? "'" + t.getInizio().format(formatter) + "'" : "NULL";
            String endStr = (t.getFine() != null) ? "'" + t.getFine().format(formatter) + "'" : "NULL";

            String query = "UPDATE TimerSessions SET "
                    + "nome = '" + nomeSafe + "', "
                    + "inizio = " + startStr + ", "
                    + "fine = " + endStr;

            // Ricalcolo della durata
            // Se inizio e fine sono presenti, calcoliamo la differenza in secondi, altrimenti durata = 0
            if (t.getInizio() != null && t.getFine() != null) {
                query += ", durata = TIMESTAMPDIFF(SECOND, " + startStr + ", " + endStr + ") ";
            } else {
                query += ", durata = 0 ";
            }
            query += " WHERE idSession = " + t.getIdSession();

            logger.info("SQL Update TimerSession: " + query);
            st.executeUpdate(query);

        } catch (SQLException e) {
            throw new DAOException("Errore update TimerSession: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }


    /**
     * Ferma una sessione attiva (STOP).
     * <p>
     * Imposta il campo 'fine' a {@code NOW()} e calcola la durata automaticamente tramite SQL.
     * </p>
     *
     * @param idSession    L'ID della sessione da fermare.
     * @param endLocalTime Orario di fine (parametro opzionale, nel metodo si usa NOW()).
     * @throws DAOException In caso di errore SQL o ID non valido.
     */
    public void stopSession(int idSession, LocalDateTime endLocalTime) throws DAOException {
        if (idSession <= 0) throw new DAOException("ID Sessione non valido.");

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            String query = "UPDATE TimerSessions SET " +
                    "fine = NOW(), " +
                    "durata = TIMESTAMPDIFF(SECOND, inizio, NOW()) " +
                    "WHERE idSession = " + idSession;

            logger.info("SQL Stop Timer: " + query);
            st.executeUpdate(query);

        } catch (SQLException e) {
            throw new DAOException("Errore stop timer: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    /**
     * Restituisce la somma totale dei secondi spesi su un task.
     * <p>
     * Considera solo le sessioni concluse (dove il campo 'fine' non è NULL).
     * </p>
     *
     * @param idTask L'ID del task.
     * @return Il totale dei secondi (long).
     * @throws DAOException In caso di errore SQL.
     */
    public long getSommaDurataPerTask(int idTask) throws DAOException {
        Statement st = null;
        long totaleSecondi = 0;

        try {
            st = DAOMySQLSettings.getStatement();
            // TIMESTAMPDIFF(SECOND, inizio, fine) calcola i secondi tra le due date
            // SUM(...) somma tutti i risultati
            String sql = "SELECT SUM(TIMESTAMPDIFF(SECOND, inizio, fine)) as totale " +
                    "FROM TimerSessions " +
                    "WHERE idTask = " + idTask + " AND fine IS NOT NULL";

            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {
                totaleSecondi = rs.getLong("totale"); // Se è null (nessuna sessione), restituisce 0
            }
            rs.close();

        } catch (SQLException e) {
            throw new DAOException("Errore nel calcolo durata totale: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }

        return totaleSecondi;
    }
}
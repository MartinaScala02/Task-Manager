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
 * Data Access Object (DAO) concreto per la gestione della tabella 'TimerSessions'.
 * <p>
 * Questa classe gestisce l'interazione con il database MySQL per le sessioni di timer.
 * Include funzionalità per creare, leggere, aggiornare ed eliminare (CRUD) le sessioni,
 * oltre a gestire lo stop delle sessioni attive e il calcolo dei totali.
 * </p>
 * <p>
 * <strong>Nota sulla Sincronizzazione Temporale:</strong><br>
 * A differenza delle implementazioni standard, questa classe utilizza formattatori di data
 * per inviare al database gli orari esatti calcolati dall'applicazione Java (client).
 * Questo previene disallineamenti tra il tempo visualizzato nel timer dell'interfaccia
 * e il tempo effettivamente registrato nel database.
 * </p>
 */
public class DAOTimerSessions implements DAO<TimerSessions> {

    private static DAOTimerSessions instance = null;
    private static Logger logger = null;

    /** Formatter per garantire la compatibilità delle date con il formato DATETIME di MySQL. */
    private static final DateTimeFormatter SQL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DAOTimerSessions() {}

    /**
     * Restituisce l'istanza Singleton della classe.
     * @return L'unica istanza di DAOTimerSessions.
     */
    public static DAOTimerSessions getInstance() {
        if (instance == null) {
            instance = new DAOTimerSessions();
            logger = Logger.getLogger(DAOTimerSessions.class.getName());
        }
        return instance;
    }

    /**
     * Recupera una lista di sessioni dal database filtrata in base ai parametri forniti.
     *
     * @param t Oggetto {@link TimerSessions} usato come filtro.
     * Può filtrare per {@code idTask} o per {@code nome} (ricerca parziale).
     * @return Una lista di sessioni trovate.
     * @throws DAOException In caso di errori di connessione o query SQL.
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
     * Inserisce una nuova sessione di timer nel database (START).
     * <p>
     * Se l'oggetto passato ha un orario di inizio impostato (da Java), usa quello.
     * Altrimenti usa la funzione SQL {@code NOW()} come fallback.
     * </p>
     *
     * @param t La sessione da inserire.
     * @throws DAOException Se i dati sono invalidi (es. idTask mancante).
     */
    @Override
    public void insert(TimerSessions t) throws DAOException {
        if (t == null || t.getIdTask() <= 0) throw new DAOException("Dati sessione non validi");

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String nomeSafe = (t.getNome() != null) ? t.getNome().replace("'", "\\'") : "";

            // Determina se usare l'orario Java (preciso) o Server (fallback)
            String inizioVal;
            if (t.getInizio() != null) {
                inizioVal = "'" + t.getInizio().format(SQL_FMT) + "'";
            } else {
                inizioVal = "NOW()";
            }

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
     * @param t La sessione da eliminare (necessario idSession).
     * @throws DAOException In caso di errori SQL.
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
     * Ricalcola automaticamente la durata (campo 'durata') se vengono modificati gli orari di inizio/fine.
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

            String startStr = (t.getInizio() != null) ? "'" + t.getInizio().format(SQL_FMT) + "'" : "NULL";
            String endStr = (t.getFine() != null) ? "'" + t.getFine().format(SQL_FMT) + "'" : "NULL";

            String query = "UPDATE TimerSessions SET "
                    + "nome = '" + nomeSafe + "', "
                    + "inizio = " + startStr + ", "
                    + "fine = " + endStr;

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
     * Ferma una sessione timer attiva (STOP).
     * <p>
     * Aggiorna il record impostando la data di fine e calcolando la durata.
     * Accetta un orario di fine esplicito da Java per garantire la massima precisione rispetto al timer visuale.
     * </p>
     *
     * @param idSession    L'ID della sessione da terminare.
     * @param endLocalTime L'orario di fine calcolato dall'interfaccia Java. Se null, usa NOW().
     * @throws DAOException In caso di errori SQL.
     */
    public void stopSession(int idSession, LocalDateTime endLocalTime) throws DAOException {
        if (idSession <= 0) throw new DAOException("ID Sessione non valido.");

        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            // Usa l'orario passato da Java
            String endStr = (endLocalTime != null) ? "'" + endLocalTime.format(SQL_FMT) + "'" : "NOW()";

            String query = "UPDATE TimerSessions SET " +
                    "fine = " + endStr + ", " +
                    "durata = TIMESTAMPDIFF(SECOND, inizio, " + endStr + ") " +
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
     * Calcola il tempo totale speso su un task specifico.
     *
     * @param idTask L'ID del task.
     * @return La somma delle durate in secondi (long).
     * @throws DAOException In caso di errori SQL.
     */
    public long getSommaDurataPerTask(int idTask) throws DAOException {
        Statement st = null;
        long totaleSecondi = 0;

        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT SUM(TIMESTAMPDIFF(SECOND, inizio, fine)) as totale " +
                    "FROM TimerSessions " +
                    "WHERE idTask = " + idTask + " AND fine IS NOT NULL";

            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {
                totaleSecondi = rs.getLong("totale");
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
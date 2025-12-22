package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.dao.DAOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) specializzato nella generazione di Statistiche.
 * <p>
 * A differenza degli altri DAO, questa classe non gestisce operazioni CRUD su un Model specifico,
 * ma esegue query di aggregazione (COUNT, SUM, GROUP BY) su più tabelle (Tasks, TimerSessions, Categorie)
 * per fornire i dati necessari alla visualizzazione di grafici e report.
 * </p>
 */
public class DAOStatistics {

    private static DAOStatistics instance = null;
    private static Logger logger = null;

    private DAOStatistics() {}

    /**
     * Restituisce l'unica istanza (Singleton) della classe.
     * @return L'istanza di DAOStatistics.
     */
    public static DAOStatistics getInstance() {
        if (instance == null) {
            instance = new DAOStatistics();
            logger = Logger.getLogger(DAOStatistics.class.getName());
        }
        return instance;
    }

    /**
     * Calcola la distribuzione dei Task per Priorità per un determinato utente.
     * <p>
     * Esegue una query di raggruppamento (GROUP BY priorità) sulla tabella Tasks.
     * Utile per alimentare un grafico a torta (PieChart) che mostra quanti task
     * sono "ALTA", "MEDIA" o "BASSA" priorità.
     * </p>
     *
     * @param idUtente L'ID dell'utente di cui si vogliono le statistiche.
     * @return Una Mappa dove la chiave è la priorità (String) e il valore è il numero di task (Integer).
     * @throws DAOException In caso di errore SQL.
     */
    public Map<String, Integer> getTaskCountByPriority(int idUtente) throws DAOException {
        Map<String, Integer> stats = new HashMap<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            String sql = "SELECT priorità, COUNT(*) as totale " +
                    "FROM Tasks " +
                    "WHERE idUtente = " + idUtente + " " +
                    "GROUP BY priorità";

            logger.info("Statistiche Priorità (User " + idUtente + "): " + sql);
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String priority = rs.getString("priorità");
                int count = rs.getInt("totale");
                if (priority == null || priority.isEmpty()) priority = "Nessuna";
                stats.put(priority, count);
            }
        } catch (SQLException e) {
            throw new DAOException("Errore stat priorità: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return stats;
    }

    /**
     * Calcola il tempo totale speso per ogni Categoria.
     * <p>
     * Questa è una query complessa che coinvolge tre tabelle:
     * </p>
     * <ol>
     * <li><b>TimerSessions:</b> Contiene la durata delle sessioni di lavoro.</li>
     * <li><b>Tasks:</b> Collega le sessioni alle categorie e agli utenti.</li>
     * <li><b>Categorie:</b> Contiene i nomi delle categorie (es. "Lavoro", "Studio").</li>
     * </ol>
     * <p>
     * La query somma le durate (SUM) raggruppandole per nome categoria.
     * </p>
     *
     * @param idUtente L'ID dell'utente.
     * @return Una Mappa dove la chiave è il nome della categoria e il valore è il totale dei secondi (Long).
     * @throws DAOException In caso di errore SQL.
     */
    public Map<String, Long> getTimeSpentByCategory(int idUtente) throws DAOException {
        Map<String, Long> stats = new HashMap<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();


            String sql = "SELECT c.nomeCategoria, SUM(ts.durata) as totaleTempo " +
                    "FROM TimerSessions ts " +
                    "JOIN Tasks t ON ts.idTask = t.idTask " +
                    "JOIN Categorie c ON t.idCategoria = c.idCategoria " +
                    "WHERE ts.fine IS NOT NULL " +
                    "AND t.idUtente = " + idUtente + " " +
                    "GROUP BY c.nomeCategoria";

            logger.info("Statistiche Tempo/Categoria (User " + idUtente + "): " + sql);
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String catName = rs.getString("nomeCategoria");
                long totalSeconds = rs.getLong("totaleTempo");
                stats.put(catName, totalSeconds);
            }
        } catch (SQLException e) {
            throw new DAOException("Errore stat tempo/categoria: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return stats;
    }

    /**
     * Confronta i Task Completati rispetto a quelli Aperti (Da fare).
     * <p>
     * Esegue un conteggio raggruppato per il campo booleano 'completamento'.
     * </p>
     *
     * @param idUtente L'ID dell'utente.
     * @return Un array di interi di dimensione 2:
     * <ul>
     * <li>Indice 0: Numero di Task Aperti (Non completati)</li>
     * <li>Indice 1: Numero di Task Completati</li>
     * </ul>
     * @throws DAOException In caso di errore SQL.
     */
    public int[] getCompletionStats(int idUtente) throws DAOException {
        int[] results = new int[2];
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT completamento, COUNT(*) as num " +
                    "FROM Tasks " +
                    "WHERE idUtente = " + idUtente + " " + // FILTRO UTENTE
                    "GROUP BY completamento";

            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                boolean isCompleted = rs.getBoolean("completamento");
                int count = rs.getInt("num");
                if(isCompleted) results[1] = count;
                else results[0] = count;
            }
        } catch (SQLException e) {
            throw new DAOException("Errore stat completamento: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return results;
    }
}
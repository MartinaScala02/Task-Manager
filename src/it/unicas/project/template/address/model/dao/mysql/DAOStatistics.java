package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.dao.DAOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DAOStatistics {

    private static DAOStatistics instance = null;
    private static Logger logger = null;

    private DAOStatistics() {}

    public static DAOStatistics getInstance() {
        if (instance == null) {
            instance = new DAOStatistics();
            logger = Logger.getLogger(DAOStatistics.class.getName());
        }
        return instance;
    }

    /**
     * 1. Distribuzione dei Task per Priorità (SOLO per l'utente specificato)
     */
    public Map<String, Integer> getTaskCountByPriority(int idUtente) throws DAOException {
        Map<String, Integer> stats = new HashMap<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            // Aggiunto filtro WHERE idUtente
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
     * 2. Tempo speso per Categoria (SOLO per l'utente specificato)
     */
    public Map<String, Long> getTimeSpentByCategory(int idUtente) throws DAOException {
        Map<String, Long> stats = new HashMap<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            // La JOIN collega Timer -> Task -> Categoria.
            // Filtriamo sulla tabella Tasks (t.idUtente)
            String sql = "SELECT c.nomeCategoria, SUM(ts.durata) as totaleTempo " +
                    "FROM TimerSessions ts " +
                    "JOIN Tasks t ON ts.idTask = t.idTask " +
                    "JOIN Categorie c ON t.idCategoria = c.idCategoria " +
                    "WHERE ts.fine IS NOT NULL " +
                    "AND t.idUtente = " + idUtente + " " + // FILTRO UTENTE
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
     * 3. Task Completati vs Aperti (SOLO per l'utente specificato)
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
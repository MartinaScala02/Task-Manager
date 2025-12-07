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

public class DAOTimerSessions implements DAO<TimerSessions> {

    private static DAOTimerSessions instance = null;
    private static Logger logger = null;

    private DAOTimerSessions() {}

    public static DAOTimerSessions getInstance() {
        if (instance == null) {
            instance = new DAOTimerSessions();
            logger = Logger.getLogger(DAOTimerSessions.class.getName());
        }
        return instance;
    }


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

    //con insert inizia il timer
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


    public void stopSession(int idSession) throws DAOException {
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
}
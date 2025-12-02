package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DAOTasks implements DAO<Tasks> {

    private DAOTasks(){}

    private static DAOTasks dao = null;
    private static Logger logger = null;

    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOTasks();
            logger = Logger.getLogger(DAOTasks.class.getName());
        }
        return dao;
    }

    @Override
    public List<Tasks> select(Tasks t) throws DAOException {
        ArrayList<Tasks> lista = new ArrayList<>();
        Statement st = null;

        try {
            st = DAOMySQLSettings.getStatement();

            // Costruzione della query dinamica
            String sql = "SELECT * FROM Tasks WHERE 1=1 ";

            // Filtri opzionali (se l'oggetto t non è null)
            if (t != null) {
                // Filtro per Titolo
                if (t.getTitolo() != null && !t.getTitolo().isEmpty()) {
                    sql += " AND titolo LIKE '" + t.getTitolo() + "%'";
                }
                // Filtro per Categoria
                if (t.getCategoria() != null && !t.getCategoria().isEmpty()) {
                    sql += " AND categoria LIKE '" + t.getCategoria() + "%'";
                }
                // Filtro per Priorità
                if (t.getPriorita() != null && !t.getPriorita().isEmpty()) {
                    sql += " AND priorità = '" + t.getPriorita() + "'";
                }
                // IMPORTANTE: Filtro per Utente (mostra solo le task di quell'utente)
                // Se idUtente è > 0 (o diverso da -1), filtriamo per quello
                if (t.getIdUtente() > 0) {
                    sql += " AND idUtente = " + t.getIdUtente();
                }
            }

            logger.info("SQL Select: " + sql);

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                // Creiamo l'oggetto Tasks con i dati dal DB
                // Ordine costruttore: titolo, descrizione, categoria, scadenza, priorita, data_creazione, completamento, idTask, idUtente
                lista.add(new Tasks(
                        rs.getString("titolo"),
                        rs.getString("descrizione"),
                        rs.getString("categoria"),
                        rs.getString("scadenza"),   // Attenzione: nel DB è DATETIME, qui lo leggiamo come stringa.
                        rs.getString("priorità"),   // Nome colonna nel DB (con accento se l'hai creato con accento)
                        rs.getString("data_creazione"),
                        rs.getBoolean("completamento"),
                        rs.getInt("idTask"),
                        rs.getInt("idUtente")
                ));
            }

        } catch (SQLException sq) {
            throw new DAOException("Errore nella select: " + sq.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    @Override
    public void delete(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() <= 0) {
            throw new DAOException("Impossibile eliminare: idTask mancante.");
        }

        String query = "DELETE FROM Tasks WHERE idTask = " + t.getIdTask();
        logger.info("SQL Delete: " + query);

        executeUpdate(query);
    }

    @Override
    public void insert(Tasks t) throws DAOException {
        verifyObject(t); // Controlla che i campi obbligatori ci siano

        // NOTA: idTask è auto_increment, data_creazione è automatico (CURRENT_TIMESTAMP)
        // Convertiamo il booleano in 1 (true) o 0 (false) per sicurezza SQL
        int completatoInt = t.getCompletamento() ? 1 : 0;

        String query = "INSERT INTO Tasks (titolo, descrizione, categoria, scadenza, priorità, completamento, idUtente) VALUES ('"
                + t.getTitolo() + "', '"
                + t.getDescrizione() + "', '"
                + t.getCategoria() + "', '"
                + t.getScadenza() + "', '"
                + t.getPriorita() + "', "
                + completatoInt + ", "
                + t.getIdUtente() + ")";

        logger.info("SQL Insert: " + query);
        executeUpdate(query);
    }

    @Override
    public void update(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() <= 0) {
            throw new DAOException("Impossibile aggiornare: idTask non valido.");
        }

        int completatoInt = t.getCompletamento() ? 1 : 0;

        String query = "UPDATE Tasks SET "
                + "titolo = '" + t.getTitolo() + "', "
                + "descrizione = '" + t.getDescrizione() + "', "
                + "categoria = '" + t.getCategoria() + "', "
                + "scadenza = '" + t.getScadenza() + "', "
                + "priorità = '" + t.getPriorita() + "', "
                + "completamento = " + completatoInt
                + " WHERE idTask = " + t.getIdTask();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    // Metodo helper per evitare inserimenti errati
    private void verifyObject(Tasks t) throws DAOException {
        if (t == null) throw new DAOException("Task è null");
        if (t.getTitolo() == null || t.getTitolo().isEmpty()) {
            throw new DAOException("Il titolo del task è obbligatorio");
        }
        if (t.getIdUtente() <= 0) {
            throw new DAOException("Task deve essere associato a un utente (idUtente mancante)");
        }
    }

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
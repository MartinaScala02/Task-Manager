package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) per la gestione della tabella 'Categorie' su database MySQL.
 * <p>
 * Implementa l'interfaccia {@link DAO} e utilizza il pattern Singleton per garantire
 * un'unica istanza di accesso ai dati.
 * </p>
 */
public class DAOCategorie implements DAO<Categorie> {

    private DAOCategorie(){}

    private static DAOCategorie dao = null;
    private static Logger logger = null;

    /**
     * Restituisce l'unica istanza (Singleton) della classe DAOCategorie.
     * @return L'istanza singleton.
     */
    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOCategorie();
            logger = Logger.getLogger(DAOCategorie.class.getName()); // Corretto il logger name
        }
        return dao;
    }

    /**
     * Esegue una query di selezione sulla tabella Categorie.
     *
     * @param c Oggetto Categorie contenente i criteri di filtro (opzionale).
     * @return Una lista di oggetti {@link Categorie} trovati.
     * @throws DAOException In caso di errore SQL.
     */
    @Override
    public List<Categorie> select(Categorie c) throws DAOException {
        ArrayList<Categorie> lista = new ArrayList<>();
        Statement st = null;

        try {
            st = DAOMySQLSettings.getStatement();

            // Costruzione della query dinamica
            String sql = "SELECT * FROM Categorie WHERE 1=1 ";

            // Filtri opzionali (se l'oggetto c non è null)
            if (c != null) {
                // Filtro per nomeCategoria (con escape apostrofi)
                if (c.getNomeCategoria() != null && !c.getNomeCategoria().isEmpty()) {
                    sql += " AND nomeCategoria LIKE '" + c.getNomeCategoria().replace("'", "\\'") + "%'";
                }

                // Filtro per ID
                if (c.getIdCategoria() != null && c.getIdCategoria() > 0) {
                    sql += " AND idCategoria = " + c.getIdCategoria();
                }
            }

            logger.info("SQL Select: " + sql);

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                // Creiamo l'oggetto Categorie con i dati dal DB
                lista.add(new Categorie(
                        rs.getString("nomeCategoria"),
                        rs.getInt("idCategoria")
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
     * Elimina una categoria dal database.
     *
     * @param c La categoria da eliminare (richiede ID valido).
     * @throws DAOException Se l'ID è mancante o non valido.
     */
    @Override
    public void delete(Categorie c) throws DAOException {
        if (c == null || c.getIdCategoria() == null || c.getIdCategoria() <= 0) {
            throw new DAOException("Impossibile eliminare: idCategoria mancante.");
        }

        String query = "DELETE FROM Categorie WHERE idCategoria = " + c.getIdCategoria();
        logger.info("SQL Delete: " + query);

        executeUpdate(query);
    }

    /**
     * Inserisce una nuova categoria nel database.
     *
     * @param c La categoria da inserire.
     * @throws DAOException Se i dati non sono validi.
     */
    @Override
    public void insert(Categorie c) throws DAOException {
        verifyObject(c);

        // Escape del nome per sicurezza (gestione apostrofi)
        String nomeSafe = c.getNomeCategoria().replace("'", "\\'");

        // CORREZIONE FONDAMENTALE:
        // 1. Rimuoviamo idCategoria dall'insert: è AUTO_INCREMENT, ci pensa MySQL.
        // 2. Mettiamo gli apici corretti intorno al valore stringa.
        String query = "INSERT INTO Categorie (nomeCategoria) VALUES ('" + nomeSafe + "')";

        logger.info("SQL Insert: " + query);
        executeUpdate(query);
    }

    /**
     * Aggiorna i dati di una categoria esistente.
     *
     * @param c La categoria con i dati aggiornati (richiede ID).
     * @throws DAOException Se l'ID è mancante o non valido.
     */
    @Override
    public void update(Categorie c) throws DAOException {
        if (c == null || c.getIdCategoria() == null || c.getIdCategoria() <= 0) {
            throw new DAOException("Impossibile aggiornare: idCategoria non valido.");
        }

        String nomeSafe = c.getNomeCategoria().replace("'", "\\'");

        // CORREZIONE: Aggiunto l'apice di chiusura mancante dopo il nome
        String query = "UPDATE Categorie SET "
                + "nomeCategoria = '" + nomeSafe + "' "
                + "WHERE idCategoria = " + c.getIdCategoria();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    // Metodo helper per evitare inserimenti errati
    private void verifyObject(Categorie c) throws DAOException {
        if (c == null) throw new DAOException("Categorie è null");
        if (c.getNomeCategoria() == null || c.getNomeCategoria().isEmpty()) {
            throw new DAOException("Il nome della Categoria è obbligatorio");
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
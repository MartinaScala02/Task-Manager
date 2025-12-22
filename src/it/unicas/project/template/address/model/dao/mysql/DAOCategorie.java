package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DAOCategorie implements DAO<Categorie> {

    private DAOCategorie(){}

    private static DAOCategorie dao = null;
    private static Logger logger = null;

    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOCategorie();
            logger = Logger.getLogger(DAOCategorie.class.getName());
        }
        return dao;
    }

    @Override
    public List<Categorie> select(Categorie c) throws DAOException {
        ArrayList<Categorie> lista = new ArrayList<>();
        Statement st = null;

        try {
            st = DAOMySQLSettings.getStatement();
            // Iniziamo la query
            String sql = "SELECT * FROM Categorie WHERE 1=1 ";

            if (c != null) {
                // FILTRO FONDAMENTALE: Carica solo le categorie dell'utente
                if (c.getIdUtente() != null) {
                    sql += " AND idUtente = " + c.getIdUtente();
                }

                if (c.getNomeCategoria() != null && !c.getNomeCategoria().isEmpty()) {
                    sql += " AND nomeCategoria LIKE '" + c.getNomeCategoria().replace("'", "\\'") + "%'";
                }

                if (c.getIdCategoria() != null && c.getIdCategoria() > 0) {
                    sql += " AND idCategoria = " + c.getIdCategoria();
                }
            }

            logger.info("SQL Select: " + sql);

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Categorie cat = new Categorie(
                        rs.getString("nomeCategoria"),
                        rs.getInt("idCategoria")
                );
                // Impostiamo anche l'idUtente nell'oggetto restituito
                cat.setIdUtente(rs.getInt("idUtente"));
                lista.add(cat);
            }

        } catch (SQLException sq) {
            throw new DAOException("Errore nella select: " + sq.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    @Override
    public void insert(Categorie c) throws DAOException {
        verifyObject(c);

        // Controllo di sicurezza
        if (c.getIdUtente() == null) {
            throw new DAOException("Impossibile inserire categoria: idUtente mancante.");
        }

        String nomeSafe = c.getNomeCategoria().replace("'", "\\'");

        // MODIFICA SQL: Inseriamo anche l'idUtente
        String query = "INSERT INTO Categorie (nomeCategoria, idUtente) VALUES ('" + nomeSafe + "', " + c.getIdUtente() + ")";
        logger.info("SQL Insert: " + query);

        // Usiamo una versione modificata di executeUpdate per recuperare l'ID
        insertAndPopulateId(c, query);
    }

    /**
     * Metodo helper specifico per l'insert che recupera l'ID autogenerato
     */
    private void insertAndPopulateId(Categorie c, String query) throws DAOException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DAOMySQLSettings.getStatement().getConnection();

            ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                c.setIdCategoria(rs.getInt(1)); // Imposta l'ID nell'oggetto Categorie
            }
        } catch (SQLException e) {
            throw new DAOException("Errore Database nell'insert: " + e.getMessage());
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException e) {}
        }
    }

    @Override
    public void update(Categorie c) throws DAOException {
        if (c == null || c.getIdCategoria() == null || c.getIdCategoria() <= 0) {
            throw new DAOException("Impossibile aggiornare: idCategoria non valido.");
        }

        String nomeSafe = c.getNomeCategoria().replace("'", "\\'");
        String query = "UPDATE Categorie SET "
                + "nomeCategoria = '" + nomeSafe + "' "
                + "WHERE idCategoria = " + c.getIdCategoria();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    @Override
    public void delete(Categorie c) throws DAOException {
        if (c == null || c.getIdCategoria() == null || c.getIdCategoria() <= 0) {
            throw new DAOException("Impossibile eliminare: idCategoria mancante.");
        }
        String query = "DELETE FROM Categorie WHERE idCategoria = " + c.getIdCategoria();
        executeUpdate(query);
    }

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
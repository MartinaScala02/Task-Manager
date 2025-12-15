package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DAOUtenti implements DAO<Utenti> {

    // Singleton Pattern
    private DAOUtenti() {} //costruttore privato->nessuno fuori della classe può creare istanze
    //static significa che l'istanza è condivisa con tutto il programma
    private static DAOUtenti dao = null; //unica istanza della classe per tutta l'applicazione
    private static Logger logger = null; //serve per i messaggi di log -> errori/warnings/info/etc.

    //si controlla se l'istanza dao è già stata creata
    public static DAOUtenti getInstance() {
        //se no la crea
        if (dao == null) {
            dao = new DAOUtenti();
            logger = Logger.getLogger(DAOUtenti.class.getName());
        }//se sì la restituisce
        return dao;
        //serve per garantire che ci sia una sola istanza della classe DAOUtenti in tutta l'applicazione -> singleton
    }

    //legge dal database gli utenti che corrispondono a certi creiteri (nome, cognome, email, ecc.) e restituisce una lista di Utenti
    @Override
    public List<Utenti> select(Utenti u) throws DAOException { //il metodo select può lanciare un'eccezione di tipo DAOException
        ArrayList<Utenti> lista = new ArrayList<>(); //serve per contenere i risultati della query di ricerca
        Statement st = null; //oggetto per eseguire query SQL

        try {
            st = DAOMySQLSettings.getStatement(); //si apre la connessione al database

            // 1. Iniziamo con una query base sempre vera
            // "WHERE 1=1" è un trucco SQL per poter aggiungere tutti gli "AND" dopo senza preoccuparsi
            String sql = "SELECT * FROM Utenti WHERE 1=1 ";

            // 2. COSTRUZIONE DINAMICA DELLA QUERY
            // Aggiungiamo alla query SOLO i campi che non sono null
            if (u != null) {

                // Filtro Email (Fondamentale per Login)
                if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                    sql += " AND email = '" + u.getEmail() + "'";
                }

                // Filtro Password (Fondamentale per Login)
                // Verifica se nel tuo model il getter si chiama getPsw() o getPassword()
                if (u.getPsw() != null && !u.getPsw().isEmpty()) {
                    sql += " AND psw = '" + u.getPsw() + "'";
                }

                // Filtro Nome - Se è NULL (come nel login), questo IF viene saltato
                // e NON rompe la query.
                if (u.getNome() != null && !u.getNome().isEmpty()) {
                    sql += " AND nome LIKE '" + u.getNome() + "%'";
                }

                // Filtro Cognome - Idem, se è NULL viene ignorato
                if (u.getCognome() != null && !u.getCognome().isEmpty()) {
                    sql += " AND cognome LIKE '" + u.getCognome() + "%'";
                }

                // Filtro ID (se serve cercare per ID specifico)
                if (u.getIdUtente() != null && u.getIdUtente() > 0) {
                    sql += " AND idUtente = " + u.getIdUtente();
                }
            }

            // logger.info("SQL Select generata: " + sql); // De-commenta per debug

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                lista.add(new Utenti(
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email"),
                        rs.getString("psw"),
                        rs.getInt("idUtente")
                ));
            }

        } catch (SQLException e) {
            throw new DAOException("In select(): " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista; //restituisce una lista di utenti che corrispondono ai criteri di ricerca
    }

    @Override
    public void delete(Utenti u) throws DAOException {
        if (u == null || u.getIdUtente() == null) {
            throw new DAOException("In delete: idUtente cannot be null");
        }
        String query = "DELETE FROM Utenti WHERE idUtente=" + u.getIdUtente();

        logger.info("SQL Delete: " + query);
        executeUpdate(query);
    }

    @Override
    public void insert(Utenti u) throws DAOException {
        // Per l'inserimento, invece, vogliamo essere severi: nome ed email servono!
        if (u == null || u.getNome() == null || u.getEmail() == null) {
            throw new DAOException("Impossibile inserire utente: dati mancanti");
        }

        String query = "INSERT INTO Utenti (nome, cognome, email, psw, idUtente) VALUES ('" +
                u.getNome() + "', '" +
                u.getCognome() + "', '" +
                u.getEmail() + "', '" +
                u.getPsw() + "', NULL)";

        logger.info("SQL Insert: " + query);
        executeUpdate(query);
    }

    @Override
    public void update(Utenti u) throws DAOException {
        if (u == null || u.getIdUtente() == null) {
            throw new DAOException("In update: utente o ID nullo");
        }

        String query = "UPDATE Utenti SET " +
                "nome = '" + u.getNome() + "', " +
                "cognome = '" + u.getCognome() + "', " +
                "email = '" + u.getEmail() + "', " +
                "psw = '" + u.getPsw() + "'" +
                " WHERE idUtente = " + u.getIdUtente();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    private void executeUpdate(String query) throws DAOException {
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            st.executeUpdate(query);
        } catch (SQLException e) {
            throw new DAOException("Database Error: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }
}
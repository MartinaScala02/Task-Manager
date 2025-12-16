package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Allegati;
import it.unicas.project.template.address.model.dao.DAOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) per la gestione della tabella 'Allegati' su database MySQL.
 * <p>
 * Gestisce il salvataggio, il recupero e l'eliminazione dei riferimenti ai file allegati.
 * Implementa logiche specifiche per la gestione dei percorsi file (path escaping) per evitare errori SQL.
 * </p>
 */
public class DAOAllegati {

    // Singleton
    private static DAOAllegati instance = null;

    /**
     * Costruttore privato (Singleton).
     */
    private DAOAllegati() {}

    /**
     * Restituisce l'unica istanza di DAOAllegati.
     * @return L'istanza Singleton.
     */
    public static DAOAllegati getInstance() {
        if (instance == null) instance = new DAOAllegati();
        return instance;
    }

    // INSERT: Salva il riferimento del file nel DB
    /**
     * Inserisce un nuovo allegato nel database.
     * <p>
     * Esegue l'escape dei caratteri speciali nei percorsi dei file (in particolare il backslash {@code \} di Windows)
     * e degli apostrofi per garantire la corretta esecuzione della query SQL.
     * </p>
     *
     * @param a L'oggetto Allegati da inserire.
     * @throws DAOException In caso di errore SQL durante l'inserimento.
     */
    public void insert(Allegati a) throws DAOException {
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();

            // CORREZIONE FONDAMENTALE:
            // .replace("\\", "\\\\") -> Raddoppia le barre per Windows (C:\ diventa C:\\)
            // .replace("'", "\\'")   -> Gestisce gli apostrofi (L'albero diventa L\'albero)

            String percorsoSicuro = a.getPercorsoFile()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'");

            String nomeSicuro = a.getNomeFile()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'");

            String sql = "INSERT INTO Allegati (idTask, nomeFile, percorsoFile, tipoFile) VALUES ("
                    + a.getIdTask() + ", '"
                    + nomeSicuro + "', '"
                    + percorsoSicuro + "', '"
                    + a.getTipoFile() + "')";

            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new DAOException("Errore inserimento allegato: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    /**
     * Recupera tutti gli allegati associati a uno specifico Task.
     *
     * @param idTask L'ID del task di cui recuperare gli allegati.
     * @return Una lista di oggetti {@link Allegati}.
     * @throws DAOException In caso di errore SQL durante la lettura.
     */
    public List<Allegati> selectByTaskId(int idTask) throws DAOException {
        List<Allegati> lista = new ArrayList<>();
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT * FROM Allegati WHERE idTask = " + idTask;
            ResultSet rs = st.executeQuery(sql);

            while(rs.next()) {
                Allegati a = new Allegati();
                a.setIdAllegato(rs.getInt("idAllegato"));
                a.setIdTask(rs.getInt("idTask"));
                a.setNomeFile(rs.getString("nomeFile"));
                a.setPercorsoFile(rs.getString("percorsoFile"));
                a.setTipoFile(rs.getString("tipoFile"));
                lista.add(a);
            }
        } catch (SQLException e) {
            throw new DAOException("Errore lettura allegati: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    /**
     * Elimina un allegato dal database tramite il suo ID.
     * <p>
     * Rimuove solo il record nel database, non il file fisico dal disco.
     * </p>
     *
     * @param idAllegato L'ID dell'allegato da eliminare.
     * @throws DAOException In caso di errore SQL durante la cancellazione.
     */
    public void delete(int idAllegato) throws DAOException {
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            st.executeUpdate("DELETE FROM Allegati WHERE idAllegato = " + idAllegato);
        } catch (SQLException e) {
            throw new DAOException("Errore cancellazione allegato: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }
}
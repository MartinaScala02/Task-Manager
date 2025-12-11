package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Allegati;
import it.unicas.project.template.address.model.dao.DAOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DAOAllegati {

    // Singleton
    private static DAOAllegati instance = null;
    private DAOAllegati() {}
    public static DAOAllegati getInstance() {
        if (instance == null) instance = new DAOAllegati();
        return instance;
    }

    // INSERT: Salva il riferimento del file nel DB
    // INSERT: Salva il riferimento del file nel DB
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
    // SELECT BY TASK: Recupera tutti gli allegati di un task specifico
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

    // DELETE: Rimuove un allegato specifico
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

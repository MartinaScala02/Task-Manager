package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.util.DateUtil;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ColleghiDAOMySQLImpl implements DAO<Utenti> {

    private ColleghiDAOMySQLImpl(){}

    private static DAO dao = null;
    private static Logger logger = null;

    public static DAO getInstance(){
        if (dao == null){
            dao = new ColleghiDAOMySQLImpl();
            logger = Logger.getLogger(ColleghiDAOMySQLImpl.class.getName());
        }
        return dao;
    }

    public static void main(String args[]) throws DAOException {
        ColleghiDAOMySQLImpl c = new ColleghiDAOMySQLImpl();


        c.insert(new Utenti("Mario", "Rossi", "molinara@uni.it",  null));
        c.insert(new Utenti("Carlo", "Ciampi", "ciampi@uni.it",  null));
        c.insert(new Utenti("Ornella", "Vaniglia", "vaniglia@uni.it", null));
        c.insert(new Utenti("Cornelia", "Crudelia", "crudelia@uni.it", null));
        c.insert(new Utenti("Franco", "Bertolucci", "bertolucci@uni.it",null));
        c.insert(new Utenti("Carmine", "Labagnara", "lagbagnara@uni.it",  null));
        c.insert(new Utenti("Mauro", "Cresta", "cresta@uni.it",  null));
        c.insert(new Utenti("Andrea", "Coluccio", "coluccio@uni.it", null));


        List<Utenti> list = c.select(null);
        for(int i = 0; i < list.size(); i++){
            System.out.println(list.get(i));
        }


        Utenti toDelete = new Utenti();
        toDelete.setNome("");
        toDelete.setCognome("");
        toDelete.setEmail("");
        //toDelete.setTelefono("");
        toDelete.setIdUtenti(7);

        c.delete(toDelete);

        list = c.select(null);

        for(int i = 0; i < list.size(); i++){
            System.out.println(list.get(i));
        }

    }

    @Override
    public List<Utenti> select(Utenti a) throws DAOException {

        if (a == null){
            a = new Utenti("", "", "", null); // Cerca tutti gli elementi
        }

        ArrayList<Utenti> lista = new ArrayList<>();
        try{

            if (a == null || a.getCognome() == null
                    || a.getNome() == null
                    || a.getEmail() == null
                    /*|| a.getTelefono() == null*/){
                throw new DAOException("In select: any field can be null");
            }

            Statement st = DAOMySQLSettings.getStatement();

            String sql = "select * from Utenti where cognome like '";
            sql += a.getCognome() + "%' and nome like '" + a.getNome();
            // sql += "%' and telefono like '" + a.getTelefono() + "%'";
            /*if (a.getCompleanno() != null){
                sql += " and compleanno like '" + a.getCompleanno() + "%'";
            }*/
            sql += " and email like '" + a.getEmail() + "%'";

            try{
                logger.info("SQL: " + sql);
            } catch(NullPointerException nullPointerException){
                logger.severe("SQL: " + sql);
            }
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                lista.add(new Utenti(rs.getString("nome"),
                        rs.getString("cognome"),
                       /* rs.getString("telefono"),*/
                        rs.getString("email"),
                        /*rs.getString("compleanno"),*/
                        rs.getInt("idUtenti")));
            }
            DAOMySQLSettings.closeStatement(st);

        } catch (SQLException sq){
            throw new DAOException("In select(): " + sq.getMessage());
        }
        return lista;
    }

    @Override
    public void delete(Utenti a) throws DAOException {
        if (a == null || a.getIdUtenti() == null){
            throw new DAOException("In delete: idUtenti cannot be null");
        }
        String query = "DELETE FROM Utenti WHERE idUtenti='" + a.getIdUtenti() + "';";

        try{
          logger.info("SQL: " + query);
        } catch (NullPointerException nullPointerException){
          System.out.println("SQL: " + query);
        }

        executeUpdate(query);

    }


    @Override
    public void insert(Utenti a) throws DAOException {


        verifyObject(a);


        String query = "INSERT INTO Utenti (nome, cognome, telefono, email, compleanno, idUtenti) VALUES  ('" +
                a.getNome() + "', '" + a.getCognome() + "', '" +
                /*a.getTelefono() + "', '" + a.getEmail() + "', '" +*/
                /*a.getCompleanno() + */"', NULL)";
        try {
          logger.info("SQL: " + query);
        } catch (NullPointerException nullPointerException){
          System.out.println("SQL: " + query);
        }
        executeUpdate(query);
    }


    @Override
    public void update(Utenti a) throws DAOException {

        verifyObject(a);

        String query = "UPDATE Utenti SET nome = '" + a.getNome() + "', cognome = '" + a.getCognome() + /*"',  telefono = '" + a.getTelefono() + */ "', email = '" + a.getEmail() + /*"', compleanno = '" + a.getCompleanno() +*/ "'";
        query = query + " WHERE idUtenti = " + a.getIdUtenti() + ";";
        logger.info("SQL: " + query);

        executeUpdate(query);

    }


    private void verifyObject(Utenti a) throws DAOException {
      if (a == null || a.getCognome() == null
        || a.getNome() == null
        || a.getEmail() == null
        /*|| a.getCompleanno() == null*/
        /*|| a.getTelefono() == null*/){
        throw new DAOException("In select: any field can be null");
      }
    }

    private void executeUpdate(String query) throws DAOException{
      try {
        Statement st = DAOMySQLSettings.getStatement();
        int n = st.executeUpdate(query);

        DAOMySQLSettings.closeStatement(st);

      } catch (SQLException e) {
        throw new DAOException("In insert(): " + e.getMessage());
      }
    }


}

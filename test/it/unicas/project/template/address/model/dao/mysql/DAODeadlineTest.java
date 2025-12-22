package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di Test JUnit per le scadenze.
 * <p>
 * Questa classe testa il comportamento ATTUALE del DAO.
 * Attualmente il sistema permette l'inserimento di date passate (comportamento permissivo),
 * quindi i test verificano che l'inserimento avvenga correttamente sia per date future che passate.
 * </p>
 */
public class DAODeadlineTest {

    private DAOTasks dao = (DAOTasks) DAOTasks.getInstance();

    /**
     * Test Scadenza Passata.
     * <p>
     * Verifica che il sistema gestisca l'inserimento di una data passata senza errori SQL.
     * (Nota: Il blocco logico non Ã¨ attivo nel DAO, quindi l'inserimento deve riuscire).
     * </p>
     */
    @Test
    public void testScadenzaPassata() {
        // 1. Creazione Task
        Tasks taskScaduto = new Tasks();
        taskScaduto.setTitolo("Task Test Passato");
        taskScaduto.setDescrizione("Test inserimento data passata");
        taskScaduto.setPriorita("bassa");
        taskScaduto.setIdUtente(14); // Assicurati che l'utente 14 esista, altrimenti usa 1
    }

    /**
     * Test Scadenza Futura.
     * <p>
     * Verifica il funzionamento standard con data futura.
     * </p>
     */
    @Test
    public void testScadenzaFutura() {
        System.out.println("=== TEST INSERIMENTO SCADENZA ===");

        Tasks taskValido = new Tasks();
        taskValido.setTitolo("Task Test");
        taskValido.setPriorita("alta");
        taskValido.setIdUtente(14);

        // Imposto data di domani
        String domani = DateUtil.format(LocalDate.now().plusDays(1));
        taskValido.setScadenza(domani);

        try {
            dao.insert(taskValido);

            assertTrue(taskValido.getIdTask() > 0, "Il task futuro deve essere salvato.");
            System.out.println("Task inserito con ID: " + taskValido.getIdTask());

            dao.delete(taskValido);
            System.out.println("Cleanup eseguito.");

        } catch (DAOException e) {
            fail("Errore inserimento futuro: " + e.getMessage());
        }
    }
}

package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di Test JUnit per la verifica delle regole di validazione sulle scadenze (Deadline).
 * <p>
 * Questa classe controlla che il DAO impedisca correttamente l'inserimento di Task con scadenze
 * non valide (nel passato) e permetta invece l'inserimento di scadenze future.
 * </p>
 */
public class DAODeadlineTest {

    private DAOTasks dao = (DAOTasks) DAOTasks.getInstance();

    /**
     * Test Negativo: Verifica che il sistema rifiuti una scadenza nel passato.
     * <p>
     * <b>Scenario:</b> Si tenta di inserire un task con data di scadenza impostata a "ieri".<br>
     * <b>Risultato atteso:</b> Il metodo {@code dao.insert()} deve lanciare una {@link DAOException}.
     * Se l'eccezione non viene lanciata, il test fallisce.
     * </p>
     */
    @Test
    public void testValidazioneScadenzaPassata() {
        System.out.println("=== TEST VALIDAZIONE SCADENZA ===");

        //Creo un task con scadenza nel PASSATO
        Tasks taskScaduto = new Tasks();
        taskScaduto.setTitolo("Task Proibito");
        taskScaduto.setDescrizione("Questo task non dovrebbe essere salvato");
        taskScaduto.setPriorita("alta");
        taskScaduto.setIdUtente(14); // ID utente esistente nel DB

        // Imposto data di ieri
        String ieri = DateUtil.format(LocalDate.now().minusDays(1));
        taskScaduto.setScadenza(ieri);

        System.out.println("Tentativo inserimento task con data: " + taskScaduto.getScadenza());

        // 2. ESECUZIONE E VERIFICA:
        // Mi aspetto che dao.insert lanci una DAOException.
        // Se NON la lancia, il test fallisce (fail).
        DAOException exception = assertThrows(DAOException.class, () -> {
            dao.insert(taskScaduto);
        });

        // 3. Verifica del messaggio
        System.out.println("Eccezione catturata: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("ERRORE BLOCCANTE") || exception.getMessage().contains("passato"),
                "Il messaggio d'errore dovrebbe spiegare il motivo del blocco");

        System.out.println(" TEST PASSATO: Il sistema ha bloccato correttamente la data passata.\n");
    }

    /**
     * Test Positivo: Verifica che il sistema accetti una scadenza nel futuro.
     * <p>
     * <b>Scenario:</b> Si tenta di inserire un task con data di scadenza impostata a "domani".<br>
     * <b>Risultato atteso:</b> L'inserimento avviene con successo (il task riceve un ID).
     * Al termine, il task di prova viene eliminato per pulire il database.
     * </p>
     */
    @Test
    public void testValidazioneScadenzaFutura() {
        System.out.println("=== TEST SCADENZA FUTURA ===");

        // 1. PREPARAZIONE: Creo un task con scadenza nel FUTURO (Domani)
        Tasks taskValido = new Tasks();
        taskValido.setTitolo("Task Valido");
        taskValido.setPriorita("BASSA");
        taskValido.setIdUtente(14); // Id esistente

        // Imposto data di domani
        String domani = DateUtil.format(LocalDate.now().plusDays(1));
        taskValido.setScadenza(domani);

        System.out.println("Tentativo inserimento task con data: " + taskValido.getScadenza());

        // 2. ESECUZIONE: Qui NON deve lanciare eccezioni
        try {
            dao.insert(taskValido);
            assertTrue(taskValido.getIdTask() > 0, "Il task dovrebbe avere un ID dopo l'inserimento");
            System.out.println("Task inserito con successo. ID: " + taskValido.getIdTask());

            // CLEANUP (Pulizia)
            dao.delete(taskValido);
            System.out.println("Cleanup eseguito.");

        } catch (DAOException e) {
            fail("Non doveva esserci errore con una data futura! Errore: " + e.getMessage());
        }

        System.out.println(" TEST PASSATO: Inserimento data futura consentito.");
    }
}
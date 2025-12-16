package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.TimerSessions;
import it.unicas.project.template.address.model.dao.DAOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Classe di Test JUnit per verificare il ciclo di vita e i calcoli delle sessioni di Timer.
 * <p>
 * Verifica il corretto funzionamento di inserimento (Start), aggiornamento (Stop)
 * e il calcolo automatico della durata della sessione.
 * </p>
 */
public class DAOTimerSessionTest {

    private DAOTimerSessions dao = DAOTimerSessions.getInstance();

    // Creiamo un formatter per rendere leggibili le date (es. "07-12-2025 10:30:00")
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * Test del ciclo di vita completo di una sessione di timer.
     * <p>
     * Passaggi del test:
     * </p>
     * <ol>
     * <li><b>Preparazione:</b> Crea un oggetto sessione associato a un task esistente.</li>
     * <li><b>Start:</b> Inserisce la sessione nel DB (dao.insert). Verifica che venga generato un ID.</li>
     * <li><b>Update (Simulazione Stop):</b> Imposta manualmente orari di inizio e fine per simulare una durata nota (3h 30m 45s).</li>
     * <li><b>Verifica:</b> Recupera la sessione dal DB e controlla che la durata calcolata corrisponda a 12645 secondi.</li>
     * <li><b>Cleanup:</b> Elimina la sessione di test.</li>
     * </ol>
     *
     * @throws DAOException In caso di errori SQL.
     */
    @Test
    public void testLifecycleTimer() throws DAOException {

        System.out.println("=== INIZIO TEST TIMER AVANZATO ===");

        // 1. PREPARAZIONE
        int idTaskEsistente = 13; // deve essere già presente nel DB
        TimerSessions sessione = new TimerSessions();
        sessione.setIdTask(idTaskEsistente);
        sessione.setNome("Sessione Test Formattazione");

        // 2. START
        dao.insert(sessione);
        assertTrue(sessione.getIdSession() > 0);
        System.out.println("1. Start eseguito. ID: " + sessione.getIdSession());

        // 3. UPDATE (Simuliamo una sessione di durata specifica)
        // Simuliamo: Inizio alle 09:00:00, Fine alle 12:30:45 (Durata: 3 ore, 30 min, 45 sec)
        LocalDateTime startSimulato = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0).withSecond(0);
        LocalDateTime endSimulato = LocalDateTime.now().minusDays(1).withHour(12).withMinute(30).withSecond(45);

        sessione.setInizio(startSimulato);
        sessione.setFine(endSimulato);

        dao.update(sessione);

        // 4. VERIFICA E VISUALIZZAZIONE DATI
        // Recuperiamo la sessione aggiornata dal DB
        List<TimerSessions> lista = dao.select(null);
        TimerSessions recuperata = lista.stream()
                .filter(t -> t.getIdSession() == sessione.getIdSession())
                .findFirst()
                .orElse(null);

        assertNotNull(recuperata, "Sessione non trovata!");

        System.out.println("\n--- DETTAGLI SESSIONE RECUPERATA ---");
        System.out.println("Nome:    " + recuperata.getNome());

        // Visualizziamo Inizio e Fine formattati
        if (recuperata.getInizio() != null)
            System.out.println("Inizio:  " + recuperata.getInizio().format(formatter));

        if (recuperata.getFine() != null)
            System.out.println("Fine:    " + recuperata.getFine().format(formatter));

        // Visualizziamo la durata (Secondi grezzi vs Formattata)
        System.out.println("Durata (sec): " + recuperata.getDurataSecondi());
        System.out.println("Durata (fmt): " + recuperata.getDurataFormattata() + " <--- FORMATO LEGGIBILE");
        System.out.println("------------------------------------\n");

        // verifichiamo la durata (3h 30m 45s = 12645 secondi)
        long durataAttesa = (3 * 3600) + (30 * 60) + 45;
        assertEquals(durataAttesa, recuperata.getDurataSecondi());

        // 5. CLEANUP
        dao.delete(sessione);
        System.out.println("5. Pulizia eseguita (Delete). Test Superato.");
    }
}
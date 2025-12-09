package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class DAOTasksFilterTest {

    private DAOTasks dao = (DAOTasks) DAOTasks.getInstance();

    @Test
    public void testPrivacyEFiltri() throws DAOException {

        System.out.println("=== INIZIO TEST PRIVACY & FILTRI ===");

        // --- 1. PREPARAZIONE DATI ---
        // Task MIO: Da fare, Priorità Alta
        Tasks tMio = new Tasks("Mio Task Importante", "Cose private", "2025-12-31", "alta", null, false, null, 12, -1);

        // Task DELL'ALTRO: Identico al mio (stessa priorità, stato, ecc.)
        Tasks tAltro = new Tasks("Task di Martina", "Cose di Mario", "2025-12-31", "alta", null, false, null, 14, -1);

        // Task MIO Completato
        Tasks tMioFinito = new Tasks("Mio Finito", "Già fatto", "2025-12-31", "bassa", null, true, null, 12, -1);

        try {
            dao.insert(tMio);
            dao.insert(tAltro);
            dao.insert(tMioFinito);
            System.out.println("1. Tasks inseriti. (MioID: " + tMio.getIdTask() + ", AltroID: " + tAltro.getIdTask() + ")");

            // --- 2. TEST PRIVACY BASE ---
            // Cerco TUTTI i task associati al MIO id
            System.out.println("\n--- Test Privacy: Cerco tutti i task di ID_IO ---");
            Tasks filtroMio = new Tasks();
            filtroMio.setIdUtente(12); // Filtro per utente corrente
            filtroMio.setCompletamento(null); // Nessun filtro stato

            List<Tasks> risultatiMiei = dao.select(filtroMio);
            stampaRisultati(risultatiMiei);

            // VERIFICHE
            assertTrue(contieneTask(risultatiMiei, tMio.getIdTask()), "Devo vedere il mio task");
            assertTrue(contieneTask(risultatiMiei, tMioFinito.getIdTask()), "Devo vedere il mio task finito");
            assertFalse(contieneTask(risultatiMiei, tAltro.getIdTask()), "ERRORE GRAVE: Vedo il task di un altro utente!");
            System.out.println(" ESITO: OK (Privacy rispettata)");


            // --- 3. TEST PRIVACY + FILTRI (Priorità) ---
            // Cerco i task con priorità 'High'. Nel DB ce ne sono due: uno mio e uno dell'altro.
            // Devo trovarne SOLO UNO (il mio).
            System.out.println("\n--- Test Privacy + Filtro Priorità 'alta' ---");
            Tasks filtroPrio = new Tasks();
            filtroPrio.setIdUtente(12);
            filtroPrio.setPriorita("alta");
            filtroPrio.setCompletamento(null);

            List<Tasks> risultatiPrio = dao.select(filtroPrio);
            stampaRisultati(risultatiPrio);

            assertTrue(contieneTask(risultatiPrio, tMio.getIdTask()), "Devo trovare il mio task 'alta' priorità");
            assertFalse(contieneTask(risultatiPrio, tAltro.getIdTask()), "NON devo trovare il task 'alta' priorità dell'altro utente");
            System.out.println(" ESITO: OK (Filtro applicato solo ai miei dati)");


            // --- 4. TEST PRIVACY + COMPLETAMENTO ---
            System.out.println("\n--- Test Privacy + Filtro Completati (True) ---");
            Tasks filtroFinito = new Tasks();
            filtroFinito.setIdUtente(12);
            filtroFinito.setCompletamento(true);

            List<Tasks> risultatiFiniti = dao.select(filtroFinito);
            stampaRisultati(risultatiFiniti);

            // CORREZIONE: Non controlliamo la size esatta (perché ci sono vecchi dati),
            // ma controlliamo che il NOSTRO task sia stato trovato.
            assertTrue(risultatiFiniti.size() >= 1, "Dovrei trovare almeno il task che ho inserito");
            assertTrue(contieneTask(risultatiFiniti, tMioFinito.getIdTask()), "La lista deve contenere il task 'Mio Finito'");

            System.out.println(" ESITO: OK");

        } finally {
            // --- 5. CLEANUP  ---
            System.out.println("\n--- Pulizia Database ---");

            // Cancella solo se l'ID è valido (> 0)
            if (tMio != null && tMio.getIdTask() > 0) dao.delete(tMio);
            if (tAltro != null && tAltro.getIdTask() > 0) dao.delete(tAltro);
            if (tMioFinito != null && tMioFinito.getIdTask() > 0) dao.delete(tMioFinito);

            System.out.println("Pulizia completata.");
        }
    }

    private void stampaRisultati(List<Tasks> lista) {
        if (lista.isEmpty()) {
            System.out.println("   [!] Nessun risultato.");
        } else {
            System.out.println("   [Trovati " + lista.size() + "]:");
            for (Tasks t : lista) {
                System.out.printf("   -> ID: %-3d | Utente: %-3d | Titolo: %-15s | Prio: %s%n",
                        t.getIdTask(), t.getIdUtente(), t.getTitolo(), t.getPriorita());
            }
        }
    }

    private boolean contieneTask(List<Tasks> lista, int idDaCercare) {
        return lista.stream().anyMatch(t -> t.getIdTask() == idDaCercare);
    }
}
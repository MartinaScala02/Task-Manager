package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.dao.DAOException;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di Test JUnit per verificare il recupero delle statistiche dal DAO.
 * <p>
 * Questa classe testa i metodi di aggregazione dati presenti in {@link DAOStatistics},
 * assicurandosi che le query SQL restituiscano risultati coerenti (mappe non nulle, valori non negativi)
 * per un utente di test specifico.
 * </p>
 */
class DAOStatisticsTest {

    /** ID dell'utente utilizzato per i test di recupero statistiche. */
    private static final int TEST_USER_ID = 14;

    /**
     * Test per la distribuzione dei Task per Priorità.
     * <p>
     * Verifica che il metodo {@code getTaskCountByPriority} restituisca una Mappa valida.
     * Stampa a video il numero di task raggruppati per livello di priorità (Alta, Media, Bassa).
     * </p>
     */
    @Test
    void getTaskCountByPriority() {
        System.out.println("--- TEST: Distribuzione Priorità (Utente " + TEST_USER_ID + ") ---");
        try {
            Map<String, Integer> stats = DAOStatistics.getInstance().getTaskCountByPriority(TEST_USER_ID);

            assertNotNull(stats);
            if (stats.isEmpty()) {
                System.out.println("Nessun dato trovato per l'utente " + TEST_USER_ID);
            } else {
                stats.forEach((k, v) -> System.out.println("Priorità: " + k + " -> Task: " + v));
            }
        } catch (DAOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test per il calcolo del Tempo speso per Categoria.
     * <p>
     * Verifica che il metodo {@code getTimeSpentByCategory} restituisca una Mappa valida.
     * Stampa a video il tempo totale (in secondi e minuti) speso su task di diverse categorie.
     * </p>
     */
    @Test
    void getTimeSpentByCategory() {
        System.out.println("\n--- TEST: Tempo per Categoria (Utente " + TEST_USER_ID + ") ---");
        try {
            Map<String, Long> stats = DAOStatistics.getInstance().getTimeSpentByCategory(TEST_USER_ID);

            assertNotNull(stats);
            if (stats.isEmpty()) {
                System.out.println("Nessuna sessione timer conclusa per l'utente " + TEST_USER_ID);
            } else {
                stats.forEach((k, v) -> {
                    long minuti = v / 60;
                    System.out.println("Categoria: " + k + " -> " + v + " sec (" + minuti + " min)");
                });
            }
        } catch (DAOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test per il conteggio dei Task Aperti vs Completati.
     * <p>
     * Verifica che il metodo {@code getCompletionStats} restituisca un array di due interi
     * (indice 0 = aperti, indice 1 = completati) con valori non negativi.
     * </p>
     */
    @Test
    void getCompletionStats() {
        System.out.println("\n--- TEST: Task Aperti/Chiusi (Utente " + TEST_USER_ID + ") ---");
        try {
            int[] results = DAOStatistics.getInstance().getCompletionStats(TEST_USER_ID);

            System.out.println("Aperti: " + results[0]);
            System.out.println("Completati: " + results[1]);

            assertTrue(results[0] >= 0 && results[1] >= 0);
        } catch (DAOException e) {
            fail(e.getMessage());
        }
    }
}
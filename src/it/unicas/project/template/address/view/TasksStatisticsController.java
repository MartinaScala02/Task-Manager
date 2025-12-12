package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOStatistics;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.util.Map;

public class TasksStatisticsController {

    // Riferimenti agli oggetti nell'FXML
    @FXML
    private PieChart pieChartPriorita;

    @FXML
    private BarChart<String, Number> barChartCategorie;

    @FXML
    private CategoryAxis xAxisCategorie; // Asse X Categorie

    @FXML
    private NumberAxis yAxisCategorie;   // Asse Y Tempo

    @FXML
    private Label lblTaskAperti;

    @FXML
    private Label lblTaskCompletati;

    @FXML
    private Label lblWorkload; // Per avvisi sulle scadenze imminenti

    /**
     * Metodo chiamato automaticamente dopo il caricamento del file FXML.
     */
    @FXML
    private void initialize() {
        // Opzionale: pulisci i grafici all'avvio o metti testi di default
        pieChartPriorita.getData().clear();
        barChartCategorie.getData().clear();
        pieChartPriorita.setLegendVisible(true);
    }

    /**
     * Questo metodo deve essere chiamato dal MainApp o dal controller principale
     * quando si apre la dashboard, passando l'ID dell'utente loggato.
     */
    public void loadStatistics(int idUtente) {
        try {
            // 1. CARICAMENTO PIE CHART (Priorità)
            loadPriorityData(idUtente);

            // 2. CARICAMENTO BAR CHART (Tempo per Categoria)
            loadCategoryTimeData(idUtente);

            // 3. AGGIORNAMENTO ETICHETTE (Aperti vs Chiusi)
            loadCompletionStats(idUtente);

        } catch (DAOException e) {
            showAlert("Errore Caricamento Statistiche", "Impossibile recuperare i dati dal database:\n" + e.getMessage());
        }
    }

    // --- METODI DI SUPPORTO PRIVATI ---

    private void loadPriorityData(int idUtente) throws DAOException {
        Map<String, Integer> data = DAOStatistics.getInstance().getTaskCountByPriority(idUtente);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        // Convertiamo la Mappa in dati per il grafico
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        pieChartPriorita.setData(pieData);
        pieChartPriorita.setTitle("Task per Priorità");
    }

    private void loadCategoryTimeData(int idUtente) throws DAOException {
        Map<String, Long> data = DAOStatistics.getInstance().getTimeSpentByCategory(idUtente);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tempo Speso (Minuti)");

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            // Convertiamo i secondi in minuti per rendere il grafico leggibile
            // Se preferite ore: entry.getValue() / 3600.0
            double minuti = entry.getValue() / 60.0;
            series.getData().add(new XYChart.Data<>(entry.getKey(), minuti));
        }

        barChartCategorie.getData().clear();
        barChartCategorie.getData().add(series);
        barChartCategorie.setAnimated(false); // A volte l'animazione fa glitch se si ricarica spesso
    }

    private void loadCompletionStats(int idUtente) throws DAOException {
        int[] stats = DAOStatistics.getInstance().getCompletionStats(idUtente);

        // stats[0] = Aperti, stats[1] = Completati
        if (lblTaskAperti != null) lblTaskAperti.setText(String.valueOf(stats[0]));
        if (lblTaskCompletati != null) lblTaskCompletati.setText(String.valueOf(stats[1]));

//        // Bonus: Carico di lavoro (scadenze prossimi 7 giorni)
//        Map<String, Integer> workload = DAOStatistics.getInstance().getUpcomingWorkload(idUtente);
//        int totalUpcoming = workload.values().stream().mapToInt(Integer::intValue).sum();
//
//        if (lblWorkload != null) {
//            lblWorkload.setText("Hai " + totalUpcoming + " scadenze nei prossimi 7 giorni!");
//        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
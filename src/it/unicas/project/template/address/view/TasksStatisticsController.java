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

/**
 * Controller per la visualizzazione delle statistiche utente.
 * <p>
 * Questa classe gestisce la logica dietro la schermata delle statistiche, caricando i dati
 * dal database tramite {@link DAOStatistics} e popolando i grafici (PieChart, BarChart)
 * e le etichette riassuntive (Task Aperti, Completati, Carico di Lavoro).
 */
public class TasksStatisticsController {



    /** Grafico a torta per la distribuzione delle priorità dei task. */
    @FXML
    private PieChart pieChartPriorita;

    /** Grafico a barre per il tempo speso per ogni categoria. */
    @FXML
    private BarChart<String, Number> barChartCategorie;

    /** Etichetta che mostra il numero totale di task aperti (non completati). */
    @FXML
    private Label lblTaskAperti;

    /** Etichetta che mostra il numero totale di task completati. */
    @FXML
    private Label lblTaskCompletati;

    /** Etichetta per avvisi sul carico di lavoro (es. "Leggero", "Pesante") basato sui task aperti. */
    @FXML
    private Label lblWorkload;

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX dopo il caricamento del file FXML.
     * <p>
     * Esegue il setup iniziale dei grafici, pulendo eventuali dati precedenti e configurando
     * la visibilità della legenda.
     */
    @FXML
    private void initialize() {
        // Pulisce i grafici per evitare sovrapposizioni di dati vecchi
        pieChartPriorita.getData().clear();
        barChartCategorie.getData().clear();
        pieChartPriorita.setLegendVisible(true);
    }

    /**
     * Metodo principale per avviare il caricamento delle statistiche.
     * <p>
     * Deve essere chiamato dal controller principale (es. MainScreenController) quando si apre
     * la finestra delle statistiche. Coordina il caricamento dei dati per:
     * <ol>
     * <li>Grafico a torta delle priorità.</li>
     * <li>Grafico a barre del tempo per categoria.</li>
     * <li>Etichette di riepilogo (Aperti/Chiusi e Workload).</li>
     * </ol>
     *
     * @param idUtente L'ID dell'utente loggato di cui visualizzare le statistiche.
     */
    public void loadStatistics(int idUtente) {
        try {

            loadPriorityData(idUtente);

            loadCategoryTimeData(idUtente);

            loadCompletionStats(idUtente);

        } catch (DAOException e) {
            showAlert("Errore Caricamento Statistiche", "Impossibile recuperare i dati dal database");
        }
    }

    /**
     * Carica i dati per il grafico a torta (PieChart) che mostra la distribuzione dei task per priorità.
     *
     * @param idUtente L'ID dell'utente.
     * @throws DAOException Se si verifica un errore durante l'interrogazione del database.
     */
    private void loadPriorityData(int idUtente) throws DAOException {

        Map<String, Integer> data = DAOStatistics.getInstance().getTaskCountByPriority(idUtente);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();


        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        pieChartPriorita.getData().clear();
        pieChartPriorita.setData(pieData);
        pieChartPriorita.setTitle("Task per Priorità");
    }

    /**
     * Carica i dati per il grafico a barre (BarChart) che mostra il tempo speso per ogni categoria.
     * I dati temporali vengono convertiti da secondi a minuti per una migliore leggibilità.
     *
     * @param idUtente L'ID dell'utente.
     * @throws DAOException Se si verifica un errore durante l'interrogazione del database.
     */
    private void loadCategoryTimeData(int idUtente) throws DAOException {

        Map<String, Long> data = DAOStatistics.getInstance().getTimeSpentByCategory(idUtente);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tempo Speso (Minuti)");

        for (Map.Entry<String, Long> entry : data.entrySet()) {

            double minuti = entry.getValue() / 60.0;
            series.getData().add(new XYChart.Data<>(entry.getKey(), minuti));
        }

        barChartCategorie.getData().clear();
        barChartCategorie.getData().add(series);
        barChartCategorie.setTitle("Tempo Speso per Categoria");

        barChartCategorie.setAnimated(false);
    }

    /**
     * Carica le statistiche numeriche di completamento e calcola il "Carico di Lavoro" (Workload).
     * <p>
     * Aggiorna le label {@code lblTaskAperti}, {@code lblTaskCompletati} e costruisce un componente
     * grafico personalizzato per {@code lblWorkload} che mostra sia il numero di task aperti
     * che un giudizio testuale (es. "Leggero", "Pesante") con colori codificati.
     *
     * @param idUtente L'ID dell'utente.
     * @throws DAOException Se si verifica un errore durante l'interrogazione del database.
     */
    private void loadCompletionStats(int idUtente) throws DAOException {

        int[] stats = DAOStatistics.getInstance().getCompletionStats(idUtente);
        int taskAperti = stats[0];
        int taskCompletati = stats[1];

        if (lblTaskAperti != null) lblTaskAperti.setText(String.valueOf(taskAperti));
        if (lblTaskCompletati != null) lblTaskCompletati.setText(String.valueOf(taskCompletati));


        if (lblWorkload != null) {
            String testo;
            String colore;


            if (taskAperti == 0) {
                testo = "Niente da fare!";
                colore = "#50fa7b";
            } else if (taskAperti <= 4) {
                testo = "Leggero";
                colore = "#8be9fd";
            } else if (taskAperti <= 9) {
                testo = "Moderato";
                colore = "#ffb86c";
            } else {
                testo = "Pesante";
                colore = "#ff5555";
            }



            Label numLabel = new Label(String.valueOf(taskAperti));
            numLabel.setStyle("-fx-text-fill: " + colore + "; -fx-font-size: 42px; -fx-font-weight: bold;");


            Label textLabel = new Label(testo);
            textLabel.setStyle("-fx-text-fill: " + colore + "; -fx-font-size: 14px; -fx-opacity: 0.9;");


            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(-5);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            box.getChildren().addAll(numLabel, textLabel);


            lblWorkload.setGraphic(box);
            lblWorkload.setText("");
            lblWorkload.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        }
    }

    /**
     * Mostra una finestra di dialogo di errore.
     *
     * @param title   Il titolo della finestra di alert.
     * @param content Il messaggio di dettaglio dell'errore.
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
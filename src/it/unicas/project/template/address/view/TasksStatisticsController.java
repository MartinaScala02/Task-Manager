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

    //Riferimenti agli oggetti nell'FXML
    @FXML
    private PieChart pieChartPriorita;

    @FXML
    private BarChart<String, Number> barChartCategorie;

    @FXML
    private Label lblTaskAperti;

    @FXML
    private Label lblTaskCompletati;

    @FXML
    private Label lblWorkload; //Per avvisi sulle scadenze imminenti

    /**
     * Metodo chiamato automaticamente dopo il caricamento del file FXML.
     */
    @FXML
    private void initialize() {

        //serve per fare setup iniziale dei grafici
        //li pulisce in modo che non mostrino dati vecchi e imposta alcune proprietà
        pieChartPriorita.getData().clear();
        barChartCategorie.getData().clear();
        pieChartPriorita.setLegendVisible(true);
    }

    /**
     * Questo metodo deve essere chiamato dal MainApp o dal controller principale
     * quando si apre la dashboard, passando l'ID dell'utente loggato.
     */

    //metodo principale per caricare statistiche
    public void loadStatistics(int idUtente) {
        try {
            // 1. CARICAMENTO PIE CHART (Task per Priorità)
            loadPriorityData(idUtente);

            // 2. CARICAMENTO BAR CHART (Tempo per Categoria)
            loadCategoryTimeData(idUtente);

            // 3. AGGIORNAMENTO ETICHETTE (task Aperti vs Chiusi)
            loadCompletionStats(idUtente);

        } catch (DAOException e) {
            showAlert("Errore Caricamento Statistiche", "Impossibile recuperare i dati dal database");
        }
    }

    // --- METODI DI SUPPORTO PRIVATI ---
    //il throws serve per indicare che il metodo può generare un'eccezione di tipo DAOException -> chi chiama il metodo deve gestirla (fatto in loadStatistics)
    //il daoexception serve per gestire gli errori di accesso al database -> gestito con un alert
    private void loadPriorityData(int idUtente) throws DAOException {
        Map<String, Integer> data = DAOStatistics.getInstance().getTaskCountByPriority(idUtente); //si recuperano i dati dal database (il metodo getTaskCountByPriority interroga il database per contare quantitativi di task per priorità)
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        // Convertiamo la Mappa in dati per il grafico

        for (Map.Entry<String, Integer> entry : data.entrySet()) { //modo per scorrere una mappa in java con entryset
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue())); //entry.getKey() prende la priorità, entry.getValue() prende il conteggio
        }

        pieChartPriorita.setData(pieData); //pieChartPriorita è l'oggetto grafico nell'FXML (il semplice pieData è un observablelist di dati per il grafico)
        pieChartPriorita.setTitle("Task per Priorità");
    }

    //metodo per caricare i dati del tempo speso per categoria
    private void loadCategoryTimeData(int idUtente) throws DAOException {
        Map<String, Long> data = DAOStatistics.getInstance().getTimeSpentByCategory(idUtente); //recupero dei dati dal database (somma del tempo speso per categoria in secondi) ((usiamo long perchè il tempo può essere grande)
        XYChart.Series<String, Number> series = new XYChart.Series<>(); //serve per creare una serie di dati per il grafico a barre
        series.setName("Tempo Speso (Minuti)");

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            // Convertiamo i secondi in minuti per rendere il grafico leggibile (diviso 60)
            double minuti = entry.getValue() / 60.0;
            series.getData().add(new XYChart.Data<>(entry.getKey(), minuti)); //si prende la categoria e il tempo in minuti e si crea una nuova barra nel grafico
        }

        barChartCategorie.getData().clear();
        barChartCategorie.getData().add(series); //barchartcategorie è l'oggetto grafico nell'FXML
        barChartCategorie.setTitle("Tempo Speso per Categoria");
        barChartCategorie.setAnimated(false); //togliamo perchè a volte l'animazione fa glitch se si ricarica spesso
    }


    private void loadCompletionStats(int idUtente) throws DAOException {
        // stats[0] = Aperti, stats[1] = Completati
        int[] stats = DAOStatistics.getInstance().getCompletionStats(idUtente);
        int taskAperti = stats[0];

        if (lblTaskAperti != null) lblTaskAperti.setText(String.valueOf(taskAperti));
        if (lblTaskCompletati != null) lblTaskCompletati.setText(String.valueOf(stats[1]));

        // --- LOGICA CARICO DI LAVORO ---
        if (lblWorkload != null) {
            String testo;
            String colore;

            if (taskAperti == 0) {
                testo = "Nessuno";
                colore = "#50fa7b"; // Verde
            } else if (taskAperti <= 4) {
                testo = "Leggero";
                colore = "#8be9fd"; // Ciano
            } else if (taskAperti <= 9) {
                testo = "Moderato";
                colore = "#ffb86c"; // Arancione
            } else {
                testo = "Pesante";
                colore = "#ff5555"; // Rosso
            }

            // --- CREAZIONE GRAFICA PERSONALIZZATA ---

            // 1. Etichetta per il NUMERO (Grande)
            Label numLabel = new Label(String.valueOf(taskAperti));
            numLabel.setStyle("-fx-text-fill: " + colore + "; -fx-font-size: 42px; -fx-font-weight: bold;");

            // 2. Etichetta per il TESTO (Piccolo)
            Label textLabel = new Label(testo);
            textLabel.setStyle("-fx-text-fill: " + colore + "; -fx-font-size: 14px; -fx-opacity: 0.9;");

            // 3. Contenitore Verticale (Mette uno sopra l'altro)
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(-5); // -5 riduce lo spazio tra i due
            box.setAlignment(javafx.geometry.Pos.CENTER);
            box.getChildren().addAll(numLabel, textLabel);

            // 4. Imposta il contenitore dentro la label originale
            lblWorkload.setGraphic(box);
            lblWorkload.setText(""); // Pulisci il testo vecchio
            lblWorkload.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
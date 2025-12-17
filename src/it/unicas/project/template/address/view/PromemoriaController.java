package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller per la gestione della finestra dei Promemoria (Notifiche).
 * <p>
 * Questa classe gestisce la visualizzazione delle attività urgenti (scadute,
 * in scadenza oggi o domani) per l'utente corrente. Fornisce un'interfaccia
 * semplificata per visualizzare rapidamente le scadenze e navigare verso
 * i dettagli del task selezionato.
 */
public class PromemoriaController {

    /**
     * Componente grafico che visualizza la lista delle task urgenti.
     */
    @FXML
    private ListView<Tasks> reminderListView;

    /**
     * Lo stage (finestra) in cui questo controller è ospitato.
     */
    private Stage dialogStage;

    /**
     * Callback da eseguire quando la finestra viene chiusa (es. per aggiornare il badge delle notifiche nella Home).
     */
    private Runnable onCloseCallback;

    /**
     * Consumer da eseguire quando un task viene selezionato con doppio click.
     * Permette di passare il task selezionato al controller principale per aprirne i dettagli.
     */
    private Consumer<Tasks> onTaskSelected;


    /**
     * Imposta lo stage per questa finestra di dialogo.
     *
     * @param dialogStage Lo stage della finestra.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Imposta l'azione da eseguire quando la finestra viene chiusa.
     *
     * @param onCloseCallback Un {@link Runnable} contenente la logica di chiusura (es. nascondere il pallino rosso).
     */
    public void setOnCloseCallback(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    /**
     * Imposta l'azione da eseguire quando un task viene selezionato tramite doppio click.
     *
     * @param onTaskSelected Un {@link Consumer} che accetta il {@link Tasks} selezionato.
     */
    public void setOnTaskSelected(Consumer<Tasks> onTaskSelected) {
        this.onTaskSelected = onTaskSelected;
    }

    // --- NUOVO METODO HELPER PER GESTIRE I FORMATI DATA MISTI ---
    private LocalDate smartParse(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            // 1. Prova il formato standard ISO (yyyy-MM-dd) usato dalla creazione rapida
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            try {
                // 2. Se fallisce, prova il formato custom (dd/MM/yyyy) usato da DateUtil
                return DateUtil.parse(dateString);
            } catch (Exception ex) {
                return null; // Data non valida
            }
        }
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX dopo il caricamento del file FXML.
     * <p>
     * Configura:
     * <ul>
     * <li>Il messaggio placeholder se la lista è vuota.</li>
     * <li>Il {@link javafx.scene.control.ListView#setCellFactory} per personalizzare l'aspetto grafico di ogni riga (colori, formattazione date).</li>
     * <li>Il listener per il doppio click del mouse per aprire i dettagli del task.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        // Messaggio se la lista è vuota
        reminderListView.setPlaceholder(new Label("Nessuna scadenza imminente! 🎉"));

        // Setup Grafica Cella
        reminderListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Tasks item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // 1. Configurazione Testi (Titolo e Data)
                    Text title = new Text(item.getTitolo() + "\n");
                    title.setFont(Font.font("System", FontWeight.BOLD, 14));
                    title.setFill(Color.WHITE);

                    String msgData = item.getScadenza();
                    Color colorData = Color.web("#bd93f9"); // Colore default (viola chiaro)

                    try {
                        // MODIFICA: Usiamo smartParse per gestire entrambi i formati
                        LocalDate due = smartParse(item.getScadenza());
                        LocalDate today = LocalDate.now();

                        if (due != null) {
                            if (due.isBefore(today)) {
                                msgData = "SCADUTA IL: " + item.getScadenza();
                                colorData = Color.web("#ff5555"); // Rosso
                            } else if (due.isEqual(today)) {
                                msgData = "SCADE OGGI!";
                                colorData = Color.web("#ffb86c"); // Arancio
                            } else if (due.isEqual(today.plusDays(1))) {
                                msgData = "SCADE DOMANI";
                                colorData = Color.web("#8be9fd"); // Ciano
                            }
                        }
                    } catch (Exception e) {
                        // In caso di errore estremo, mantiene il testo originale
                    }

                    Text date = new Text(msgData);
                    date.setFill(colorData);
                    date.setFont(Font.font("System", FontWeight.BOLD, 11));

                    // TextFlow permette di avere stili diversi nello stesso blocco di testo
                    TextFlow textFlow = new TextFlow(title, date);

                    // Layout HBox per contenere il testo
                    HBox container = new HBox(textFlow);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    setGraphic(container);
                    // Stile CSS inline per la cella (sfondo scuro, bordi arrotondati, cursore mano)
                    setStyle("-fx-background-color: #3F2E51; -fx-background-radius: 10; -fx-border-width: 0 0 5 0; -fx-border-color: #2b2236; -fx-padding: 10; -fx-cursor: hand;");
                }
            }
        });

        // Gestione Doppio Click sulla Lista
        reminderListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Tasks selected = reminderListView.getSelectionModel().getSelectedItem();
                if (selected != null && onTaskSelected != null) {
                    // Notifica il main controller e chiudi questa finestra
                    onTaskSelected.accept(selected);
                    handleClose();
                }
            }
        });
    }


    /**
     * Carica dal database le task urgenti per l'utente specificato e popola la ListView.
     * <p>
     * Criteri di urgenza:
     * <ul>
     * <li>Task non completata.</li>
     * <li>Data di scadenza passata (scaduta).</li>
     * <li>Data di scadenza uguale a oggi.</li>
     * <li>Data di scadenza uguale a domani.</li>
     * </ul>
     *
     * @param userId L'ID dell'utente loggato di cui recuperare le task.
     */
    public void loadUrgentTasks(int userId) {
        try {
            // Creazione template per filtrare query SQL
            Tasks template = new Tasks();
            template.setIdUtente(userId);
            template.setCompletamento(false); // Solo task aperte

            // Recupero tutte le task aperte dell'utente
            List<Tasks> allTasks = DAOTasks.getInstance().select(template);

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            // Filtraggio logico (Java Stream) per le date
            List<Tasks> urgentTasks = allTasks.stream()
                    .filter(t -> {
                        if (t.getScadenza() == null || t.getScadenza().isEmpty()) return false;

                        // MODIFICA: Usiamo smartParse invece di DateUtil.parse
                        LocalDate due = smartParse(t.getScadenza());

                        if (due == null) return false;
                        // Mantieni se la data NON è dopo domani (quindi è scaduta, oggi o domani)
                        return !due.isAfter(tomorrow);
                    })
                    .collect(Collectors.toList());

            // Aggiornamento UI
            ObservableList<Tasks> data = FXCollections.observableArrayList(urgentTasks);
            reminderListView.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Gestisce la chiusura della finestra.
     * <p>
     * Esegue il callback di chiusura (se impostato) e chiude lo stage corrente.
     * Questo metodo può essere collegato a un bottone o chiamato programmaticamente.
     */
    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
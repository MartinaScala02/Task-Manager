package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Allegati;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOAllegati;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import it.unicas.project.template.address.util.DateUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller per la finestra di dialogo di modifica/creazione di un task.
 * <p>
 * Questa classe gestisce l'interazione con l'utente per la modifica delle proprietà di un {@link Tasks},
 * inclusi titolo, descrizione, categoria, priorità, scadenza e la gestione degli allegati (aggiunta, rimozione e apertura).
 * </p>
 */
public class TaskEditDialogController {

    // --- Componenti FXML ---
    @FXML private TextField titoloField;
    @FXML private TextArea descrizioneField;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private DatePicker scadenzaField;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ListView<Allegati> attachmentListView;

    // --- Variabili di stato ---
    private Stage dialogStage;
    private Tasks task;
    private boolean okClicked = false;
    private MainApp mainApp;

    /** Buffer per memorizzare gli allegati aggiunti durante la sessione corrente, prima del salvataggio definitivo. */
    private List<Allegati> newAttachmentsBuffer = new ArrayList<>();

    /** Buffer per memorizzare gli allegati rimossi durante la sessione corrente, da eliminare dal DB al salvataggio. */
    private List<Allegati> attachmentsToDelete = new ArrayList<>();

    /**
     * Metodo di inizializzazione chiamato automaticamente dopo il caricamento del file FXML.
     * Configura i componenti UI come ComboBox, DatePicker e la lista degli allegati.
     */
    @FXML
    private void initialize() {
        setupComboBoxes();
        setupDatePicker();
        setupAttachmentList();
    }

    /**
     * Configura il DatePicker per la scadenza.
     * Disabilita la modifica manuale del testo e colora di rosso le date passate.
     */
    private void setupDatePicker() {
        if (scadenzaField != null) {
            scadenzaField.setShowWeekNumbers(false);
            scadenzaField.setEditable(false);
            scadenzaField.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null && !empty && date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;"); // Rosso chiaro per date passate
                    }
                }
            });
        }
    }

    /**
     * Configura la ListView degli allegati con una CellFactory personalizzata.
     * Ogni cella mostra il nome del file e un pulsante per la rimozione.
     * Gestisce anche il doppio click per l'apertura del file.
     */
    private void setupAttachmentList() {
        if (attachmentListView != null) {
            attachmentListView.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Allegati item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        HBox root = new HBox(10);
                        root.setAlignment(Pos.CENTER_LEFT);

                        Label lblName = new Label("📎 " + item.getNomeFile());
                        lblName.setMaxWidth(Double.MAX_VALUE);

                        // Colora di verde i nuovi allegati, viola quelli esistenti
                        if (newAttachmentsBuffer.contains(item)) {
                            lblName.setStyle("-fx-text-fill: #50fa7b;");
                        } else {
                            lblName.setStyle("-fx-text-fill: #bd93f9;");
                        }

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button btnDelete = new Button("✖");
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-cursor: hand;");

                        // Logica di rimozione allegato
                        btnDelete.setOnAction(event -> {
                            getListView().getItems().remove(item);
                            if (newAttachmentsBuffer.contains(item)) {
                                newAttachmentsBuffer.remove(item);
                            } else {
                                attachmentsToDelete.add(item);
                            }
                        });

                        root.getChildren().addAll(lblName, spacer, btnDelete);
                        setGraphic(root);
                        setText(null);

                        // Doppio click per aprire il file
                        root.setOnMouseClicked(e -> {
                            if (e.getClickCount() == 2 && !btnDelete.isHover()) {
                                openFile(item.getPercorsoFile());
                            }
                        });
                        setTooltip(new Tooltip(item.getPercorsoFile()));
                    }
                }
            });
        }
    }

    /**
     * Configura e popola le ComboBox per Priorità e Categorie.
     * Carica le categorie dal database tramite {@link DAOCategorie}.
     */
    private void setupComboBoxes() {
        priorityComboBox.getItems().clear();
        priorityComboBox.getItems().addAll("BASSA", "MEDIA", "ALTA");
        priorityComboBox.getSelectionModel().selectFirst();

        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add(new Categorie("Tutte le categorie", -1));
        try {
            List<Categorie> listaCategorie = DAOCategorie.getInstance().select(null);
            if (listaCategorie != null) categoryComboBox.getItems().addAll(listaCategorie);
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Errore caricamento categorie: " + e.getMessage());
        }

        // Converter per mostrare solo il nome della categoria nella ComboBox
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomeCategoria(); }
            @Override public Categorie fromString(String string) { return new Categorie(string, null); }
        });
        categoryComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Imposta lo stage della finestra di dialogo.
     * @param dialogStage Lo stage da associare.
     */
    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }

    /**
     * Imposta il riferimento all'applicazione principale.
     * @param mainApp L'istanza di MainApp.
     */
    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }

    /**
     * Restituisce true se l'utente ha cliccato OK, false altrimenti.
     * @return boolean stato del click su OK.
     */
    public boolean isOkClicked() { return okClicked; }

    /**
     * Imposta il task da modificare e popola i campi della form con i suoi dati.
     * Carica anche gli allegati associati dal database.
     *
     * @param task Il task da modificare.
     */
    public void setTask(Tasks task) {
        this.task = task;

        titoloField.setText(task.getTitolo());
        descrizioneField.setText(task.getDescrizione());
        scadenzaField.setValue(DateUtil.parse(task.getScadenza()));
        priorityComboBox.setValue(task.getPriorita());


        if (task.getScadenza() != null && !task.getScadenza().isEmpty()) {
            scadenzaField.setValue(DateUtil.parse(task.getScadenza()));
        } else {

            scadenzaField.setValue(null);
        }
        // Seleziona la categoria corretta nella ComboBox
        if (task.getIdCategoria() != null) {
            for (Categorie c : categoryComboBox.getItems()) {
                if (c.getIdCategoria() != null && c.getIdCategoria().equals(task.getIdCategoria())) {
                    categoryComboBox.setValue(c);
                    break;
                }
            }
        } else {
            categoryComboBox.getSelectionModel().selectFirst();
        }


        loadAttachments();
    }

    /**
     * Carica gli allegati associati al task corrente dal database e li visualizza nella lista.
     */
    private void loadAttachments() {
        if (task.getIdTask() == null) return;
        try {
            List<Allegati> allegatiDB = DAOAllegati.getInstance().selectByTaskId(task.getIdTask());
            attachmentListView.getItems().setAll(allegatiDB);
            attachmentListView.getItems().addAll(newAttachmentsBuffer);
        } catch (DAOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Impossibile caricare gli allegati.");
        }
    }

    /**
     * Gestisce l'aggiunta di un nuovo allegato.
     * Apre un FileChooser, copia il file selezionato nella cartella locale "attachments"
     * e crea un nuovo oggetto {@link Allegati} nel buffer.
     */
    @FXML
    private void handleAddAttachment() {
        FileChooser fileChooser = new FileChooser(); //si apre la finestra di dialogo per la selezione del file
        fileChooser.setTitle("Seleziona file");
        File selected = fileChooser.showOpenDialog(dialogStage); //apre la finestra di dialogo

        if (selected != null) {
            try {
                String projectPath = System.getProperty("user.dir"); //si prende la directory
                File destDir = new File(projectPath, "attachments"); //si crea la cartella attachments se non esiste
                if (!destDir.exists()) destDir.mkdir();

                // Genera nome file univoco con timestamp
                String newFileName = System.currentTimeMillis() + "_" + selected.getName();
                File destFile = new File(destDir, newFileName);
                Files.copy(selected.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                //copia fisica del file nella cartella attachments
                Allegati nuovoAllegato = new Allegati();
                nuovoAllegato.setIdTask(task.getIdTask());
                nuovoAllegato.setNomeFile(selected.getName());
                nuovoAllegato.setPercorsoFile(destFile.getAbsolutePath());

                // Estrai estensione file
                String ext = "";
                int i = newFileName.lastIndexOf('.');
                if (i > 0) ext = newFileName.substring(i+1);
                nuovoAllegato.setTipoFile(ext);

                attachmentListView.getItems().add(nuovoAllegato);
                newAttachmentsBuffer.add(nuovoAllegato);

            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Errore copia file: " + e.getMessage());
            }
        }
    }

    /**
     * Tenta di aprire il file specificato utilizzando l'applicazione predefinita del sistema operativo.
     *
     * @param path Il percorso assoluto del file da aprire.
     */
    private void openFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            File file = new File(path);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert(AlertType.ERROR, "File non trovato o apertura non supportata.");
            }
        } catch (IOException ex) {
            showAlert(AlertType.ERROR, "Errore apertura: " + ex.getMessage());
        }
    }

    /**
     * Gestisce il click sul pulsante OK.
     * Valida l'input, aggiorna l'oggetto Task con i nuovi dati e salva le modifiche agli allegati nel database.
     */
    @FXML
    private void handleOk() {
        if (isInputValid()) {
            task.setTitolo(titoloField.getText());
            task.setDescrizione(descrizioneField.getText());

            // Gestione data opzionale
            if (scadenzaField.getValue() != null) {
                task.setScadenza(DateUtil.format(scadenzaField.getValue()));
            } else {
                task.setScadenza(null);
            }

            task.setPriorita(priorityComboBox.getValue());

            Categorie selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null && selectedCategory.getIdCategoria() != -1) {
                task.setIdCategoria(selectedCategory.getIdCategoria());
            } else {
                task.setIdCategoria(null);
            }

            // Salvataggio modifiche allegati
            try {
                // 1. Elimina quelli rimossi
                for (Allegati a : attachmentsToDelete) {
                    if (a.getIdAllegato() != 0) {
                        DAOAllegati.getInstance().delete(a.getIdAllegato());
                    }
                }

                // 2. Aggiungi i nuovi
                if (task.getIdTask() != null && !newAttachmentsBuffer.isEmpty()) {
                    for (Allegati a : newAttachmentsBuffer) {
                        a.setIdTask(task.getIdTask());
                        DAOAllegati.getInstance().insert(a);
                    }
                }
            } catch (DAOException e) {
                showAlert(AlertType.ERROR, "Errore salvataggio allegati: " + e.getMessage());
                return;
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * Chiude la finestra di dialogo senza salvare le modifiche.
     */
    @FXML private void handleCancel() { dialogStage.close(); }

    /**
     * Valida l'input dell'utente nei campi di testo.
     * Controlla che il titolo non sia vuoto, la data non sia nel passato e la priorità sia selezionata.
     *
     * @return true se l'input è valido, false altrimenti.
     */
    private boolean isInputValid() {
        String errorMessage = "";
        if (titoloField.getText() == null || titoloField.getText().isEmpty()) errorMessage += "Titolo non valido!\n";

        if (scadenzaField.getValue() != null && scadenzaField.getValue().isBefore(LocalDate.now())) {
            errorMessage += "Data passata!\n";
        }

        if (priorityComboBox.getValue() == null) errorMessage += "Priorità mancante!\n";

        if (!errorMessage.isEmpty()) {
            showAlert(AlertType.ERROR, errorMessage);
            return false;
        }
        return true;
    }

    /**
     * Mostra un alert di errore o informazione.
     * @param type Tipo di alert (es. ERROR, INFORMATION).
     * @param msg Messaggio da visualizzare.
     */
    private void showAlert(AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Pulisce il campo della data di scadenza.
     */
    @FXML
    private void handleClearDate() {
        scadenzaField.setValue(null);
    }
}
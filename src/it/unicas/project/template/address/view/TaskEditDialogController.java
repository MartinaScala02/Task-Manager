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
 * Controller per la finestra di dialogo di modifica e creazione di un Task.
 * <p>
 * Questa classe gestisce l'interazione con l'utente per inserire o modificare i dettagli di un'attività,
 * tra cui titolo, descrizione, categoria, priorità, data di scadenza e allegati.
 * Implementa logiche per la validazione dei dati e la gestione dei file allegati (aggiunta/rimozione).
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

    /** Buffer temporaneo per i nuovi allegati aggiunti ma non ancora salvati nel DB. */
    private List<Allegati> newAttachmentsBuffer = new ArrayList<>();

    /** Buffer temporaneo per gli allegati rimossi dall'interfaccia, da cancellare dal DB al salvataggio. */
    private List<Allegati> attachmentsToDelete = new ArrayList<>();

    /**
     * Metodo di inizializzazione chiamato automaticamente dopo il caricamento del file FXML.
     * Configura i componenti UI (ComboBox, DatePicker, ListView).
     */
    @FXML
    private void initialize() {
        setupComboBoxes();
        setupDatePicker();
        setupAttachmentList();
    }

    /**
     * Metodo helper "intelligente" per il parsing della data.
     * Tenta di leggere la data sia nel formato standard ISO (yyyy-MM-dd) usato dal Database,
     * sia nel formato localizzato (es. dd/MM/yyyy) usato dall'interfaccia.
     *
     * @param dateString La stringa che rappresenta la data.
     * @return L'oggetto {@link LocalDate} o null se il parsing fallisce.
     */
    private LocalDate smartParse(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return LocalDate.parse(dateString); // Prova formato ISO (DB standard)
        } catch (Exception e) {
            try {
                return DateUtil.parse(dateString); // Prova formato Italiano (DateUtil)
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Configura il comportamento del DatePicker.
     * Disabilita le date passate e l'editing manuale del testo.
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
     * Definisce l'aspetto di ogni riga (Nome file + pulsante Cancella) e gestisce gli eventi.
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

                        // Colora di verde i nuovi file, viola quelli già esistenti
                        if (newAttachmentsBuffer.contains(item)) {
                            lblName.setStyle("-fx-text-fill: #50fa7b;");
                        } else {
                            lblName.setStyle("-fx-text-fill: #bd93f9;");
                        }

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button btnDelete = new Button("✖");
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-cursor: hand;");

                        // Azione pulsante Cancella
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
     * Popola le ComboBox per la priorità e le categorie.
     * Le categorie vengono caricate dal database.
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

        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Categorie c) { return c == null ? "" : c.getNomeCategoria(); }
            @Override public Categorie fromString(String string) { return new Categorie(string, null); }
        });
        categoryComboBox.getSelectionModel().selectFirst();
    }

    /** Imposta lo stage del dialogo. */
    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }

    /** Imposta il riferimento all'applicazione principale. */
    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }

    /** Restituisce true se l'utente ha confermato con OK. */
    public boolean isOkClicked() { return okClicked; }

    /**
     * Imposta il task da visualizzare/modificare nella finestra.
     * Popola i campi con i valori esistenti e carica gli allegati.
     *
     * @param task L'oggetto Task da modificare.
     */
    public void setTask(Tasks task) {
        this.task = task;

        titoloField.setText(task.getTitolo());
        descrizioneField.setText(task.getDescrizione());

        // Utilizza smartParse per gestire correttamente il formato data proveniente dal DB
        scadenzaField.setValue(smartParse(task.getScadenza()));

        priorityComboBox.setValue(task.getPriorita());

        // Seleziona la categoria corretta
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
     * Carica gli allegati associati al task dal database.
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
     * Gestisce l'aggiunta di un nuovo allegato tramite FileChooser.
     * Copia il file nella cartella locale "attachments" e lo aggiunge al buffer.
     */
    @FXML
    private void handleAddAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona file");
        File selected = fileChooser.showOpenDialog(dialogStage);

        if (selected != null) {
            try {
                String projectPath = System.getProperty("user.dir");
                File destDir = new File(projectPath, "attachments");
                if (!destDir.exists()) destDir.mkdir();

                // Nome univoco per evitare sovrascritture
                String newFileName = System.currentTimeMillis() + "_" + selected.getName();
                File destFile = new File(destDir, newFileName);
                Files.copy(selected.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                Allegati nuovoAllegato = new Allegati();
                nuovoAllegato.setIdTask(task.getIdTask());
                nuovoAllegato.setNomeFile(selected.getName());
                nuovoAllegato.setPercorsoFile(destFile.getAbsolutePath());

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
     * Apre il file specificato con l'applicazione di sistema predefinita.
     * @param path Percorso del file.
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
     * Valida i dati, aggiorna l'oggetto Task con i valori inseriti (convertendo la data in formato ISO)
     * e salva le modifiche agli allegati nel database.
     */
    @FXML
    private void handleOk() {
        if (isInputValid()) {
            task.setTitolo(titoloField.getText());
            task.setDescrizione(descrizioneField.getText());

            // IMPORTANTE: Salva la data in formato ISO (yyyy-MM-dd) per compatibilità con il DB MySQL
            if (scadenzaField.getValue() != null) {
                task.setScadenza(scadenzaField.getValue().toString());
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

            // Gestione persistenza allegati
            try {
                // Elimina quelli rimossi
                for (Allegati a : attachmentsToDelete) {
                    if (a.getIdAllegato() != 0) {
                        DAOAllegati.getInstance().delete(a.getIdAllegato());
                    }
                }
                // Inserisce i nuovi
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

    /** Chiude la finestra senza salvare. */
    @FXML private void handleCancel() { dialogStage.close(); }

    /**
     * Verifica la validità dei campi di input.
     * @return true se i dati sono validi, false altrimenti.
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

    /** Mostra un alert all'utente. */
    private void showAlert(AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Pulisce il campo della data di scadenza. */
    @FXML
    private void handleClearDate() {
        scadenzaField.setValue(null);
    }
}
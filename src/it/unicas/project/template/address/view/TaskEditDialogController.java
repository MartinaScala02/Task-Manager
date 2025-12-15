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

public class TaskEditDialogController {

    @FXML private TextField titoloField;
    @FXML private TextArea descrizioneField;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private DatePicker scadenzaField;
    @FXML private ComboBox<String> priorityComboBox;

    // ALLEGATI
    @FXML private ListView<Allegati> attachmentListView;
    //@FXML private Button btnAddAttachment;

    private Stage dialogStage;
    private Tasks task;
    private boolean okClicked = false;
    private MainApp mainApp;

    // Buffer per i nuovi allegati (da aggiungere al salvataggio)
    private List<Allegati> newAttachmentsBuffer = new ArrayList<>();

    // Buffer per gli allegati da eliminare (da rimuovere al salvataggio)
    private List<Allegati> attachmentsToDelete = new ArrayList<>();

    @FXML
    private void initialize() {
        setupComboBoxes();
        setupDatePicker();
        setupAttachmentList();
    }

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
                        setStyle("-fx-background-color: #ffc0cb;");
                    }
                }
            });
        }
    }

    // --- LOGICA CUSTOM PER LA LISTA ALLEGATI (X ROSSA) ---
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
                        // Creiamo un layout orizzontale
                        HBox root = new HBox(10);
                        root.setAlignment(Pos.CENTER_LEFT);

                        // Icona e Nome
                        Label lblName = new Label("📎 " + item.getNomeFile());
                        lblName.setMaxWidth(Double.MAX_VALUE);

                        // Colore diverso se è nuovo
                        if (newAttachmentsBuffer.contains(item)) {
                            lblName.setStyle("-fx-text-fill: #50fa7b;"); // Verde (Nuovo)
                        } else {
                            lblName.setStyle("-fx-text-fill: #bd93f9;"); // Viola (Esistente)
                        }

                        // Spaziatore per spingere la X a destra
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        // --- BOTTONE X ROSSA SENZA BORDO ---
                        Button btnDelete = new Button("✖");
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 5 0 5;");

                        // Azione Cancella
                        btnDelete.setOnAction(event -> {
                            // Rimuovi dalla vista
                            getListView().getItems().remove(item);

                            if (newAttachmentsBuffer.contains(item)) {
                                // Se era nuovo e non salvato, basta toglierlo dal buffer
                                newAttachmentsBuffer.remove(item);
                            } else {
                                // Se era nel DB, aggiungilo alla lista di quelli da eliminare
                                attachmentsToDelete.add(item);
                            }
                        });

                        // Componiamo la cella
                        root.getChildren().addAll(lblName, spacer, btnDelete);
                        setGraphic(root);
                        setText(null);

                        // Doppio click sulla label per aprire il file
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

    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }
    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }
    public boolean isOkClicked() { return okClicked; }

    public void setTask(Tasks task) {
        this.task = task;

        titoloField.setText(task.getTitolo());
        descrizioneField.setText(task.getDescrizione());
        scadenzaField.setValue(DateUtil.parse(task.getScadenza()));
        priorityComboBox.setValue(task.getPriorita());

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

    @FXML
    private void handleOk() {
        if (isInputValid()) {
            task.setTitolo(titoloField.getText());
            task.setDescrizione(descrizioneField.getText());

            // MODIFICA: La data ora è opzionale
            if (scadenzaField.getValue() != null) {
                task.setScadenza(DateUtil.format(scadenzaField.getValue()));
            } else {
                task.setScadenza(null); // Imposta a null se vuota
            }

            task.setPriorita(priorityComboBox.getValue());

            Categorie selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null && selectedCategory.getIdCategoria() != -1) {
                task.setIdCategoria(selectedCategory.getIdCategoria());
            } else {
                task.setIdCategoria(null);
            }

            // GESTIONE DATABASE ALLEGATI
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

    @FXML private void handleCancel() { dialogStage.close(); }

    private boolean isInputValid() {
        String errorMessage = "";
        if (titoloField.getText() == null || titoloField.getText().isEmpty()) errorMessage += "Titolo non valido!\n";

        // MODIFICA: Controllo data solo se non è null. Se è null, va bene (è opzionale).
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

    private void showAlert(AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


    @FXML
    private void handleClearDate() {
        scadenzaField.setValue(null);
    }

}
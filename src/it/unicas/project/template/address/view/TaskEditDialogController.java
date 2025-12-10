package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.control.DateCell; // IMPORT FONDAMENTALE

import java.time.LocalDate;
import java.util.List;

public class TaskEditDialogController {

    @FXML private TextField titoloField;
    @FXML private TextArea descrizioneField;
    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private DatePicker scadenzaField;
    @FXML private ComboBox<String> priorityComboBox;

    private Stage dialogStage;
    private Tasks task;
    private boolean okClicked = false;
    private MainApp mainApp;

    @FXML
    private void initialize() {
        setupComboBoxes();

        // --- PROTEZIONE CALENDARIO ---
        if (scadenzaField != null) {
            // 1. Rimuove i numeri della settimana (colonna "50, 51..." inutile)
            scadenzaField.setShowWeekNumbers(false);

            // 2. Impedisce di scrivere la data a mano (obbliga a usare il mouse)
            scadenzaField.setEditable(false);

            // 3. Colora di rosa i giorni passati e li rende non cliccabili
            scadenzaField.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null && !empty && date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;"); // Rosa
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
    }

    @FXML
    private void handleOk() {
        if (isInputValid()) {
            task.setTitolo(titoloField.getText());
            task.setDescrizione(descrizioneField.getText());
            task.setScadenza(DateUtil.format(scadenzaField.getValue()));
            task.setPriorita(priorityComboBox.getValue());

            Categorie selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null && selectedCategory.getIdCategoria() != -1) {
                task.setIdCategoria(selectedCategory.getIdCategoria());
            } else {
                task.setIdCategoria(null);
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML private void handleCancel() { dialogStage.close(); }

    private boolean isInputValid() {
        String errorMessage = "";

        if (titoloField.getText() == null || titoloField.getText().isEmpty()) {
            errorMessage += "Titolo non valido!\n";
        }

        // --- VALIDAZIONE DATA CORRETTA ---
        if (scadenzaField.getValue() == null) {
            errorMessage += "Inserisci una data di scadenza!\n";
        } else {
            // Se la data selezionata è PRIMA di oggi, blocca il salvataggio
            if (scadenzaField.getValue().isBefore(LocalDate.now())) {
                errorMessage += "La scadenza non può essere nel passato! Aggiornala.\n";
            }
        }

        if (priorityComboBox.getValue() == null || priorityComboBox.getValue().isEmpty()) {
            errorMessage += "Priorità non valida!\n";
        }

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
}
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

import java.time.LocalDate;
import java.util.List;

public class TaskEditDialogController {

    @FXML
    private TextField titoloField;
    @FXML
    private TextArea descrizioneField;
    @FXML
    private ComboBox<Categorie> categoryComboBox;
    @FXML
    private DatePicker scadenzaField;
    @FXML
    private ComboBox<String> priorityComboBox;

    private Stage dialogStage;
    private Tasks task;
    private boolean okClicked = false;
    private MainApp mainApp;

    @FXML
    private void initialize() {
        setupComboBoxes();
    }

    private void setupComboBoxes() {
        // Priorità
        priorityComboBox.getItems().addAll("BASSA", "MEDIA", "ALTA");
        priorityComboBox.getSelectionModel().selectFirst();

        // Categorie
        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add(new Categorie("Tutte le categorie", -1));
        try {
            List<Categorie> listaCategorie = DAOCategorie.getInstance().select(null);
            categoryComboBox.getItems().addAll(listaCategorie);
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Errore caricamento categorie: " + e.getMessage());
        }

        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie c) {
                return c == null ? "" : c.getNomeCategoria();
            }

            @Override
            public Categorie fromString(String string) {
                return new Categorie(string, null);
            }
        });

        categoryComboBox.getSelectionModel().selectFirst();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

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
                task.setIdCategoria(null); // O un valore di default
            }

            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (titoloField.getText() == null || titoloField.getText().isEmpty()) {
            errorMessage += "Titolo non valido!\n";
        }
//        if (descrizioneField.getText() == null || descrizioneField.getText().isEmpty()) {
//            errorMessage += "Descrizione non valida!\n";
//        }
//        if (scadenzaField.getValue() == null) {
//            errorMessage += "Data di scadenza non valida!\n";
//        }
        if (priorityComboBox.getValue() == null || priorityComboBox.getValue().isEmpty()) {
            errorMessage += "Priorità non valida!\n";
        }
//        if (categoryComboBox.getSelectionModel().getSelectedItem() == null) {
//            errorMessage += "Categoria non valida!\n";
//        }

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
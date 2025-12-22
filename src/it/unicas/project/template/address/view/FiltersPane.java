package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Gestisce la logica dei filtri nell'interfaccia grafica dei task.
 * <p>
 * La classe collega componenti UI come ComboBox, DatePicker, ToggleButton e menu categorie
 * con {@link TasksList}, che applica effettivamente i filtri.
 * Si occupa anche di caricare e gestire le categorie dal database e aggiornare la UI di conseguenza.
 * </p>
 */
public class FiltersPane {

    private final ComboBox<String> filterPriorityCombo;
    private final DatePicker filterDatePicker;
    private final ToggleButton btnFilterTodo;
    private final ToggleButton btnFilterDone;
    private final VBox categoryMenuContainer;
    private final ComboBox<Categorie> formCategoryCombo;
    private final TasksList tasksListHelper;

    /**
     * Costruisce un FiltersPane collegando i componenti UI e l'helper della lista task.
     *
     * @param filterPriorityCombo   ComboBox per la priorità
     * @param filterDatePicker      DatePicker per la data
     * @param btnFilterTodo         ToggleButton per TODO
     * @param btnFilterDone         ToggleButton per DONE
     * @param categoryMenuContainer VBox del menu categorie
     * @param formCategoryCombo     ComboBox categorie nel form
     * @param tasksListHelper       Helper per gestire i task e i filtri
     */
    public FiltersPane(ComboBox<String> filterPriorityCombo,
                       DatePicker filterDatePicker,
                       ToggleButton btnFilterTodo,
                       ToggleButton btnFilterDone,
                       VBox categoryMenuContainer,
                       ComboBox<Categorie> formCategoryCombo,
                       TasksList tasksListHelper) {
        this.filterPriorityCombo = filterPriorityCombo;
        this.filterDatePicker = filterDatePicker;
        this.btnFilterTodo = btnFilterTodo;
        this.btnFilterDone = btnFilterDone;
        this.categoryMenuContainer = categoryMenuContainer;
        this.formCategoryCombo = formCategoryCombo;
        this.tasksListHelper = tasksListHelper;

        initialize();
    }

    /**
     * Inizializza il pannello dei filtri impostando listener e caricando le categorie.
     */
    private void initialize() {
        setupListeners();
        refreshCategories();
    }

    /**
     * Configura i listener dei componenti UI per aggiornare i filtri in TasksList.
     */
    private void setupListeners() {
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("TUTTE", "ALTA", "MEDIA", "BASSA");
            filterPriorityCombo.valueProperty().addListener((o, oldV, newV) ->
                    tasksListHelper.setFilterPriority(
                            (newV == null || newV.equals("TUTTE")) ? null : newV
                    )
            );
        }

        if (filterDatePicker != null) {
            filterDatePicker.valueProperty().addListener((o, oldV, newV) ->
                    tasksListHelper.setFilterDate(newV)
            );
        }
    }

    /**
     * Imposta il filtro sullo stato dei task.
     *
     * @param status {@code true} per DONE, {@code false} per TODO, {@code null} per nessun filtro
     */
    public void setFilterStatus(Boolean status) {
        tasksListHelper.setFilterStatus(status);
        updateStatusButtons(status);
    }

    /**
     * Aggiorna lo stile dei pulsanti di stato in base al filtro attivo.
     *
     * @param status stato del filtro
     */
    private void updateStatusButtons(Boolean status) {
        String defaultStyle = "-fx-alignment: CENTER_LEFT;";
        if (btnFilterTodo != null) btnFilterTodo.setStyle(defaultStyle);
        if (btnFilterDone != null) btnFilterDone.setStyle(defaultStyle);

        if (status != null) {
            ToggleButton activeBtn = status ? btnFilterDone : btnFilterTodo;
            if (activeBtn != null) {
                activeBtn.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        }
    }

    /**
     * Ricarica le categorie dal database e aggiorna la UI (ComboBox e menu laterale).
     */
    public void refreshCategories() {
        try {
            List<Categorie> list = DAOCategorie.getInstance().select(null);

            if (formCategoryCombo != null) {
                formCategoryCombo.setItems(FXCollections.observableArrayList(list));
            }

            if (categoryMenuContainer != null) {
                categoryMenuContainer.getChildren().clear();

                Hyperlink allLink = new Hyperlink("Tutte le Categorie");
                allLink.setStyle("-fx-text-fill: white; -fx-underline: false;");
                allLink.setOnAction(e -> resetAllFilters());
                categoryMenuContainer.getChildren().add(allLink);

                for (Categorie c : list) {
                    HBox row = new HBox(5);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Hyperlink link = new Hyperlink(c.getNomeCategoria());
                    link.setStyle("-fx-text-fill: white; -fx-underline: false;");
                    link.setOnAction(e -> {
                        tasksListHelper.setFilterCategory(c);
                        updateStatusButtons(null);
                    });

                    Button btnX = new Button("x");
                    btnX.setStyle("-fx-text-fill: #e74c3c; -fx-background-color: transparent; -fx-border-color: transparent;");
                    btnX.setOnAction(e -> deleteCategory(c));

                    row.getChildren().addAll(link, btnX);
                    categoryMenuContainer.getChildren().add(row);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * Elimina una categoria dal database dopo conferma dell'utente.
     *
     * @param c categoria da eliminare
     */
    private void deleteCategory(Categorie c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare categoria " + c.getNomeCategoria() + "?",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                DAOCategorie.getInstance().delete(c);
                resetAllFilters();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR,
                        "Errore! Impossibile eliminare la categoria.").show();
            }
        }
    }

    /**
     * Resetta tutti i filtri e aggiorna l'interfaccia grafica.
     */
    public void resetAllFilters() {
        tasksListHelper.clearFilters();

        if (formCategoryCombo != null) formCategoryCombo.getSelectionModel().clearSelection();
        if (filterPriorityCombo != null) filterPriorityCombo.getSelectionModel().selectFirst();
        if (filterDatePicker != null) filterDatePicker.setValue(null);

        updateStatusButtons(null);
        refreshCategories();
    }
}
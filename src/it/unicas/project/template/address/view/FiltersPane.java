package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class FiltersPane {

    //per l'UI (final-> riferimenti immutabili dopo costruzione)
    private final ComboBox<String> filterPriorityCombo;
    private final DatePicker filterDatePicker;
    private final Button btnFilterTodo;
    private final Button btnFilterDone;
    private final VBox categoryMenuContainer;
    private final ComboBox<Categorie> formCategoryCombo; //serve per aggiornare anche la combobox nel form di creazione


    private final TasksList tasksListHelper;

    //costruttore
    public FiltersPane(ComboBox<String> filterPriorityCombo, DatePicker filterDatePicker, Button btnFilterTodo, Button btnFilterDone, VBox categoryMenuContainer, ComboBox<Categorie> formCategoryCombo, TasksList tasksListHelper) {

        this.filterPriorityCombo = filterPriorityCombo;
        this.filterDatePicker = filterDatePicker;
        this.btnFilterTodo = btnFilterTodo;
        this.btnFilterDone = btnFilterDone;
        this.categoryMenuContainer = categoryMenuContainer;
        this.formCategoryCombo = formCategoryCombo;
        this.tasksListHelper = tasksListHelper;

        initialize();
    }

    private void initialize() {
        setupListeners();
        refreshCategories(); // Carica le categorie all'avvio
    }

    private void setupListeners() {
        //filtro per priorità
        if (filterPriorityCombo != null) {
            filterPriorityCombo.getItems().setAll("TUTTE", "ALTA", "MEDIA", "BASSA"); //setta gli items della combobox
            filterPriorityCombo.valueProperty().addListener((o, oldV, newV) -> //il listener sta 'in ascolto', ogni volta che l'utente cambia scelta chiama automaticamente la funzione passata (taskslisthelper)
                    tasksListHelper.setFilterPriority((newV == null || newV.equals("TUTTE")) ? null : newV)); //se si selezoina tutte non si filtra altrimenti si applica il filtro con la priorità scelta con setfilterpriority (devo mettere tutte come scelta finta perchè nella combobox non posso non scegliere)
        }
       //filtro per data
        if (filterDatePicker != null) {
            filterDatePicker.valueProperty().addListener((o, oldV, newV) ->
                    tasksListHelper.setFilterDate(newV)); //posso lasciare il campo vuoto quindi non metto l'opzione finta
        }
    }

   //gestione dello stato
    public void setFilterStatus(Boolean status) {
        tasksListHelper.setFilterStatus(status); //imposta il filtro sullo stato scelto
        updateStatusButtons(status); //aggiorna l'interfaccia grafica per far vedere i filtri scelti
    }

    private void updateStatusButtons(Boolean status) {
        //reset stile
        String defaultStyle = "-fx-alignment: CENTER_LEFT;";
        if (btnFilterTodo != null) btnFilterTodo.setStyle(defaultStyle);
        if (btnFilterDone != null) btnFilterDone.setStyle(defaultStyle);


        if (status != null) {
            Button activeBtn = status ? btnFilterDone : btnFilterTodo; //se status true mi evidenzia il pulsante done se false mi evidenzia il pulsante todo -> colora il pulsante corrispondente a quello selezionato
            if (activeBtn != null) {
                activeBtn.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        }
    }


    public void refreshCategories() {
        try {
            List<Categorie> list = DAOCategorie.getInstance().select(null);

            // 1. Aggiorna la ComboBox nel form di creazione
            if (formCategoryCombo != null) {
                formCategoryCombo.setItems(FXCollections.observableArrayList(list));
            }

            // 2. Aggiorna il Menu Laterale
            if (categoryMenuContainer != null) {
                categoryMenuContainer.getChildren().clear();

                // Link "Tutte"
                Hyperlink allLink = new Hyperlink("Tutte le Categorie");
                // Forza il colore bianco e rimuove la sottolineatura
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

                        link.setStyle("-fx-text-fill: white; -fx-underline: false;");

                        // Reset visivo degli altri filtri
                        updateStatusButtons(null);
                    });

                    Button btnX = new Button("x");
                    btnX.setStyle("-fx-text-fill: #e74c3c; -fx-background-color:transparent; -fx-font-weight: bold;");
                    btnX.setOnAction(e -> deleteCategory(c));

                    row.getChildren().addAll(link, btnX);
                    categoryMenuContainer.getChildren().add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //DOMANDA: devo fare un controllo in più per la gestione dell'errore? fare check
    private void deleteCategory(Categorie c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare categoria " + c.getNomeCategoria() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            try {
                DAOCategorie.getInstance().delete(c);
                resetAllFilters(); //aggiorna l'interfaccia e ricarica la lista
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Errore!!! Impossibile eliminare la categoria scelta.");
                error.show();
            }
        }
    }



    public void resetAllFilters() {

        tasksListHelper.clearFilters(); //reset della logica

        //reset dell'UI
        if (filterPriorityCombo != null) filterPriorityCombo.getSelectionModel().selectFirst();
        if (filterDatePicker != null) filterDatePicker.setValue(null);
        updateStatusButtons(null);

        refreshCategories(); //ricarica la lista
    }
}
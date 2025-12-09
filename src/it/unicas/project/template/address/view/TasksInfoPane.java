package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOSubTasks;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;

public class TasksInfoPane {

    private final VBox rightDetailPanel;
    private final Label detailTitleLabel;
    private final Label detailCategoryLabel;
    private final DatePicker detailDueDatePicker;
    private final TextArea detailDescArea;
    private final ListView<SubTasks> subTaskListView; //creo una lista per mostrare le subtasks ->listview mi serve per mostrarle a schermo in lista
    private final TextField newSubTaskField;
    private final ListView<Tasks> mainListView;

    // --- NUOVI CAMPI TIMER ---
    private final Label timerLabel;
    private final Label timerStatusLabel;
    private final Button btnTimerToggle;
    private final Button btnTimerReset;

    // --- STATO TIMER ---
    private Timeline timeline;
    private int secondsElapsed = 0;
    private boolean isTimerRunning = false;
    // -------------------------

    private ObservableList<SubTasks> subTasksList; //contenitore effettivo dei dati presenti nella lista di sotto task
    private Tasks currentSelectedTask;
    private boolean isOpen = false;

    //costruttore AGGIORNATO con i parametri del timer
    public TasksInfoPane(VBox rightDetailPanel, Label detailTitleLabel, Label detailCategoryLabel,
                         DatePicker detailDueDatePicker, TextArea detailDescArea,
                         ListView<SubTasks> subTaskListView, TextField newSubTaskField,
                         ListView<Tasks> mainListView,
                         // Parametri Timer:
                         Label timerLabel, Label timerStatusLabel, Button btnTimerToggle, Button btnTimerReset) {

        this.rightDetailPanel = rightDetailPanel;
        this.detailTitleLabel = detailTitleLabel;
        this.detailCategoryLabel = detailCategoryLabel;
        this.detailDueDatePicker = detailDueDatePicker;
        this.detailDescArea = detailDescArea;
        this.subTaskListView = subTaskListView;
        this.newSubTaskField = newSubTaskField;
        this.mainListView = mainListView;

        // Assegnazione Timer
        this.timerLabel = timerLabel;
        this.timerStatusLabel = timerStatusLabel;
        this.btnTimerToggle = btnTimerToggle;
        this.btnTimerReset = btnTimerReset;

        init();
    }

    private void init() {

        //voglio che le informazioni vengano soltanto mostrate
        //serve per non rendere editabile la descrizione -> voglio che si veda soltanto
        if (detailDescArea != null) {
            detailDescArea.setEditable(false);
            detailDescArea.setWrapText(true);
        }
        //per non rendere editabile la scadenza
        if (detailDueDatePicker != null) {
            detailDueDatePicker.setEditable(false);
            detailDueDatePicker.setMouseTransparent(true);
            detailDueDatePicker.setFocusTraversable(false);
            detailDueDatePicker.setStyle("-fx-opacity: 1;");
        }

        subTasksList = FXCollections.observableArrayList();
        if (subTaskListView != null) {
            subTaskListView.setItems(subTasksList);
            setupSubTaskCellFactory();
        }

        // Inizializzo logica timer
        setupTimerLogic();
    }

    // --- NUOVO METODO: LOGICA TIMER ---
    private void setupTimerLogic() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateTimerDisplay();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        if (btnTimerToggle != null) btnTimerToggle.setOnAction(e -> toggleTimer());
        if (btnTimerReset != null) btnTimerReset.setOnAction(e -> resetTimer());
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            timeline.stop();
            isTimerRunning = false;
            if (timerStatusLabel != null) timerStatusLabel.setText("In pausa");
            if (btnTimerToggle != null) {
                btnTimerToggle.setText("▶");
                btnTimerToggle.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-background-radius: 30; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
            }
        } else {
            timeline.play();
            isTimerRunning = true;
            if (timerStatusLabel != null) timerStatusLabel.setText("In corso...");
            if (btnTimerToggle != null) {
                btnTimerToggle.setText("⏸");
                btnTimerToggle.setStyle("-fx-background-color: #50fa7b; -fx-text-fill: white; -fx-background-radius: 30; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
            }
        }
    }

    public void resetTimer() {
        if (timeline != null) timeline.stop();
        isTimerRunning = false;
        secondsElapsed = 0;
        updateTimerDisplay();
        if (timerStatusLabel != null) timerStatusLabel.setText("In pausa");
        if (btnTimerToggle != null) {
            btnTimerToggle.setText("▶");
            btnTimerToggle.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-background-radius: 30; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
        }
    }

    private void updateTimerDisplay() {
        if (timerLabel != null) {
            int h = secondsElapsed / 3600;
            int m = (secondsElapsed % 3600) / 60;
            int s = secondsElapsed % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }


    public void openPanel(Tasks task, String categoryName) {
        this.currentSelectedTask = task;

        if (detailTitleLabel != null) detailTitleLabel.setText(task.getTitolo()); //prende il titolo task
        if (detailDescArea != null) detailDescArea.setText(task.getDescrizione()); //prende la descrizione

        if (detailDueDatePicker != null) {
            if (task.getScadenza() != null && !task.getScadenza().isEmpty())
                detailDueDatePicker.setValue(LocalDate.parse(task.getScadenza())); //localdate.parse mi serve per convertire la string in localdate -> nel database la data è salvata come stringa mentre il datepicker lavora solo con localdate
            else
                detailDueDatePicker.setValue(null);
        }

        //inutile tanto categoria non può mai essere vuota
        //if (detailCategoryLabel != null) detailCategoryLabel.setText(categoryName.isEmpty() ? "Categoria" : categoryName); // condizione ? valore se vera : valore se falsa (è un if else compatto)
        detailCategoryLabel.setText(categoryName);

        refreshSubTasks(); //carica le subtask nel DB

        if (!isOpen && rightDetailPanel != null) { //se pannello non aperto
            animatePanel(0); //mi trasla il pannello alla x = 0, posizione neutrale -> io parto già con il pannello traslato a dx con traslatex positivo pari alla larghezza del pannello
            isOpen = true;
        }
    }

    public void closePanel() {
        if (isOpen && rightDetailPanel != null) {
            double width = rightDetailPanel.getWidth(); //mi calcolo la larghezza del pannello -> mi serve per la traslazione
            if (width == 0) width = 450; //450 è la larghezza del pannello -> questa riga di codice mi serve per quando non mi reinderizza subito il pannello e quindi la larghezza è zero
            animatePanel(width); //mi sposta il pannello fuori
            isOpen = false;

            mainListView.getSelectionModel().clearSelection();
            currentSelectedTask = null;
        }
    }

    public boolean isOpen() { return isOpen; }

    public Tasks getCurrentTask() { return currentSelectedTask; }

    private void animatePanel(double toX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), rightDetailPanel); //crea transizione sul panel laterale
        tt.setToX(toX);
        tt.play();
    }

    private void refreshSubTasks() {
        if (currentSelectedTask == null) return;
        try {
            //creo un filtro per filtrare solo con l'id della task corrente -> il DB prende solo le subtasks legate a quella task
            SubTasks filtro = new SubTasks();
            filtro.setIdTask(currentSelectedTask.getIdTask()); //prendo id task corrente
            DAO<SubTasks> dao = (DAO<SubTasks>) DAOSubTasks.getInstance(); //chiamo il DAO per leggere dal DB
            List<SubTasks> list = dao.select(filtro); //faccio il select con il filtro
            subTasksList.setAll(list); //aggiorno la lista così si vedono le subtasks
        } catch (DAOException e) {
            e.printStackTrace();
        }
    }

    public void createSubTask() {
        String titolo = newSubTaskField.getText().trim();
        if (titolo.isEmpty() || currentSelectedTask == null) return;

        try {
            SubTasks st = new SubTasks();
            st.setTitolo(titolo);
            st.setIdTask(currentSelectedTask.getIdTask());
            st.setCompletamento(false);
            st.setDescrizione(""); //ATTENZIONE ->ha senso che le sottotasks abbiano una descrizione???

            DAO<SubTasks> dao = (DAO<SubTasks>) DAOSubTasks.getInstance();
            dao.insert(st); //chiamo l'insert per mettere i dati nel DB -> DOMANDA: setIdTasks dà problemi? vedere se l'id delle subtasks sono autoassegnate e incrementali
            subTasksList.add(st);
            newSubTaskField.clear();
        } catch (DAOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Errore subtask: " + e.getMessage());
            a.show();
        }
    }

    //mi serve per definire come appare ogni cella della subtasklistview
    private void setupSubTaskCellFactory() {
        subTaskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(SubTasks item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    CheckBox cb = new CheckBox();
                    cb.setSelected(item.getCompletamento());
                    cb.setOnAction(e -> {
                        item.setCompletamento(cb.isSelected());
                        try { DAOSubTasks.getInstance().update(item); } catch (DAOException ex) { ex.printStackTrace(); }
                        updateItem(item, false);
                    });

                    Label label = new Label(item.getTitolo());
                    label.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                    if (item.getCompletamento()) label.setStyle("-fx-text-fill: #888; -fx-strikethrough: true; -fx-font-size: 13px;");
                    HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
                    label.setMaxWidth(Double.MAX_VALUE);

                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> {
                        try { DAOSubTasks.getInstance().delete(item); subTasksList.remove(item); }
                        catch (DAOException ex) { ex.printStackTrace(); }
                    });

                    box.getChildren().addAll(cb, label, delBtn);
                    setGraphic(box);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }
}
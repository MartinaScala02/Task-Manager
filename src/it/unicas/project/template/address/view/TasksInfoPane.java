package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.TimerSessions;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOSubTasks;
import it.unicas.project.template.address.model.dao.mysql.DAOTimerSessions;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime; // <--- IMPORTANTE
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TasksInfoPane {

    // --- UI COMPONENTS ---
    private final VBox rightDetailPanel;
    private final Label detailTitleLabel;
    private final Label detailCategoryLabel;
    private final DatePicker detailDueDatePicker;
    private final TextArea detailDescArea;
    private final ListView<SubTasks> subTaskListView;
    private final TextField newSubTaskField;
    private final ListView<Tasks> mainListView;

    // --- TIMER UI ---
    private final Label timerLabel;
    private final Label timerStatusLabel;
    private final Button btnTimerToggle;
    private final Button btnTimerReset;

    // --- STORICO UI ---
    private final Button btnTimerMenu;
    private final VBox timerHistoryContainer;
    private final ListView<TimerSessions> timerHistoryList;
    private final Label timerTotalLabel;

    // --- LOGICA TIMER ---
    private Timeline timeline;
    private int secondsElapsed = 0;
    private boolean isTimerRunning = false;
    private Tasks currentSelectedTask;
    private boolean isOpen = false;
    private ObservableList<SubTasks> subTasksList;

    // *** ID DB e ORARIO LOCALE ***
    private volatile int currentDbSessionId = -1;
    private LocalDateTime startLocalTime; // <--- NUOVA VARIABILE PER SINCRONIZZARE IL TEMPO

    public TasksInfoPane(VBox rightDetailPanel, Label detailTitleLabel, Label detailCategoryLabel,
                         DatePicker detailDueDatePicker, TextArea detailDescArea,
                         ListView<SubTasks> subTaskListView, TextField newSubTaskField,
                         ListView<Tasks> mainListView,
                         Label timerLabel, Label timerStatusLabel, Button btnTimerToggle, Button btnTimerReset,
                         Button btnTimerMenu, VBox timerHistoryContainer,
                         ListView<TimerSessions> timerHistoryList, Label timerTotalLabel) {

        this.rightDetailPanel = rightDetailPanel;
        this.detailTitleLabel = detailTitleLabel;
        this.detailCategoryLabel = detailCategoryLabel;
        this.detailDueDatePicker = detailDueDatePicker;
        this.detailDescArea = detailDescArea;
        this.subTaskListView = subTaskListView;
        this.newSubTaskField = newSubTaskField;
        this.mainListView = mainListView;
        this.timerLabel = timerLabel;
        this.timerStatusLabel = timerStatusLabel;
        this.btnTimerToggle = btnTimerToggle;
        this.btnTimerReset = btnTimerReset;
        this.btnTimerMenu = btnTimerMenu;
        this.timerHistoryContainer = timerHistoryContainer;
        this.timerHistoryList = timerHistoryList;
        this.timerTotalLabel = timerTotalLabel;

        init();
    }

    private void init() {
        if (detailDescArea != null) {
            detailDescArea.setEditable(false);
            detailDescArea.setWrapText(true);
        }
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

        if (timerHistoryList != null) {
            setupHistoryCellFactory();
        }

        setupTimerLogic();
    }

    // =================================================================================
    //  LOGICA TIMER (CORRETTA, ASINCRONA E SENZA ERRORI DI ARROTONDAMENTO)
    // =================================================================================

    private void setupTimerLogic() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateTimerDisplay(secondsElapsed);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        if (btnTimerToggle != null) btnTimerToggle.setOnAction(e -> toggleTimer());
        if (btnTimerReset != null) btnTimerReset.setOnAction(e -> resetTimer());
        if (btnTimerMenu != null) btnTimerMenu.setOnAction(e -> toggleHistoryMenu());
    }

    public void toggleTimer() {
        if (currentSelectedTask == null) return;

        if (isTimerRunning) {
            // === PAUSA (STOP) ===
            timeline.stop();
            isTimerRunning = false;
            updateUIState(false);

            // STOP DB
            stopDbSessionAndReload();

        } else {
            // === AVVIA (START) ===
            secondsElapsed = 0;
            updateTimerDisplay(0);

            // 1. Catturiamo l'ora esatta tronca ai secondi PRIMA di far partire l'animazione
            startLocalTime = LocalDateTime.now().withNano(0);

            timeline.play();
            isTimerRunning = true;
            updateUIState(true);

            // START DB
            startDbSession();
        }
    }

    private void startDbSession() {
        int taskId = currentSelectedTask.getIdTask();
        // Copia locale per sicurezza nel thread
        LocalDateTime myStart = this.startLocalTime;

        CompletableFuture.runAsync(() -> {
            try {
                TimerSessions session = new TimerSessions();
                session.setIdTask(taskId);
                session.setNome("Sessione");

                // IMPORTANTE: Impostiamo l'ora locale tronca, NON usiamo NOW() del DB
                session.setInizio(myStart);

                DAOTimerSessions.getInstance().insert(session);

                currentDbSessionId = session.getIdSession();

            } catch (DAOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Errore Start Timer: " + e.getMessage());
                    a.show();
                });
            }
        });
    }

    private void stopDbSessionAndReload() {
        int idDaChiudere = currentDbSessionId;
        int taskId = currentSelectedTask.getIdTask();

        // CALCOLO MATEMATICO PRECISO:
        // Fine = Inizio + Secondi Contati dal Timer Grafico
        // Questo elimina il disallineamento dei millisecondi
        LocalDateTime endLocalTime = startLocalTime.plusSeconds(secondsElapsed);

        CompletableFuture.runAsync(() -> {
            try {
                if (idDaChiudere > 0) {
                    // Chiamiamo il metodo aggiornato che accetta l'ora di fine esplicita
                    DAOTimerSessions.getInstance().stopSession(idDaChiudere, endLocalTime);
                }

                // Ricarica storico
                TimerSessions filtro = new TimerSessions();
                filtro.setIdTask(taskId);
                List<TimerSessions> history = DAOTimerSessions.getInstance().select(filtro);
                long totale = DAOTimerSessions.getInstance().getSommaDurataPerTask(taskId);

                Platform.runLater(() -> {
                    if (timerHistoryList != null) timerHistoryList.getItems().setAll(history);
                    updateTotalTimeLabel(totale);
                });

                currentDbSessionId = -1;

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void resetTimer() {
        if (isTimerRunning) return;
        secondsElapsed = 0;
        updateTimerDisplay(0);
        if (timerStatusLabel != null) timerStatusLabel.setText("Pronto");
    }

    // =================================================================================
    //  GESTIONE SUBTASK
    // =================================================================================

    public void createSubTask() {
        String titolo = newSubTaskField.getText().trim();
        if (titolo.isEmpty() || currentSelectedTask == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                SubTasks st = new SubTasks();
                st.setTitolo(titolo);
                st.setIdTask(currentSelectedTask.getIdTask());
                st.setCompletamento(false);
                st.setDescrizione("");

                DAOSubTasks.getInstance().insert(st);

                Platform.runLater(() -> {
                    subTasksList.add(st);
                    newSubTaskField.clear();
                });
            } catch (DAOException e) {
                e.printStackTrace();
            }
        });
    }

    private void refreshSubTasks() {
        if (currentSelectedTask == null) return;
        CompletableFuture.supplyAsync(() -> {
            try {
                SubTasks filtro = new SubTasks();
                filtro.setIdTask(currentSelectedTask.getIdTask());
                return ((DAO<SubTasks>) DAOSubTasks.getInstance()).select(filtro);
            } catch (Exception e) { return null; }
        }).thenAccept(list -> {
            if (list != null) Platform.runLater(() -> subTasksList.setAll(list));
        });
    }

    // =================================================================================
    //  GESTIONE PANNELLO (OPEN/CLOSE)
    // =================================================================================

    public void openPanel(Tasks task, String categoryName) {
        this.currentSelectedTask = task;

        if (detailTitleLabel != null) detailTitleLabel.setText(task.getTitolo());
        if (detailDescArea != null) detailDescArea.setText(task.getDescrizione());
        if (detailCategoryLabel != null) detailCategoryLabel.setText(categoryName);

        if (detailDueDatePicker != null) {
            if (task.getScadenza() != null && !task.getScadenza().isEmpty())
                detailDueDatePicker.setValue(LocalDate.parse(task.getScadenza()));
            else detailDueDatePicker.setValue(null);
        }

        // Reset completo Timer
        resetTimer();
        isTimerRunning = false;
        if(timeline != null) timeline.stop();
        updateUIState(false);
        currentDbSessionId = -1;

        // Reset UI Storico
        if (timerHistoryList != null) timerHistoryList.getItems().clear();
        if (timerTotalLabel != null) timerTotalLabel.setText("--:--:--");

        // Chiudi tendina
        if (timerHistoryContainer != null) {
            timerHistoryContainer.setVisible(false);
            timerHistoryContainer.setManaged(false);
            if(btnTimerMenu!=null) btnTimerMenu.setText("▼");
        }

        refreshSubTasks();
        loadHistory(task.getIdTask());

        if (!isOpen && rightDetailPanel != null) {
            animatePanel(0);
            isOpen = true;
        }
    }

    private void loadHistory(int taskId) {
        CompletableFuture.runAsync(() -> {
            try {
                TimerSessions filtro = new TimerSessions();
                filtro.setIdTask(taskId);
                List<TimerSessions> history = DAOTimerSessions.getInstance().select(filtro);
                long totale = DAOTimerSessions.getInstance().getSommaDurataPerTask(taskId);

                Platform.runLater(() -> {
                    if (timerHistoryList != null) timerHistoryList.getItems().setAll(history);
                    updateTotalTimeLabel(totale);
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public void closePanel() {
        if (isTimerRunning) {
            toggleTimer(); // Ferma e salva se stai chiudendo mentre gira
        }

        if (isOpen && rightDetailPanel != null) {
            double width = rightDetailPanel.getWidth();
            if (width == 0) width = 450;
            animatePanel(width);
            isOpen = false;

            mainListView.getSelectionModel().clearSelection();
            currentSelectedTask = null;
        }
    }

    // =================================================================================
    //  HELPERS UI & FACTORIES
    // =================================================================================

    private void updateUIState(boolean running) {
        if (timerStatusLabel != null) timerStatusLabel.setText(running ? "In corso..." : "In pausa");
        if (btnTimerToggle != null) {
            if (running) {
                btnTimerToggle.setText("|| PAUSA");
                btnTimerToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #F071A7; -fx-border-color: #F071A7; -fx-border-radius: 4; -fx-min-width: 40; -fx-cursor: hand; -fx-font-weight: bold;");
            } else {
                btnTimerToggle.setText("▶ AVVIA");
                btnTimerToggle.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-background-radius: 4; -fx-min-width: 40; -fx-cursor: hand; -fx-font-weight: bold;");
            }
        }
    }

    private void updateTimerDisplay(long seconds) {
        if (timerLabel != null) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }

    private void updateTotalTimeLabel(long totalSeconds) {
        if (timerTotalLabel == null) return;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        timerTotalLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    public void toggleHistoryMenu() {
        if (timerHistoryContainer == null) return;
        boolean isVisible = timerHistoryContainer.isVisible();
        timerHistoryContainer.setVisible(!isVisible);
        timerHistoryContainer.setManaged(!isVisible);
        if (btnTimerMenu != null) btnTimerMenu.setText(!isVisible ? "▲" : "▼");
    }

    private void setupHistoryCellFactory() {
        timerHistoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TimerSessions item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // 1. Creiamo il layout orizzontale
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    // 2. Creiamo l'etichetta con i dati (Data e Durata)
                    String dateStr = (item.getInizio() != null) ? item.getInizio().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "--/--";
                    Label label = new Label(dateStr + "  ➜  " + item.getDurataFormattata());
                    label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

                    // Spingiamo il bottone tutto a destra
                    HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
                    label.setMaxWidth(Double.MAX_VALUE);

                    // 3. Creiamo il bottone Elimina (X)
                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 5 0 5; -fx-border-color: transparent;");

                    // 4. Azione di eliminazione
                    delBtn.setOnAction(e -> deleteSession(item));

                    // 5. Assembliamo la cella
                    box.getChildren().addAll(label, delBtn);
                    setGraphic(box);
                    setText(null); // Importante: puliamo il testo semplice perché usiamo il grafico
                    setStyle("-fx-background-color: transparent; -fx-padding: 2;");
                }
            }
        });
    }

    private void deleteSession(TimerSessions item) {
        if (item == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi davvero eliminare questa sessione?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {

            // ⚡ 1. FEEDBACK ISTANTANEO (OTTIMISTICO)
            // Rimuoviamo l'elemento dalla lista grafica SUBITO.
            // L'utente vedrà la riga sparire all'istante.
            timerHistoryList.getItems().remove(item);

            // 🐢 2. LAVORO SPORCO NEL DATABASE (IN BACKGROUND)
            CompletableFuture.runAsync(() -> {
                try {
                    // Cancelliamo dal DB mentre l'utente continua a lavorare
                    DAOTimerSessions.getInstance().delete(item);

                    // Aggiorniamo solo l'etichetta del totale per correttezza matematica
                    if (currentSelectedTask != null) {
                        long nuovoTotale = DAOTimerSessions.getInstance().getSommaDurataPerTask(currentSelectedTask.getIdTask());
                        Platform.runLater(() -> updateTotalTimeLabel(nuovoTotale));
                    }

                } catch (DAOException e) {
                    e.printStackTrace();
                    // 🚨 ROLLBACK GRAFICO IN CASO DI ERRORE
                    // Se il DB fallisce, dobbiamo rimettere l'elemento nella lista e avvisare
                    Platform.runLater(() -> {
                        timerHistoryList.getItems().add(item);
                        new Alert(Alert.AlertType.ERROR, "Errore di rete: Impossibile eliminare. La sessione è stata ripristinata.").show();
                    });
                }
            });
        }
    }
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
                        CompletableFuture.runAsync(() -> {
                            try { DAOSubTasks.getInstance().update(item); } catch (DAOException ex) { ex.printStackTrace(); }
                        });
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
                        CompletableFuture.runAsync(() -> {
                            try { DAOSubTasks.getInstance().delete(item); } catch (DAOException ex) { ex.printStackTrace(); }
                        });
                        subTasksList.remove(item);
                    });

                    box.getChildren().addAll(cb, label, delBtn);
                    setGraphic(box);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }

    private void animatePanel(double toX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), rightDetailPanel);
        tt.setToX(toX);
        tt.play();
    }

    public boolean isOpen() { return isOpen; }
    public Tasks getCurrentTask() { return currentSelectedTask; }
}
package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Allegati;
import it.unicas.project.template.address.model.SubTasks;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.TimerSessions;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOAllegati;
import it.unicas.project.template.address.model.dao.mysql.DAOSubTasks;
import it.unicas.project.template.address.model.dao.mysql.DAOTimerSessions;
import it.unicas.project.template.address.util.DateUtil;

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
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestore del pannello laterale dei dettagli (InfoPane) per l'applicazione Task Manager.
 * Questa classe gestisce la visualizzazione e l'interazione con i dettagli di un singolo task,
 * inclusi i subtask, gli allegati, lo storico del timer e le sessioni di lavoro.
 * <p>Le operazioni di database sono eseguite in modo asincrono tramite {@link CompletableFuture}
 * per garantire la fluidità dell'interfaccia utente (UI).</p>
 */
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
    private final ListView<Allegati> attachmentListView;

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

    // --- LOGICA INTERNA ---
    private Timeline timeline;
    private int secondsElapsed = 0;
    private boolean isTimerRunning = false;
    private Tasks currentSelectedTask;
    private boolean isOpen = false;
    private ObservableList<SubTasks> subTasksList;

    private volatile int currentDbSessionId = -1;
    private LocalDateTime startLocalTime;

    /**
     * Costruttore della classe TasksInfoPane. Inizializza tutti i riferimenti ai componenti UI
     * e prepara i listener per il timer e le liste.
     *
     * @param rightDetailPanel      VBox del pannello dettagli.
     * @param detailTitleLabel      Label per il titolo del task.
     * @param detailCategoryLabel   Label per la categoria.
     * @param detailDueDatePicker   DatePicker per la scadenza.
     * @param detailDescArea        Area di testo per la descrizione.
     * @param subTaskListView       ListView per i subtask.
     * @param newSubTaskField       Campo input per nuovi subtask.
     * @param mainListView          Lista principale dei task.
     * @param attachmentListView    ListView per gli allegati.
     * @param timerLabel            Label tempo trascorso.
     * @param timerStatusLabel      Label stato timer.
     * @param btnTimerToggle        Bottone Avvia/Pausa.
     * @param btnTimerReset         Bottone Reset.
     * @param btnTimerMenu          Bottone Storico.
     * @param timerHistoryContainer Container storico.
     * @param timerHistoryList      ListView sessioni.
     * @param timerTotalLabel       Label tempo totale.
     */
    public TasksInfoPane(VBox rightDetailPanel, Label detailTitleLabel, Label detailCategoryLabel,
                         DatePicker detailDueDatePicker, TextArea detailDescArea,
                         ListView<SubTasks> subTaskListView, TextField newSubTaskField,
                         ListView<Tasks> mainListView,
                         ListView<Allegati> attachmentListView,
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
        this.attachmentListView = attachmentListView;

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

    /**
     * Inizializza i componenti grafici, configura le CellFactory e imposta la logica del timer.
     */
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

        initAttachmentList();
        setupTimerLogic();
    }

    /**
     * Configura la visualizzazione della lista allegati e la gestione del doppio click per l'apertura file.
     */
    private void initAttachmentList() {
        if (attachmentListView == null) return;

        attachmentListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Allegati item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("📎 " + item.getNomeFile());
                    setStyle("-fx-text-fill: #bd93f9; -fx-cursor: hand; -fx-padding: 5; -fx-background-color: transparent;");
                    setTooltip(new Tooltip(item.getPercorsoFile()));
                }
            }
        });

        attachmentListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Allegati selected = attachmentListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openFile(selected.getPercorsoFile());
                }
            }
        });
    }

    /**
     * Carica asincronamente gli allegati dal database per il task specificato.
     * @param taskId ID del task.
     */
    private void loadAttachments(int taskId) {
        if (attachmentListView == null) return;
        attachmentListView.getItems().clear();

        CompletableFuture.supplyAsync(() -> {
            try {
                return DAOAllegati.getInstance().selectByTaskId(taskId);
            } catch (DAOException e) {
                e.printStackTrace();
                return null;
            }
        }).thenAccept(list -> {
            if (list != null) {
                Platform.runLater(() -> attachmentListView.getItems().setAll(list));
            }
        });
    }

    /**
     * Apre un file utilizzando l'applicazione predefinita del sistema operativo.
     * @param path Percorso del file.
     */
    private void openFile(String path) {
        if (path == null || path.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                File file = new File(path);
                if (file.exists()) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Apertura non supportata.").show());
                    }
                } else {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "File non trovato:\n" + path).show());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Errore apertura: " + ex.getMessage()).show());
            }
        });
    }

    /**
     * Configura la timeline del timer e i relativi pulsanti.
     */
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

    /**
     * Avvia o mette in pausa il timer gestendo la sessione sul database.
     */
    public void toggleTimer() {
        if (currentSelectedTask == null) return;

        if (isTimerRunning) {
            timeline.stop();
            isTimerRunning = false;
            updateUIState(false);
            stopDbSessionAndReload();
        } else {
            secondsElapsed = 0;
            updateTimerDisplay(0);
            startLocalTime = LocalDateTime.now().withNano(0);
            timeline.play();
            isTimerRunning = true;
            updateUIState(true);
            startDbSession();
        }
    }

    /**
     * Registra l'inizio di una sessione di lavoro nel database.
     */
    private void startDbSession() {
        int taskId = currentSelectedTask.getIdTask();
        LocalDateTime myStart = this.startLocalTime;

        CompletableFuture.runAsync(() -> {
            try {
                TimerSessions session = new TimerSessions();
                session.setIdTask(taskId);
                session.setNome("Sessione");
                session.setInizio(myStart);
                DAOTimerSessions.getInstance().insert(session);
                currentDbSessionId = session.getIdSession();
            } catch (DAOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Chiude la sessione corrente e ricarica lo storico.
     */
    private void stopDbSessionAndReload() {
        int idDaChiudere = currentDbSessionId;
        int taskId = currentSelectedTask.getIdTask();
        LocalDateTime endLocalTime = startLocalTime.plusSeconds(secondsElapsed);

        CompletableFuture.runAsync(() -> {
            try {
                if (idDaChiudere > 0) {
                    DAOTimerSessions.getInstance().stopSession(idDaChiudere, endLocalTime);
                }
                loadHistory(taskId);
                currentDbSessionId = -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Resetta il contatore del timer se non è in esecuzione.
     */
    public void resetTimer() {
        if (isTimerRunning) return;
        secondsElapsed = 0;
        updateTimerDisplay(0);
        if (timerStatusLabel != null) timerStatusLabel.setText("Pronto");
    }

    /**
     * Crea un nuovo subtask asincronamente.
     */
    public void createSubTask() {
        String titolo = newSubTaskField.getText().trim();
        if (titolo.isEmpty() || currentSelectedTask == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                SubTasks st = new SubTasks();
                st.setTitolo(titolo);
                st.setIdTask(currentSelectedTask.getIdTask());
                st.setCompletamento(false);
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

    /**
     * Aggiorna la lista dei subtask per il task corrente.
     */
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

    /**
     * Apre il pannello dettagli popolandolo con i dati del task.
     * @param task Task da visualizzare.
     * @param categoryName Nome della categoria.
     */
    public void openPanel(Tasks task, String categoryName) {
        this.currentSelectedTask = task;

        if (detailTitleLabel != null) detailTitleLabel.setText(task.getTitolo());

        if (detailDescArea != null) {
            String desc = task.getDescrizione();
            if (desc == null || desc.trim().isEmpty()) {
                detailDescArea.setText("Nessuna descrizione aggiunta.");
                detailDescArea.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-background-color: transparent;");
            } else {
                detailDescArea.setText(desc);
                detailDescArea.setStyle("-fx-text-fill: white; -fx-font-style: normal; -fx-background-color: transparent;");
            }
        }

        if (detailCategoryLabel != null) {
            if (categoryName == null || categoryName.trim().isEmpty()) {
                detailCategoryLabel.setText("Nessuna categoria");
                detailCategoryLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
            } else {
                detailCategoryLabel.setText(categoryName.toUpperCase());
                detailCategoryLabel.setStyle("-fx-text-fill: #bd93f9; -fx-font-weight: bold;");
            }
        }

        if (detailDueDatePicker != null) {
            detailDueDatePicker.setPromptText("Nessuna scadenza");
            if (task.getScadenza() != null && !task.getScadenza().isEmpty()) {
                LocalDate date;
                try { date = LocalDate.parse(task.getScadenza()); }
                catch (Exception e) { date = DateUtil.parse(task.getScadenza()); }
                detailDueDatePicker.setValue(date);
                detailDueDatePicker.setStyle("-fx-opacity: 1;");
            } else {
                detailDueDatePicker.setValue(null);
                detailDueDatePicker.setStyle("-fx-opacity: 0.7;");
            }
        }

        resetTimer();
        isTimerRunning = false;
        if(timeline != null) timeline.stop();
        updateUIState(false);
        currentDbSessionId = -1;

        //if (timerHistoryList != null) timerHistoryList.getItems().clear();
        //if (timerTotalLabel != null) timerTotalLabel.setText("--:--:--");

        if (timerHistoryContainer != null) {
            timerHistoryContainer.setVisible(false);
            timerHistoryContainer.setManaged(false);
            if(btnTimerMenu!=null) btnTimerMenu.setText("▼");
        }

        refreshSubTasks();
        loadHistory(task.getIdTask());
        loadAttachments(task.getIdTask());

        if (!isOpen && rightDetailPanel != null) {
            animatePanel(0);
            isOpen = true;
        }
    }

    /**
     * Carica lo storico sessioni e il tempo totale speso.
     * @param taskId ID del task.
     */
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



    /**
     * Aggiorna lo stato visivo dei pulsanti del timer.
     * @param running True se attivo.
     */
    private void updateUIState(boolean running) {
        if (timerStatusLabel != null) timerStatusLabel.setText(running ? "In corso..." : "In pausa");
        if (btnTimerToggle != null) {
            if (running) {
                btnTimerToggle.setText("|| PAUSA");
                btnTimerToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #F071A7; -fx-border-color: #F071A7; -fx-border-radius: 4; -fx-font-weight: bold;");
            } else {
                btnTimerToggle.setText("▶ AVVIA");
                btnTimerToggle.setStyle("-fx-background-color: #F071A7; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-weight: bold;");
            }
        }
    }

    /**
     * Aggiorna il display del timer corrente.
     * @param seconds Secondi trascorsi.
     */
    private void updateTimerDisplay(long seconds) {
        if (timerLabel != null) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    }

    /**
     * Aggiorna il tempo totale accumulato.
     * @param totalSeconds Secondi totali.
     */
    private void updateTotalTimeLabel(long totalSeconds) {
        if (timerTotalLabel == null) return;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        timerTotalLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    /**
     * Mostra o nasconde lo storico sessioni.
     */
    public void toggleHistoryMenu() {
        if (timerHistoryContainer == null) return;
        boolean isVisible = timerHistoryContainer.isVisible();
        timerHistoryContainer.setVisible(!isVisible);
        timerHistoryContainer.setManaged(!isVisible);
        if (btnTimerMenu != null) btnTimerMenu.setText(!isVisible ? "▲" : "▼");
    }

    /**
     * Configura la visualizzazione delle sessioni nello storico.
     */
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
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    String dateStr = (item.getInizio() != null) ? item.getInizio().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "--/--";
                    Label label = new Label(dateStr + "  ➜  " + item.getDurataFormattata());
                    label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
                    HBox.setHgrow(label, Priority.ALWAYS);
                    label.setMaxWidth(Double.MAX_VALUE);

                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 5 0 5; -fx-border-color: transparent;");

                    delBtn.setOnAction(e -> deleteSession(item));

                    box.getChildren().addAll(label, delBtn);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 2;");
                }
            }
        });
    }
    /**
     * Elimina una sessione dal database e aggiorna la UI.
     * @param item Sessione da eliminare.
     */
    private void deleteSession(TimerSessions item) {
        if (item == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare sessione?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            timerHistoryList.getItems().remove(item);
            CompletableFuture.runAsync(() -> {
                try {
                    DAOTimerSessions.getInstance().delete(item);
                    if (currentSelectedTask != null) loadHistory(currentSelectedTask.getIdTask());
                } catch (DAOException e) { e.printStackTrace(); }
            });
        }
    }

    /**
     * Configura la visualizzazione dei subtask con checkbox e tasto elimina.
     */

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
                    HBox.setHgrow(label, Priority.ALWAYS);
                    label.setMaxWidth(Double.MAX_VALUE);

                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
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

    /**
     * Gestisce l'animazione di traslazione del pannello.
     * @param toX Coordinata X finale.
     */
    private void animatePanel(double toX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), rightDetailPanel);
        tt.setToX(toX);
        tt.play();
    }

    /**
     * Chiude il pannello, mette in pausa il timer e deseleziona il task.
     */
    public void closePanel() {
        if (isTimerRunning) toggleTimer(); //ferma timer

        if (isOpen && rightDetailPanel != null) {
            double width = rightDetailPanel.getWidth();
            if (width == 0) width = 450;
            animatePanel(width);
            isOpen = false;
            mainListView.getSelectionModel().clearSelection();
            currentSelectedTask = null;
        }
    }

    /** @return True se il pannello è aperto. */
    public boolean isOpen() { return isOpen; }
    /** @return Task correntemente selezionato. */
    public Tasks getCurrentTask() { return currentSelectedTask; }
}
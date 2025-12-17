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
 * Controller ausiliario che gestisce il pannello laterale destro (Info Pane).
 * <p>
 * Questa classe è responsabile di tutta la logica di visualizzazione e interazione all'interno
 * del pannello che appare quando si seleziona un task. Gestisce:
 * <ul>
 * <li>Visualizzazione dei dettagli del task (Titolo, Descrizione, Categoria, Scadenza).</li>
 * <li>Gestione dei sotto-task (creazione, completamento, eliminazione).</li>
 * <li>Gestione del Timer (start/stop/reset) e salvataggio delle sessioni di lavoro nel DB.</li>
 * <li>Visualizzazione dello storico delle sessioni di timer.</li>
 * <li>Visualizzazione e apertura degli allegati associati al task.</li>
 * </ul>
 * <p>
 * Utilizza operazioni asincrone (CompletableFuture) per le chiamate al database al fine di
 * mantenere fluida l'interfaccia utente.
 */
public class TasksInfoPane {

    // --- Componenti UI ---
    private final VBox rightDetailPanel;
    private final Label detailTitleLabel;
    private final Label detailCategoryLabel;
    private final DatePicker detailDueDatePicker;
    private final TextArea detailDescArea;
    private final ListView<SubTasks> subTaskListView;
    private final TextField newSubTaskField;
    private final ListView<Tasks> mainListView;
    private final ListView<Allegati> attachmentListView;

    // --- Componenti Timer ---
    private final Label timerLabel;
    private final Label timerStatusLabel;
    private final Button btnTimerToggle;
    private final Button btnTimerReset;

    // --- Componenti Storico Timer ---
    private final Button btnTimerMenu;
    private final VBox timerHistoryContainer;
    private final ListView<TimerSessions> timerHistoryList;
    private final Label timerTotalLabel;

    // --- Stato Interno ---
    private Timeline timeline;
    private int secondsElapsed = 0;
    private boolean isTimerRunning = false;
    private Tasks currentSelectedTask;
    private boolean isOpen = false;
    private ObservableList<SubTasks> subTasksList;

    /** ID della sessione timer attualmente attiva nel database. -1 se nessuna sessione è attiva. */
    private volatile int currentDbSessionId = -1;
    /** Data e ora di inizio dell'ultima sessione timer locale. */
    private LocalDateTime startLocalTime;

    /**
     * Costruttore principale. Inizializza i riferimenti ai componenti UI e avvia la logica interna.
     *
     * @param rightDetailPanel Il contenitore principale del pannello laterale.
     * @param detailTitleLabel Label per il titolo del task.
     * @param detailCategoryLabel Label per la categoria del task.
     * @param detailDueDatePicker DatePicker per la scadenza (sola lettura).
     * @param detailDescArea Area di testo per la descrizione.
     * @param subTaskListView Lista per i sotto-task.
     * @param newSubTaskField Campo di input per nuovi sotto-task.
     * @param mainListView Riferimento alla lista task principale (per deselezione).
     * @param attachmentListView Lista per visualizzare gli allegati.
     * @param timerLabel Label digitale del timer (HH:MM:SS).
     * @param timerStatusLabel Label di stato del timer (In corso/In pausa).
     * @param btnTimerToggle Bottone Start/Pausa timer.
     * @param btnTimerReset Bottone Reset timer.
     * @param btnTimerMenu Bottone per mostrare/nascondere lo storico timer.
     * @param timerHistoryContainer Contenitore per lo storico timer.
     * @param timerHistoryList Lista visuale delle sessioni passate.
     * @param timerTotalLabel Label per il tempo totale speso sul task.
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
     * Inizializzazione interna dei componenti e dei listener.
     * Configura le proprietà di sola lettura per i campi di dettaglio, le liste osservabili
     * e avvia la logica del timer.
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
     * Configura la ListView degli allegati.
     * Imposta la CellFactory per mostrare icona e nome file, e gestisce il doppio click per l'apertura.
     */
    private void initAttachmentList() {
        if (attachmentListView == null) return;

        // Configurazione Cella (Visualizzazione)
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

        // Gestione Doppio Click (Apertura File)
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
     * Carica asincronamente gli allegati per il task specificato dal DB.
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
     * Apre il file specificato utilizzando l'applicazione di sistema predefinita.
     * Esegue controlli di esistenza del file e supporto della piattaforma.
     * @param path Percorso assoluto del file.
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
                        Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Apertura non supportata dal sistema.").show());
                    }
                } else {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "File non trovato:\n" + path).show());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Errore apertura file: " + ex.getMessage()).show());
            }
        });
    }

    /**
     * Configura la Timeline per l'aggiornamento secondo per secondo del timer e i listener dei pulsanti.
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
     * Avvia o mette in pausa il timer.
     * Gestisce sia l'aggiornamento UI che l'apertura/chiusura delle sessioni nel DB.
     */
    public void toggleTimer() {
        if (currentSelectedTask == null) return;

        if (isTimerRunning) {
            // STOP TIMER
            timeline.stop();
            isTimerRunning = false;
            updateUIState(false);
            stopDbSessionAndReload();
        } else {
            // START TIMER
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
     * Crea una nuova sessione timer nel database in modo asincrono.
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
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Errore Start Timer: " + e.getMessage()).show());
            }
        });
    }

    /**
     * Chiude la sessione timer corrente nel database e ricarica lo storico.
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
     * Resetta il contatore visivo del timer a zero (solo se non in esecuzione).
     */
    public void resetTimer() {
        if (isTimerRunning) return;
        secondsElapsed = 0;
        updateTimerDisplay(0);
        if (timerStatusLabel != null) timerStatusLabel.setText("Pronto");
    }


    /**
     * Crea un nuovo sotto-task nel database e lo aggiunge alla lista visuale.
     * Utilizza il testo inserito nel campo {@code newSubTaskField}.
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

    /**
     * Ricarica la lista dei sotto-task per il task corrente dal DB.
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
     * Apre il pannello laterale popolandolo con i dettagli del task passato.
     * <p>
     * Questo metodo si occupa di:
     * 1. Aggiornare i testi (titolo, descrizione, categoria).
     * 2. Resettare timer e stato interno.
     * 3. Avviare il caricamento asincrono di subtask, storico e allegati.
     * 4. Avviare l'animazione di apertura.
     *
     * @param task Il task da visualizzare.
     * @param categoryName Il nome della categoria del task (per visualizzazione).
     */
    public void openPanel(Tasks task, String categoryName) {
        this.currentSelectedTask = task;

        // Aggiornamento UI Dettagli
        if (detailTitleLabel != null) detailTitleLabel.setText(task.getTitolo());

        if (detailDescArea != null) {
            String desc = task.getDescrizione();
            if (desc == null || desc.trim().isEmpty()) {
                detailDescArea.setText("Nessuna descrizione aggiunta.");
                detailDescArea.setStyle("-fx-text-fill: #777777; -fx-font-style: italic; -fx-control-inner-background: transparent; -fx-background-color: transparent;");
            } else {
                detailDescArea.setText(desc);
                detailDescArea.setStyle("-fx-text-fill: white; -fx-font-style: normal; -fx-control-inner-background: transparent; -fx-background-color: transparent;");
            }
        }

        if (detailCategoryLabel != null) {
            if (categoryName == null || categoryName.trim().isEmpty()) {
                detailCategoryLabel.setText("Nessuna categoria");
                detailCategoryLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic; -fx-font-size: 11px;");
            } else {
                detailCategoryLabel.setText(categoryName.toUpperCase());
                detailCategoryLabel.setStyle("-fx-text-fill: #bd93f9; -fx-font-weight: bold; -fx-font-style: normal; -fx-font-size: 11px;");
            }
        }

        if (detailDueDatePicker != null) {
            detailDueDatePicker.setPromptText("Nessuna scadenza");
            if (task.getScadenza() != null && !task.getScadenza().isEmpty()) {
                LocalDate date = null;
                try {
                    date = LocalDate.parse(task.getScadenza());
                } catch (Exception e) {
                    date = DateUtil.parse(task.getScadenza());
                }
                detailDueDatePicker.setValue(date);
                detailDueDatePicker.setStyle("-fx-opacity: 1;");
            } else {
                detailDueDatePicker.setValue(null);
                detailDueDatePicker.setStyle("-fx-opacity: 0.7;");
            }
        }

        // Reset Stato
        resetTimer();
        isTimerRunning = false;
        if(timeline != null) timeline.stop();
        updateUIState(false);
        currentDbSessionId = -1;

        if (timerHistoryList != null) timerHistoryList.getItems().clear();
        if (timerTotalLabel != null) timerTotalLabel.setText("--:--:--");

        if (timerHistoryContainer != null) {
            timerHistoryContainer.setVisible(false);
            timerHistoryContainer.setManaged(false);
            if(btnTimerMenu!=null) btnTimerMenu.setText("▼");
        }

        // Caricamento Dati Asincrono
        refreshSubTasks();
        loadHistory(task.getIdTask());
        loadAttachments(task.getIdTask());

        // Animazione Apertura
        if (!isOpen && rightDetailPanel != null) {
            animatePanel(0);
            isOpen = true;
        }
    }

    /**
     * Carica lo storico delle sessioni timer dal database.
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
     * Chiude il pannello laterale con un'animazione.
     * Se il timer è in esecuzione, lo mette in pausa automaticamente.
     */
    public void closePanel() {
        if (isTimerRunning) {
            toggleTimer(); // Pausa automatica
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

    /**
     * Aggiorna lo stato visivo dei pulsanti e delle label del timer.
     * @param running true se il timer è attivo, false altrimenti.
     */
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

    /**
     * Aggiorna il display digitale del timer (HH:MM:SS).
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
     * Aggiorna la label del tempo totale trascorso sul task.
     * @param totalSeconds Totale secondi da visualizzare.
     */
    private void updateTotalTimeLabel(long totalSeconds) {
        if (timerTotalLabel == null) return;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        timerTotalLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    /**
     * Mostra o nasconde la lista dello storico delle sessioni.
     */
    public void toggleHistoryMenu() {
        if (timerHistoryContainer == null) return;
        boolean isVisible = timerHistoryContainer.isVisible();
        timerHistoryContainer.setVisible(!isVisible);
        timerHistoryContainer.setManaged(!isVisible);
        if (btnTimerMenu != null) btnTimerMenu.setText(!isVisible ? "▲" : "▼");
    }

    /**
     * Configura la CellFactory per la lista dello storico.
     * Permette di visualizzare data/durata e il pulsante di eliminazione per ogni sessione.
     */
    private void setupHistoryCellFactory() {
        timerHistoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TimerSessions item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    HBox box = new HBox(10); box.setAlignment(Pos.CENTER_LEFT);
                    String dateStr = (item.getInizio() != null) ? item.getInizio().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "--/--";
                    Label label = new Label(dateStr + "  ➜  " + item.getDurataFormattata());
                    label.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
                    HBox.setHgrow(label, Priority.ALWAYS); label.setMaxWidth(Double.MAX_VALUE);

                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 5 0 5; -fx-border-color: transparent;");
                    delBtn.setOnAction(e -> deleteSession(item));

                    box.getChildren().addAll(label, delBtn);
                    setGraphic(box); setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 2;");
                }
            }
        });
    }

    /**
     * Elimina una sessione timer dallo storico e dal DB dopo conferma utente.
     * @param item La sessione da eliminare.
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
     * Configura la CellFactory per la lista dei sotto-task.
     * Aggiunge CheckBox per il completamento e pulsante di cancellazione.
     */
    private void setupSubTaskCellFactory() {
        subTaskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(SubTasks item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    HBox box = new HBox(10); box.setAlignment(Pos.CENTER_LEFT);

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
                    HBox.setHgrow(label, Priority.ALWAYS); label.setMaxWidth(Double.MAX_VALUE);

                    Button delBtn = new Button("×");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> {
                        CompletableFuture.runAsync(() -> {
                            try { DAOSubTasks.getInstance().delete(item); } catch (DAOException ex) { ex.printStackTrace(); }
                        });
                        subTasksList.remove(item);
                    });

                    box.getChildren().addAll(cb, label, delBtn);
                    setGraphic(box); setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }

    /**
     * Esegue l'animazione di scorrimento laterale del pannello.
     * @param toX Coordinata X finale (0 per aperto, larghezza pannello per chiuso).
     */
    private void animatePanel(double toX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), rightDetailPanel);
        tt.setToX(toX);
        tt.play();
    }

    public boolean isOpen() { return isOpen; }
    public Tasks getCurrentTask() { return currentSelectedTask; }
}
package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.MainApp;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/**
 * Controller per la gestione della finestra del profilo utente.
 * <p>
 * Questa classe gestisce la visualizzazione dei dettagli dell'utente attualmente loggato
 * (nome, cognome, email). Fornisce inoltre le funzionalità per:
 * <ul>
 * <li>Modificare i dati dell'utente tramite una finestra di dialogo dedicata.</li>
 * <li>Eliminare definitivamente l'account utente dal database.</li>
 * </ul>
 */
public class UtentiProfileController {

    // --- Componenti FXML ---

    /** Label per la visualizzazione del nome utente. */
    @FXML
    private Label nomeLabel;

    /** Label per la visualizzazione del cognome utente. */
    @FXML
    private Label cognomeLabel;

    /** Label per la visualizzazione dell'email utente. */
    @FXML
    private Label emailLabel;

    // --- Variabili di stato ---

    private Stage dialogStage;
    private Utenti user;
    private MainApp mainApp;
    private boolean okClicked = false;

    /**
     * Collega questo controller all'applicazione principale.
     * Necessario per invocare metodi di navigazione o dialoghi (es. edit dialog).
     *
     * @param mainApp L'istanza dell'applicazione principale.
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Metodo helper interno per aggiornare le etichette dell'interfaccia con i dati dell'utente.
     * Se l'utente è null, le etichette vengono svuotate.
     *
     * @param user L'oggetto {@link Utenti} da visualizzare.
     */
    private void showUserDetails(Utenti user) {
        if (user != null) {
            nomeLabel.setText(user.getNome());
            cognomeLabel.setText(user.getCognome());
            emailLabel.setText(user.getEmail());
        } else {
            // Utente null, rimuovi tutto il testo.
            nomeLabel.setText("");
            cognomeLabel.setText("");
            emailLabel.setText("");
        }
    }

    /**
     * Gestisce il click sul pulsante "Modifica".
     * <p>
     * Apre la finestra di dialogo per la modifica dei dati utente.
     * Se le modifiche vengono confermate, aggiorna la visualizzazione del profilo.
     */
    @FXML
    private void handleEditUtenti() {
        if (user == null) return;

        boolean okClicked = mainApp.showUtentiEditDialog(user);
        if (okClicked) {
            showUserDetails(user); // aggiorna la UI con i nuovi dati
        }
    }

    /**
     * Gestisce il click sul pulsante "Elimina".
     * <p>
     * Mostra un avviso di conferma. Se l'utente conferma:
     * <ol>
     * <li>Elimina l'utente dal database tramite {@link DAOUtenti}.</li>
     * <li>Rimuove l'utente dalla sessione corrente.</li>
     * <li>Chiude la finestra e termina l'applicazione (o reindirizza al login).</li>
     * </ol>
     */
    @FXML
    private void handleDelete() {
        if (user == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(dialogStage);
        confirmation.setTitle("Conferma eliminazione");
        confirmation.setHeaderText("Sei sicuro di voler eliminare questo profilo?");
        confirmation.setContentText("Questa operazione non può essere annullata.");

        if (confirmation.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                DAOUtenti.getInstance().delete(user);
                mainApp.setCurrentUser(null); // Rimuovi l'utente dalla sessione

                // Chiudi il dialog
                dialogStage.close();

                // Termina l'applicazione (comportamento attuale)
                System.exit(0);
                // Alternativa: mainApp.showUtentiLogin();

            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(dialogStage);
                alert.setTitle("Errore eliminazione");
                alert.setHeaderText("Errore durante l'eliminazione dell'utente");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Imposta l'utente da visualizzare nel profilo.
     * Aggiorna immediatamente le label con i dati dell'utente passato.
     *
     * @param user L'oggetto {@link Utenti} corrente.
     */
    public void setUser(Utenti user) {
        this.user = user;
        // Aggiornamento diretto delle label
        nomeLabel.setText(user.getNome());
        cognomeLabel.setText(user.getCognome());
        emailLabel.setText(user.getEmail());
    }

    /**
     * Imposta lo stage per questa finestra di dialogo.
     *
     * @param dialogStage Lo stage della finestra.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Restituisce lo stato del click su un eventuale pulsante di conferma (se previsto).
     *
     * @return true se è stato cliccato OK, false altrimenti.
     */
    public boolean isOkClicked() {
        return okClicked;
    }
}
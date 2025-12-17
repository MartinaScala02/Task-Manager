package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
/**
 * Controller del layout principale.
 * Il layout principale fornisce la struttura base dell'applicazione,
 * contenendo una barra dei menu e uno spazio in cui possono essere
 * inseriti altri elementi JavaFX.
 */
public class RootLayoutController {

    // Riferimento all'applicazione principale
    private MainApp mainApp;

    /**
     * Viene chiamato dall'applicazione principale per fornire
     * un riferimento a se stessa.
     *
     * @param mainApp riferimento all'applicazione principale
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
}

package it.unicas.project.template.address.model;

/**
 * Classe Model che rappresenta un file allegato a un Task.
 * <p>
 * Memorizza le informazioni essenziali per localizzare e gestire un file esterno
 * associato a un'attività, come il nome, il percorso su disco e il tipo di file.
 * </p>
 */
public class Allegati {
    private String nomeFile;
    private String percorsoFile;
    private String tipoFile;
    private int idAllegato;
    private int idTask;

    /**
     * Costruttore di default.
     * Inizializza un oggetto Allegati vuoto.
     */
    public Allegati() {}

    /**
     * Costruttore completo per la creazione di un nuovo allegato.
     *
     * @param nomeFile     Il nome del file (es. "documento.pdf").
     * @param percorsoFile Il percorso assoluto del file sul disco.
     * @param tipoFile     Il tipo di file o estensione (es. "PDF", "JPG").
     * @param idTask       L'ID del task a cui l'allegato è associato.
     */
    public Allegati(String nomeFile, String percorsoFile, String tipoFile, int idTask) {
        this.nomeFile = nomeFile;
        this.percorsoFile = percorsoFile;
        this.tipoFile = tipoFile;
        this.idTask = idTask;
    }

    /**
     * Restituisce l'ID univoco dell'allegato.
     * @return L'ID allegato.
     */
    public int getIdAllegato() { return idAllegato; }

    /**
     * Imposta l'ID univoco dell'allegato.
     * @param idAllegato Il nuovo ID.
     */
    public void setIdAllegato(int idAllegato) { this.idAllegato = idAllegato; }

    /**
     * Restituisce l'ID del task associato.
     * @return L'ID del task.
     */
    public int getIdTask() { return idTask; }

    /**
     * Imposta l'ID del task associato.
     * @param idTask Il nuovo ID task.
     */
    public void setIdTask(int idTask) { this.idTask = idTask; }

    /**
     * Restituisce il nome del file.
     * @return Il nome del file.
     */
    public String getNomeFile() { return nomeFile; }

    /**
     * Imposta il nome del file.
     * @param nomeFile Il nuovo nome.
     */
    public void setNomeFile(String nomeFile) { this.nomeFile = nomeFile; }

    /**
     * Restituisce il percorso completo del file.
     * @return Il percorso del file.
     */
    public String getPercorsoFile() { return percorsoFile; }

    /**
     * Imposta il percorso completo del file.
     * @param percorsoFile Il nuovo percorso.
     */
    public void setPercorsoFile(String percorsoFile) { this.percorsoFile = percorsoFile; }

    /**
     * Restituisce il tipo di file.
     * @return Il tipo o estensione del file.
     */
    public String getTipoFile() { return tipoFile; }

    /**
     * Imposta il tipo di file.
     * @param tipoFile Il nuovo tipo.
     */
    public void setTipoFile(String tipoFile) { this.tipoFile = tipoFile; }
}
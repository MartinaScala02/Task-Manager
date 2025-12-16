package it.unicas.project.template.address.model;

/**
 * Classe Model che rappresenta una voce di registro (Audit Log).
 * <p>
 * Questa classe serve a tracciare le operazioni critiche (es. INSERT, UPDATE, DELETE)
 * eseguite sui Task, mantenendo un registro storico delle modifiche.
 * </p>
 */
public class AuditLog {
    private String operazione;
    private int idTask;
    private int idUtente;
    private String dettagli;

    /**
     * Costruttore completo.
     *
     * @param operazione Tipo di operazione eseguita (es. "INSERT", "UPDATE").
     * @param idTask     ID del task coinvolto.
     * @param idUtente   ID dell'utente che ha effettuato l'operazione.
     * @param dettagli   Descrizione testuale dell'evento.
     */
    public AuditLog(String operazione, int idTask, int idUtente, String dettagli) {
        this.operazione = operazione;
        this.idTask = idTask;
        this.idUtente = idUtente;
        this.dettagli = dettagli;
    }

    /**
     * Restituisce il tipo di operazione.
     * @return Stringa che descrive l'operazione (es. "DELETE").
     */
    public String getOperazione() { return operazione; }

    /**
     * Restituisce l'ID del task coinvolto.
     * @return L'ID del task.
     */
    public int getIdTask() { return idTask; }

    /**
     * Restituisce l'ID dell'utente autore dell'operazione.
     * @return L'ID dell'utente.
     */
    public int getIdUtente() { return idUtente; }

    /**
     * Restituisce i dettagli dell'operazione.
     * @return Descrizione o note sull'evento.
     */
    public String getDettagli() { return dettagli; }
}
package it.unicas.project.template.address.model;

public class AuditLog {
    private String operazione;
    private int idTask;
    private int idUtente;
    private String dettagli;

    public AuditLog(String operazione, int idTask, int idUtente, String dettagli) {
        this.operazione = operazione;
        this.idTask = idTask;
        this.idUtente = idUtente;
        this.dettagli = dettagli;
    }

    // Getter e Setter (generali con l'IDE)
    public String getOperazione() { return operazione; }
    public int getIdTask() { return idTask; }
    public int getIdUtente() { return idUtente; }
    public String getDettagli() { return dettagli; }
}

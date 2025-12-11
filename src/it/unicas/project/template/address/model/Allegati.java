package it.unicas.project.template.address.model;

public class Allegati {
    private String nomeFile;
    private String percorsoFile;
    private String tipoFile;
    private int idAllegato;
    private int idTask;

    // Costruttore vuoto
    public Allegati() {}

    // Costruttore completo
    public Allegati(String nomeFile, String percorsoFile, String tipoFile, int idTask) {
        this.nomeFile = nomeFile;
        this.percorsoFile = percorsoFile;
        this.tipoFile = tipoFile;
        this.idTask = idTask;
    }

    // Getter e Setter
    public int getIdAllegato() { return idAllegato; }
    public void setIdAllegato(int idAllegato) { this.idAllegato = idAllegato; }

    public int getIdTask() { return idTask; }
    public void setIdTask(int idTask) { this.idTask = idTask; }

    public String getNomeFile() { return nomeFile; }
    public void setNomeFile(String nomeFile) { this.nomeFile = nomeFile; }

    public String getPercorsoFile() { return percorsoFile; }
    public void setPercorsoFile(String percorsoFile) { this.percorsoFile = percorsoFile; }

    public String getTipoFile() { return tipoFile; }
    public void setTipoFile(String tipoFile) { this.tipoFile = tipoFile; }
}
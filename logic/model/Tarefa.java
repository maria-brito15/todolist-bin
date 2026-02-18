package logic.model;

public class Tarefa {

    public int id;
    public int idUsuario;
    public String descricao;
    public boolean concluida;
    public boolean ativo;
    public int dia;
    public int mes;
    public int ano;

    public Tarefa() {
        this.ativo = true;
        this.concluida = false;
    }

    public Tarefa(int id, int idUsuario, String descricao, int dia, int mes, int ano) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.descricao = descricao;
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
        this.concluida = false;
        this.ativo = true;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | %02d/%02d/%d | %s",
            id, descricao, dia, mes, ano,
            concluida ? "Concluída" : "Pendente"
        );
    }
}
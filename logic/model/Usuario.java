package logic.model;

public class Usuario {

    public int id;
    public String nome;
    public String email;
    public String senha;
    public boolean ativo;

    public Usuario() {
        this.ativo = true;
    }

    public Usuario(int id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = true;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s", id, nome, email);
    }
}
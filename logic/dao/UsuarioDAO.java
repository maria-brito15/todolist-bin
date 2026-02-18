package logic.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import logic.model.Usuario;
import logic.util.BinInput;
import logic.util.BinOutput;

public class UsuarioDAO {

    public static final String ARQUIVO = "files/usuarios.bin";

    public static final int TAM_NOME = 60;
    public static final int TAM_EMAIL = 40;
    public static final int TAM_SENHA = 20;
    // Calculation: 4(id) + (60*2) + (40*2) + (20*2) + 1(active)
    public static final int TAM_REGISTRO = 4 + (TAM_NOME * 2) + (TAM_EMAIL * 2) + (TAM_SENHA * 2) + 1;
    public static final int TAM_CABECALHO = 12;

    private static final char CHAVE_XOR = 'K'; // Simple key for encryption

    private static int[] lerCabecalho() throws IOException {
        File f = new File(ARQUIVO);
        if (!f.exists() || f.length() < TAM_CABECALHO) return new int[]{ 0, 0, 0 };

        BinInput in = new BinInput(ARQUIVO);
        int[] cab = { in.readInt(), in.readInt(), in.readInt() };
        in.close();
        return cab;
    }

    private static void salvarCabecalho(int total, int ultimoId, int ativos) throws IOException {
        BinOutput out = new BinOutput(ARQUIVO, false);
        out.writeInt(total);
        out.writeInt(ultimoId);
        out.writeInt(ativos);
        out.close();
    }

    /**
     * Primitive encryption/decryption using bitwise XOR.
     */
    public static String xor(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            sb.append((char) (c ^ CHAVE_XOR));
        }
        return sb.toString();
    }

    public static int inserir(String nome, String email, String senha) throws IOException {
        if (buscarPorEmail(email) != null) return -1; // Prevent duplicate emails

        int[] cab = lerCabecalho();
        int novoId = cab[1] + 1;

        BinOutput out = new BinOutput(ARQUIVO, false);
        if (new File(ARQUIVO).length() == 0) {
            out.writeInt(0); out.writeInt(0); out.writeInt(0);
        }
        out.seek(TAM_CABECALHO + (long) cab[0] * TAM_REGISTRO);
        out.writeInt(novoId);
        out.writeString(nome, TAM_NOME);
        out.writeString(email, TAM_EMAIL);
        out.writeString(xor(senha), TAM_SENHA); // Encrypt password
        out.writeBoolean(true);
        out.close();

        salvarCabecalho(cab[0] + 1, novoId, cab[2] + 1);
        return novoId;
    }

    public static Usuario buscarPorId(int idBuscado) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            in.seek(TAM_CABECALHO + (long) i * TAM_REGISTRO);
            Usuario u = lerRegistro(in);
            if (u.id == idBuscado && u.ativo) {
                in.close();
                return u;
            }
        }

        in.close();
        return null;
    }

    public static Usuario buscarPorEmail(String emailBuscado) throws IOException {
        File f = new File(ARQUIVO);
        if (!f.exists()) return null;

        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            in.seek(TAM_CABECALHO + (long) i * TAM_REGISTRO);
            Usuario u = lerRegistro(in);
            if (u.ativo && u.email.equalsIgnoreCase(emailBuscado)) {
                in.close();
                return u;
            }
        }

        in.close();
        return null;
    }

    public static List<Usuario> listarTodos() throws IOException {
        List<Usuario> lista = new ArrayList<>();
        File f = new File(ARQUIVO);
        if (!f.exists()) return lista;

        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            in.seek(TAM_CABECALHO + (long) i * TAM_REGISTRO);
            Usuario u = lerRegistro(in);
            if (u.ativo) lista.add(u);
        }

        in.close();
        return lista;
    }

    public static Usuario login(String email, String senha) throws IOException {
        Usuario u = buscarPorEmail(email);
        // Compare against encrypted password
        if (u != null && u.senha.equals(xor(senha))) return u;
        return null;
    }

    public static boolean atualizarNome(int id, String novoNome) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            long pos = TAM_CABECALHO + (long) i * TAM_REGISTRO;
            in.seek(pos);
            Usuario u = lerRegistro(in);

            if (u.id == id && u.ativo) {
                in.close();
                BinOutput out = new BinOutput(ARQUIVO, false);
                out.seek(pos + 4); // Seek past ID to Name field
                out.writeString(novoNome, TAM_NOME);
                out.close();
                return true;
            }
        }

        in.close();
        return false;
    }

    public static boolean deletar(int idBuscado) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            long pos = TAM_CABECALHO + (long) i * TAM_REGISTRO;
            in.seek(pos);
            Usuario u = lerRegistro(in);

            if (u.id == idBuscado && u.ativo) {
                in.close();
                BinOutput out = new BinOutput(ARQUIVO, false);
                // Seek to the very last byte (Active Boolean)
                out.seek(pos + TAM_REGISTRO - 1);
                out.writeBoolean(false);
                out.close();
                salvarCabecalho(cab[0], cab[1], cab[2] - 1);
                return true;
            }
        }

        in.close();
        return false;
    }

    private static Usuario lerRegistro(BinInput in) throws IOException {
        Usuario u = new Usuario();
        u.id = in.readInt();
        u.nome = in.readString(TAM_NOME);
        u.email = in.readString(TAM_EMAIL);
        u.senha = in.readString(TAM_SENHA);
        u.ativo = in.readBoolean();
        return u;
    }
}
package logic.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import logic.model.Tarefa;
import logic.util.BinInput;
import logic.util.BinOutput;

public class TarefaDAO {

    public static final String ARQUIVO = "files/tarefas.bin";

    // Fixed sizes for binary record calculation
    public static final int TAM_DESC = 100;
    // Calculation: 4(id)+4(uId)+(100*2)(desc)+1(done)+1(active)+4+4+4(date)
    public static final int TAM_REGISTRO = 4 + 4 + (TAM_DESC * 2) + 1 + 1 + 4 + 4 + 4;
    public static final int TAM_CABECALHO = 12; // 3 Integers (Total, LastID, ActiveCount)

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

    public static int inserir(int idUsuario, String descricao, int dia, int mes, int ano) throws IOException {
        int[] cab = lerCabecalho();
        int novoId = cab[1] + 1;

        BinOutput out = new BinOutput(ARQUIVO, false);
        if (new File(ARQUIVO).length() == 0) {
            out.writeInt(0); out.writeInt(0); out.writeInt(0);
        }
        // Jump to the end of the last record
        out.seek(TAM_CABECALHO + (long) cab[0] * TAM_REGISTRO);
        out.writeInt(novoId);
        out.writeInt(idUsuario);
        out.writeString(descricao, TAM_DESC);
        out.writeBoolean(false);
        out.writeBoolean(true); // Active flag
        out.writeDate(dia, mes, ano);
        out.close();

        salvarCabecalho(cab[0] + 1, novoId, cab[2] + 1);
        return novoId;
    }

    public static Tarefa buscarPorId(int idBuscado) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        // Linear search through the binary file
        for (int i = 0; i < cab[0]; i++) {
            in.seek(TAM_CABECALHO + (long) i * TAM_REGISTRO);
            Tarefa t = lerRegistro(in);
            if (t.id == idBuscado && t.ativo) {
                in.close();
                return t;
            }
        }

        in.close();
        return null;
    }

    public static List<Tarefa> listarPorUsuario(int idUsuario) throws IOException {
        List<Tarefa> lista = new ArrayList<>();
        File f = new File(ARQUIVO);
        if (!f.exists()) return lista;

        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            in.seek(TAM_CABECALHO + (long) i * TAM_REGISTRO);
            Tarefa t = lerRegistro(in);
            if (t.ativo && t.idUsuario == idUsuario) {
                lista.add(t);
            }
        }

        in.close();
        return lista;
    }

    public static List<Tarefa> listarPendentes(int idUsuario) throws IOException {
        List<Tarefa> resultado = new ArrayList<>();
        for (Tarefa t : listarPorUsuario(idUsuario)) {
            if (!t.concluida) resultado.add(t);
        }
        return resultado;
    }

    public static List<Tarefa> listarConcluidas(int idUsuario) throws IOException {
        List<Tarefa> resultado = new ArrayList<>();
        for (Tarefa t : listarPorUsuario(idUsuario)) {
            if (t.concluida) resultado.add(t);
        }
        return resultado;
    }

    public static boolean marcarConcluida(int idTarefa) throws IOException {
        return setConcluida(idTarefa, true);
    }

    public static boolean desmarcarConcluida(int idTarefa) throws IOException {
        return setConcluida(idTarefa, false);
    }

    /**
     * Updates only the 'concluida' byte without rewriting the whole record.
     */
    private static boolean setConcluida(int idTarefa, boolean valor) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            long pos = TAM_CABECALHO + (long) i * TAM_REGISTRO;
            in.seek(pos);
            Tarefa t = lerRegistro(in);

            if (t.id == idTarefa && t.ativo) {
                in.close();
                // Offset calculation: skip id(4), uId(4), and desc(100*2)
                long offsetConcluida = pos + 4 + 4 + (TAM_DESC * 2);
                BinOutput out = new BinOutput(ARQUIVO, false);
                out.seek(offsetConcluida);
                out.writeBoolean(valor);
                out.close();
                return true;
            }
        }

        in.close();
        return false;
    }

    public static boolean atualizarDescricao(int idTarefa, String novaDesc) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            long pos = TAM_CABECALHO + (long) i * TAM_REGISTRO;
            in.seek(pos);
            Tarefa t = lerRegistro(in);

            if (t.id == idTarefa && t.ativo) {
                in.close();
                BinOutput out = new BinOutput(ARQUIVO, false);
                out.seek(pos + 4 + 4); // Seek to the description field
                out.writeString(novaDesc, TAM_DESC);
                out.close();
                return true;
            }
        }

        in.close();
        return false;
    }

    public static boolean deletar(int idTarefa) throws IOException {
        int[] cab = lerCabecalho();
        BinInput in = new BinInput(ARQUIVO);

        for (int i = 0; i < cab[0]; i++) {
            long pos = TAM_CABECALHO + (long) i * TAM_REGISTRO;
            in.seek(pos);
            Tarefa t = lerRegistro(in);

            if (t.id == idTarefa && t.ativo) {
                in.close();
                // Logical deletion: set 'ativo' byte to false
                long offsetAtivo = pos + 4 + 4 + (TAM_DESC * 2) + 1;
                BinOutput out = new BinOutput(ARQUIVO, false);
                out.seek(offsetAtivo);
                out.writeBoolean(false);
                out.close();
                // Update header active count
                salvarCabecalho(cab[0], cab[1], cab[2] - 1);
                return true;
            }
        }

        in.close();
        return false;
    }

    private static Tarefa lerRegistro(BinInput in) throws IOException {
        Tarefa t = new Tarefa();
        t.id = in.readInt();
        t.idUsuario = in.readInt();
        t.descricao = in.readString(TAM_DESC);
        t.concluida = in.readBoolean();
        t.ativo = in.readBoolean();
        int[] data = in.readDate();
        t.dia = data[0];
        t.mes = data[1];
        t.ano = data[2];
        return t;
    }
}
package logic.util;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Utility class for writing primitive types and structured data to a binary file.
 * Supports both overwriting and appending.
 */
public class BinOutput {

    private final RandomAccessFile raf;

    /**
     * Initializes the writer in read-write ("rw") mode.
     * @param arquivo Path to the binary file.
     * @param append If true, moves the pointer to the end of the file immediately.
     */
    public BinOutput(String arquivo, boolean append) throws IOException {
        this.raf = new RandomAccessFile(arquivo, "rw");
        if (append) {
            raf.seek(raf.length());
        }
    }

    public void writeInt(int valor) throws IOException {
        raf.writeInt(valor);
    }

    public void writeFloat(float valor) throws IOException {
        raf.writeFloat(valor);
    }

    public void writeDouble(double valor) throws IOException {
        raf.writeDouble(valor);
    }

    public void writeBoolean(boolean valor) throws IOException {
        raf.writeBoolean(valor);
    }

    /**
     * Writes a string to the file with a fixed character length.
     * Pads with spaces if the string is too short; truncates if it is too long.
     * @param s The string to write.
     * @param tam The fixed size (in characters) to occupy in the file.
     */
    public void writeString(String s, int tam) throws IOException {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(s);
        
        // Pad with spaces if necessary
        while (sb.length() < tam) {
            sb.append(' ');
        }
        
        // Write exactly 'tam' characters
        for (int i = 0; i < tam; i++) {
            raf.writeChar(sb.charAt(i));
        }
    }

    /**
     * Writes a date as three distinct integers.
     */
    public void writeDate(int dia, int mes, int ano) throws IOException {
        raf.writeInt(dia);
        raf.writeInt(mes);
        raf.writeInt(ano);
    }

    public void seek(long posicao) throws IOException {
        raf.seek(posicao);
    }

    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    public long length() throws IOException {
        return raf.length();
    }

    public void close() throws IOException {
        raf.close();
    }
}
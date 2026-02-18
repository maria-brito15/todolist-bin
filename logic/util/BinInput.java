package logic.util;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Utility class for reading primitive types and structured data from a binary file.
 * Uses RandomAccessFile in read-only mode.
 */
public class BinInput {

    private final RandomAccessFile raf;

    /**
     * Initializes the reader for a specific file in read-only ("r") mode.
     * @param arquivo Path to the binary file.
     * @throws IOException If the file cannot be opened.
     */
    public BinInput(String arquivo) throws IOException {
        this.raf = new RandomAccessFile(arquivo, "r");
    }

    public int readInt() throws IOException {
        return raf.readInt();
    }

    public float readFloat() throws IOException {
        return raf.readFloat();
    }

    public double readDouble() throws IOException {
        return raf.readDouble();
    }

    public boolean readBoolean() throws IOException {
        return raf.readBoolean();
    }

    /**
     * Reads a fixed number of characters and returns them as a trimmed String.
     * @param tam The number of characters to read.
     * @return The resulting string, stripped of trailing whitespace.
     */
    public String readString(int tam) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tam; i++) {
            sb.append(raf.readChar());
        }
        return sb.toString().trim();
    }

    /**
     * Reads three consecutive integers representing a date (Day, Month, Year).
     * @return An integer array [day, month, year].
     */
    public int[] readDate() throws IOException {
        return new int[]{ raf.readInt(), raf.readInt(), raf.readInt() };
    }

    /**
     * Moves the file pointer to a specific byte offset.
     */
    public void seek(long posicao) throws IOException {
        raf.seek(posicao);
    }

    /**
     * Skips a specified number of bytes from the current position.
     */
    public void skip(int bytes) throws IOException {
        raf.skipBytes(bytes);
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
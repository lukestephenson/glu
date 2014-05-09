package org.linkedin.glu.utils.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class FileTailer {
    static final int MAX_BYTES_TO_BUFFER = 1204 * 4;
    private static final byte DELIMETER = (byte) "\n".charAt(0);

    /**
     * Returns an {@link InputStream} restricted to the last X lines (delimited by the newline character).
     * <p>
     * Attempts to replicate the unix {@code tail -<lines> -c <maxBytesToStream>}
     * <p>
     * 
     * @param file
     * @param lines
     *            the maximum number of lines to retrieve from the file or -1 for no limit on the number
     * @param maxBytesToStream
     *            max bytes to stream or -1 for no limit on the number of bytes
     * @return
     * @throws IOException
     */
    public InputStream getTail(final File file, final int lines, final int maxBytesToStream) throws IOException {
        if (lines == -1) {
            // size limited only
            return getTailBytesRestricted(file, 0, maxBytesToStream);
        }

        boolean sizeLimit = maxBytesToStream > 0;
        long startPoint = 0;

        int linesProcessed = -1; // ignore the first end of line at the end of the file. Nothing read at that point.

        byte[] buffer = new byte[MAX_BYTES_TO_BUFFER];

        RandomAccessFile raf = new RandomAccessFile(file, "r"); // open read only
        try {
            long fileLength = raf.length();
            long filePosition = fileLength;

            while ((!sizeLimit || fileLength - filePosition < maxBytesToStream) && startPoint == 0 && filePosition > 0) {
                int bytesToRead = (int) Math.min(filePosition, MAX_BYTES_TO_BUFFER);

                filePosition = Math.max(filePosition - bytesToRead, 0);

                raf.seek(filePosition);
                int bytesRead = raf.read(buffer, 0, bytesToRead);

                if (bytesRead == -1) {
                    break;
                }
                for (int i = bytesRead - 1; i >= 0; i--) {
                    if (buffer[i] == DELIMETER) {
                        linesProcessed++;

                        if (linesProcessed >= lines) {
                            startPoint = filePosition + i + 1; // exclude the last newline read.
                            break;
                        }
                    }
                }
            }

            return getTailBytesRestricted(file, startPoint, maxBytesToStream);

        } finally {
            try {
                raf.close();
            } catch (IOException ex) {

            }
        }
    }

    private InputStream getTailBytesRestricted(final File file, long startPoint, final int maxBytesToStream)
            throws IOException {
        long fileSize = file.length();

        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        if (maxBytesToStream != -1) {
            startPoint = Math.max(startPoint, fileSize - maxBytesToStream);
        }

        inputStream.skip(startPoint);

        if (maxBytesToStream == -1) {
            // return the entire stream
            return inputStream;
        } else {
            return new LimitedInputStream(inputStream, maxBytesToStream);
        }
    }

}

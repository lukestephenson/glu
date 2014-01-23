package org.linkedin.glu.utils.jarchive;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


public class JavaArchiveInputStream extends FilterInputStream {

    private JavaArchiveEntry currentEntry;
    private long currentFileSize;
    private long bytesRead;
    private boolean defaultSkip = false;

    public JavaArchiveInputStream(InputStream in) {
        super(in);
        currentFileSize = 0;
        bytesRead = 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Read a byte
     *
     * @see java.io.FilterInputStream#read()
     */
    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];

        int res = this.read(buf, 0, 1);

        if (res != -1) {
            return 0xFF & buf[0];
        }

        return res;
    }

    /**
     * Checks if the bytes being read exceed the entry size and adjusts the byte
     * array length. Updates the byte counters
     *
     *
     * @see java.io.FilterInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (currentEntry != null) {
            if (currentFileSize == currentEntry.getSize()) {
                return -1;
            } else if ((currentEntry.getSize() - currentFileSize) < len) {
                len = (int) (currentEntry.getSize() - currentFileSize);
            }
        }

        int br = super.read(b, off, len);

        if (br != -1) {
            if (currentEntry != null) {
                currentFileSize += br;
            }

            bytesRead += br;
        }

        return br;
    }

    /**
     * Returns the next entry in the archive file
     *
     * @return JavaArchiveEntry
     * @throws java.io.IOException
     */
    public JavaArchiveEntry getNextEntry() throws IOException {
        closeCurrentEntry();

        byte[] header = new byte[JavaArchiveConstants.HEADER_BLOCK];
        byte[] theader = new byte[JavaArchiveConstants.HEADER_BLOCK];
        int tr = 0;

        // Read full header
        while (tr < JavaArchiveConstants.HEADER_BLOCK) {
            int res = read(theader, 0, JavaArchiveConstants.HEADER_BLOCK - tr);

            if (res < 0) {
                break;
            }

            System.arraycopy(theader, 0, header, tr, res);
            tr += res;
        }

        // Check if record is null
        boolean eof = true;
        for (byte b : header) {
            if (b != 0) {
                eof = false;
                break;
            }
        }

        if (!eof) {
            currentEntry = new JavaArchiveEntry(header);
        }

        return currentEntry;
    }

    /**
     * Closes the current archive entry
     *
     * @throws java.io.IOException
     */
    protected void closeCurrentEntry() throws IOException {
        if (currentEntry != null) {
            if (currentEntry.getSize() > currentFileSize) {
                // Not fully read, skip rest of the bytes
                long bs = 0;
                while (bs < currentEntry.getSize() - currentFileSize) {
                    long res = skip(currentEntry.getSize() - currentFileSize - bs);

                    if (res == 0 && currentEntry.getSize() - currentFileSize > 0) {
                        // I suspect file corruption
                        throw new IOException("Possible Archive file corruption");
                    }

                    bs += res;
                }
            }

            currentEntry = null;
            currentFileSize = 0L;
            skipPad();
        }
    }

    /**
     * Skips the pad at the end of each Archive Entry file content
     *
     * @throws java.io.IOException
     */
    protected void skipPad() throws IOException {
        if (bytesRead > 0) {
            int extra = (int) (bytesRead % JavaArchiveConstants.DATA_BLOCK);

            if (extra > 0) {
                long bs = 0;
                while (bs < JavaArchiveConstants.DATA_BLOCK - extra) {
                    long res = skip(JavaArchiveConstants.DATA_BLOCK - extra - bs);
                    bs += res;
                }
            }
        }
    }

    /**
     * Skips 'n' bytes on the InputStream<br>
     * Overrides default implementation of skip
     *
     */
    @Override
    public long skip(long n) throws IOException {
        if (defaultSkip) {
            // use skip method of parent stream
            // may not work if skip not implemented by parent
            long bs = super.skip(n);
            bytesRead += bs;

            return bs;
        }

        if (n <= 0) {
            return 0;
        }

        long left = n;
        int skipSize = JavaArchiveConstants.BUFFER_SIZE;
        byte[] sBuff = new byte[skipSize];

        while (left > 0) {
            int res = read(sBuff, 0, (int) (left < skipSize ? left : skipSize));
            if (res < 0) {
                break;
            }
            left -= res;
        }

        return n - left;
    }

    public boolean isDefaultSkip() {
        return defaultSkip;
    }

    public void setDefaultSkip(boolean defaultSkip) {
        this.defaultSkip = defaultSkip;
    }
}

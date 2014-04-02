package org.linkedin.glu.utils.jarchive;

import java.io.File;
import java.util.Date;

/**
 * JavaArchiveEntry class represents an individual file or folder in archive
 *
 */
public class JavaArchiveEntry {
    protected File file;
    protected JavaArchiveHeader header;

    private JavaArchiveEntry() {
        this.file = null;
        header = new JavaArchiveHeader();
    }

    public JavaArchiveEntry(File file, String entryName) {
        this();
        this.file = file;
        this.extractTarHeader(entryName);
    }

    public JavaArchiveEntry(byte[] headerBuf) {
        this();
        this.parseTarHeader(headerBuf);
    }

    /**
     * Constructor to create an entry from an existing TarHeader object.
     *
     * This method is useful to add new entries programmatically (e.g. for
     * adding files or directories that do not exist in the file system).
     *
     * @param header
     *
     */
    public JavaArchiveEntry(JavaArchiveHeader header) {
        this.file = null;
        this.header = header;
    }

    public boolean equals(JavaArchiveEntry it) {
        return header.name.toString().equals(it.header.name.toString());
    }

    public boolean isDescendent(JavaArchiveEntry desc) {
        return desc.header.name.toString().startsWith(header.name.toString());
    }

    public JavaArchiveHeader getHeader() {
        return header;
    }

    public String getName() {
        String name = header.name.toString();
        if (header.namePrefix != null && !header.namePrefix.toString().equals("")) {
            name = header.namePrefix.toString() + "/" + name;
        }

        return name;
    }

    public void setName(String name) {
        header.name = new StringBuffer(name);
    }

    public int getUserId() {
        return header.userId;
    }

    public void setUserId(int userId) {
        header.userId = userId;
    }

    public int getGroupId() {
        return header.groupId;
    }

    public void setGroupId(int groupId) {
        header.groupId = groupId;
    }

    public String getUserName() {
        return header.userName.toString();
    }

    public void setUserName(String userName) {
        header.userName = new StringBuffer(userName);
    }

    public String getGroupName() {
        return header.groupName.toString();
    }

    public void setGroupName(String groupName) {
        header.groupName = new StringBuffer(groupName);
    }

    public void setIds(int userId, int groupId) {
        this.setUserId(userId);
        this.setGroupId(groupId);
    }

    public void setModTime(long time) {
        header.modTime = time / 1000;
    }

    public void setModTime(Date time) {
        header.modTime = time.getTime() / 1000;
    }

    public Date getModTime() {
        return new Date(header.modTime * 1000);
    }

    public File getFile() {
        return this.file;
    }

    public long getSize() {
        return header.size;
    }

    public void setSize(long size) {
        header.size = size;
    }

    /**
     * Checks if the JarchiveEntry is a directory
     *
     * @return
     */
    public boolean isDirectory() {
        if (this.file != null)
            return this.file.isDirectory();

        if (header != null) {
            if (header.linkFlag == JavaArchiveConstants.LF_DIR)
                return true;

            if (header.name.toString().endsWith("/"))
                return true;
        }

        return false;
    }

    /**
     * Extract header from File
     *
     * @param entryName
     */
    public void extractTarHeader(String entryName) {
        header = JavaArchiveHeader.createHeader(entryName, file.length(), file.lastModified() / 1000, file.isDirectory());
    }

    /**
     * Calculate checksum
     *
     * @param buf
     * @return
     */
    public long computeCheckSum(byte[] buf) {
        long sum = 0;

        for (int i = 0; i < buf.length; ++i) {
            sum += 255 & buf[i];
        }

        return sum;
    }

    /**
     * Writes the header to the byte buffer
     *
     * @param outbuf
     */
    public void writeEntryHeader(byte[] outbuf) {
        int offset = 0;

        offset = JavaArchiveHeader.getNameBytes(header.name, outbuf, offset, JavaArchiveConstants.NAMELEN);
        offset = Octal.getOctalBytes(header.mode, outbuf, offset, JavaArchiveConstants.MODELEN);
        offset = Octal.getOctalBytes(header.userId, outbuf, offset, JavaArchiveConstants.UIDLEN);
        offset = Octal.getOctalBytes(header.groupId, outbuf, offset, JavaArchiveConstants.GIDLEN);

        long size = header.size;

        offset = Octal.getLongOctalBytes(size, outbuf, offset, JavaArchiveConstants.SIZELEN);
        offset = Octal.getLongOctalBytes(header.modTime, outbuf, offset, JavaArchiveConstants.MODTIMELEN);

        int csOffset = offset;
        for (int c = 0; c < JavaArchiveConstants.CHKSUMLEN; ++c){
            outbuf[offset++] = (byte) ' ';
        }
        outbuf[offset++] = header.linkFlag;

        offset = JavaArchiveHeader.getNameBytes(header.linkName, outbuf, offset, JavaArchiveConstants.NAMELEN);
        offset = JavaArchiveHeader.getNameBytes(header.magic, outbuf, offset, JavaArchiveConstants.USTAR_MAGICLEN);
        offset = JavaArchiveHeader.getNameBytes(header.userName, outbuf, offset, JavaArchiveConstants.USTAR_USER_NAMELEN);
        offset = JavaArchiveHeader.getNameBytes(header.groupName, outbuf, offset, JavaArchiveConstants.USTAR_GROUP_NAMELEN);
        offset = Octal.getOctalBytes(header.devMajor, outbuf, offset, JavaArchiveConstants.USTAR_DEVLEN);
        offset = Octal.getOctalBytes(header.devMinor, outbuf, offset, JavaArchiveConstants.USTAR_DEVLEN);
        offset = JavaArchiveHeader.getNameBytes(header.namePrefix, outbuf, offset, JavaArchiveConstants.USTAR_FILENAME_PREFIX);

        for (; offset < outbuf.length;){
            outbuf[offset++] = 0;
        }
        
        long checkSum = this.computeCheckSum(outbuf);

        Octal.getCheckSumOctalBytes(checkSum, outbuf, csOffset, JavaArchiveConstants.CHKSUMLEN);
    }

    /**
     * Parses the tar header to the byte buffer
     *
     * @param bh
     */
    public void parseTarHeader(byte[] bh) {
        int offset = 0;

        header.name = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.NAMELEN);
        offset += JavaArchiveConstants.NAMELEN;

        header.mode = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.MODELEN);
        offset += JavaArchiveConstants.MODELEN;

        header.userId = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.UIDLEN);
        offset += JavaArchiveConstants.UIDLEN;

        header.groupId = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.GIDLEN);
        offset += JavaArchiveConstants.GIDLEN;

        header.size = Octal.parseOctal(bh, offset, JavaArchiveConstants.SIZELEN);
        offset += JavaArchiveConstants.SIZELEN;

        header.modTime = Octal.parseOctal(bh, offset, JavaArchiveConstants.MODTIMELEN);
        offset += JavaArchiveConstants.MODTIMELEN;

        header.checkSum = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.CHKSUMLEN);
        offset += JavaArchiveConstants.CHKSUMLEN;

        header.linkFlag = bh[offset++];

        header.linkName = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.NAMELEN);
        offset += JavaArchiveConstants.NAMELEN;

        header.magic = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.USTAR_MAGICLEN);
        offset += JavaArchiveConstants.USTAR_MAGICLEN;

        header.userName = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.USTAR_USER_NAMELEN);
        offset += JavaArchiveConstants.USTAR_USER_NAMELEN;

        header.groupName = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.USTAR_GROUP_NAMELEN);
        offset += JavaArchiveConstants.USTAR_GROUP_NAMELEN;

        header.devMajor = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.USTAR_DEVLEN);
        offset += JavaArchiveConstants.USTAR_DEVLEN;

        header.devMinor = (int) Octal.parseOctal(bh, offset, JavaArchiveConstants.USTAR_DEVLEN);
        offset += JavaArchiveConstants.USTAR_DEVLEN;

        header.namePrefix = JavaArchiveHeader.parseName(bh, offset, JavaArchiveConstants.USTAR_FILENAME_PREFIX);
    }
    
    
    public static class Octal {

        /**
         * Parse an octal string from a header buffer. This is used for the file
         * permission mode value.
         *
         * @param header: The header buffer from which to parse.
         * @param offset: The offset into the buffer from which to parse.
         * @param length: The number of header bytes to parse.
         *
         * @return The long value of the octal string.
         */
        public static long parseOctal(byte[] header, int offset, int length) {
            long result = 0;
            boolean stillPadding = true;

            int end = offset + length;
            for (int i = offset; i < end; ++i) {
                if (header[i] == 0)
                    break;

                if (header[i] == (byte) ' ' || header[i] == '0') {
                    if (stillPadding)
                        continue;

                    if (header[i] == (byte) ' ')
                        break;
                }

                stillPadding = false;

                result = ( result << 3 ) + ( header[i] - '0' );
            }

            return result;
        }

        /**
         * Parse an octal integer from a header buffer.
         *
         * @param value
         * @param buf
         *            The header buffer from which to parse.
         * @param offset
         *            The offset into the buffer from which to parse.
         * @param length
         *            The number of header bytes to parse.
         *
         * @return The integer value of the octal bytes.
         */
        public static int getOctalBytes(long value, byte[] buf, int offset, int length) {
            int idx = length - 1;

            buf[offset + idx] = 0;
            --idx;
            buf[offset + idx] = (byte) ' ';
            --idx;

            if (value == 0) {
                buf[offset + idx] = (byte) '0';
                --idx;
            } else {
                for (long val = value; idx >= 0 && val > 0; --idx) {
                    buf[offset + idx] = (byte) ( (byte) '0' + (byte) ( val & 7 ) );
                    val = val >> 3;
                }
            }

            for (; idx >= 0; --idx) {
                buf[offset + idx] = (byte) ' ';
            }

            return offset + length;
        }

        /**
         * Parse the checksum octal integer from a header buffer.
         *
         * @param value
         * @param buf: The header buffer from which to parse.
         * @param offset: The offset into the buffer from which to parse.
         * @param length: The number of header bytes to parse.
         * @return The integer value of the entry's checksum.
         */
        public static int getCheckSumOctalBytes(long value, byte[] buf, int offset, int length) {
            getOctalBytes( value, buf, offset, length );
            buf[offset + length - 1] = (byte) ' ';
            buf[offset + length - 2] = 0;
            return offset + length;
        }

        /**
         * Parse an octal long integer from a header buffer.
         *
         * @param value
         * @param buf: The header buffer from which to parse.
         * @param offset: The offset into the buffer from which to parse.
         * @param length: The number of header bytes to parse.
         *
         * @return The long value of the octal bytes.
         */
        public static int getLongOctalBytes(long value, byte[] buf, int offset, int length) {
            byte[] temp = new byte[length + 1];
            getOctalBytes( value, temp, 0, length + 1 );
            System.arraycopy( temp, 0, buf, offset, length );
            return offset + length;
        }

    }
}

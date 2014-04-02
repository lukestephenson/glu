package org.linkedin.glu.utils.jarchive;

import java.io.File;

public class JavaArchiveHeader {

    // Header values
    public StringBuffer name;
    public int mode;
    public int userId;
    public int groupId;
    public long size;
    public long modTime;
    public int checkSum;
    public byte linkFlag;
    public StringBuffer linkName;
    public StringBuffer magic; // ustar indicator and version
    public StringBuffer userName;
    public StringBuffer groupName;
    public int devMajor;
    public int devMinor;
    public StringBuffer namePrefix;

    public JavaArchiveHeader() {
        this.magic = new StringBuffer(JavaArchiveConstants.USTAR_MAGIC);

        this.name = new StringBuffer();
        this.linkName = new StringBuffer();

        String user = System.getProperty("user.name", "");

        if (user.length() > 31)
            user = user.substring(0, 31);

        this.userId = 0;
        this.groupId = 0;
        this.userName = new StringBuffer(user);
        this.groupName = new StringBuffer("");
        this.namePrefix = new StringBuffer();
    }

    /**
     * Parse an entry name from a header buffer.
     *
     * @param header: The header buffer from which to parse.
     * @param offset: The offset into the buffer from which to parse.
     * @param length: The number of header bytes to parse.
     * @return The header's entry name.
     */
    public static StringBuffer parseName(byte[] header, int offset, int length) {
        StringBuffer result = new StringBuffer(length);

        int end = offset + length;
        for (int i = offset; i < end; ++i) {
            if (header[i] == 0)
                break;
            result.append((char) header[i]);
        }

        return result;
    }

    /**
     * Determine the number of bytes in an entry name.
     *
     * @param name: File name
     * @param buf: The header buffer from which to parse.
     * @param offset: The offset into the buffer from which to parse.
     * @param length: The number of header bytes to parse.
     * @return The number of bytes in a header's entry name.
     */
    public static int getNameBytes(StringBuffer name, byte[] buf, int offset, int length) {
        int i;

        for (i = 0; i < length && i < name.length(); ++i) {
            buf[offset + i] = (byte) name.charAt(i);
        }

        for (; i < length; ++i) {
            buf[offset + i] = 0;
        }

        return offset + length;
    }

    /**
     * Creates a new header for a file/directory entry.
     *
     *
     * @param entryName: File name
     * @param size: File size in bytes
     * @param modTime: Last modification time in numeric Unix time format
     * @param dir: Is directory
     *
     * @return
     */
    public static JavaArchiveHeader createHeader(String entryName, long size, long modTime, boolean dir) {
        String name = entryName;
        name = trim(name.replace(File.separatorChar, '/'), '/');

        JavaArchiveHeader header = new JavaArchiveHeader();
        header.linkName = new StringBuffer("");

        if (name.length() > 100) {
            header.namePrefix = new StringBuffer(name.substring(0, name.lastIndexOf('/')));
            header.name = new StringBuffer(name.substring(name.lastIndexOf('/') + 1));
        } else {
            header.name = new StringBuffer(name);
        }

        if (dir) {
            header.mode = 040755;
            header.linkFlag = JavaArchiveConstants.LF_DIR;
            if (header.name.charAt(header.name.length() - 1) != '/') {
                header.name.append("/");
            }
            header.size = 0;
        } else {
            header.mode = 0100644;
            header.linkFlag = JavaArchiveConstants.LF_NORMAL;
            header.size = size;
        }

        header.modTime = modTime;
        header.checkSum = 0;
        header.devMajor = 0;
        header.devMinor = 0;

        return header;
    }
    
    /**
     * 
     * @param s
     * @param c
     * @return
     */
    public static String trim(String s, char c) {
        StringBuffer tmp = new StringBuffer(s);
        for (int i = 0; i < tmp.length(); i++) {
            if (tmp.charAt(i) != c) {
                break;
            } else {
                tmp.deleteCharAt(i);
            }
        }

        for (int i = tmp.length() - 1; i >= 0; i--) {
            if (tmp.charAt(i) != c) {
                break;
            } else {
                tmp.deleteCharAt(i);
            }
        }

        return tmp.toString();
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utils contains a few commonly occurring methods.
 *
 * @author richardrodgers
 */
public class Utils
{
    private static final int BUFF_SIZE = 4096;
    // we can live with 4k preallocation
    private static final byte[] buffer = new byte[BUFF_SIZE];
    
    /**
     * Calculates and returns a checksum for the passed file using the passed
     * algorithm.
     * 
     * @param file
     *        file on which to calculate checksum
     * @param algorithm
     *        string for algorithm: 'MD5', 'SHA1', etc
     * @return checksum
     *        string of the calculated checksum
     *        
     * @throws IOException if IO error
     */
    public static String checksum(File file, String algorithm) throws IOException
    {
        InputStream in = null;
        String chkSum = null;
        try
        {
            in = new FileInputStream(file);
            chkSum = checksum(in, algorithm);
        }
        finally
        {
            if (in != null)
            {
               in.close(); 
            }
        }
        return chkSum;
    }

    /**
     * Calculates and returns a checksum for the passed IO stream using the passed
     * algorithm.
     * 
     * @param in
     *        input stream on which to calculate checksum
     * @param algorithm
     *        string for algorithm: 'MD5', 'SHA1', etc
     * @return checksum
     *        string of the calculated checksum
     *        
     * @throws IOException if IO error
     */
    public static String checksum(InputStream in, String algorithm) throws IOException
    {
        try
        {
            DigestInputStream din = new DigestInputStream(in,
                                        MessageDigest.getInstance(algorithm));
            while (true)
            {
                synchronized (buffer)
                {
                    if (din.read(buffer) == -1)
                    {
                        break;
                    }
                    // otherwise, a no-op
                }
            }
            return toHex(din.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException nsaE) {
            throw new IOException(nsaE.getMessage(), nsaE);
        }
    }

    /**
     * Reasonably efficient Hex checksum converter
     * 
     * @param data
     *        byte array
     * @return hexString
     *        checksum
     */
    static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    public static String toHex(byte[] data) {
         if ((data == null) || (data.length == 0)) {
            return null;
        }
        char[] chars = new char[2 * data.length];
        for (int i = 0; i < data.length; ++i) {
            chars[2 * i] = HEX_CHARS[(data[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[data[i] & 0x0F];
        }
        return new String(chars);
    }
    
    /**
     * Performs a buffered copy from one file into another.
     * 
     * @param inFile input file
     * @param outFile output file
     * @throws IOException if IO error
     */
    public static void copy(File inFile, File outFile) throws IOException
    {
        FileInputStream in = null;
        FileOutputStream out = null;
        try
        {
            in = new FileInputStream(inFile);
            out = new FileOutputStream(outFile);
            copy(in, out);
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            
            if (out != null)
            {
                out.close();
            }
        }
    }

    /**
     * Performs a buffered copy from one IO stream into another. Note that stream
     * closure is responsibility of caller.
     * 
     * @param in
     *        input stream
     * @param out
     *        output stream
     * @throws IOException if IO error
     */
    public static void copy(InputStream in, OutputStream out) throws IOException
    {
        while (true)
        {
            synchronized (buffer)
            {
                int count = in.read(buffer);
                if (-1 == count)
                {
                    break;
                }
                // write out those same bytes
                out.write(buffer, 0, count);
            }
        }
        // needed to flush cache
        out.flush();
    }
}

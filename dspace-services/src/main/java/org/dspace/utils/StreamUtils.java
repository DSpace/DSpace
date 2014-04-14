/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Simple set of utilities which handle various stream operations.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class StreamUtils {

    /**
     * A simple utility to convert {@code InputStream}s into strings.
     *
     * @param is the input stream
     * @return the string version of the IS
     */
    public static String convertStreamToString(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException("Invalid input stream, cannot be null");
        }
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}

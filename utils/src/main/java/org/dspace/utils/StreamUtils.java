/**
 * $Id: StreamUtils.java 3434 2009-02-04 18:00:29Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/utils/src/main/java/org/dspace/utils/StreamUtils.java $
 * StreamUtils.java - DS2 - Feb 4, 2009 3:19:01 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
 */

package org.dspace.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Simple set of utils which handle various stream operations
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class StreamUtils {

    /**
     * A simple utility to convert inputstreams into strings
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

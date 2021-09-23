/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.util;

/**
 * Standard implementation of console IO using {@code System.console()}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class ConsoleServiceImpl
        implements ConsoleService {
    @Override
    public char[] readPassword(String prompt, Object... args) {
        return System.console().readPassword(prompt, args);
    }
}

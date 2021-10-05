/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * Make System.console mock-able for testing.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public interface ConsoleService {
    public char[] readPassword(String prompt, Object... args);
}

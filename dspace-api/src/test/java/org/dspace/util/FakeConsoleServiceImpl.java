/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * A test version of ConsoleService which supplies any password input that we
 * want.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class FakeConsoleServiceImpl
        implements ConsoleService {
    private String prompt;
    private Object[] args;
    private char[] password;

    @Override
    public char[] readPassword(String prompt, Object... args) {
        this.prompt = prompt;
        this.args = args;
        return this.password;
    }

    public String getPasswordPrompt() {
        return prompt;
    }

    public Object[] getArgs() {
        return this.args;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}

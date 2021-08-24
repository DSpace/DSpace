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
 * <p>This can return different passwords on even/odd calls, to test
 * confirmation dialogs.  See {@link setPassword1} and {@link setPassword2}.
 * Use {@link setPassword} to set both identically.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class FakeConsoleServiceImpl
        implements ConsoleService {
    private String prompt;
    private Object[] args;
    private char[] password1;
    private char[] password2;
    private int passwordCalls = 0;

    @Override
    public char[] readPassword(String prompt, Object... args) {
        this.prompt = prompt;
        this.args = args;
        passwordCalls++;
        if (passwordCalls % 2 != 0) {
            return password1;
        } else {
            return password2;
        }
    }

    public String getPasswordPrompt() {
        return prompt;
    }

    public Object[] getArgs() {
        return this.args;
    }

    /**
     * Set both passwords identically.
     * @param password the password to be returned each time.
     */
    public void setPassword(char[] password) {
        setPassword1(password);
        setPassword2(password);
    }

    /**
     * Set the password returned on odd calls to {@link readPassword}.
     * @param password the password to be returned.
     */
    public void setPassword1(char[] password) {
        password1 = password;
    }

    /**
     * Set the password returned on even calls to {@link readPassword},
     * and reset the call counter.
     * @param password the password to be returned.
     */
    public void setPassword2(char[] password) {
        password2 = password;
        passwordCalls = 0;
    }
}

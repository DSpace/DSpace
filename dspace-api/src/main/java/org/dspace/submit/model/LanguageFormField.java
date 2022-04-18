/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.model;

/**
 * A simple POJO to store information about the available languages for a field
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class LanguageFormField {
    /**
     * The value to present to the user
     */
    private String display;

    /**
     * The internal iso code to store in the database
     */
    private String code;

    public LanguageFormField(String code, String display) {
        this.code = code;
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


}

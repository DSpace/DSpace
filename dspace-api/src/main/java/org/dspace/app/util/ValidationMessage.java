/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

public class ValidationMessage
{
    private String i18nKey;
    
    private Object[] parameters;

    public String getI18nKey()
    {
        return i18nKey;
    }

    public void setI18nKey(String i18nKey)
    {
        this.i18nKey = i18nKey;
    }

    public Object[] getParameters()
    {
        return parameters;
    }

    public void setParameters(Object[] parameters)
    {
        this.parameters = parameters;
    }
}

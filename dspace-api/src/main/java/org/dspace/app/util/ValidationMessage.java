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

package it.cineca.surplus.ir.defaultvalues;

public class DefaultValuesBean
{

    private String[] values;

    private String[] authorities;

    private int[] confidences;

    private String metadataSchema, metadataElement, metadataQualifier,
            language;

    public String[] getValues()
    {
        return values;
    }

    public void setValues(String... values)
    {
        this.values = values;
    }

    public String getMetadataSchema()
    {
        return metadataSchema;
    }

    public void setMetadataSchema(String metadataSchema)
    {
        this.metadataSchema = metadataSchema;
    }

    public String getMetadataElement()
    {
        return metadataElement;
    }

    public void setMetadataElement(String metadataElement)
    {
        this.metadataElement = metadataElement;
    }

    public String getMetadataQualifier()
    {
        return metadataQualifier;
    }

    public void setMetadataQualifier(String metadataQualifier)
    {
        this.metadataQualifier = metadataQualifier;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String[] getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities(String... authorities)
    {
        this.authorities = authorities;
    }

    public int[] getConfidences()
    {
        return confidences;
    }

    public void setConfidences(int... confidences)
    {
        this.confidences = confidences;
    }
}
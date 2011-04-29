package org.dspace.content.crosswalk;

public interface IConverter
{
    /**
     * Get an alternative format for the input string. Useful examples are
     * conversion from a metadata language value in ISO-639-3 to ISO-639-1, etc.
     * 
     * @param value
     *            the input string to convert
     * @return the converted string returned by the "conversion algorithm" 
     */
    public String makeConversion(String value);
}

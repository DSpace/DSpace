package org.dspace.app.webui.cris.metrics;

/**
 * @author Pascarelli
 *
 */
public class NumberFormatter
{
    
    /**
     * NUMBER, CURRENCY, or PERCENT 
     */
    private String type;           
    
    
    /**
     * Specify a custom formatting pattern for the output. 
     */
    private String pattern;                 
    
    
    /**
     * Currency code (for type="currency") 
     */
    private String currencyCode;            
    
    /**
     * Currency symbol (for type="currency")
     */
    private String currencySymbol;
        
    /**
     * Whether to group numbers (TRUE or FALSE)
     */
    private String groupingUsed;
    
    /**
     * Maximum number of integer digits to print
     */
    private String maxIntegerDigits;
    
    /**
     * Minimum number of integer digits to print
     */
    private String minIntegerDigits;
    
    /**
     * Maximum number of fractional digits to print 
     */
    private String maxFractionDigits;
    
    /**
     * Minimum number of fractional digits to print
     */
    private String minFractionDigits;
    
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String getPattern()
    {
        return pattern;
    }
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }
    public String getCurrencyCode()
    {
        return currencyCode;
    }
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }
    public String getCurrencySymbol()
    {
        return currencySymbol;
    }
    public void setCurrencySymbol(String currencySymbol)
    {
        this.currencySymbol = currencySymbol;
    }
    public String getGroupingUsed()
    {
        return groupingUsed;
    }
    public void setGroupingUsed(String groupingUsed)
    {
        this.groupingUsed = groupingUsed;
    }
    public String getMaxIntegerDigits()
    {
        return maxIntegerDigits;
    }
    public void setMaxIntegerDigits(String maxIntegerDigits)
    {
        this.maxIntegerDigits = maxIntegerDigits;
    }
    public String getMinIntegerDigits()
    {
        return minIntegerDigits;
    }
    public void setMinIntegerDigits(String minIntegerDigits)
    {
        this.minIntegerDigits = minIntegerDigits;
    }
    public String getMaxFractionDigits()
    {
        return maxFractionDigits;
    }
    public void setMaxFractionDigits(String maxFractionDigits)
    {
        this.maxFractionDigits = maxFractionDigits;
    }
    public String getMinFractionDigits()
    {
        return minFractionDigits;
    }
    public void setMinFractionDigits(String minFractionDigits)
    {
        this.minFractionDigits = minFractionDigits;
    }       
    
    
}

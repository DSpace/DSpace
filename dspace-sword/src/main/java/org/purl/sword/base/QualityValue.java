/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;


/**
 * A representation of a quality value.
 * 
 * The quality value must be between 0 and 1, with no more than three digits
 * after the decimal place.
 * 
 * @author Stuart Lewis
 */
public class QualityValue {
    
    /** The quality value. */
    private float quality;
    
    /**
     * Create a quality value defaulting to 1
     * 
     * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
     */
    public QualityValue() throws NumberFormatException
    {
        // As per the spec, default to value 1
        setQualityValue(1f);
    }
    
    /**
     * Create a quality value
     * 
     * @param q The quality value
     * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
     */
    public QualityValue(float q) throws NumberFormatException
    {
        setQualityValue(q);
    }
    
    /**
     * Set the quality value.
     * 
     * @param q The quality value
     * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
     */
    public final void setQualityValue(float q) throws NumberFormatException
    {
        // Check the float is in range
        if ((q < 0) || (q > 1))
        {
            throw new NumberFormatException("Invalid value - must be between 0 and 1");
        }
        
        // Check there are no more than three digits after the decimal point
        String qStr = "" + q;
        int pos = qStr.indexOf('.');
        if (qStr.substring(pos + 1).length() > 3)
        {
            throw new NumberFormatException("Invalid value - no more than three digits after the decimal point: " + qStr);
        }
        quality = q;
    }
    
    /**
     * Get the quality value
     * 
     * @return the quality value
     */
    public final float getQualityValue()
    {
        return quality;
    }
    
    /**
     * Get a String representation of this quality value
     * 
     * @return The String representation of the quality value
     */
    public String toString()
    {
        return Float.toString(quality);
    }
    
    /**
     * A main method with rudimentary tests to check the class
     */
/*
    public static void main(String[] args)
    {
        // Test the class
        
        // Fail - under 0
        try
        {
            QualityValue qv1 = new QualityValue(-0.01f);
            System.out.println("1) Fail: -0.01 passed unexpectedly");
        }
        catch (NumberFormatException nfe)
        {
            System.out.print("1) Pass: -0.01 failed as expected ");
            System.out.println(nfe);
        }
        
        // Fail - over 1
        try
        {
            QualityValue qv2 = new QualityValue(1.01f);
            System.out.println("2) Fail: 1.01 passed unexpectedly");
        }
        catch (NumberFormatException nfe)
        {
            System.out.print("2) Pass: 1.01 failed as expected ");
            System.out.println(nfe);
        }
        
        // Fail - to many decimal points
        try
        {
            QualityValue qv3 = new QualityValue(0.1234f);
            System.out.println("3) Fail: 0.1234 passed unexpectedly");
        }
        catch (NumberFormatException nfe)
        {
            System.out.print("3) Pass: 0.1234 failed as expected ");
            System.out.println(nfe);
        }
        
        // Pass - no decimal places 0
        try
        {
            QualityValue qv4 = new QualityValue(0f);
            System.out.println("4) Pass: 0 passed as expected");
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("4) Fail: 0 failed unexpectedly");
        }
        
        // Pass - no decimal places 1
        try
        {
            QualityValue qv5 = new QualityValue(1f);
            System.out.println("5) Pass: 1 passed as expected");
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("5) Fail: 1 failed unexpectedly");
        }

        // Pass - 3 decimal places
        try
        {
            QualityValue qv6 = new QualityValue(0.123f);
            System.out.print("6) Pass: 0.123 passed as expected - ");
            System.out.println(qv6);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("6) Fail: 0.123 failed unexpectedly");
        }
        
        // Pass - No value given
        try
        {
            QualityValue qv6 = new QualityValue();
            System.out.print("7) Pass: no value passed as expected - ");
            System.out.println(qv6);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("7) Fail: no value failed unexpectedly");
        }
    }
*/
}

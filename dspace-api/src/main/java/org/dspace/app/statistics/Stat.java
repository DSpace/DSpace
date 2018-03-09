/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

/**
 * This is a primitive class to represent a single statistic, which will 
 * generally be a key value pair but with the capabilities for being sorted
 *
 * Note: this class has a natural ordering that is inconsistent with equals
 *
 * @author  Richard Jones
 */
public class Stat implements Comparable
{
    // FIXME: this class is functional but a bit messy, and should be neatened
    // up and completed
    
    /** the key, which is effectively the text of the statistic */
    private String key = null;
    
    /** the value assigned to the key, generally a count of the key */
    private int value = 0;
    
    /** a reference to an external resource which relates to this statistic */
    private String reference = null;
    
    /** the units that this statistic is in */
    private String units = null;
    
    /**
     * constructor to create new statistic
     *
     * @param   key     the key for the statistic
     * @param   value   the value for the statistic
     */
    Stat(String key, int value)
    {
        this.key = key;
        this.value = value;
    }
    
    /**
     * constructor to create new statistic
     *
     * @param   key     the key for the statistic
     * @param   value   the value for the statistic
     * @param   reference   the value for the external reference
     */
    Stat(String key, int value, String reference)
    {
        this.key = key;
        this.value = value;
        this.reference = reference;
    }
    
    /**
     * set the units of this statistic
     *
     * @param   unit    the units that this statistic is measured in
     */
    public void setUnits(String unit)
    {
        this.units = unit;
    }
    
    /**
     * get the unts that this statistic is measured in
     *
     * @return      the units this statistic is measured in
     */
    public String getUnits()
    {
        return this.units;
    }
    
    /** 
     * get the value of the statistic
     *
     * @return      the value of this statistic
     */
    public int getValue()
    {
        return this.value;
    }
    
    
    /**
     * get the key (text describing) the statistic
     *
     * @return      the key for this statistic
     */
    public String getKey()
    {
        return this.key;
    }
    
    
    /**
     * get the reference to related statistic information
     *
     * @return      the reference for this statistic
     */
    public String getReference()
    {
        return this.reference;
    }
    
    
    /**
     * set the reference information
     *
     * @param   key     the key for this statistic
     */
    public void setKey(String key)
    {
        this.key = key;
    }
    
    
    /**
     * set the reference information
     *
     * @param   reference   the reference for this statistic
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }
    
    
    /** 
     * compare the current object to the given object returning -1 if o is less
     * than the current object, 0 if they are the same, and +1 if o is greater
     * than the current object.
     *
     * @param  o   the object to compare to the current one
     *
     * @return      +1, 0, -1 if o is less than, equal to, or greater than the
     *              current object value.
     */
    @Override
    public int compareTo(Object o)
    {
        int objectValue = ((Stat) o).getValue();
        
        if (objectValue < this.getValue())
        {
            return -1;
        }
        else if (objectValue == this.getValue())
        {
            return 0;
        }
        else if (objectValue > this.getValue())
        {
            return 1;
        }
        
        return 0;
    }    
    
}

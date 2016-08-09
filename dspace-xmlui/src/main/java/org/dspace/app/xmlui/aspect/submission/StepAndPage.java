/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Holds a step number and a page number within that step.
 * 
 * @author Mark Wood
 */
public class StepAndPage implements Comparable<StepAndPage>
{
    /** Magic value meaning "no value" */
    private static final int UNSET = -1;

    /** Step number */
    private int step;

    /** Page number within step */
    private int page;

    /** Default constructor returns an unset instance */
    public StepAndPage()
    {
        step = UNSET; 
        page = UNSET;
    }

    /**
     * Initialize a new StepAndPage from given step and page numbers
     * 
     * @param step step number.
     * @param page page number.
     */
    public StepAndPage(int step, int page)
    {
        this.step = step;
        this.page = page;
    }

    /**
     * Initialize a new StepAndPage by parsing a string of the form "step.page".
     * 
     * @param asString
     *            decimal step and page numbers separated by a period.
     */
    public StepAndPage(String asString)
    {
        String[] components = asString.split("\\.");
        if (components.length > 0)
        {
            step = Integer.parseInt(components[0]);
            if (components.length > 1)
            {
                page = Integer.parseInt(components[1]);
            }
            else
            {
                page = UNSET;
            }
        }
        else
        {
            step = UNSET;
        }
    }

    public int getStep()
    {
        return step;
    }

    public int getPage()
    {
        return page;
    }

    /**
     * Does this instance have a value?
     * 
     * @return true if step and page have been set
     */
    public boolean isSet()
    {
        return (step != UNSET) && (page != UNSET);
    }

    /**
     * Compare this StepAndPage with another
     * 
     * @param other the other comparand.
     * @return true if both objects have same step value and same page value.
     */
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof StepAndPage)
        {
            StepAndPage sapOther = (StepAndPage)other;
            return (this.step == sapOther.step) && (this.page == sapOther.page);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(step).append(page).hashCode();
    }

    @Override
    public String toString()
    {
        return Integer.toString(step) + "." + Integer.toString(page);
    }

    @Override
    public int compareTo(StepAndPage o)
    {
        if (this.step == o.step)
        {
            return this.page - o.page;
        }
        else
        {
            return this.step - o.step;
        }
    }
}

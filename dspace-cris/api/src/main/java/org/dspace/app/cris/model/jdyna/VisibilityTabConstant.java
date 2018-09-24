/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import java.util.LinkedList;
import java.util.List;

public class VisibilityTabConstant
{

    /**
     * Show only to satisfy certain rule
     */
    public final static Integer POLICY = 4;

    /**
     * Show to all
     */
    public final static Integer HIGH = 3;

    /**
     * Tab and box show only to RP owner and admin
     */
    public final static Integer STANDARD = 2;

    /**
     * Hiding to RP, show only admin
     */
    public final static Integer ADMIN = 1;

    /**
     * Tab and box show only to RP owner
     */
    public final static Integer LOW = 0;

    public static List<Integer> getValues()
    {
        List<Integer> values = new LinkedList<Integer>();
        values.add(POLICY);
        values.add(HIGH);
        values.add(STANDARD);
        values.add(ADMIN);
        values.add(LOW);
        return values;
    }

    public static List<Integer> getEditValues()
    {
        List<Integer> values = new LinkedList<Integer>();
        values.add(POLICY);
        values.add(STANDARD);
        values.add(ADMIN);
        values.add(LOW);
        return values;
    }

    public static List<Integer> getLimitedValues()
    {
        return getValues();
    }
    
    public static List<Integer> getLimitedEditValues()
    {
        return getEditValues();
    }
    
}

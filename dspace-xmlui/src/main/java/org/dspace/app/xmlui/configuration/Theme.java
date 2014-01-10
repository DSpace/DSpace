/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.configuration;

import java.util.regex.Pattern;

/**
 * This class represents a theme.
 * 
 * @author Scott Phillips
 */

public class Theme
{
    /** The unique name of the theme */
    private final String name;
    /** The directory path of the theme */
    private final String path;
    /** The unique id of the theme */
    private final String id;
    
    /** The regular exrpession for this theme rule, if supplied */
    private final String regex;
    
    /** The handle expression for this theme rule, if supplied */
    private final String handle;
    
    /** The compiled regex expression */
    private final Pattern pattern;

    /**
     * Create a new theme rule.
     * 
     * @param name A unique name of the theme
     * @param path The directory path to the theme
     * @param id The unique ID of the theme
     * @param regex The regular exrpession for this theme rule
     * @param handle handle expression for this theme rule
     */
    public Theme(String name, String path, String id, String regex, String handle) {
        this.name = name;
        this.path = path;
        this.id = id;
        this.regex = regex;
        if (regex != null && regex.length() > 0)
        {
            this.pattern = Pattern.compile(regex);
        }
        else
        {
            this.pattern = null;
        }
        this.handle = handle;
    }
    
    /**
     * 
     * @return If there is a handle component to this theme rule.
     */
    public boolean hasHandle() {
        return (handle != null && handle.length() > 0);
    }
    
    /**
     * 
     * @return If there is a regex component to this theme rule.
     */
    public boolean hasRegex() {
        return (regex != null && regex.length() > 0);
    }
    
    /**
     * 
     * @return The unique name of this theme.
     */
    public String getName() {
        return name;
    }
    
    /**
     * 
     * @return The directory path to this theme.
     */
    public String getPath() {
        return path;
    }
    
    /**
     * 
     * @return The regex component of this theme rule.
     */
    public String getRegex() {
        return regex;
    }
    
    /**
     * 
     * @return The regex component of this theme rule, compiled as a regex Pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }
    
    /**
     * 
     * @return The handle component of this theme rule.
     */
    public String getHandle() {
        return handle;
    }
    
    /**
     * @return The theme's unique ID
     */
    public String getId(){
    	return id;
    }
    
}

/*
 * Theme.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/01/10 05:19:09 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
            this.pattern = Pattern.compile(regex);
        else
            this.pattern = null;
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

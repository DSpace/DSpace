/*
 * SelfNamedPlugin.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.core;

/**
 * Simple lightweight "framework" for managing plugins.
 * <p>
 * This is a superclass of all classes which are managed as self-named
 * plugins.  They must extend <code>SelfNamedPlugin</code> or its
 * subclass.
 * <p>
 * Unfortunately, this has to be an <code>abstract class</code> because
 * an <code>interface</code> may not have static methods.  The
 * <code>pluginAliases</code> method is static so it can be invoked
 * without creating an instance, and thus let the aliases live in the
 * class itself so there is no need for name mapping in a separate
 * configuration file.
 * <p>
 * See the documentation in the
 * <code>PluginManager</code> class for more details.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PluginManager
 */
public abstract class SelfNamedPlugin
{
    // the specific alias used to find the class that created this instance.
    private String myName = null;

    /**
     * Get the names of this plugin implementation.
     * Returns all names to which this plugin answers.
     * <p>
     * A name should be a short generic name illustrative of the
     * service, e.g. <code>"PDF"</code>, <code>"JPEG"</code>, <code>"GIF"</code>
     * for media filters.
     * <p>
     * Each name must be unique among all the plugins implementing any
     * given interface, but it can be the same as a name of
     * a plugin for a different interface.  For example, two classes
     * may each have a <code>"default"</code> name if they do not
     * implement any of the same interfaces.
     *
     * @return array of names of this plugin
     */
    public static String[] getPluginNames()
    {
        return null;
    }

    /**
     * Get an instance's particular name.
     * Returns the name by which the class was chosen when
     * this instance was created.  Only works for instances created
     * by <code>PluginManager</code>, or if someone remembers to call <code>setPluginName.</code>
     * <p>
     * Useful when the implementation class wants to be configured differently
     * when it is invoked under different names.
     *
     * @return name or null if not available.
     */
    public String getPluginInstanceName()
    {
        return myName;
    }

    /**
     * Set the name under which this plugin was instantiated.
     * Not to be invoked by application code, it is
     * called automatically by <code>PluginManager.getNamedPlugin()</code>
     * when the plugin is instantiated.
     *
     * @param name -- name used to select this class.
     */
    protected void setPluginInstanceName(String name)
    {
        myName = name;
    }
}

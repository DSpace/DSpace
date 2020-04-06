/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.configuration;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.BeanNameAware;

/**
 * This class represents an Abstract class that a ScriptConfiguration can inherit to further implement this
 * and represent a script's configuration
 */
public abstract class ScriptConfiguration<T extends DSpaceRunnable> implements BeanNameAware {

    /**
     * The possible options for this script
     */
    protected Options options;

    private String description;

    private String name;

    /**
     * Generic getter for the description
     * @return the description value of this ScriptConfiguration
     */
    public String getDescription() {
        return description;
    }

    /**
     * Generic setter for the description
     * @param description   The description to be set on this ScriptConfiguration
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Generic getter for the name
     * @return the name value of this ScriptConfiguration
     */
    public String getName() {
        return name;
    }

    /**
     * Generic setter for the name
     * @param name   The name to be set on this ScriptConfiguration
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generic getter for the dspaceRunnableClass
     * @return the dspaceRunnableClass value of this ScriptConfiguration
     */
    public abstract Class<T> getDspaceRunnableClass();

    /**
     * Generic setter for the dspaceRunnableClass
     * @param dspaceRunnableClass   The dspaceRunnableClass to be set on this IndexDiscoveryScriptConfiguration
     */
    public abstract void setDspaceRunnableClass(Class<T> dspaceRunnableClass);
    /**
     * This method will return if the script is allowed to execute in the given context. This is by default set
     * to the currentUser in the context being an admin, however this can be overwritten by each script individually
     * if different rules apply
     * @param context   The relevant DSpace context
     * @return          A boolean indicating whether the script is allowed to execute or not
     */
    public abstract boolean isAllowedToExecute(Context context);

    /**
     * The getter for the options of the Script
     * @return the options value of this ScriptConfiguration
     */
    public abstract Options getOptions();

    @Override
    public void setBeanName(String beanName) {
        this.name = beanName;
    }
}

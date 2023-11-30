/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.configuration;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents an Abstract class that a ScriptConfiguration can inherit to further implement this
 * and represent a script's configuration.
 * By default script are available only to repository administrators script that have a broader audience
 * must override the {@link #isAllowedToExecute(Context, List)} method.
 */
public abstract class ScriptConfiguration<T extends DSpaceRunnable> implements BeanNameAware {

    @Autowired
    protected AuthorizeService authorizeService;

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
     * @param commandLineParameters the parameters that will be used to start the process if known,
     *        <code>null</code> otherwise
     * @return          A boolean indicating whether the script is allowed to execute or not
     */
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    /**
     * The getter for the options of the Script
     * @return the options value of this ScriptConfiguration
     */
    public abstract Options getOptions();

    /**
     * The getter for the options of the Script (help informations)
     *
     * @return the options value of this ScriptConfiguration for help
     */
    public Options getHelpOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h").longOpt("help").desc("help").hasArg(false).required(false).build());

        return options;
    }

    @Override
    public void setBeanName(String beanName) {
        this.name = beanName;
    }
}

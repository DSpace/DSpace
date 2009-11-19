/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.config;

import org.dspace.kernel.Activator;

/**
 * This represents a single config setting for a DSpace instance
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceConfig {

    public final static String ACTIVATOR_PREFIX = "activator.";
    public final static String ACTIVATOR_CLASS_PREFIX = ACTIVATOR_PREFIX + "class.";

    private String key;
    private String value;

    private String beanProperty;
    private String beanName;

    private boolean activator = false;
    private boolean activatorClass = false;
    private String activatorClassName;
    private String activatorName;
    private String activatorAutowire;

    public DSpaceConfig(String key, String value) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("Failure with config creation, key is empty or null");
        }
        this.key = key.trim();
        this.value = value;
        // extract the property and class if possible
        int atLoc = key.indexOf('@');
        if (atLoc > 0) {
            try {
                this.beanProperty = key.substring(0, atLoc);
                this.beanName = key.substring(atLoc + 1);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid bean key ("+key+"): could not parse key (should be like param@org.dspace.BeanName): " + e.getMessage(), e);
            }
        }
        // extract the activator info if possible
        if (this.key.startsWith(ACTIVATOR_CLASS_PREFIX)) {
            if (this.value == null || this.value.length() <= 2) {
                throw new IllegalArgumentException("Invalid configuration value for key ("+this.key
                        +"), must not be blank or null and must be set to a class activator string (e.g. org.dspace.MyClass;org.dspace.MyServiceName)");
            }
            this.activatorClass = true;
            if (this.value.indexOf(';') == -1) {
                this.activatorClassName = this.value.trim();
            } else {
                // split it
                String[] parts = this.value.trim().split(";");
                this.activatorClassName = parts[0].trim();
                if (parts.length > 1) {
                    this.activatorName = parts[1].trim();
                }
                if (parts.length > 2) {
                    this.activatorAutowire = parts[2].trim().toLowerCase();
                }
            }
            if (this.activatorName == null) {
                this.activatorName = this.activatorClassName;
            }
            if (this.activatorAutowire == null) {
                this.activatorAutowire = "auto";
            }
        } else if (this.key.startsWith(ACTIVATOR_PREFIX)) {
            if (this.value == null || this.value.length() <= 2) {
                throw new IllegalArgumentException("Invalid configuration value for key ("+this.key
                        +"), must not be blank or null and must be set to an activator class (e.g. org.dspace.MyActivator)");
            }
            this.activator = true;
            this.activatorClassName = this.value.trim();
        }

    }
    
    /**
     * gets the part before the @ in a config line
     * @return the bean property if there is one OR null if this is not a bean config
     */
    public String getBeanProperty() {
        return beanProperty;
    }
    
    /**
     * get the part after the @ in a config line
     * @return the bean name which the property goes with OR null if there is none
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Indicates that this configuration is for an activator (a class that implements {@link Activator}),
     * @return true if this config is for an activator OR false if not
     */
    public boolean isActivator() {
        return activator;
    }

    /**
     * Indicates this is a config for an activator class (a class which will be started
     * as a service during the service manager startup)
     * @return true if this is a config for an activator class OR false if not
     */
    public boolean isActivatorClass() {
        return activatorClass;
    }

    /**
     * get the classname of the activator defined by the activator string if there is one,
     * this will be the activator class or the class which should be activated,
     * check the {@link #isActivator()} and {@link #isActivatorClass()} booleans to see which
     * @return the activator class name OR null if this is not an activator config
     */
    public String getActivatorClassName() {
        return activatorClassName;
    }

    /**
     * get the name to use for this activator (as defined by the activator string),
     * this will always be set if the {@link #activatorClassName} is set
     * @return the activator name OR null if this is not an activator config
     */
    public String getActivatorName() {
        return activatorName;
    }

    /**
     * get the activator autowire string: <br/>
     * auto - determine which type of autowiring automatically <br/>
     * constructor - autowire the constructor <br/>
     * setter - autowire the setters by type <br/>
     * none - disable any autowiring (this will only start up the class using the default constructor) <br/>
     * @return the autowiring setting (auto/constructor/setter/none) OR null if this is not an activator config
     */
    public String getActivatorAutowire() {
        return activatorAutowire;
    }

    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }

    protected void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
       if (null == obj)
          return false;
       if (!(obj instanceof DSpaceConfig))
          return false;
       else {
           DSpaceConfig castObj = (DSpaceConfig) obj;
           if (this.key == null || this.value == null) {
               return false;
           } else {
               return (this.key.equals(castObj.key) &&
                       this.value.equals(castObj.value));
           }
       }
    }

    @Override
    public int hashCode() {
       if (null == key)
          return super.hashCode();
       return key.hashCode() + value.hashCode();
    }

    @Override
    public String toString() {
        return (beanName == null ? key : beanName+"("+beanProperty+")") + " => " + value;
    }

    /**
     * Get the bean name from a configuration key if it contains one
     * @param key a config key
     * @return the bean name if there is one OR null if none
     */
    public static String getBeanName(String key) {
        // extract the property and class if possible
        String name = null;
        int atLoc = key.indexOf('@');
        if (atLoc > 0) {
            try {
                //property = key.substring(0, atLoc);
                name = key.substring(atLoc + 1);
            } catch (RuntimeException e) {
                name = null;
            }
        }
        return name;
    }

    /**
     * Get the bean property from a configuration key if it contains one
     * @param key a config key
     * @return the bean property if there is one OR null if none
     */
    public static String getBeanProperty(String key) {
        // extract the property and class if possible
        String property = null;
        int atLoc = key.indexOf('@');
        if (atLoc > 0) {
            try {
                property = key.substring(0, atLoc);
                //name = key.substring(atLoc + 1);
            } catch (RuntimeException e) {
                property = null;
            }
        }
        return property;
    }

}

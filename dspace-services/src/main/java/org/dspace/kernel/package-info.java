/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * The DSpace Services kernel and service manager.
 * <p>
 * The {@link DSpaceKernel kernel} provides an MBean, and locators for ServiceManager and
 * ConfigurationService.  Instantiating a kernel causes the DSpace configuration
 * to be loaded into the ConfigurationService and the initialization of a
 * ServiceManager.
 * </p>
 * <p>
 * The {@link ServiceManager} loads and provides locator methods for <em>services</em>.
 * "Singleton" services are simple dependencies, for which an implementation may
 * be configured.  Other types of services are grouped into listener chains by an
 * interface that they share.
 * </p>
 */

package org.dspace.kernel;

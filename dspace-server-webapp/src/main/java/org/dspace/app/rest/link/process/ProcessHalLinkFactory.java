/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.process;

import org.dspace.app.rest.ProcessRestController;
import org.dspace.app.rest.link.HalLinkFactory;

/**
 * This factory provides a means to add links to the Process REST functionality. This class will hold functions
 * to build links that will be usable for all Process Resources
 */
public abstract class ProcessHalLinkFactory<T> extends HalLinkFactory<T, ProcessRestController> {
}
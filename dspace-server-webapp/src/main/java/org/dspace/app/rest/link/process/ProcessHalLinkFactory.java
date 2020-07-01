/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.process;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.link.HalLinkFactory;

/**
 * This abstract class offers an easily extendable HalLinkFactory class to use methods on the RestResourceController
 * and make it more easy to read or define which methods should be found in the getMethodOn methods when building links
 * @param <T>   This parameter should be of type {@link org.dspace.app.rest.model.hateoas.HALResource}
 */
public abstract class ProcessHalLinkFactory<T> extends HalLinkFactory<T, RestResourceController> {
}
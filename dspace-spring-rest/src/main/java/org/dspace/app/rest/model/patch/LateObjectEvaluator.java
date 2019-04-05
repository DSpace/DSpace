/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.patch;


/**
 * <p>
 * Strategy interface for resolving values from an operation definition.
 * </p>
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public interface LateObjectEvaluator {

    <T> Object evaluate(Class<T> type);
}

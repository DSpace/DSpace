/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation type for CurationTasks. A task is distributive if it
 * distributes its performance to the component parts of it's target object.
 * This usually implies container iteration.
 * 
 * @author richardrodgers
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Distributive
{
}

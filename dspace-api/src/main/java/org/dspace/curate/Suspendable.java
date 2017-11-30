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
 * Annotation type for CurationTasks. A task is suspendable if it may
 * be suspended (halted) when a condition detected by the curation framework
 * occurs. The current implementation monitors and uses the status code
 * returned from the task to determine suspension, together with the
 * 'invocation mode' - optionally set by the caller on the curation object.
 * Thus, it effectively means that if a task is iterating over a collection,
 * the first error, or failure will halt the process.
 * This ensures that the status code and result of the failure are preserved.
 * 
 * @author richardrodgers
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Suspendable
{    
    // by default, suspension occurs however task is invoked
    Curator.Invoked invoked() default Curator.Invoked.ANY;
    // by default, either ERROR or FAILURE status codes trigger suspension
    int[] statusCodes() default {-1, 1};
}

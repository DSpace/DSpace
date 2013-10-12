/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.List;

import org.dspace.submit.util.SubmissionLookupPublication;

public interface EnhancerSubmissionLookup {

	List<String> getValues(
			SubmissionLookupPublication publication);

}

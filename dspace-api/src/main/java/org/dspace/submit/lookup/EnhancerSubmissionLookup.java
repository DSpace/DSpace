package org.dspace.submit.lookup;

import java.util.List;

import org.dspace.submit.util.SubmissionLookupPublication;

public interface EnhancerSubmissionLookup {

	List<String> getValues(
			SubmissionLookupPublication publication);

}

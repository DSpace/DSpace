package org.dspace.submit.lookup;

import org.dspace.submit.util.SubmissionLookupPublication;


public abstract class ConfigurableLookupProvider implements SubmissionLookupProvider {
	protected SubmissionLookupPublication convert(Object bean) {
		try {
			return SubmissionLookupUtils.convert(getShortName(), bean);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}

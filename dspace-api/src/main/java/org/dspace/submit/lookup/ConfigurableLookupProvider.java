/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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

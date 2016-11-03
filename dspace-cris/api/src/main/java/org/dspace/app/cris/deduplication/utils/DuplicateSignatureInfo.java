/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

public class DuplicateSignatureInfo extends DuplicateInfo {

    public DuplicateSignatureInfo(String type) {
        setSignatureId(type);        
    }

	public DuplicateSignatureInfo(String type,
			String signature) {
		setSignatureId(type);
		setSignature(signature);
	}


}

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

package org.dspace.app.cris.deduplication.utils;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.DSpaceObject;

public abstract class DuplicateInfo {
	private String signatureId;
	private List<DSpaceObject> items;
	private String signature;
	private List<String> otherSignature;
	
	public int getNumItems() {
		return items.size();
	}

	public List<DSpaceObject> getItems() {
		if(items==null) {
			items = new ArrayList<DSpaceObject>();
		}
		return items;
	}

	public String getSignature() {
		return signature;
	}

	public String getSignatureId() {
		return signatureId;
	}

	public void setSignatureId(String signatureId) {
		this.signatureId = signatureId;
	}

	public void setItems(List<DSpaceObject> items) {
		this.items = items;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

    public List<String> getOtherSignature()
    {
        if(this.otherSignature==null) {
            this.otherSignature = new ArrayList<String>();
        }
        return otherSignature;
    }

    public void setOtherSignature(List<String> otherSignature)
    {
        this.otherSignature = otherSignature;
    }
}

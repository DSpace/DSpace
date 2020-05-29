/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;

public abstract class DuplicateInfo {
    private String signatureId;
    private List<Item> items;
    private String signature;
    private List<String> otherSignature;

    public int getNumItems() {
        return items.size();
    }

    public List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<Item>();
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

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getOtherSignature() {
        if (this.otherSignature == null) {
            this.otherSignature = new ArrayList<String>();
        }
        return otherSignature;
    }

    public void setOtherSignature(List<String> otherSignature) {
        this.otherSignature = otherSignature;
    }
}

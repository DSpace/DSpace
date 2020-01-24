/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

import org.dspace.app.deduplication.service.impl.SolrDedupServiceImpl.DeduplicationFlag;

public class DuplicateDecisionObjectRest {

	String value;

	DuplicateDecisionType type;

	String note;

	public DuplicateDecisionValue getValue() {
		return (value != null) ? DuplicateDecisionValue.fromString(value) : null;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DuplicateDecisionType getType() {
		return type;
	}

	public void setType(DuplicateDecisionType type) {
		this.type = type;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public DeduplicationFlag getDecisionFlag() {
		DeduplicationFlag flag = DeduplicationFlag.MATCH;
		if (getValue() != null) {
			switch (getValue()) {
			case REJECT:
				flag = getRejectDecisionFlagByType(getType());
				break;
			case VERIFY:
				flag = getVerifyDecisionFlagByType(getType());
				break;

			}
		}
		return flag;
	}

	private DeduplicationFlag getRejectDecisionFlagByType(DuplicateDecisionType type) {
		DeduplicationFlag flag = null;
		switch (getType()) {
		case ADMIN:
			flag = DeduplicationFlag.REJECTADMIN;
			break;
		case WORKSPACE:
			flag = DeduplicationFlag.REJECTWS;
			break;
		case WORKFLOW:
			flag = DeduplicationFlag.REJECTWF;
			break;
		}

		return flag;
	}

	private DeduplicationFlag getVerifyDecisionFlagByType(DuplicateDecisionType type) {
		DeduplicationFlag flag = null;
		switch (getType()) {
		case ADMIN:
			flag = null;
			break;
		case WORKSPACE:
			flag = DeduplicationFlag.VERIFYWS;
			break;
		case WORKFLOW:
			flag = DeduplicationFlag.VERIFYWF;
			break;
		}

		return flag;
	}

}
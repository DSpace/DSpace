/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * The Bitstream REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class BitstreamRest extends DSpaceObjectRest {
	public static final String PLURAL_NAME = "bitstreams";
	public static final String NAME = "bitstream";
	public static final String CATEGORY = RestModel.CORE;
	private String bundleName;

	// avoid to serialize this object inline as we want a full resource embedded
	// TODO extends this annotation to provide information about lazy loading
	// and projection behavior
	@JsonProperty(access = Access.WRITE_ONLY)
	private BitstreamFormatRest format;
	private Long sizeBytes;
	private CheckSumRest checkSum;
	private Integer sequenceId;

	public String getBundleName() {
		return bundleName;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public BitstreamFormatRest getFormat() {
		return format;
	}

	public void setFormat(BitstreamFormatRest format) {
		this.format = format;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public CheckSumRest getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(CheckSumRest checkSum) {
		this.checkSum = checkSum;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
}
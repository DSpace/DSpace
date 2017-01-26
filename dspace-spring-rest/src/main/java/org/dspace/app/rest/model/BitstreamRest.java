/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

public class BitstreamRest extends DSpaceRestObject {
	public static final String NAME = "bitstream";
	private String bundleName;
	private BitstreamFormatRest format;
	private Long sizeBytes;
	private CheckSum checkSum;
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

	public CheckSum getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(CheckSum checkSum) {
		this.checkSum = checkSum;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

}
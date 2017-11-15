package org.dspace.app.rest.model.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.model.AccessConditionRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataValueRest;

public class UploadBitstreamRest extends UploadStatusResponse {
	
	private UUID uuid;
	private Map<String, List<MetadataValueRest>> metadata = new HashMap<>();
	private List<AccessConditionRest> accessConditions;
	private Long sizeBytes;
	private CheckSumRest checkSum;
	private String url;
	
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public Map<String, List<MetadataValueRest>> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, List<MetadataValueRest>> metadata) {
		this.metadata = metadata;
	}

	public List<AccessConditionRest> getAccessConditions() {
		if(accessConditions==null) {
			accessConditions = new ArrayList<>();
		}
		return accessConditions;
	}

	public void setAccessConditions(List<AccessConditionRest> accessConditions) {
		this.accessConditions = accessConditions;
	}
}
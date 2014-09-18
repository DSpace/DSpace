package org.dspace.folderimport.dto;

import java.io.File;
import java.util.Map;

/**
 * 
 * @author Márcio Ribeiro Gurgel do Amaral
 *
 */
public class FolderAnalyseResult {

	private Map<Long, String> userReadble;
	private Map<Long, File> serverReadble;
	private Map<Long, Long> mappingParent;
	private boolean allImportsAreFinished;
	
	
	public FolderAnalyseResult(Map<Long, String> userReadble,
			Map<Long, File> serverReadble, Map<Long, Long> mappingParent) {
		super();
		this.userReadble = userReadble;
		this.serverReadble = serverReadble;
		this.mappingParent = mappingParent;
	}
	
	public Map<Long, String> getUserReadble() {
		return userReadble;
	}
	public Map<Long, File> getServerReadble() {
		return serverReadble;
	}

	public Map<Long, Long> getMappingParent() {
		return mappingParent;
	}

	public boolean isAllImportsAreFinished() {
		return allImportsAreFinished;
	}

	public void setAllImportsAreFinished(boolean allImportsAreFinished) {
		this.allImportsAreFinished = allImportsAreFinished;
	}
	
	
}

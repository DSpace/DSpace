package org.dspace.folderimport.exception;

/**
 * 
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class FolderMetadataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String messageKey;

	public FolderMetadataException(String messageKey) {
		this.messageKey = messageKey;
	}
	
	public String getMessage() {
		return messageKey;
	}

}

package org.dspace.export;

/**
 * DTO used to store the generated file plus the suggested file name
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class ItemExportDTO 
{

	private String fileContent;
	private String fileName;
	
	public ItemExportDTO(String fileContent, String fileName) {
		super();
		this.fileContent = fileContent;
		this.fileName = fileName;
	}
	
	public String getFileContent() {
		return fileContent;
	}
	public String getFileName() {
		return fileName;
	}
	
}

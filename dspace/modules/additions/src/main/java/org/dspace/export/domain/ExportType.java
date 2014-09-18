package org.dspace.export.domain;

/**
 * Register the available options for item export.
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public enum ExportType 
{

	BIBTEX("item-export/BibTeX.vm", ""), 
	ENDNOTE("/item-export/endnote.vm", "enw"),
	CITATION("/item-export/input-forms-citation.vm", "");

	
	private String fileLocation;
	private String format;

	private ExportType(String fileLocation, String format) {
		this.fileLocation = fileLocation;
		this.format = format;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public String getFormat() {
		return format;
	}
	
}

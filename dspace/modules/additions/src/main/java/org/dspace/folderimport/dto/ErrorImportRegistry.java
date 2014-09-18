package org.dspace.folderimport.dto;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dspace.folderimport.domain.ImportErrorType;


/**
 * DTO para registro de erros de importação
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class ErrorImportRegistry implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String title;
	private Map<Long, File> itemFiles;
	private List<String> errorsDescription;
	private ImportErrorType importErrorType;
	private Long internalIdentifer;

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Map<Long, File> getItemFiles() {
		return itemFiles;
	}
	public void setItemFiles(Map<Long, File> itemFiles) {
		this.itemFiles = itemFiles;
	}
	public List<String> getErrorsDescription() {
		return errorsDescription;
	}
	public void setErrorsDescription(List<String> errorsDescription) {
		this.errorsDescription = errorsDescription;
	}
	public ImportErrorType getImportErrorType() {
		return importErrorType;
	}
	public void setImportErrorType(ImportErrorType importErrorType) {
		this.importErrorType = importErrorType;
	}
	public Long getInternalIdentifer() {
		return internalIdentifer;
	}
	public void setInternalIdentifer(Long internalIdentifer) {
		this.internalIdentifer = internalIdentifer;
	}
	
}

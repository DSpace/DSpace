package org.dspace.folderimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.dspace.app.itemupdate.DtoMetadata;
import org.dspace.app.itemupdate.MetadataUtilities;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.file.utils.FileUtils;
import org.dspace.folderimport.constants.FolderMetadataImportConstants;
import org.dspace.folderimport.domain.ImportErrorType;
import org.dspace.folderimport.dto.ErrorImportRegistry;

import com.ibm.icu.text.MessageFormat;

/**
 * Utilidade que visam tornar visíveis itens que por motivo de erro não foram importados na funcionalidade <b>FolderMetadataImport</b>
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class ImportErrorsReader {

	private static final int IMPORT_INDEX_FOLDER_LOCATION = 0;
	private static final int IMPORT_INDEX_ERROR_DESCRIPTION = 1;
	private static final int POSITION_WITH_MESSAGE = 1;
	private static final int EXPECTED_LOG_LENGTH_AFTER_SPLIT = 1;
	private static final String LOG_ERROR_DELIMITER = "@";
	private static final int NUMBER_OF_PARENT_DIR_TO_REACH = 1;
	private static Logger logger = Logger.getLogger(ImportErrorsReader.class);
	private static DocumentBuilder builder;
	
	/**
	 * Constrói DTO representativo de itens (e seus arquivos) que sofreram erro no processo de importação.
	 * @param currentRootExportFolder Root selected directory
	 * @return DTO preenchido com informações de itens importados com erro
	 * @throws IOException
	 */
	public static Map<Long, ErrorImportRegistry> listErrorsImport(File currentRootExportFolder) throws IOException
	{
		Map<Long, ErrorImportRegistry> analisysResult = new LinkedHashMap<Long, ErrorImportRegistry>();
		
		File exportErrorFolder = new File(currentRootExportFolder.getParent(), ConfigurationManager.getProperty("foldermetadataimport.directory.error"));
		if(exportErrorFolder.exists())
		{
			List<File> searchRecursiveAddingDirs = FileUtils.searchRecursiveAddingDirs(exportErrorFolder, "logfile", NUMBER_OF_PARENT_DIR_TO_REACH);
			fillItemData(analisysResult, searchRecursiveAddingDirs, ImportErrorType.EXPORT, null);
		}
		
		searchImportErrors(currentRootExportFolder, analisysResult);
		
		return analisysResult;
	}


	/**
	 * Recover import associated errors
	 * @param currentRootExportFolder Selected export folder
	 * @param analisysResult Map to register values
	 * @throws IOException
	 */
	private static void  searchImportErrors(File currentRootExportFolder, Map<Long, ErrorImportRegistry> analisysResult)
			throws IOException {
		
		File importFolder = new File(FileUtils.getImportFolderName());

		/** O diretório de importação não é nativo do DSpace, logo se faz necessária sua criação **/
		if(!importFolder.exists())
		{
			importFolder.mkdirs();
		}
			
		/** Recupera arquivos de registro de erros **/
		List<File> foundFiles = FileUtils.searchFileNoDepthListReturn(importFolder, FolderMetadataImportConstants.FOLDERIMPORT_ERROR_MAPPING_FILE_SUFFIX);
		
		if(!foundFiles.isEmpty())
		{
			List<File> foldersContainingErrorItems = new ArrayList<File>();
			Map<File, String> folderToException = new HashMap<File, String>();
			
			/** Cada arquivo "fundFiles" contém referências as pastas contenedoras de itens com problema de importação **/
			for(File foundFile : foundFiles)
			{
				List<String> linesInsidefile = FileUtils.readFile(foundFile, Integer.MAX_VALUE);
				
				for(String folderCandidate : linesInsidefile)
				{
					/** Only adds the line if its contents (file location) belongs to currentRootExportFolder **/
					if(folderCandidate.startsWith(currentRootExportFolder.getCanonicalPath()))
					{
						String[] splitedErrorDescription = folderCandidate.split(LOG_ERROR_DELIMITER);
						if(splitedErrorDescription.length > EXPECTED_LOG_LENGTH_AFTER_SPLIT)
						{
							String folderName = splitedErrorDescription[IMPORT_INDEX_FOLDER_LOCATION];
							File folderFile = new File(folderName);
							foldersContainingErrorItems.add(folderFile);
							folderToException.put(folderFile, splitedErrorDescription[IMPORT_INDEX_ERROR_DESCRIPTION]);
						}
					}
				}
			}
			
			/** Devem existir registros nos arquivos **/
			if(!foldersContainingErrorItems.isEmpty())
			{
				fillItemData(analisysResult, foldersContainingErrorItems, ImportErrorType.IMPORT, folderToException);
			}
		
		}
	}


	/**
	 * Common method to be used by: import error or export error.
	 * @param analisysResult Map to register values
	 * @param foldersContainingErrorItems Folders wich has items with error
	 * @param errorType Type of error (import/export)
	 * @param folderToException Used only for "error import": for a given folder location (key "file") there's a error description (value "error description")
	 */
	private static void fillItemData(Map<Long, ErrorImportRegistry> analisysResult, List<File> foldersContainingErrorItems, ImportErrorType errorType, Map<File, String> folderToException) {
		Set<Long> identifiersForFolders = new HashSet<Long>();
		
		for(File folderItemContainer : foldersContainingErrorItems)
		{
			/** Somente diretórios armazenam conjunto de arquivos para exportação **/
			if(folderItemContainer.exists() && folderItemContainer.isDirectory())
			{
				try 
				{
					/** Recupera título **/
					File dublinCoreXmlFile = new File(folderItemContainer, FolderMetadataImportConstants.DUBLIN_CORE_XML);
					
					String title = readTitleFromXML(dublinCoreXmlFile);
					
					ErrorImportRegistry errorImportRegistry = new ErrorImportRegistry();
					errorImportRegistry.setImportErrorType(errorType);
					errorImportRegistry.setTitle(title);
					
					ArrayList<String> errorsDescription = new ArrayList<String>();
					if(errorType.equals(ImportErrorType.IMPORT) && folderToException != null && folderToException.containsKey(folderItemContainer))
					{
						errorImportRegistry.setErrorsDescription(errorsDescription);
						errorsDescription.add(folderToException.get(folderItemContainer));
					}
					
					LinkedHashMap<Long, File> itemRegistry = new LinkedHashMap<Long, File>();
					
					Set<Long> identifiersForFiles = new HashSet<Long>();

					
					for(File itemContent : folderItemContainer.listFiles())
					{
						if(itemContent.isFile())
						{
							if(!itemContent.getName().equals("logfile"))
							{
								long unusedKey = FileUtils.garanteeUnusedKey(identifiersForFiles);
								itemRegistry.put(unusedKey, itemContent);
							}
							else
							{
								/** Read content file, to get info about the error **/
								List<String> readFile = FileUtils.readFile(itemContent, Integer.MAX_VALUE);
								for(String fileContent : readFile)
								{
									if(fileContent != null && !fileContent.isEmpty())
									{
										String[] splitedLog = fileContent.split(LOG_ERROR_DELIMITER);
										
										if(splitedLog.length > EXPECTED_LOG_LENGTH_AFTER_SPLIT)
										{
											/** Lazy init **/
											if(errorImportRegistry.getErrorsDescription() == null)
											{
												errorImportRegistry.setErrorsDescription(errorsDescription);
											}
											
											errorImportRegistry.getErrorsDescription().add(splitedLog[POSITION_WITH_MESSAGE]);
										}
										else
										{
											logger.warn(MessageFormat.format("O arquivo {0} possui registro fora do padrão: {1}", itemContent.getCanonicalPath(), fileContent));
										}
									}
								}
							}
						}
						
					}
					errorImportRegistry.setItemFiles(itemRegistry);
					long garanteeUnusedKey = FileUtils.garanteeUnusedKey(identifiersForFolders);
					errorImportRegistry.setInternalIdentifer(garanteeUnusedKey);
					analisysResult.put(garanteeUnusedKey, errorImportRegistry);
					
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				
			}
		}
	}


	/**
	 * Recovers title from <b>input-forms.xml</b>
	 * @param dublinCoreXmlFile File to be readed
	 * @param loadDublinCore 
	 * @return
	 * @throws IOException
	 */
	private static String readTitleFromXML(File dublinCoreXmlFile) throws IOException {
		
		String title = null;
		
		try
		{
			List<DtoMetadata> loadDublinCore = MetadataUtilities.loadDublinCore(getDocumentBuilder(), new FileInputStream(dublinCoreXmlFile));
			title = searchMetadata(loadDublinCore, "dc.title");
		}
		catch(Exception e)
		{
			title = I18nUtil.getMessage("jsp.dspace-admin.foldermetadataerror.notitle");
			logger.error(MessageFormat.format("Ocorreu um erro no ato de processamento do arquivo \"{0}\".", dublinCoreXmlFile.getCanonicalPath()), e);
		}
		
		return title;
	}
	
	
	/**
	 * Itera sob a lista de metadados recebidos, na intenção de encontrar a 
	 * ocorrência de metadado informada por parâmetro.
	 * @param registries Lista contenedora de metadados de um item
	 * @param metadata Metadado a ser recuperado
	 * @return Valor do metadado encontrado
	 */
	private static String searchMetadata(List<DtoMetadata> registries, String metadata) {
		
		for(DtoMetadata currentDto : registries)
		{
			if(currentDto.matches(metadata, false))
			{
				return currentDto.getValue();
			}
		}
		
		return null;
	}


	private static DocumentBuilder getDocumentBuilder()
	throws ParserConfigurationException
	{
		if (builder == null)
		{
		    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		return builder;
	}
	
	
}

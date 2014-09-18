package org.dspace.export;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.I18nUtil;
import org.dspace.export.domain.ExportType;

/**
 * Get item's metadata and transforms it to a file.<br>
 * It uses <i>Apache Velocity</i> for file conversion. 
 * The template files can be found at: <b>src/main/resources/item-export</b><br>
 * Currently the supported formats are (also registered in {@link ExportType}:
 * <ul>
 * 	<li>Endnote</li>
 * 	<li>BibTex</li>
 * </ul>
 * 
 * 
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
@SuppressWarnings("deprecation")
public class ItemExportFormats 
{
	
    private static Logger log = Logger.getLogger(ItemExportFormats.class);
	private static final int ZERO_CHARACTER = 0;
	private static final int YEAR_LENGTH = 4;
	private static final int YEARH_LENGTH_COMPARATOR = 3;
    private MessageDigest messageDigest;
	
    /**
     * Initializes {@link #messageDigest}, using <i>MD5</i> hash
     */
	private void initMessageDigest() {
		try 
        {
        	messageDigest = MessageDigest.getInstance("MD5");
        } 
        catch (NoSuchAlgorithmException e) 
        {
        	log.error(e.getMessage(), e);
        }
	}

	/**
	 * Convert instance of {@link Item} on a representative String
	 * @param item Item to be exported
	 * @param exportType Type or export (endnote, bibtext, etc...)
	 */
	public ItemExportDTO process(Item item, ExportType exportType)
	{
		if(messageDigest == null)
		{
			initMessageDigest();
		}
		
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
		velocityEngine.init();
		
		Template template = velocityEngine.getTemplate(exportType.getFileLocation());
		VelocityContext context = new VelocityContext();
		
		Map<String, String> itemMetadata = new LinkedHashMap<String, String>();
		
		for(DCValue currentMetadata : item.getMetadata("dc", Item.ANY, Item.ANY, Item.ANY))
		{
			String value = null;
			
			/** Must have only "year" for date.issued **/
			if(currentMetadata.getField().contains("dc.date"))
			{
				value = currentMetadata.value != null && currentMetadata.value.length() > 
					YEARH_LENGTH_COMPARATOR ? currentMetadata.value.substring(ZERO_CHARACTER, YEAR_LENGTH) : "";
			}
			else
			{
				value = currentMetadata.value != null ? currentMetadata.value.replaceAll("\n", " ") : "";
			}
			itemMetadata.put(currentMetadata.getField(), value);
		}
		
		context.put("itemMetadata", itemMetadata);
		String generatedIdentifier = generateIdentifier(itemMetadata);
		context.put("generatedId", generatedIdentifier);
		
		/** Citation has specific values **/
		if(exportType.equals(ExportType.CITATION))
		{
			context.put("pageNumber", I18nUtil.getMessage("jsp.submit.dc.identifier.citation.variable.pagenumber"));
			context.put("place", I18nUtil.getMessage("jsp.submit.dc.identifier.citation.variable.place"));
		}
		
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		
		return new ItemExportDTO(stringWriter.toString(), generatedIdentifier);
	}
	
	
	/**
	 * Generates a identifer for a given item, using the following pattern:
	 * <code>
	 * 	LASTNAME:YEAR:(HASH FROM TITLE)
	 * </code>
	 * @param itemMetadata Metadata from item
	 * @return Generated hash
	 */
    private String generateIdentifier(Map<String, String> itemMetadata) 
    {
    	StringBuilder identifierBuilder = new StringBuilder();
    	String author = itemMetadata.get("dc.contributor.author");
    	String separator = "";
    	
    	if(author != null)
    	{
    		String[] authorSplited = author.split(",");
			if(authorSplited.length > 0)
    		{
    			identifierBuilder.append(authorSplited[0]);
    			separator = ":";
    		}
    	}
    	
    	String yearIssued = itemMetadata.get("dc.date.issued");
    	if(yearIssued != null && !yearIssued.isEmpty())
    	{
    		identifierBuilder.append(separator);
    		identifierBuilder.append(yearIssued);
    		separator = ":";
    	}
    	
    	String title = itemMetadata.get("dc.title");
    	if(title != null && !title.isEmpty())
    	{
    		identifierBuilder.append(separator);
    		identifierBuilder.append(generateHashFromString(title));
    		separator = ":";
    	}
    	
		return identifierBuilder.toString();
	}

	/**
     * Generate a {@link Long} using {@link #messageDigest} plus a String value
     * @param value
     * @return Long value (converted to String)
     */
    private Integer generateHashFromString(String value) 
	{
		ByteBuffer byteBuffer = ByteBuffer.wrap(messageDigest.digest(value.getBytes()));
		int generatedValue = byteBuffer.getInt();
		/** Positive numbers **/
		return generatedValue < 0 ? (generatedValue * -1) : generatedValue;
	}
	
}

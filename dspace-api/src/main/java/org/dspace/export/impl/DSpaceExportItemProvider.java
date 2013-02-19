/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.export.api.ExportItemException;
import org.dspace.export.api.ExportItemProvider;
import org.dspace.util.ItemUtils;

import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.util.MarshallingUtils;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * Generic export provider based on XSL transformation.
 * Information comes has XOAI metadata.
 * See documentation for more information.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public class DSpaceExportItemProvider implements ExportItemProvider {
	private static final String DEFAULT_IMAGE_EXTENSION = ".png";
	private static Map<String, DSpaceExportItemProvider> providers = null;
	private static TransformerFactory tFactory = TransformerFactory.newInstance();
	
	public static DSpaceExportItemProvider getInstance (String id) {
		if (providers == null) providers = new TreeMap<String, DSpaceExportItemProvider>();
		if (!providers.containsKey(id)) {
			String xslt = ConfigurationManager.getProperty("export", id+".xslt");
			if (xslt != null) {
				String image = ConfigurationManager.getProperty("export", id+".image");
				if (image == null) image = id+DEFAULT_IMAGE_EXTENSION;
				String contentType = ConfigurationManager.getProperty("export", id+".mimeType");
				String fileExtension = ConfigurationManager.getProperty("export", id+".extension");
				String dir = ConfigurationManager.getProperty("dspace.dir");
				providers.put(id, new DSpaceExportItemProvider(new File(dir, xslt), id, contentType, fileExtension, image));
			}
		}
		return providers.get(id);
	}
		
	private File xslt;
	private String id;
	private String image;
	private String contentType;
	private String fileExtension;
	
	public DSpaceExportItemProvider(File xslt, String id, String contentType, String fileExtension, String image) {
		super();
		this.xslt = xslt;
		this.id = id;
		this.image = image;
		this.contentType = contentType;
		this.fileExtension = fileExtension;
	}

	@Override
	public File getXSLT() {
		return xslt;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getImage() {
		return image;
	}

	@Override
	public void export(Item item, OutputStream output) throws ExportItemException {
		Metadata m = ItemUtils.retrieveMetadata(item);
		try {
			if (this.getXSLT() == null || !this.getXSLT().exists())
				throw new ExportItemException("Invalid stylesheet for Export Provider "+this.getId());
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MarshallingUtils.writeMetadata(out, m);
			Transformer transformer = tFactory
					.newTransformer(new StreamSource(this.getXSLT()));
			
			transformer.transform(new StreamSource(new ByteArrayInputStream(out.toByteArray())),
					new StreamResult(output));
			
		} catch (MetadataBindException e) {
			throw new ExportItemException(e);
		} catch (TransformerConfigurationException e) {
			throw new ExportItemException(e);
		} catch (TransformerException e) {
			throw new ExportItemException(e);
		}
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getFileExtension() {
		return fileExtension;
	}

}

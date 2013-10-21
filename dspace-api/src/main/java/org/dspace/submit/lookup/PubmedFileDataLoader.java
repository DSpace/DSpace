/**
 * 
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author kstamatis
 *
 */
public class PubmedFileDataLoader extends FileDataLoader {

		/**
		 * 
		 */
		public PubmedFileDataLoader() {
		}

		/**
		 * @param filename
		 */
		public PubmedFileDataLoader(String filename) {
			super(filename);
		}

		/* (non-Javadoc)
		 * @see gr.ekt.bte.core.DataLoader#getRecords()
		 */
		@Override
		public RecordSet getRecords() throws MalformedSourceException {
			
			RecordSet recordSet = new RecordSet();
			
			try {
				InputStream inputStream = new FileInputStream(new File(filename));
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				factory.setIgnoringElementContentWhitespace(true);

				DocumentBuilder builder = factory.newDocumentBuilder();
				Document inDoc = builder.parse(inputStream);

				Element xmlRoot = inDoc.getDocumentElement();
				List<Element> pubArticles = XMLUtils
				        .getElementList(xmlRoot, "PubmedArticle");

				for (Element xmlArticle : pubArticles)
				{
					Record pubmedItem = null;
					try {
						pubmedItem = PubmedUtils.convertCrossRefDomToRecord(xmlArticle);
						recordSet.addRecord(pubmedItem);
					} catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return recordSet;
			
		}

		/* (non-Javadoc)
		 * @see gr.ekt.bte.core.DataLoader#getRecords(gr.ekt.bte.core.DataLoadingSpec)
		 */
		@Override
		public RecordSet getRecords(DataLoadingSpec spec)
				throws MalformedSourceException {
			
			return getRecords();
		}

	}

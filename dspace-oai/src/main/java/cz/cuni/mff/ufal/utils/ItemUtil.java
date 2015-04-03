/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.utils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.dspace.app.util.DCInput;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class ItemUtil {

	/** log4j logger */
	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(ItemUtil.class);

	public static Node getUploadedMetadata(String handle){
		Node ret = null;
		Context context = null;
		try {
			context = new Context();

			DSpaceObject dso = HandleManager.resolveToObject(context, handle);

			if (dso != null && dso.getType() == Constants.ITEM && ((Item)dso).hasOwnMetadata()) {
				Bitstream bitstream = ((Item) dso).getBundles("METADATA")[0]
						.getBitstreams()[0];
				context.turnOffAuthorisationSystem();
				Reader reader = new InputStreamReader(bitstream.retrieve());
				context.restoreAuthSystemState();
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setNamespaceAware(true);
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new InputSource(reader));
					ret = doc.getDocumentElement();
				} finally {
					reader.close();
				}

			}
		} catch (Exception e) {
			log.error(e);
		} finally{
			closeContext(context);
		}
		return ret;
	}
	
	public static Node getFunding(String mdValue){
		String ns = "http://www.clarin.eu/cmd/";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element el = doc.createElementNS(ns, "funding");
			doc.appendChild(el);
			Element organization = doc.createElementNS(ns, "organization");
			Element projName = doc.createElementNS(ns, "projectName");
			Element code = doc.createElementNS(ns, "code");
			Element fundsType = doc.createElementNS(ns, "fundsType");

			String[] values = mdValue
					.split(DCInput.ComplexDefinition.SEPARATOR);

			// mind the order in input forms, org;code;projname;type
			Element[] elements = { organization, code, projName, fundsType };
			for (int i = 0; i < values.length; i++) {
				elements[i].appendChild(doc.createTextNode(values[i]));
				el.appendChild(elements[i]);
			}
			return doc.getDocumentElement();
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	public static Node getContact(String mdValue){
		String ns = "http://www.clarin.eu/cmd/";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element el = doc.createElementNS(ns, "contactPerson");
			doc.appendChild(el);
			Element first = doc.createElementNS(ns, "firstName");
			Element last = doc.createElementNS(ns, "lastName");
			Element email = doc.createElementNS(ns, "email");
			Element affil = doc.createElementNS(ns, "affiliation");

			String[] values = mdValue
					.split(DCInput.ComplexDefinition.SEPARATOR);

			Element[] elements = { first, last, email, affil };
			for (int i = 0; i < values.length; i++) {
				elements[i].appendChild(doc.createTextNode(values[i]));
				el.appendChild(elements[i]);
			}
			return doc.getDocumentElement();
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	public static Node getSize(String mdValue){
		String ns = "http://www.clarin.eu/cmd/";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element el = doc.createElementNS(ns, "size");
			doc.appendChild(el);
			Element size = doc.createElementNS(ns, "size");
			Element unit = doc.createElementNS(ns, "unit");

			String[] values = mdValue
					.split(DCInput.ComplexDefinition.SEPARATOR);

			Element[] elements = {size, unit};
			for (int i = 0; i < values.length; i++) {
				elements[i].appendChild(doc.createTextNode(values[i]));
				el.appendChild(elements[i]);
			}
			return doc.getDocumentElement();
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	public static Node getAuthor(String mdValue){
		String ns = "http://www.clarin.eu/cmd/";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element el = doc.createElementNS(ns, "author");
			doc.appendChild(el);
			Element last = doc.createElementNS(ns, "lastName");

			String[] values = mdValue
					.split(",",2);

			last.appendChild(doc.createTextNode(values[0]));
			el.appendChild(last);
			if(values.length>1){
                Element first = doc.createElementNS(ns, "firstName");
                first.appendChild(doc.createTextNode(values[1]));
                el.appendChild(first);
			}
			return doc.getDocumentElement();
		} catch (ParserConfigurationException e) {
			return null;
		}
	}
	
	private static void closeContext(Context c){
		if(c != null)
			c.abort();
	}

}

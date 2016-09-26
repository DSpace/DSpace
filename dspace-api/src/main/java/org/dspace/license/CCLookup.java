/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license; 

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import org.dspace.core.ConfigurationManager;


/**
 *  A wrapper around Creative Commons REST web services.
 *
 * @author Wendy Bossons
 */
public class CCLookup {

        /** log4j logger */
        private static Logger log = Logger.getLogger(CCLookup.class);

	private static String cc_root = ConfigurationManager.getProperty("cc.api.rooturl");
	private static String jurisdiction; 
	private static List<String> lcFilter = new ArrayList<String>();
	
	private Document license_doc        = null;
	private String rdfString            = null;
	private String errorMessage         = null;
	private boolean success             = false;

	private SAXBuilder parser           = new SAXBuilder();
	private List<CCLicense> licenses    = new ArrayList<CCLicense>();
	private List<CCLicenseField> licenseFields = new ArrayList<CCLicenseField>();
	
	static {
		String jurisProp = ConfigurationManager.getProperty("cc.license.jurisdiction");
		jurisdiction = (jurisProp != null) ? jurisProp : "";
		
		String filterList = ConfigurationManager.getProperty("cc.license.classfilter");
		if (filterList != null) {
			for (String name: filterList.split(",")) {
				lcFilter.add(name.trim());
			}
		}
	}

	/**
	 * Constructs a new instance with the default web services root.
	 *
	 */
	public CCLookup() {
		super();
	}

	/**
	 * Returns the id for a particular CCLicense label.  Returns an
	 * empty string if no match is found.
	 *
	 * @param class_label The CCLicense label to find.
	 * @return Returns a String containing the License class ID if the label
	 * 			is found; if not found, returns an empty string.
	 *
	 * @see CCLicense
	 *
	 */
	public String getLicenseId (String class_label) {
		for (int i = 0; i < this.licenses.size(); i++) {
			if ( ((CCLicense)this.licenses.get(i)).getLicenseName().equals(class_label)) {
				return ( (CCLicense)this.licenses.get(i) ).getLicenseId();
			}
		}

		return "";
	}

	/**
	 * Queries the web service for the available licenses.
	 *
	 * @param language The language to request labels and description strings in.
	 * @return Returns a Map of CCLicense objects.
	 *
	 * @see Map
	 * @see CCLicense
	 *
	 */
	public Collection<CCLicense> getLicenses(String language) {

		// create XPath expressions
		try {
			JDOMXPath xp_Licenses = new JDOMXPath("//licenses/license");
			JDOMXPath xp_LicenseID = new JDOMXPath("@id");
			URL classUrl = new URL(this.cc_root + "/?locale=" + language);
			Document classDoc = this.parser.build(classUrl);
			// extract the identifiers and labels using XPath
			List<Element> results = xp_Licenses.selectNodes(classDoc);
			// populate licenses container
			this.licenses.clear();
			for (int i = 0; i < results.size(); i++) {
				Element license = results.get(i);
				// add if not filtered
				String liD = ((Attribute)xp_LicenseID.selectSingleNode(license)).getValue();
				if (! lcFilter.contains(liD)) {
					this.licenses.add(new CCLicense(liD, license.getText(), i));
				}
			}
		} catch (JaxenException jaxen_e) {
			return null;
		} catch (JDOMException jdom_e) {
			return null;
		} catch (IOException io_e) {
			return null;
		} catch (Exception e) {
			// do nothing... but we should
			return null;
		}			

		return licenses;
	}


	/**
	 * Queries the web service for a set of licenseFields for a particular license class.
	 *
	 * @param license A String specifying the CCLicense identifier to
	 * 			retrieve fields for.
	 * @return A Collection of LicenseField objects.
	 *
	 * @see CCLicense
	 *
	 */
	public Collection<CCLicenseField> getLicenseFields(String license, String language) {

		JDOMXPath xp_LicenseField;
		JDOMXPath xp_LicenseID;
		JDOMXPath xp_FieldType;
		JDOMXPath xp_Description;
		JDOMXPath xp_Label;
		JDOMXPath xp_Enum;

		Document fieldDoc;

		URL classUrl;
		List results = null;
		List enumOptions = null;

		// create XPath expressions
		try {
			xp_LicenseField = new JDOMXPath("//field");
			xp_LicenseID = new JDOMXPath("@id");
			xp_Description = new JDOMXPath("description");
			xp_Label = new JDOMXPath("label");
			xp_FieldType = new JDOMXPath("type");
			xp_Enum = new JDOMXPath("enum");

		} catch (JaxenException e) {
			return null;
		}

		// retrieve and parse the license class document
		try {
			classUrl = new URL(this.cc_root + "/license/" + license + "?locale=" + language);
		} catch (Exception err) {
			// do nothing... but we should
			return null;
		}

		// parse the licenses document
		try {
			fieldDoc = this.parser.build(classUrl);
		} catch (JDOMException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

		// reset the field definition container
		this.licenseFields.clear();

		// extract the identifiers and labels using XPath
		try {
			results = xp_LicenseField.selectNodes(fieldDoc);
		} catch (JaxenException e) {
			return null;
		}

		for (int i=0; i < results.size(); i++) {
			Element field = (Element)results.get(i);

			try {
				// create the field object
				CCLicenseField cclicensefield = new CCLicenseField(((Attribute)xp_LicenseID.selectSingleNode(field)).getValue(),
						((Element)xp_Label.selectSingleNode(field)).getText() );

				// extract additional properties
				cclicensefield.setDescription( ((Element)xp_Description.selectSingleNode(field)).getText() );
				cclicensefield.setType( ((Element)xp_FieldType.selectSingleNode(field)).getText() );

				enumOptions = xp_Enum.selectNodes(field);

				for (int j = 0; j < enumOptions.size(); j++) {
					String id = ((Attribute)xp_LicenseID.selectSingleNode(enumOptions.get(j))).getValue();
					String label =((Element)xp_Label.selectSingleNode(enumOptions.get(j))).getText();

					cclicensefield.getEnum().put( id, label);

				} // for each enum option

				this.licenseFields.add(cclicensefield);
			} catch (JaxenException e) {
				return null;
			}
		}

		return licenseFields;
	} // licenseFields

	/**
	 * Passes a set of "answers" to the web service and retrieves a license.
	 *
	 * @param licenseId The identifier of the license class being requested.
	 * @param answers A Map containing the answers to the license fields;
	 * 			each key is the identifier of a LicenseField, with the value
	 * 			containing the user-supplied answer.
	 * @param lang The language to request localized elements in.
	 *
	 * @throws IOException
	 *
	 * @see CCLicense
	 * @see Map
	 */
	public void issue(String licenseId, Map answers, String lang)
		throws IOException{

		// Determine the issue URL
		String issueUrl = cc_root + "/license/" + licenseId + "/issue";
		// Assemble the "answers" document
		String answer_doc = "<answers>\n<locale>" + lang + "</locale>\n" + "<license-" + licenseId + ">\n";
		Iterator keys = answers.keySet().iterator();

		try {
			String current = (String)keys.next();

			while (true) {
				answer_doc += "<" + current + ">" + (String)answers.get(current) + "</" + current + ">\n";
				current = (String)keys.next();
			}


		} catch (NoSuchElementException e) {
			// exception indicates we've iterated through the
			// entire collection; just swallow and continue
		}
		// answer_doc +=	"<jurisdiction></jurisidiction>\n";  FAILS with jurisdiction argument
		answer_doc +=						"</license-" + licenseId + ">\n</answers>\n";
		String post_data;

		try {
			post_data = URLEncoder.encode("answers", "UTF-8") + "=" + URLEncoder.encode(answer_doc, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return;
		}

		URL post_url;
		try {
			post_url = new URL(issueUrl);
		} catch (MalformedURLException e) {
			return;
		}
		URLConnection connection = post_url.openConnection();
		// this will not be needed after I'm done TODO: remove
		connection.setDoOutput(true);
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write(post_data);
		writer.flush();
		// end TODO
		try {
			// parsing document from input stream
			java.io.InputStream stream = connection.getInputStream();
			this.license_doc = this.parser.build(stream);
		} catch (JDOMException jde) {
                        log.warn(jde.getMessage());
		} catch (Exception e) {
			log.warn(e.getCause());
		}
		return;
	} // issue

/**
	 * Passes a set of "answers" to the web service and retrieves a license.
	 *
	 * @param licenseURI The uri of the license.
	 *
	 * Note: does not support localization in 1.5 -- not yet
	 *
	 * @throws IOException
	 *
	 * @see CCLicense
	 * @see Map
	 */
	public void issue(String licenseURI)
		throws IOException{

		// Determine the issue URL
                 // Example: http://api.creativecommons.org/rest/1.5/details?
                //  license-uri=http://creativecommons.org/licenses/by-nc-sa/3.0/
		String issueUrl = cc_root + "/details?license-uri=" + licenseURI;
		// todo : modify for post as in the above issue
		String post_data;
		try {
			post_data = URLEncoder.encode("license-uri", "UTF-8") + "=" + URLEncoder.encode(licenseURI, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return;
		}
                //end todo
		URL request_url;
		try {
			request_url = new URL(issueUrl);
		} catch (MalformedURLException e) {
			return;
		}
		URLConnection connection = request_url.openConnection();
		// this will not be needed after I'm done TODO: remove
		connection.setDoOutput(true);
		try {
			// parsing document from input stream
			java.io.InputStream stream = connection.getInputStream();
			license_doc = this.parser.build(stream);
		} catch (JDOMException jde) {
			log.warn( jde.getMessage());
		} catch (Exception e) {
			log.warn(e.getCause());
		}
		return;
	} // issue

	/**
	 * Retrieves the URI for the license issued.
	 *
	 * @return A String containing the URI for the license issued.
	 */
	public String getLicenseUrl() {
		String text = null;
		try {
			JDOMXPath xp_LicenseName = new JDOMXPath("//result/license-uri");
		    text =  ((Element)xp_LicenseName.selectSingleNode(this.license_doc)).getText();
		}
		catch (Exception e) {
			log.warn(e.getMessage());
			setSuccess(false);
			text = "An error occurred getting the license - uri.";
		}
		finally
		{
			return text;
		}
	} // getLicenseUrl

	/**
	 * Retrieves the human readable name for the license issued.
	 *
	 * @return A String containing the license name.
	 */
	public String getLicenseName() {
		String text = null;
		try {
			JDOMXPath xp_LicenseName = new JDOMXPath("//result/license-name");
			text =  ((Element)xp_LicenseName.selectSingleNode(this.license_doc)).getText();
		}
		catch (Exception e) {
			log.warn(e.getMessage());
			setSuccess(false);
			text = "An error occurred on the license name.";
		}
		finally
		{
			return text;
		}
	} // getLicenseName


	public org.jdom.Document getLicenseDocument() {
		return this.license_doc;
	}

	public String getRdf()
		throws IOException {
		String result = ""; 
		try {
			result = CreativeCommons.fetchLicenseRDF(license_doc);
		} catch (Exception e) {
			log.warn("An error occurred getting the rdf . . ." + e.getMessage() );
			setSuccess(false);
		} 
		return result;
	}

	public boolean isSuccess() {
		setSuccess(false);
		JDOMXPath xp_Success = null;
		String text = null;
		try {
			xp_Success = new JDOMXPath("//message");
			text =  ((Element)xp_Success.selectSingleNode(this.license_doc)).getText();
			setErrorMessage(text);
		}
		catch (Exception e) {
			log.warn("There was an issue . . . " + text);
			setSuccess(true);
		}
		return this.success;
	}

	private void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	private void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}

/**
 *
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license; 

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

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



	private static String cc_root     = ConfigurationManager.getProperty("webui.submit.cc-rooturl");
	private static String jurisdiction  = (ConfigurationManager.getProperty("webui.submit.cc-jurisdiction") != null) ? ConfigurationManager.getProperty("webui.submit.cc-jurisdiction") : "";
	private Document license_doc        = null;
	private String rdfString            = null;
	private String errorMessage         = null;
	private boolean success             = false;



	private SAXBuilder parser           = new SAXBuilder();
	private List<CCLicense> licenses    = new Vector();
	private static List<CCLicenseField> licenseFields = new Vector();

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
		boolean isCurrent = isCurrent();
		if (isCurrent) {
			return licenses;
		} else {
			JDOMXPath xp_Licenses;
			JDOMXPath xp_LicenseID;

			Document classDoc;
			URL classUrl;
			List results = null;

			// create XPath expressions
			try {
				xp_Licenses = new JDOMXPath("//licenses/license");
				xp_LicenseID = new JDOMXPath("@id");
			} catch (JaxenException jaxen_e) {
				return null;
			}

			// retrieve and parse the license class document
			try {
				classUrl = new URL(this.cc_root + "/classes");
			} catch (Exception e) {
				// do nothing... but we should
				return null;
			}

			// parse the licenses document
			try {
				classDoc = this.parser.build(classUrl);
			} catch (JDOMException jdom_e) {
				return null;
			} catch (IOException io_e) {
				return null;
			}

			// extract the identifiers and labels using XPath
			try {
				results = xp_Licenses.selectNodes(classDoc);
			} catch (JaxenException jaxen_e) {
				return null;
			}

			// reset the licenses container
			this.licenses.clear();
			for (int i=0; i < results.size(); i++) {
				Element license = (Element)results.get(i);
				try {
					CCLicense lc = new CCLicense(((Attribute)xp_LicenseID.selectSingleNode(license)).getValue(), license.getText(), i);
					this.licenses.add(lc);
				} catch (JaxenException jaxen_e) {
					return null;
				}
			}
			sort();
		}
		return licenses;
	}

	/** contains
	 *
	 * is the token in the licenses vector
	 *
	 * @param token
	 * @return boolean
	 */
	protected boolean contains(String token) {
		Iterator iterator = licenses.iterator();
		for (CCLicense cclicense : licenses) {
			if (cclicense.getLicenseId().equals(token)) {
				return true;
			}
		}
		return false;
	}

	/** sort
	 *
	 *  sort the licenses according to the configuration file
	 *
	 *
	 */
	protected void sort() {
		StringTokenizer stringtokenizer = getLicenseTokens();
		String nexttoken    = null;
		int i               = 0;
		ListIterator<CCLicense> iterator = null;
		CCLicense license   = null;
		boolean isMember    = false;
		while (stringtokenizer.hasMoreTokens()) {
			nexttoken   = stringtokenizer.nextToken().trim();
			iterator    = licenses.listIterator();
			if (contains(nexttoken)) {
				while (iterator.hasNext()) {
						license = iterator.next();
						if (nexttoken.equals(license.getLicenseId().trim())) {
							license.setOrder(i);
							break;
						}
				  }
				} else {  // This is an instance specific selection . . .
				  // String optiontext 			= nexttoken.replace("_", " ");
				String optiontext = nexttoken; // this will be a key such as xmlui.Submission.submit.CCLicenseStep.submit_choose_creative_commons
				  CCLicense newlicense 	= new CCLicense(nexttoken, nexttoken, i);
				  iterator.add(newlicense);
			  }
			i++;
		}
		Comparator comparator = new Comparator() {
			public int compare( Object a, Object b )
	       {
				CCLicense c1 = (CCLicense)a;
				CCLicense c2 = (CCLicense)b;

				return(Integer.valueOf(c1.getOrder()).compareTo(Integer.valueOf(c2.getOrder())));
	       }
		};
			Collections.sort(licenses, comparator);
    }

	/** getLicenseTokens
	 *
	 *
	 * @return StringTokenizer representing the configured allowed license types (classes)
	 */
    protected StringTokenizer getLicenseTokens() {
    	String currentLicenses = ConfigurationManager.getProperty("webui.submit.cc.licenseclasses");
		StringTokenizer stringtokenizer = new StringTokenizer(currentLicenses, ",");
		return stringtokenizer;
    }

    /** isCurrent
     *
     * check to see if the configured license types(classes) have changed
     *
     * @return a boolean
     */
    private boolean isCurrent() {
		StringTokenizer stringtokenizer = getLicenseTokens();
		int i = 0;
		boolean isCurrent = false;
		while (licenses.size() > 0 && stringtokenizer.hasMoreTokens()) {
			String license = stringtokenizer.nextToken();
			if (license.equals(this.licenses.get(i).getLicenseName())) {
				i++;
				continue;
			} else {
				isCurrent = false;
				break;
			}
		}
		return isCurrent;
	}

	/**
	 * Queries the web service for a set of licenseFields for a particular license class.
	 *
	 * @param license A String specifying the CCLicense identifier to
	 * 			retrieve fields for.
	 * @return A Collection of LicenseField objects.
	 *
	 * @see CCLicense
	 * @see LicenseField
	 *
	 */
	public Collection<CCLicenseField> getLicenseFields(String license) {

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
			classUrl = new URL(this.cc_root + "/license/" + license);
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
		String issueUrl = this.cc_root + "/license/" + licenseId + "/issue";
		System.out.println("the default locale is " + lang);
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
		System.out.println("The answer _doc is " + answer_doc);
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
			System.out.println("The size of the license doc is " + license_doc.getContent().size());
		} catch (JDOMException jde) {
			System.out.print( jde.getMessage());
		} catch (Exception e) {
			System.out.println("Error reading the file " + e.getCause());
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
			System.out.print( jde.getMessage());
		} catch (Exception e) {
			System.out.println("Error reading the file " + e.getCause());
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
			System.out.println(e.getMessage());
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
			System.out.println(e.getMessage());
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
		String myString = null;
		java.io.ByteArrayOutputStream outputstream = new java.io.ByteArrayOutputStream();
		try {
			outputstream.write("<result>\n".getBytes()); 
			JDOMXPath xpathRdf 				= new JDOMXPath("//result/rdf");
			JDOMXPath xpathLicenseRdf 				= new JDOMXPath("//result/licenserdf");
			XMLOutputter xmloutputter 	= new XMLOutputter();
			Element rdfParent     				= ((Element)xpathRdf.selectSingleNode(this.license_doc));
			xmloutputter.output(rdfParent, outputstream);
			Element licenseRdfParent       = ((Element)xpathLicenseRdf.selectSingleNode(this.license_doc));
			outputstream.write("\n".getBytes());
			xmloutputter.output(licenseRdfParent, outputstream);
			outputstream.write("\n</result>\n".getBytes());
		} catch (Exception e) {
			System.out.println("An error occurred getting the rdf . . ." + e.getMessage() );
			setSuccess(false);
		} finally {
			outputstream.close();
			System.out.println(outputstream.toString());
			return outputstream.toString();
		}
	}

	public boolean isSuccess() {
		setSuccess(false);
		java.io.ByteArrayOutputStream outputstream = new java.io.ByteArrayOutputStream();
		JDOMXPath xp_Success = null;
		String text = null;
		try {
			xp_Success = new JDOMXPath("//message");
			text =  ((Element)xp_Success.selectSingleNode(this.license_doc)).getText();
			setErrorMessage(text);
		}
		catch (Exception e) {
			System.out.println("There was an issue . . . " + text);
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

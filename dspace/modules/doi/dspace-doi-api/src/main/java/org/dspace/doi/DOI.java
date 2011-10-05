package org.dspace.doi;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.garret.perst.PersistentString;


public class DOI {

    public enum Type {

        TOMBSTONE ("info:dspace/tombstone" );

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String toString(){
            return value;
        }
    };

	transient private Logger LOG = Logger.getLogger(DOIDatabase.class);

	private PersistentString myURL;
	private PersistentString myPrefix;
	private PersistentString mySuffix;

	protected DOI() {
		super();
	}

    public DOI(String aDOIString, DSpaceObject dso) throws DOIFormatException {
		this(aDOIString);

        // TODO: Make IdentifierService return the identifer for any provider + Item
        myURL = new PersistentString("info:dspace/" + Constants.typeText[dso.getType()] + "/" + dso.getID());
	}

    public DOI(String aDOIString, Type internalForm) throws DOIFormatException {
		this(aDOIString);
        myURL = new PersistentString(internalForm.toString());
	}

	public DOI(String aPrefix, String aSuffix, DSpaceObject dso) throws DOIFormatException {
		myPrefix = new PersistentString(aPrefix);
		mySuffix = new PersistentString(aSuffix);
        myURL = new PersistentString("info:dspace/" + Constants.typeText[dso.getType()] + "/" + dso.getID());
	}

	private DOI(String aDOIString) throws DOIFormatException {
		String id = aDOIString.substring(4);
		int index = id.indexOf('/');

		if (!aDOIString.startsWith("doi:"))
			throw new DOIFormatException("DOI strings must start with doi:");

		if (!id.startsWith("10."))
			throw new DOIFormatException("DOI prefixes must start with 10.");

		if (index == -1)
			throw new DOIFormatException("No DOI prefix / suffix separator");

		myPrefix = new PersistentString(id.substring(0, index));
		mySuffix = new PersistentString(id.substring(index + 1));
	}


	private DOI(String aPrefix, String aSuffix, String aURL) throws DOIFormatException {
		myPrefix = new PersistentString(aPrefix);
		mySuffix = new PersistentString(aSuffix);
		myURL = new PersistentString(aURL);
	}



    public static String getInternalForm(DSpaceObject dso){
        return "info:dspace/" + Constants.typeText[dso.getType()] + "/" + dso.getID();
    }


	/**
	 * Outputs a string form of the DOI (<code>doi:10.3456/dryad.12054</code>).
	 * 
	 * @return A String form of the DOI
	 */
	public String toString() {
		return "doi:" + myPrefix.toString() + "/" + mySuffix.toString();
	}

	/**
	 * Outputs the unique ID part of the DOI (<code>10.3456/dryad.12054</code>).
	 * 
	 * @return The raw content (ID) of the DOI
	 */
	public String toID() {
		return myPrefix.toString() + "/" + mySuffix.toString();
	}

	/**
	 * Outputs the URL string that references the DOI resolving service at
	 * doi.org (<code>http://dx.doi.org/10.3456/dryad.12054</code>).
	 *
	 * @return The URL at which the DOI can be resolved
	 */
	public String toExternalForm() {
		return "http://dx.doi.org/" + myPrefix.toString() + "/"
				+ mySuffix.toString();
	}

	/**
	 * The URL form of the external form returned by
	 * <code>toExternalForm()</code>.
	 * 
	 * @return The URL of the external form of the DOI
	 * @throws MalformedURLException If the URL isn't a proper URL
	 */
	public URL toURL() throws MalformedURLException {
		return new URL(toExternalForm());
	}


    /**
	 * Outputs the string that references the internal object at
	 * doi.org (<code>info:dspace/item/123</code>).
	 *
	 * @return The URL at which the DOI can be resolved
	 */
	public String getInternalIdentifier() {
		return myURL.toString();
	}

	public URL getTargetURL() {

        String external = ConfigurationManager.getProperty("dspace.url");

        if(myURL.toString().equals(Type.TOMBSTONE))
        {
             external += "/tombstone";
        }
        else
        {
            external += "/resource/doi:" + myPrefix.toString() + "/"
				+ mySuffix.toString();
        }

		try {
		     return new URL(external);
		}
		catch (MalformedURLException details) {
			throw new RuntimeException(details); // shouldn't happen
		}

	}

	public String getPrefix() {
		return myPrefix.toString();
	}

	public String getSuffix() {
		return mySuffix.toString();
	}

	/**
	 * Use clones to make sure the transactions are used in explicit put()s and
	 * set()s to the database.
	 */
	public DOI cloneDOI() {
		DOI doi; // non-persisted DOI

		try {
			doi = new DOI(myPrefix.toString(), mySuffix.toString(), myURL.toString());
		}
		catch (DOIFormatException details) {
			throw new RuntimeException(details);
		}

		return doi;
	}

	public boolean equals(Object aObject) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Checking DOIs using equals()");
		}

		if (aObject == null) { return false; }
		if (aObject == this) { return true; }
		if (aObject instanceof String && toString().equals(aObject)) {
			return true;
		}
		if (!(aObject instanceof DOI)) {
			LOG.warn("In equals() instanceof comparison, DOI was "
					+ aObject.getClass().getName());
			return false;
		}

		DOI doi = (DOI) aObject;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Checking DOI prefix | suffix: \"" + myPrefix + "\" = \""
					+ doi.myPrefix + "\" | \"" + mySuffix + "\" = \""
					+ doi.mySuffix + "\"");
		}

		if (myPrefix.toString().equals(doi.myPrefix.toString())
				&& mySuffix.toString().equals(doi.mySuffix.toString())) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(doi.toString() + " is equal to compared DOI");
			}

			return true;
		}
		else {
			if (LOG.isDebugEnabled()) {
				LOG.debug(doi.toString() + " isn't equal to compared DOI: "
						+ myPrefix.toString().equals(doi.myPrefix.toString()) + " | "
						+ mySuffix.toString().equals(doi.mySuffix.toString()));
			}

			return false;
		}
	}

	public int hashCode() {
		int hash = (myPrefix.toString().hashCode() + mySuffix.toString()
				.hashCode()) * 9;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Checking hashCode(): " + hash);
		}

		return hash;
	}
}

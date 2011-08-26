package org.swordapp.server;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Link;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepositReceipt
{
    private List<String> packagingFormats = new ArrayList<String>();
    private IRI editIRI = null;
    private IRI seIRI = null;
    private IRI emIRI = null;
    private IRI feedIRI = null;
	private IRI location = null;
    private Entry entry;
    private Map<String, String> statements = new HashMap<String, String>();
    private String treatment = null;
    private String verboseDescription = null;
    private String splashUri = null;
    private String originalDepositUri = null;
    private String originalDepositType = null;
    private Map<String, String> derivedResources = new HashMap<String, String>();
	private boolean empty = false;
    private Date lastModified = null;

    public DepositReceipt()
    {
        Abdera abdera = new Abdera();
        this.entry = abdera.newEntry();
    }

    public Entry getWrappedEntry()
    {
        return this.entry;
    }

    public Entry getAbderaEntry()
    {
        Entry abderaEntry = (Entry) this.entry.clone();

		// use the edit iri as the id
		abderaEntry.setId(this.editIRI.toString());

        // add the Edit IRI Link
        if (this.editIRI != null)
        {
            abderaEntry.addLink(this.editIRI.toString(), "edit");
        }

        // add the Sword Edit IRI link
        if (this.seIRI != null)
        {
            abderaEntry.addLink(this.seIRI.toString(), UriRegistry.REL_SWORD_EDIT);
        }

        // add the atom formatted feed
        if (this.feedIRI != null)
        {
            Link fl = abderaEntry.addLink(this.feedIRI.toString(), "edit-media");
            fl.setMimeType("application/atom+xml;type=feed");
        }

        // add the edit-media link
        if (this.emIRI != null)
        {
            abderaEntry.addLink(this.emIRI.toString(), "edit-media");
        }

        // add the packaging formats
        for (String pf : this.packagingFormats)
        {
            abderaEntry.addSimpleExtension(UriRegistry.SWORD_PACKAGING, pf);
        }

        // add the statement URIs
        for (String statement : this.statements.keySet())
        {
            Link link = abderaEntry.addLink(statement, UriRegistry.REL_STATEMENT);
            link.setMimeType(this.statements.get(statement));
        }

        if (this.treatment != null)
        {
            abderaEntry.addSimpleExtension(UriRegistry.SWORD_TREATMENT, this.treatment);
        }

        if (this.verboseDescription != null)
        {
            abderaEntry.addSimpleExtension(UriRegistry.SWORD_VERBOSE_DESCRIPTION, this.verboseDescription);
        }

        if (this.splashUri == null)
        {
            abderaEntry.addLink(this.splashUri, "alternate");
        }

        if (this.originalDepositUri != null)
        {
            Link link = abderaEntry.addLink(this.originalDepositUri, UriRegistry.REL_ORIGINAL_DEPOSIT);
            if (this.originalDepositType != null)
            {
                link.setMimeType(this.originalDepositType);
            }
        }

        for (String uri : this.derivedResources.keySet())
        {
            Link link = abderaEntry.addLink(uri, UriRegistry.REL_DERIVED_RESOURCE);
            if (this.derivedResources.get(uri) != null)
            {
                link.setMimeType(this.derivedResources.get(uri));
            }
        }

        return abderaEntry;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date lastModified)
    {
        this.lastModified = lastModified;
    }

    public boolean isEmpty()
	{
		return empty;
	}

	public void setEmpty(boolean empty)
	{
		this.empty = empty;
	}

	public void setMediaFeedIRI(IRI feedIRI)
    {
        this.feedIRI = feedIRI;
    }

    public void setEditMediaIRI(IRI emIRI)
    {
        this.emIRI = emIRI;
    }

    public void setEditIRI(IRI editIRI)
    {
        this.editIRI = editIRI;

        // set the SE-IRI as the same if it has not already been set
        if (this.seIRI == null)
        {
            this.seIRI = editIRI;
        }
    }

	public IRI getLocation()
	{
		return this.location == null ? this.editIRI : this.location;
	}

	public void setLocation(IRI location)
	{
		this.location = location;
	}

    public IRI getEditIRI()
    {
        return this.editIRI;
    }

    public IRI getSwordEditIRI()
    {
        return this.seIRI;
    }

    public void setSwordEditIRI(IRI seIRI)
    {
        this.seIRI = seIRI;

        // set the Edit-IRI the same if it has not already been set
        if (this.editIRI == null)
        {
            this.editIRI = seIRI;
        }
    }

    public void setContent(IRI href, String mediaType)
    {
        this.entry.setContent(href, mediaType);
    }

    public void addEditMediaIRI(IRI href)
    {
        this.entry.addLink(href.toString(), "edit-media");
    }

    public void addEditMediaIRI(IRI href, String mediaType)
    {
        Abdera abdera = new Abdera();
        Link link = abdera.getFactory().newLink();
        link.setHref(href.toString());
        link.setRel("edit-media");
        link.setMimeType(mediaType);
        this.entry.addLink(link);
    }

    public void addEditMediaFeedIRI(IRI href)
    {
        this.addEditMediaIRI(href, "application/atom+xml;type=feed");
    }

    public void setPackaging(List<String> packagingFormats)
    {
        this.packagingFormats = packagingFormats;
    }

    public void addPackaging(String packagingFormat)
    {
        this.packagingFormats.add(packagingFormat);
    }

    public void setOREStatementURI(String statement)
    {
        this.setStatementURI("application/rdf+xml", statement);
    }

    public void setAtomStatementURI(String statement)
    {
        this.setStatementURI("application/atom+xml;type=feed", statement);
    }

    public void setStatementURI(String type, String statement)
    {
        this.statements.put(statement, type);
    }

    public Element addSimpleExtension(QName qname, String value)
    {
        return this.entry.addSimpleExtension(qname, value);
    }

    public Element addDublinCore(String element, String value)
    {
        return this.entry.addSimpleExtension(new QName(UriRegistry.DC_NAMESPACE, element), value);
    }

    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }

    public void setVerboseDescription(String verboseDescription)
    {
        this.verboseDescription = verboseDescription;
    }

    public void setSplashUri(String splashUri)
    {
        this.splashUri = splashUri;
    }

    public void setOriginalDeposit(String originalDepositUri, String originalDepositType)
    {
        this.originalDepositUri = originalDepositUri;
        this.originalDepositType = originalDepositType;
    }

    public void setDerivedResources(Map<String, String> derivedResources)
    {
        this.derivedResources = derivedResources;
    }

    public void addDerivedResource(String resourceUri, String resourceType)
    {
        this.derivedResources.put(resourceUri, resourceType);
    }
}

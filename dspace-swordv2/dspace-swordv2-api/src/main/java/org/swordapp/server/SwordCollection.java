package org.swordapp.server;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwordCollection
{
    private Collection collection;
    private List<String> multipartAccept = new ArrayList<String>();
    private Abdera abdera;
    private String collectionPolicy = null;
    private boolean mediation = false;
    private String treatment = null;
    private List<String> acceptPackaging = new ArrayList<String>();
    private List<IRI> subServices = new ArrayList<IRI>();
	private String dcAbstract = null;

    public SwordCollection()
    {
        this.abdera = new Abdera();
        this.collection = this.abdera.getFactory().newCollection();
    }

    public Collection getWrappedCollection()
    {
        return this.collection;
    }

    public Collection getAbderaCollection()
    {
        Collection abderaCollection = (Collection) this.collection.clone();

		// FIXME: this is wrong; clients must be free to leave the accepts blank
//        // ensure that there is an accept field set
//        List<String> singlePartAccepts = this.getSinglepartAccept();
//        if (singlePartAccepts.size() == 0)
//        {
//            abderaCollection.addAccepts("*/*");
//        }
//
//        // ensure that there is multipart accepts set
//        List<String> multipartAccepts = this.getMultipartAccept();
//        if (multipartAccepts.size() == 0 || this.multipartAccept.size() == 0)
//        {
//            this.multipartAccept.add("*/*");
//        }

        // add the multipart accepts as elements
        for (String mpa : this.multipartAccept)
        {
            Element element = this.abdera.getFactory().newElement(UriRegistry.APP_ACCEPT);
            element.setAttributeValue("alternate", "multipart/related");
			element.setText(mpa);
            abderaCollection.addExtension(element);
        }

        // add the collection Policy
        if (this.collectionPolicy != null)
        {
            abderaCollection.addSimpleExtension(UriRegistry.SWORD_COLLECTION_POLICY, this.collectionPolicy);
        }

        // add the mediation
        abderaCollection.addSimpleExtension(UriRegistry.SWORD_MEDIATION, (this.mediation ? "true" : "false"));

        // add the treatment
        if (this.treatment != null)
        {
            abderaCollection.addSimpleExtension(UriRegistry.SWORD_TREATMENT, this.treatment);
        }

        // add the acceptPackaging
        for (String ap : this.acceptPackaging)
        {
            abderaCollection.addSimpleExtension(UriRegistry.SWORD_ACCEPT_PACKAGING, ap);
        }

        // add the sub service document IRI
        for (IRI ss : this.subServices)
        {
            abderaCollection.addSimpleExtension(UriRegistry.SWORD_SERVICE, ss.toString());
        }

		// add the abstract
		if (this.dcAbstract != null)
		{
			abderaCollection.addSimpleExtension(UriRegistry.DC_ABSTRACT, this.dcAbstract);
		}

        return abderaCollection;
    }

	public void setLocation(String href)
	{
		this.collection.setHref(href);
	}

	public void setAbstract(String dcAbstract)
	{
		this.dcAbstract = dcAbstract;
	}

    public List<IRI> getSubServices()
    {
        return subServices;
    }

    public void setSubServices(List<IRI> subServices)
    {
        this.subServices = subServices;
    }

    public void addSubService(IRI subService)
    {
        this.subServices.add(subService);
    }

    public void setCollectionPolicy(String collectionPolicy)
    {
        this.collectionPolicy = collectionPolicy;
    }

    public void setMediation(boolean mediation)
    {
        this.mediation = mediation;
    }

    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }

    public void setAcceptPackaging(List<String> acceptPackaging)
    {
        this.acceptPackaging = acceptPackaging;
    }

    public void addAcceptPackaging(String acceptPackaging)
    {
        this.acceptPackaging.add(acceptPackaging);
    }

    public String getCollectionPolicy()
    {
        return collectionPolicy;
    }

    public boolean isMediation()
    {
        return mediation;
    }

    public String getTreatment()
    {
        return treatment;
    }

    public List<String> getAcceptPackaging()
    {
        return acceptPackaging;
    }

    public void setTitle(String title)
    {
        this.collection.setTitle(title);
    }

    public void setHref(String href)
    {
        this.collection.setHref(href);
    }

    public void setAccept(String... mediaRanges)
    {
        this.collection.setAccept(mediaRanges);
    }

    public void setAcceptsEntry()
    {
        this.collection.setAcceptsEntry();
    }

    public void setAcceptsNothing()
    {
        this.collection.setAcceptsNothing();
    }

    public void addAccepts(String mediaRange)
    {
        this.collection.addAccepts(mediaRange);
    }

    public void addAccepts(String... mediaRanges)
    {
        this.collection.addAccepts(mediaRanges);
    }

    public void addAcceptsEntry()
    {
        this.collection.addAcceptsEntry();
    }

    public void setMultipartAccept(String... mediaRanges)
    {
        List<String> mrs = Arrays.asList(mediaRanges);
        this.multipartAccept.clear();
        this.multipartAccept.addAll(mrs);
    }

    public void addMultipartAccepts(String mediaRange)
    {
        this.multipartAccept.add(mediaRange);
    }

    public void addMultipartAccepts(String... mediaRanges)
    {
        List<String> mrs = Arrays.asList(mediaRanges);
        this.multipartAccept.addAll(mrs);
    }

    public List<String> getMultipartAccept()
    {
        List<String> accepts = new ArrayList<String>();
        List<Element> elements = this.collection.getElements();
        boolean noAccept = false;
        for (Element e : elements)
        {
            String multipartRelated = e.getAttributeValue("alternate");
            QName qn = e.getQName();
            if (qn.getLocalPart().equals("accept") &&
                    qn.getNamespaceURI().equals(UriRegistry.APP_NAMESPACE) &&
                    "multipart-related".equals(multipartRelated))
            {
                String content = e.getText();
                if (content == null || "".equals(content))
                {
                    noAccept = true;
                }
                if (content != null && !"".equals(content) && !accepts.contains(content))
                {
                    accepts.add(content);
                }
            }
        }

        // if there are no accept values, and noAccept has not been triggered, then we add the
        // default accept type
        if (accepts.size() == 0 && !noAccept)
        {
            accepts.add("application/atom+xml;type=entry");
        }

        // rationalise and return
        return this.rationaliseAccepts(accepts);
    }

    public List<String> getSinglepartAccept()
    {
        List<String> accepts = new ArrayList<String>();
        List<Element> elements = this.collection.getElements();
        boolean noAccept = false;
        for (Element e : elements)
        {
            String multipartRelated = e.getAttributeValue("alternate");
            QName qn = e.getQName();
            if (qn.getLocalPart().equals("accept") &&
                    qn.getNamespaceURI().equals(UriRegistry.APP_NAMESPACE) &&
                    !"multipart-related".equals(multipartRelated))
            {
                String content = e.getText();
                if (content == null || "".equals(content))
                {
                    noAccept = true;
                }
                if (content != null && !"".equals(content) && !accepts.contains(content))
                {
                    accepts.add(content);
                }
            }
        }

        // if there are no accept values, and noAccept has not been triggered, then we add the
        // default accept type
        if (accepts.size() == 0 && !noAccept)
        {
            accepts.add("application/atom+xml;type=entry");
        }

        // rationalise and return
        return this.rationaliseAccepts(accepts);
    }

    private List<String> rationaliseAccepts(List<String> accepts)
    {
        List<String> rational = new ArrayList<String>();

        // first, if "*/*" is there, then we accept anything
        if (accepts.contains("*/*"))
        {
            rational.add("*/*");
            return rational;
        }

        // now look to see if we have <x>/* and if so eliminate the unnecessary accepts
        List<String> wildcards = new ArrayList<String>();
        for (String a : accepts)
        {
            if (a.contains("/*"))
            {
                String wild = a.substring(0, a.indexOf("/"));
                wildcards.add(wild);
                if (!rational.contains(a))
                {
                    rational.add(a);
                }
            }
        }

        for (String a : accepts)
        {
            String type = a.substring(0, a.indexOf("/"));
            if (!wildcards.contains(type))
            {
                rational.add(a);
            }
        }

        // by the time we get here we will have only unique and correctly wildcarded accept fields
        return rational;
    }
}

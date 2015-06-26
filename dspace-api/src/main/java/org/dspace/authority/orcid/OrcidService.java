/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.model.Work;
import org.dspace.authority.orcid.xml.XMLtoBio;
import org.dspace.authority.orcid.xml.XMLtoWork;
import org.dspace.authority.rest.RestSource;
import org.dspace.content.DCPersonName;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Orcid extends RestSource {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Orcid.class);

    private static Orcid orcid;

    public static Orcid getOrcid() {
        if (orcid == null) {
            orcid = new DSpace().getServiceManager().getServiceByName("OrcidSource", Orcid.class);
        }
        return orcid;
    }

    private Orcid(String url) {
        super(url);
    }

    public Bio getBio(String id) {
        Document bioDocument = restConnector.get(id + "/orcid-bio");
        XMLtoBio converter = new XMLtoBio();
        Bio bio = converter.convert(bioDocument).get(0);
        bio.setOrcid(id);
        return bio;
    }

    public List<Work> getWorks(String id) {
        Document document = restConnector.get(id + "/orcid-works");
        XMLtoWork converter = new XMLtoWork();
        return converter.convert(document);
    }

    public List<Bio> queryBio(String name, int start, int rows) {
        Document bioDocument = restConnector.get("search/orcid-bio?q=" + URLEncoder.encode("\"" + name + "\"") + "&start=" + start + "&rows=" + rows);
        XMLtoBio converter = new XMLtoBio();
        return converter.convert(bioDocument);
    }

    public List<Bio> queryBio(String field, String name, int start, int rows) {
        Document bioDocument = restConnector.get("search/orcid-bio?q=" + field +":" + URLEncoder.encode(name) + "&start=" + start + "&rows=" + rows);
        XMLtoBio converter = new XMLtoBio();
        return converter.convert(bioDocument);
    }
    
    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
        List<Bio> bios = queryBio(text, 0, max);
        List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        for (Bio bio : bios) {
            authorities.add(OrcidAuthorityValue.create(bio));
        }
        return authorities;
    }

    @Override
    public AuthorityValue queryAuthorityID(String id) {
        Bio bio = getBio(id);
        return OrcidAuthorityValue.create(bio);
    }

	@Override
	public List<AuthorityValue> queryAuthorities(String field, String text, int start, int max) {
        List<Bio> bios = null;
        if(StringUtils.isNotBlank(field)) {
        	bios = queryBio(field, text, start, max);
        }
        else {
        	bios = queryBio(text, start, max);
        }
        List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        for (Bio bio : bios) {
            authorities.add(OrcidAuthorityValue.create(bio));
        }
        return authorities;
	}

	public List<AuthorityValue> queryOrcidBioByFamilyNameAndGivenName(String text, int start, int max) {
		DCPersonName tmpPersonName = new DCPersonName(
	            text);

	    String query = "";
	    if (StringUtils.isNotBlank(tmpPersonName.getLastName()))
	    {
	        query += "family-name:"+tmpPersonName
	                .getLastName().trim()
	                + (StringUtils.isNotBlank(tmpPersonName
	                        .getFirstNames()) ? "" : "*");
	    }

	    if (StringUtils.isNotBlank(tmpPersonName.getFirstNames()))
	    {
	        query += (query.length() > 0 ? " AND given-names:" : "given-names:")
	                + tmpPersonName
	                        .getFirstNames().trim() + "*";
	    }
	    	    
	    Document bioDocument = restConnector.get("search/orcid-bio?q=" + URLEncoder.encode(query) + "&start=" + start + "&rows=" + max);
	    XMLtoBio converter = new XMLtoBio();
	    List<Bio> bios = converter.convert(bioDocument);
        
		List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        for (Bio bio : bios) {
            authorities.add(OrcidAuthorityValue.create(bio));
        }
        return authorities;
	}
}

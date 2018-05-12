/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.model.Profile;
import org.dspace.authority.orcid.model.Work;
import org.dspace.authority.orcid.xml.XMLtoBio;
import org.dspace.authority.orcid.xml.XMLtoProfile;
import org.dspace.authority.orcid.xml.XMLtoWork;
import org.dspace.authority.rest.RestSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.w3c.dom.Document;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
            orcid = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("OrcidSource", Orcid.class);
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

    public Profile getProfile(String id) {
        // The affiliations are only available using the v1.2 (or greater) API
        Document document = restConnector.get("v1.2/" + id + "/orcid-profile");
        XMLtoProfile converter = new XMLtoProfile();
        Profile profile = converter.convertProfile(document).get(0);
        profile.setOrcid(id);
        return profile;
    }

    public List<Work> getWorks(String id) {
        Document document = restConnector.get(id + "/orcid-works");
        XMLtoWork converter = new XMLtoWork();
        return converter.convert(document);
    }

    public List<Bio> queryBio(String name, int start, int rows) {
        name = name.trim();

        String query;
        if (Pattern.matches("\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]", name)) {
            // It's an ORCID
            query = "orcid:" + name;
        } else {
            List<String> queryParts = new ArrayList<String>();

            // Split the name into parts to search for
            String[] parts = name.split(" ");

            for (String part : parts) {
                // Search only the name fields. The default search also includes the work-titles field which sometimes contains names.
                queryParts.add("(given-names:" + part + " OR family-name:" + part + " OR credit-name:" + part + " OR other-names:" + part + ")");
            }

            // Additionally, make sure one of the parts matches the family-name field
            queryParts.add("(family-name:" + StringUtils.join(parts, " OR family-name:") + ")");

            query = StringUtils.join(queryParts, " AND ");
        }

        Document bioDocument = restConnector.get("search/orcid-bio?q=" + URLEncoder.encode(query) + "&start=" + start + "&rows=" + rows);

        XMLtoBio converter = new XMLtoBio();
        return converter.convert(bioDocument);
    }

    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
        List<Bio> bios = queryBio(text, 0, max);
        List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        for (Bio bio : bios) {
            // Do a separate query for each person to get their full profile including affiliations
            authorities.add(queryAuthorityID(bio.getOrcid()));
        }
        return authorities;
    }

    @Override
    public AuthorityValue queryAuthorityID(String id) {
        Profile profile = getProfile(id);
        return OrcidAuthorityValue.create(profile);
    }
}

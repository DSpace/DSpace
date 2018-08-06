/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;


import org.apache.log4j.Logger;
import org.dspace.authority.orcid.Orcidv2;
import org.dspace.utils.DSpace;
import org.orcid.jaxb.model.common_v2.OrcidId;
import org.orcid.jaxb.model.record_v2.Person;
import org.orcid.jaxb.model.search_v2.Result;
import org.orcid.jaxb.model.search_v2.Search;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLtoBio extends Converter {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(XMLtoBio.class);

    @Override
    public List<Person> convert(InputStream xml) {
        List<Person> bios= new ArrayList<>();
        try {
            Orcidv2 connector = new DSpace().getServiceManager().getServiceByName("AuthoritySource", Orcidv2.class);

            Search search = (Search) unmarshall(xml, Search.class);
            for(Result result : search.getResult()){
                OrcidId orcidIdentifier = result.getOrcidIdentifier();
                if(orcidIdentifier!=null){
                    log.debug("Found OrcidId=" + orcidIdentifier.toString());
                    String orcid = orcidIdentifier.getUriPath();
                    Person bio = connector.getBio(orcid);
                    if(bio!=null){
                        bios.add(bio);
                    }
                }
            }
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return bios;
    }

    public Person convertSinglePerson(InputStream xml) {
        Person person = null;
        try {
            person = (Person) unmarshall(xml, Person.class);
            return person;
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return null;
    }

}


/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.orcid.xml;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.orcid.jaxb.model.record_v2.Person;
import org.orcid.jaxb.model.search_v2.Result;
import org.orcid.jaxb.model.search_v2.Search;
import org.xml.sax.SAXException;

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLtoBio extends Converter<List<Result>> {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(XMLtoBio.class);

    @Override
    public List<Result> convert(InputStream xml) {
        List<Result> bios = new ArrayList<>();
        try {
            Search search = (Search) unmarshall(xml, Search.class);
            bios = search.getResult();
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return bios;
    }

    public int getNumberOfResultsFromXml(InputStream xml) {
        try {
            Search search = (Search) unmarshall(xml, Search.class);
            return search.getNumFound().intValue();
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return 0;
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

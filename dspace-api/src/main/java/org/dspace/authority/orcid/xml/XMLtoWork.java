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
import org.orcid.jaxb.model.record.summary_v2.WorkGroup;
import org.orcid.jaxb.model.record.summary_v2.WorkSummary;
import org.orcid.jaxb.model.record.summary_v2.Works;
import org.orcid.jaxb.model.record_v2.Work;
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
public class XMLtoWork extends Converter {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(XMLtoWork.class);

    public List<Work> convert(InputStream document) {
        Orcidv2 connector = new DSpace().getServiceManager().getServiceByName("AuthoritySource", Orcidv2.class);
        List<Work> result = new ArrayList<>();
        try {
            Works works = (Works) unmarshall(document, Works.class);
            for (WorkGroup workGroup : works.getWorkGroup()) {

                for (WorkSummary summary : workGroup.getWorkSummary()) {
                    String workPath = summary.getPath();
                    Work work = connector.getWork(workPath);
                    if(work!=null){
                       result.add(work);
                    }
                }
            }
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return result;
    }

    public Work toWork(InputStream input){
        try {
            Work work = (Work)unmarshall(input,Work.class);
            return work;
        } catch (SAXException | URISyntaxException e) {
            log.error(e);
        }
        return  null;
    }

}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery.json;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.discovery.*;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Class used to search in the discovery backend and return a json formatted string
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class JSONDiscoverySearcher extends AbstractReader implements Recyclable {

    private static Logger log = Logger.getLogger(JSONDiscoverySearcher.class);
    /** These are all our parameters which can be used by this generator **/
    private DiscoverQuery queryArgs;
    private String jsonWrf;


    /** The Cocoon response */
    protected Response response;

    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        //Retrieve all the given parameters
        Request request = ObjectModelHelper.getRequest(objectModel);
        this.response = ObjectModelHelper.getResponse(objectModel);
        
        queryArgs = new DiscoverQuery();

        queryArgs.setQuery(request.getParameter("q"));


        //Retrieve all our filter queries
        if(request.getParameterValues("fq") != null)
            queryArgs.addFilterQueries(request.getParameterValues("fq"));

        //Retrieve our facet fields
        if(request.getParameterValues("facet.field") != null){
            for (int i = 0; i < request.getParameterValues("facet.field").length; i++) {
                String facetField = request.getParameterValues("facet.field")[i];
                queryArgs.addFacetField(new FacetFieldConfig(facetField, false));
            }
        }

        //Retrieve our facet limit (if any)
        int facetLimit;
        if(request.getParameter("facet.limit") != null){
            try{
                facetLimit = Integer.parseInt(request.getParameter("facet.limit"));
            }catch (Exception e){
                //Should an invalid value be supplied use -1
                facetLimit = -1;
            }
        }
        else
        {
            facetLimit = -1;
        }
        queryArgs.setFacetLimit(facetLimit);

        //Retrieve our sorting value
        String facetSort = request.getParameter("facet.sort");
        if(facetSort == null || facetSort.equalsIgnoreCase("count"))
            queryArgs.setFacetSort(DiscoverQuery.FACET_SORT.COUNT);
        else
            queryArgs.setFacetSort(DiscoverQuery.FACET_SORT.INDEX);

        //Retrieve our facet min count
        int facetMinCount;
        try{
            facetMinCount = Integer.parseInt(request.getParameter("facet.mincount"));
        }catch (Exception e){
            facetMinCount = 1;
        }
        queryArgs.setFacetMinCount(facetMinCount);
        jsonWrf = request.getParameter("json.wrf");
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        String result = null;
        try {
            result = getSearchService().searchJSON(queryArgs, jsonWrf);
        } catch (SearchServiceException e) {
            log.error("Error while retrieving JSON string for Discovery auto complete", e);
        }

        if(result != null){
            ByteArrayInputStream inputStream = new ByteArrayInputStream(result.getBytes("UTF-8"));

            byte[] buffer = new byte[8192];

            response.setHeader("Content-Length", String.valueOf(result.length()));
            int length;
            while ((length = inputStream.read(buffer)) > -1)
            {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }
}

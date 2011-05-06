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
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.constants.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SolrServiceImpl;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used to search in solr and return a json formatted string
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class JSONSolrSearcher extends AbstractReader implements Recyclable {

    private static Logger log = Logger.getLogger(JSONSolrSearcher.class);
    /** These are all our parameters which can be used by this generator **/
    private String query;
    private String[] filterQueries;
    private String[] facetFields;
    private int facetLimit;
    private String facetSort;
    private int facetMinCount;
    private String solrServerUrl;
    private String jsonWrf;


    /** The Cocoon response */
    protected Response response;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        //Retrieve all the given parameters
        Request request = ObjectModelHelper.getRequest(objectModel);
        this.response = ObjectModelHelper.getResponse(objectModel);
        

        query = request.getParameter(CommonParams.Q);
        if(query == null)
        {
            query = "*:*";
        }

        //Retrieve all our filter queries
        filterQueries = request.getParameterValues(CommonParams.FQ);

        //Retrieve our facet fields
        facetFields = request.getParameterValues(FacetParams.FACET_FIELD);

        //Retrieve our facet limit (if any)
        if(request.getParameter(FacetParams.FACET_LIMIT) != null){
            try{
                facetLimit = Integer.parseInt(request.getParameter(FacetParams.FACET_LIMIT));
            }catch (Exception e){
                //Should an invalid value be supplied use -1
                facetLimit = -1;
            }
        }
        else
        {
            facetLimit = -1;
        }

        //Retrieve our sorting value
        facetSort = request.getParameter(FacetParams.FACET_SORT);
        //Make sure we have a valid sorting value
        if(!FacetParams.FACET_SORT_INDEX.equals(facetSort) && !FacetParams.FACET_SORT_COUNT.equals(facetSort))
        {
            facetSort = null;
        }

        //Retrieve our facet min count
        facetMinCount = 1;
        try{
            facetMinCount = Integer.parseInt(request.getParameter(FacetParams.FACET_MINCOUNT));
        }catch (Exception e){
            facetMinCount = 1;
        }
        jsonWrf = request.getParameter("json.wrf");

        //Retrieve our discovery solr path
        ExtendedProperties props = null;
        //Method that will retrieve all the possible configs we have

        props = ExtendedProperties.convertProperties(ConfigurationManager.getProperties());

        InputStream is = null;
        try {
            File config = new File(props.getProperty("dspace.dir") + "/config/dspace-solr-search.cfg");
            if (config.exists()) {
                props.combine(new ExtendedProperties(config.getAbsolutePath()));
            } else {
                is = SolrServiceImpl.class.getResourceAsStream("dspace-solr-search.cfg");
                ExtendedProperties defaults = new ExtendedProperties();
                defaults.load(is);
                props.combine(defaults);
            }
        }
        catch (Exception e) {
            log.error("Error while retrieving solr url", e);
            e.printStackTrace();
        }
        finally {
            if (is != null) {
                is.close();
            }
        }

        if(props.getProperty("solr.search.server") != null)
        {
            this.solrServerUrl = props.getProperty("solr.search.server").toString();
        }

    }

    public void generate() throws IOException, SAXException, ProcessingException {
        if(solrServerUrl == null)
        {
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        String solrRequestUrl = solrServerUrl + "/select";

        //Add our default parameters
        params.put(CommonParams.ROWS, "0");
        params.put(CommonParams.WT, "json");
        //We uwe json as out output type
        params.put("json.nl", "map");
        params.put("json.wrf", jsonWrf);
        params.put(FacetParams.FACET, Boolean.TRUE.toString());

        //Generate our json out of the given params
        try
        {
            params.put(CommonParams.Q, URLEncoder.encode(query, Constants.DEFAULT_ENCODING));
        }
        catch (UnsupportedEncodingException uee)
        {
            //Should never occur
            return;
        }

        params.put(FacetParams.FACET_LIMIT, String.valueOf(facetLimit));
        if(facetSort != null)
        {
            params.put(FacetParams.FACET_SORT, facetSort);
        }
        params.put(FacetParams.FACET_MINCOUNT, String.valueOf(facetMinCount));

        solrRequestUrl = AbstractDSpaceTransformer.generateURL(solrRequestUrl, params);
        if (facetFields != null || filterQueries != null) {
            StringBuilder urlBuilder = new StringBuilder(solrRequestUrl);
            if(facetFields != null){

                //Add our facet fields
                for (String facetField : facetFields) {
                    urlBuilder.append("&").append(FacetParams.FACET_FIELD).append("=");

                    //This class can only be used for autocomplete facet fields
                    if(!facetField.endsWith(".year") && !facetField.endsWith("_ac"))
                    {
                        urlBuilder.append(URLEncoder.encode(facetField + "_ac", Constants.DEFAULT_ENCODING));
                    }
                    else
                    {
                        urlBuilder.append(URLEncoder.encode(facetField, Constants.DEFAULT_ENCODING));
                    }
                }

            }
            if(filterQueries != null){
                for (String filterQuery : filterQueries) {
                    urlBuilder.append("&").append(CommonParams.FQ).append("=").append(URLEncoder.encode(filterQuery, Constants.DEFAULT_ENCODING));
                }
            }

            solrRequestUrl = urlBuilder.toString();
        }

        try {
            GetMethod get = new GetMethod(solrRequestUrl);
            new HttpClient().executeMethod(get);
            String result = get.getResponseBodyAsString();
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
        } catch (Exception e) {
            log.error("Error while getting json solr result for discovery search recommendation", e);
            e.printStackTrace();
        }


    }
}

/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import fi.helsinki.lib.simplerest.stubs.StubCollection;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.util.Series;


/**
 *
 * @author moubarik
 */
public class AllCollectionsResource extends BaseResource{
    
    private static Logger log = Logger.getLogger(AllCollectionsResource.class);
    
    private Collection[] allCollections;
    private Context context;
    
    @Options
    public void doOptions(Representation entity) {
        Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (responseHeaders == null) {
            responseHeaders = new Series(Header.class);
            getResponse().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS,
                    responseHeaders);
        }
        responseHeaders.add(new Header("Access-Control-Allow-Origin", "*"));
    }

    public AllCollectionsResource(Collection[] collections){
        this.allCollections = collections;
    }
    
    public AllCollectionsResource(){
        this.allCollections = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.INFO, e);
        }
        try{
            this.allCollections = Collection.findAll(context);
        }catch(Exception e){
            log.log(Priority.INFO, e);
        }finally{
            context.abort();
        }
    }
    
    static public String relativeUrl(int dummy){
        return "collections";
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        Gson gson = new Gson();
        GetOptions.allowAccess(getResponse());
        ArrayList<StubCollection> toJsonCollections = new ArrayList<StubCollection>(25);
        for(Collection c : allCollections){
            toJsonCollections.add(new StubCollection(c));
        }
        
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        
        return gson.toJson(toJsonCollections);
    }
}

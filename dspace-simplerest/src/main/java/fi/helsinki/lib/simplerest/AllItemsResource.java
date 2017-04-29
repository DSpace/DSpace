
package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import fi.helsinki.lib.simplerest.stubs.StubItem;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;

/**
 *
 * @author moubarik
 */
public class AllItemsResource extends BaseResource{
    
    private static Logger log = Logger.getLogger(AllItemsResource.class);
    
    private Community[] allCommunities;
    private Collection[] allCollections;
    private Item[] allItems;
    private Context context;
    
    public AllItemsResource(Community[] communities, Collection[] collections, Item[] items){
        this.allCommunities = communities;
        this.allCollections = collections;
        this.allItems = items;
    }
    
    public AllItemsResource(){
        this.allCollections = null;
        this.allCommunities = null;
        this.allItems = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.INFO, e);
        }
        try{
            this.allCollections = Collection.findAll(context);
        }catch(Exception e){
            log.log(Priority.INFO, e);
        }
    }
    
    static public String relativeUrl(int dummy){
        return "items";
    }
    
    @Options
    public void doOptions(Representation entity){
        GetOptions.allowAccess(getResponse());
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        GetOptions.allowAccess(getResponse());
        Gson gson = new Gson();
        ArrayList<StubItem> toJsonItems = new ArrayList<StubItem>(100);
        for(Collection c : allCollections){
            ItemIterator i = c.getAllItems();
            while(i.hasNext()){
                toJsonItems.add(new StubItem(i.next()));
            }
        }
        try{
            this.context.abort();
        }catch(NullPointerException e){
            log.log(Priority.FATAL, e);
        }
        return gson.toJson(toJsonItems);
    }
    
}

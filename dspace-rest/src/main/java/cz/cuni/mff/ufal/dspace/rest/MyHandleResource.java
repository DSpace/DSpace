package cz.cuni.mff.ufal.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.common.Handle;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandlePlugin;
import org.dspace.rest.Resource;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@Disable
@Path("/services")
public class MyHandleResource extends Resource {
    private static Logger log = Logger.getLogger(MyHandleResource.class);
    private static Logger logHandleHistory = Logger.getLogger("HandleHistory");

    public enum ORDER {ASC, asc, DESC, desc}

    @GET
    @Path("/handles/magic")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Handle[] getHandles(@QueryParam("limit") @DefaultValue("10") Integer limit,
                               @QueryParam("offset") @DefaultValue("0") Integer offset,
                               @QueryParam("order") @DefaultValue("DESC") ORDER order){
        org.dspace.core.Context context = null;
        if (limit == null || limit < 0 || offset == null || offset < 0)
        {
            log.warn("offset/limit was badly set, using default values.");
            limit = 10;
            offset = 0;
        }
        if (limit > 100){
            limit = 100;
        }
        //compute the actual offset in rows
        offset *= limit;
        List<Handle> result = new ArrayList<>();
        try {
            context = new org.dspace.core.Context();
            String query = "select * from handle where url like '" + HandlePlugin.magicBean + "%' " +
                    "order by handle_id " + order.toString() + " limit ? offset ?;";
            Integer[] params = new Integer[]{limit, offset};
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            List<TableRow> rows = tri.toList();
            for(TableRow row : rows){
                String magicURL = row.getStringColumn("url");
                String hdl = row.getStringColumn("handle");
                Handle handle = new Handle(hdl, magicURL);
                //don't show the token on browse
                handle.token = null;
                result.add(handle);
            }
            context.complete();
        } catch (SQLException e) {
            processException("Could not read /services/handles, SQLException. Message: " + e.getMessage(), context);
        }
        return result.toArray(new Handle[0]);
    }

    @POST
    @Path("/handles")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Handle shortenHandle(Handle handle){
        org.dspace.core.Context context = null;
        if(isNotBlank(handle.url) && !handle.url.contains(HandlePlugin.magicBean)
                && isNotBlank(handle.title) && isNotBlank(handle.reportemail)){
            try {
                context = new org.dspace.core.Context();
                String submitdate = new DCDate(new Date()).toString();
                handle.submitdate = submitdate;
                String subprefix = (isNotBlank(handle.subprefix)) ? handle.subprefix + "-" : "";
                String magicURL = handle.getMagicUrl();
                String hdl = createHandle(subprefix, magicURL, context);
                context.complete();
                return new Handle(hdl, magicURL);
            }catch (SQLException e){
                processException("Could not create handle, SQLException. Message: " + e.getMessage(), context);
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @PUT
    @Path("/handles")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Handle updateHandle(Handle updatedHandle){
        org.dspace.core.Context context = null;
        if(isNotBlank(updatedHandle.url) && !updatedHandle.url.contains(HandlePlugin.magicBean)
                && isNotBlank(updatedHandle.handle) && isNotBlank(updatedHandle.token)){
            try {
                context = new org.dspace.core.Context();
                String query = "select * from handle where handle = ?  and " +
                        "url like ?";
                String[] params = new String[]{updatedHandle.handle, "%" + updatedHandle.token.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![") + "%"};
                TableRowIterator tri = DatabaseManager.query(context,query, params);
                List<TableRow> rows = tri.toList();
                if(rows.size() == 1){
                    TableRow row = rows.get(0);
                    String magicURL = row.getStringColumn("url");
                    String hdl = row.getStringColumn("handle");
                    Handle oldHandle = new Handle(hdl, magicURL);
                    logHandleHistory.info(String.format("Handle [%s] changed url from \"%s\" to \"%s\".", hdl, oldHandle.url, updatedHandle.url));
                    //do the update
                    oldHandle.url = updatedHandle.url;
                    String newMagicUrl = oldHandle.getMagicUrl();
                    row.setColumn("url", newMagicUrl);
                    //No idea why table is null at this point
                    row.setTable("handle");
                    DatabaseManager.update(context, row);
                    context.complete();
                    return new Handle(oldHandle.handle,newMagicUrl);
                }
            }catch (SQLException e){
                processException("Could not update handle, SQLException. Message: " + e.getMessage(), context);
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    private static final String prefix;
    static{
        String tmpPrefix = ConfigurationManager.getProperty("lr", "shortener.handle.prefix");
        if(isNotBlank(tmpPrefix)){
            prefix = tmpPrefix;
        }
        else{
            prefix = "123456789";
        }
    }

    private String createHandle(String subprefix, String url, org.dspace.core.Context context) throws SQLException{
        String handle;
        TableRowIterator tri;
        String query = "select * from handle where handle like ? ;";
        while(true){
            String rnd = RandomStringUtils.random(4,true,true).toUpperCase();
            handle = prefix + "/" + subprefix + rnd;
            tri = DatabaseManager.query(context, query, handle);
            if(!tri.hasNext()){
                //no row matches stop generation;
                break;
            }
        }
        TableRow row = DatabaseManager.row("handle");
        row.setColumn("handle", handle);
        row.setColumn("url", url);
        DatabaseManager.insert(context, row);
        return handle;
    }
}


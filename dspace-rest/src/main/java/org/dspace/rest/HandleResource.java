/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.ExportFormats;
import cz.cuni.mff.ufal.dspace.rest.FeaturedService;
import cz.cuni.mff.ufal.dspace.rest.FeaturedServices;
import cz.cuni.mff.ufal.dspace.rest.RefBoxData;
import cz.cuni.mff.ufal.dspace.rest.citation.formats.AbstractFormat;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.common.DSpaceObject;
import org.dspace.rest.common.Item;
import org.dspace.rest.exceptions.ContextException;

import se.kb.oai.OAIException;
import se.kb.oai.pmh.ErrorResponseException;
import se.kb.oai.pmh.OaiPmhServer;
import se.kb.oai.pmh.Record;

import javax.security.auth.login.Configuration;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/handle")
public class HandleResource extends Resource {
    private static Logger log = Logger.getLogger(HandleResource.class);

    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.DSpaceObject getObject(@PathParam("prefix") String prefix,
            @PathParam("suffix") String suffix, @QueryParam("expand") String expand,
            @Context HttpHeaders headers) throws WebApplicationException{
        org.dspace.core.Context context = null;
        DSpaceObject result = null;

        try {
            context = createContext(getUser(headers));

            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dso.getTypeText() + "_" + dso.getID());

            if(AuthorizeManager.authorizeActionBoolean(context, dso, org.dspace.core.Constants.READ)) {
                switch(dso.getType()) {
                    case Constants.COMMUNITY:
                        result = new Community((org.dspace.content.Community) dso, expand, context);
                        break;
                    case Constants.COLLECTION:
                        result =  new Collection((org.dspace.content.Collection) dso, expand, context, null, null);
                        break;
                    case Constants.ITEM:
                        result =  new Item((org.dspace.content.Item) dso, expand, context);
                        break;
                    default:
                        result = new DSpaceObject(dso);
                }
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            context.complete();

        } catch (SQLException e) {
            processException("Could not read handle(" + prefix  + "/" + suffix + "), SQLException. Message: " + e.getMessage(), context);
        } catch (ContextException e) {
            processException("Could not read handle(" + prefix  + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        } finally{
           processFinally(context);
        }

        return result;
    }

    @GET
    @Path("/{prefix}/{suffix}/services")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public FeaturedServices getFeaturedServices(@PathParam("prefix") String prefix,
                                                @PathParam("suffix") String suffix, @QueryParam("expand") String expand,
                                                @Context HttpHeaders headers) throws WebApplicationException {
        org.dspace.core.Context context = null;
        try {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if (dso == null || dso.getType() != Constants.ITEM) {
                context.complete();
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            FeaturedServices featuredServices = getFeaturedServices((org.dspace.content.Item) dso);
            context.complete();
            return featuredServices;
        } catch (SQLException e) {
            processException("Could not read handle(" + prefix + "/" + suffix + "/services), SQLException. Message: " + e.getMessage(), context);
        } catch (ContextException e) {
            processException("Could not read handle(" + prefix + "/" + suffix + "/services), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }
        return null;
    }

    private FeaturedServices getFeaturedServices(org.dspace.content.Item item) throws SQLException {    	
        FeaturedServices featuredServices = new FeaturedServices();
        String services = ConfigurationManager.getProperty("lr", "featured.services");
        if(services!=null && !services.isEmpty()) {
	        for(String service : services.split(",")) {
	        	Metadatum[] mds = item.getMetadataByMetadataString("local.featuredService." + service);
	        	if(mds!=null && mds.length!=0) {
        			String name = ConfigurationManager.getProperty("lr", "featured.service." + service + ".fullname");
        			String url = ConfigurationManager.getProperty("lr", "featured.service." + service + ".url");
        			String description = ConfigurationManager.getProperty("lr", "featured.service." + service + ".description");	        		
        			FeaturedService fs = new FeaturedService(name, url, description);	        		
	        		for(Metadatum md : mds) {
	        			String key_value[] = md.value.split("\\|");
	        			fs.addLink(key_value[0], key_value[1]);
	        		}
	        		featuredServices.add(fs);
	        	}
	        }
        }
        return featuredServices;
    }

    @GET
    @Path("/{prefix}/{suffix}/citations/{format}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public AbstractFormat getCitations(@PathParam("prefix") String prefix,
                                         @PathParam("suffix") String suffix,
                                         @PathParam("format") String format,
                                         @QueryParam("expand") String expand,
                                         @Context HttpHeaders headers) throws WebApplicationException {
        org.dspace.core.Context context = null;
        try {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if (dso == null || dso.getType() != Constants.ITEM) {
                context.complete();
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            context.complete();
            return getCitation(prefix, suffix, format);
        } catch (Exception e) {
            processException("Could not read handle(" + prefix + "/" + suffix + "/citations/" + format +"), "+ e.getClass().getName() +". Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }
        return null;
    }

    @GET
    @Path("/{prefix}/{suffix}/refbox")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public RefBoxData getRefBoxData(@PathParam("prefix") String prefix,
                               @PathParam("suffix") String suffix,
                               @Context HttpHeaders headers) throws WebApplicationException {
        org.dspace.core.Context context = null;
        try {
            context = createContext(getUser(headers));
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if (dso == null || dso.getType() != Constants.ITEM) {
                context.complete();
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            org.dspace.content.Item item = (org.dspace.content.Item) dso;
            String title = item.getMetadata("dc.title");
            AbstractFormat displayText = null;
            ExportFormats exportFormats = null;
            try{
                displayText = getCitation(prefix, suffix, "html");
                exportFormats = ExportFormats.getFormats(prefix + "/" + suffix);
            }catch(ErrorResponseException e){
                if(e.getCode().equals(ErrorResponseException.ID_DOES_NOT_EXIST)){
                    //Refbox for item with no OAI ID, eg. ResumeStep
                    displayText = new AbstractFormat("other");
                    displayText.setValue(String.format("<a href=\"%s\">%<s</a>",String.format("http://hdl.handle.net/%s/%s", prefix, suffix)));
                    exportFormats = new ExportFormats();
                }

            }
            FeaturedServices featuredServices = getFeaturedServices(item);

            RefBoxData refBox = new RefBoxData();
            refBox.setTitle(title);
            refBox.setDisplayText(displayText);
            refBox.setFeaturedServices(featuredServices);
            refBox.setExportFormats(exportFormats);

            context.complete();
            return refBox;
        } catch (Exception e) {
            processException("Could not read handle(" + prefix + "/" + suffix + "/refbox), "+ e.getClass().getName() +". Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }
        return null;
    }

    public static AbstractFormat getCitation(String prefix, String suffix, String format) throws OAIException, JAXBException, IOException {
        String urlProp = ConfigurationManager.getProperty("oai", "dspace.oai.url");
        String identifier = String.format("oai:%s:%s/%s", ConfigurationManager.getProperty("oai","identifier.prefix"), prefix, suffix);
        return getAbstractFormat(format, urlProp, identifier);
    }
    public static AbstractFormat getAbstractFormat(String format, String baseUrl, String identifier) throws JAXBException, IOException, OAIException {
        URL url = new URL(String.format("%s/request", baseUrl));
        OaiPmhServer server = new OaiPmhServer(url);
        Record record = server.getRecord(identifier, format);
        String metadataString = record.getMetadataAsString();
        AbstractFormat af = new AbstractFormat(format);
        af.setValue(metadataString);
        return af;
    }
}

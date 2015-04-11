/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.rest.common.Field;
import org.dspace.rest.common.Schema;

import com.google.gson.Gson;

/*
Export the DSpace Metadata Schema and Field Registries as JSON using the GSON library.
This exported file can be used in conjunction with the FileAnalyzer to validate metadata field while generating DSpace Ingest Folders.
* https://github.com/Georgetown-University-Libraries/File-Analyzer/wiki/Create-Ingest-Folders-for-a-Set-of-Files
The FileAnalyzer is a desktop application containing a number of automation tasks for library and digitization projects.  
A set of DSpace related tasks are included in the code base.
 */
@Path("/metadataregistry")
public class MetadataRegistry {
    @javax.ws.rs.core.Context public static ServletContext servletContext;

    /*
    The "GET" annotation indicates this method will respond to HTTP Get requests.
    The "Produces" annotation indicates the MIME response the method will return.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getMetadataRegistry() {
        try {
            Context context = new Context();
            List<Schema> mySchemas = new ArrayList<Schema>();
            MetadataSchema[] schemas = MetadataSchema.findAll(context);
            for(MetadataSchema schema: schemas) {
                Schema mySchema = new Schema(schema.getName(), schema.getNamespace());
                mySchemas.add(mySchema);
                MetadataField[] fields = MetadataField.findAllInSchema(context, schema.getSchemaID());
                for(MetadataField field: fields){
                    String fname = Field.makeName(mySchema.prefix(), field.getElement(), field.getQualifier());
                    mySchema.addField(fname, field.getElement(), field.getQualifier(), field.getScopeNote());
                }
            }
            Gson gson = new Gson();
            String s = gson.toJson(mySchemas);
            return s;
        } catch (Exception e) {
            return "{\"error\":\""+e.getMessage()+"\"}";
        }
        
    }
}

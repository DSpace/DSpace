/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class MetadataConverterPlugin implements ConverterPlugin
{
    public final static String METADATA_MAPPING_PATH_KEY = "rdf.metadata.mappings";
    public final static String METADATA_SCHEMA_URL_KEY = "rdf.metadata.schema";
    public final static String METADATA_PREFIXES_KEY = "rdf.metadata.prefixes";
    
    private final static Logger log = Logger.getLogger(MetadataConverterPlugin.class);
    @Autowired(required=true)
    protected ConfigurationService configurationService;
    
    @Override
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public Model convert(Context context, DSpaceObject dso)
            throws SQLException, AuthorizeException {
        String uri = RDFUtil.generateIdentifier(context, dso);
        DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        if (uri == null)
        {
            log.error("Cannot create URI for " + dsoService.getTypeText(dso) + " " 
                    + dso.getID() + " stopping conversion.");
            return null;
        }

        Model convertedData = ModelFactory.createDefaultModel();
        String prefixesPath = configurationService.getProperty(METADATA_PREFIXES_KEY);
        if (!StringUtils.isEmpty(prefixesPath))
        {
            InputStream is = FileManager.get().open(prefixesPath);
            if (is == null)
            {
                log.warn("Cannot find file '" + prefixesPath + "', ignoring...");
            } else {
                convertedData.read(is, null, FileUtils.guessLang(prefixesPath));
                try {
                    is.close();
                }
                catch (IOException ex)
                {
                    // nothing to do here.
                }
            }
        }
        
        Model config = loadConfiguration();
        if (config == null)
        {
            log.error("Cannot load MetadataConverterPlugin configuration, "
                    + "skipping this plugin.");
            return null;
        }
        /*
        if (log.isDebugEnabled())
        {
            StringWriter sw = new StringWriter();
            sw.append("Inferenced the following model:\n");
            config.write(sw, "TURTLE");
            sw.append("\n");
            log.debug(sw.toString());
            try {
                sw.close();
            } catch (IOException ex) {
                // nothing to do here
            }
        }
        */

        ResIterator mappingIter = 
                config.listSubjectsWithProperty(RDF.type, DMRM.DSpaceMetadataRDFMapping);
        if (!mappingIter.hasNext())
        {
            log.warn("No metadata mappings found, returning null.");
            return null;
        }
        
        List<MetadataRDFMapping> mappings = new ArrayList<>();
        while (mappingIter.hasNext())
        {
            MetadataRDFMapping mapping = MetadataRDFMapping.getMetadataRDFMapping(
                    mappingIter.nextResource(), uri);
            if (mapping != null) mappings.add(mapping);
        }
        
        // should be changed, if Communities and Collections have metadata as well.
        if (!(dso instanceof Item))
        {
            log.error("This DspaceObject (" + dsoService.getTypeText(dso) + " " 
                    + dso.getID() + ") should not have bin submitted to this "
                    + "plugin, as it supports Items only!");
            return null;
        }
        
        List<MetadataValue> metadata_values = dsoService.getMetadata(dso, MetadataSchema.DC_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue value : metadata_values)
        {
            MetadataField metadataField = value.getMetadataField();
            MetadataSchema metadataSchema = metadataField.getMetadataSchema();
            String fieldname = metadataSchema.getName() + "." + metadataField.getElement();
            if (metadataField.getQualifier() != null) 
            {
                fieldname = fieldname + "." + metadataField.getQualifier();
            }
            if (UtilServiceFactory.getInstance().getMetadataExposureService().isHidden(context, metadataSchema.getName(), metadataField.getElement(),
                    metadataField.getQualifier()))
            {
                log.debug(fieldname + " is a hidden metadata field, won't "
                        + "convert it.");
                continue;
            }

            boolean converted = false;
            if (metadataField.getQualifier() != null)
            {
                Iterator<MetadataRDFMapping> iter = mappings.iterator();
                while (iter.hasNext())
                {
                    MetadataRDFMapping mapping = iter.next();
                    if (mapping.matchesName(fieldname) && mapping.fulfills(value.getValue()))
                    {
                        mapping.convert(value.getValue(), value.getLanguage(), uri, convertedData);
                        converted = true;
                    }
                }
            }
            if (!converted)
            {
                String name = metadataSchema.getName() + "." + metadataField.getElement();
                Iterator<MetadataRDFMapping> iter = mappings.iterator();
                while (iter.hasNext() && !converted)
                {
                    MetadataRDFMapping mapping = iter.next();
                    if (mapping.matchesName(name) && mapping.fulfills(value.getValue()))
                    {
                        mapping.convert(value.getValue(), value.getLanguage(), uri, convertedData);
                        converted = true;
                    }
                }
            }
            if (!converted)
            {
                log.debug("Did not convert " + fieldname + ". Found no "
                        + "corresponding mapping.");
            }
        }
        config.close();
        if (convertedData.isEmpty())
        {
            convertedData.close();
            return null;
        }
        return convertedData;
    }

    @Override
    public boolean supports(int type) {
        // should be changed, if Communities and Collections have metadata as well.
        return (type == Constants.ITEM);
    }
    
    protected Model loadConfiguration()
    {
        InputStream is = null;
        Model config = ModelFactory.createDefaultModel();
        String mapping = configurationService.getProperty(METADATA_MAPPING_PATH_KEY);
        if (StringUtils.isEmpty(mapping))
        {
            log.error("Cannot find metadata mappings (looking for "
                    + "property " + METADATA_MAPPING_PATH_KEY + ")!");
            return null;
        }
        else
        {
            is = FileManager.get().open(mapping);
            if (is == null)
            {
                log.warn("Cannot find file '" + mapping + "', ignoring...");
            }
            config.read(is, "file://" + mapping, FileUtils.guessLang(mapping));
            try {
                // Make sure that we have an input stream to avoid NullPointer
                if(is != null)
                {
                    is.close();
                }
            }
            catch (IOException ex)
            {
                // nothing to do here.
            }
        }
        if (config.isEmpty())
        {
            config.close();
            log.warn("Metadata RDF Mapping did not contain any triples!");
            return null;
        }
        
        String schemaURL = configurationService.getProperty(METADATA_SCHEMA_URL_KEY);
        if (schemaURL == null)
        {
            log.error("Cannot find metadata rdf mapping schema (looking for "
                    + "property " + METADATA_SCHEMA_URL_KEY + ")!");
        }
        if (!StringUtils.isEmpty(schemaURL))
        {
            log.debug("Going to inference over the rdf metadata mapping.");
            // Inferencing over the configuration data let us detect some rdf:type
            // properties out of rdfs:domain and rdfs:range properties
            // A simple rdfs reasoner is enough for this task.
            Model schema = ModelFactory.createDefaultModel();
            schema.read(schemaURL);
            Reasoner reasoner = ReasonerRegistry.getRDFSSimpleReasoner().bindSchema(schema);
            InfModel inf = ModelFactory.createInfModel(reasoner, config);

            // If we do inferencing, we can easily check for consistency.
            ValidityReport reports = inf.validate();
            if (!reports.isValid())
            {
                StringBuilder sb = new StringBuilder();
                sb.append("The configuration of the MetadataConverterPlugin is ");
                sb.append("not valid regarding the schema (");
                sb.append(DMRM.getURI());
                sb.append(").\nThe following problems were encountered:\n");
                for (Iterator<ValidityReport.Report> iter = reports.getReports();
                        iter.hasNext() ; )
                {
                    ValidityReport.Report report = iter.next();
                    if (report.isError)
                    {
                        sb.append(" - " + iter.next() + "\n");
                    }
                }
                log.error(sb.toString());
                return null;
            }
            return inf;
        }
        return config;
    }
    
}

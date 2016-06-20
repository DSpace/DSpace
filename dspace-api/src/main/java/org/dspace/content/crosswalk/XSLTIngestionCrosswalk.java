/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Configurable XSLT-driven ingestion Crosswalk
 * <p>
 * See the XSLTCrosswalk superclass for details on configuration.
 *
 * @author Larry Stone
 * @see XSLTCrosswalk
 */
public class XSLTIngestionCrosswalk
    extends XSLTCrosswalk
    implements IngestionCrosswalk
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(XSLTIngestionCrosswalk.class);

    private static final String DIRECTION = "submission";

    private static final String aliases[] = makeAliases(DIRECTION);

    private static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
    }

    // apply metadata values returned in DIM to the target item.
    private static void applyDim(Context context, List<Element> dimList, Item item, boolean createMissingMetadataFields)
            throws CrosswalkException, SQLException, AuthorizeException {
        for (Element elt : dimList)
        {
            if ("field".equals(elt.getName()) && DIM_NS.equals(elt.getNamespace()))
            {
                applyDimField(context, elt, item, createMissingMetadataFields);
            }
            else if ("dim".equals(elt.getName()) && DIM_NS.equals(elt.getNamespace()))
            {
                // if it's a <dim> container, apply its guts
                applyDim(context, elt.getChildren(), item, createMissingMetadataFields);
            }
            else
            {
                log.error("Got unexpected element in DIM list: "+elt.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+elt.toString());
            }
        }
    }

    // adds the metadata element from one <field>
    private static void applyDimField(Context context, Element field, Item item, boolean createMissingMetadataFields) throws CrosswalkException, SQLException, AuthorizeException {
        String schema = field.getAttributeValue("mdschema");
        String element = field.getAttributeValue("element");
        String qualifier = field.getAttributeValue("qualifier");
        String lang = field.getAttributeValue("lang");
        String authority = field.getAttributeValue("authority");
        String sconf = field.getAttributeValue("confidence");

        CrosswalkMetadataValidator metadataValidator = new CrosswalkMetadataValidator();
        MetadataField metadataField = metadataValidator.checkMetadata(context, schema, element, qualifier, createMissingMetadataFields);
        // sanity check: some XSL puts an empty string in qualifier,
        // change it to null so we match the unqualified DC field:
        if (qualifier != null && qualifier.equals(""))
        {
            qualifier = null;
        }

        if ((authority != null && authority.length() > 0) ||
            (sconf != null && sconf.length() > 0))
        {
            int confidence = (sconf != null && sconf.length() > 0) ?
                    Choices.getConfidenceValue(sconf) : Choices.CF_UNSET;
            itemService.addMetadata(context, item, metadataField, lang, field.getText(), authority, confidence);
        }
        else
        {
            itemService.addMetadata(context, item, metadataField, lang, field.getText());
        }
    }

    /**
     * Translate metadata with XSL stylesheet and ingest it.
     * Translation produces a list of DIM "field" elements;
     * these correspond directly to Item.addMetadata() calls so
     * they are simply executed.
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException crosswalk error
     * @throws IOException if IO error 
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> metadata,
            boolean createMissingMetadataFields)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        Transformer xform = getTransformer(DIRECTION);
        if (xform == null)
        {
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        }
        try
        {
            JDOMResult result = new JDOMResult();
            xform.transform(new JDOMSource(metadata), result);
            ingestDIM(context, dso, result.getResult(), createMissingMetadataFields);
        }
        catch (TransformerException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL Transformation failed: "+e.toString(), e);
        }
    }

    /**
     * Ingest a whole document.  Build Document object around root element,
     * and feed that to the transformation, since it may get handled
     * differently than a List of metadata elements.
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException crosswalk error
     * @throws IOException if IO error 
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        Transformer xform = getTransformer(DIRECTION);
        if (xform == null)
        {
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        }
        try
        {
            JDOMSource source = new JDOMSource(new Document((Element)root.cloneContent()));
            JDOMResult result = new JDOMResult();
            xform.transform(source, result);
            Document dimDoc = result.getDocument();
            ingestDIM(context, dso, dimDoc.getRootElement().getChildren(), createMissingMetadataFields);
        }
        catch (TransformerException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL Transformation failed: "+e.toString(), e);
        }

    }

    // return coll/comm "metadata" label corresponding to a DIM field.
    private static String getMetadataForDIM(Element field)
    {
        // make up fieldname, then look for it in xwalk
        String element = field.getAttributeValue("element");
        String qualifier = field.getAttributeValue("qualifier");
        String fname = "dc." + element;
        if (qualifier != null)
        {
            fname += "." + qualifier;
        }
        return PackageUtils.dcToContainerMetadata(fname);
    }

    /**
     * Ingest a DIM metadata expression directly, without
     * translating some other format into DIM.
     * The <code>dim</code> element is expected to be be the root of
     * a DIM document.
     * <p>
     * Note that this is ONLY implemented for Item, Collection, and
     * Community objects.  Also only works for the "dc" metadata schema.
     * <p>
     * @param context the context
     * @param dso object into which to ingest metadata
     * @param  dim root of a DIM expression
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException crosswalk error
     * @throws IOException if IO error 
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */

    public static void ingestDIM(Context context, DSpaceObject dso, Element dim, boolean createMissingMetadataFields)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        ingestDIM(context, dso, dim.getChildren(), createMissingMetadataFields);
    }

    public static void ingestDIM(Context context, DSpaceObject dso, List<Element> fields, boolean createMissingMetadataFields)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();
        if (type == Constants.ITEM)
        {
            Item item = (Item)dso;
            applyDim(context, fields, item, createMissingMetadataFields);
        }
        else if (type == Constants.COLLECTION ||
                 type == Constants.COMMUNITY)
        {
            for (Element field : fields)
            {
                String schema = field.getAttributeValue("mdschema");
                if ("dim".equals(field.getName()) && DIM_NS.equals(field.getNamespace()))
                {
                    ingestDIM(context, dso, field.getChildren(), createMissingMetadataFields);
                }
                else if ("field".equals(field.getName()) &&
                        DIM_NS.equals(field.getNamespace()) &&
                    schema != null && "dc".equals(schema))
                {
                    String md = getMetadataForDIM(field);
                    if (md == null)
                    {
                        log.warn("Cannot map to Coll/Comm metadata field, DIM element=" +
                                field.getAttributeValue("element") + ", qualifier=" + field.getAttributeValue("qualifier"));
                    }
                    else
                    {
                        if (type == Constants.COLLECTION)
                        {
                            collectionService.setMetadata(context, (Collection) dso, md, field.getText());
                        }
                        else
                        {
                            communityService.setMetadata(context, (Community) dso, md, field.getText());
                        }
                    }
                }

                else
                {
                    log.warn("ignoring unrecognized DIM element: " + field.toString());
                }
            }
        }
        else
        {
            throw new CrosswalkObjectNotSupported("XsltSubmissionionCrosswalk can only crosswalk to an Item.");
        }

    }


    /**
     * Simple command-line rig for testing the DIM output of a stylesheet.
     * Usage: {@code java XSLTIngestionCrosswalk  <crosswalk-name> <input-file>}
     * @param argv arguments
     * @throws Exception if error
     */
    public static void main(String[] argv) throws Exception
    {
        if (argv.length < 2)
        {
            System.err.println("Usage:  java XSLTIngestionCrosswalk [-l] <crosswalk-name> <input-file>");
            System.exit(1);
        }

        int i = 0;
        boolean list = false;
        // skip first arg if it's the list option
        if (argv.length > 2 && argv[0].equals("-l"))
        {
            ++i;
            list = true;
        }
        IngestionCrosswalk xwalk = (IngestionCrosswalk) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(
                IngestionCrosswalk.class, argv[i]);
        if (xwalk == null)
        {
            System.err.println("Error, cannot find an IngestionCrosswalk plugin for: \""+argv[i]+"\"");
            System.exit(1);
        }

        Transformer xform = ((XSLTIngestionCrosswalk)xwalk).getTransformer(DIRECTION);
        if (xform == null)
        {
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        }

        SAXBuilder builder = new SAXBuilder();
        Document inDoc = builder.build(new FileInputStream(argv[i+1]));
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        List dimList;
        if (list)
        {
            JDOMSource source = new JDOMSource(inDoc.getRootElement().getChildren());
            JDOMResult result = new JDOMResult();
            xform.transform(source, result);
            dimList = result.getResult();
            outputter.output(dimList, System.out);
        }
        else
        {
            JDOMSource source = new JDOMSource(inDoc);
            JDOMResult result = new JDOMResult();
            xform.transform(source, result);
            Document dimDoc = result.getDocument();
            outputter.output(dimDoc, System.out);
            dimList = dimDoc.getRootElement().getChildren();
        }

        // Sanity-check the generated DIM, make sure it would load.
        Context context = new Context();
        Iterator di = dimList.iterator();
        while (di.hasNext())
        {
            // skip over comment, text and other trash some XSLs generate..
            Object o = di.next();
            if (!(o instanceof Element))
            {
                continue;
            }

            Element elt = (Element)o;
            if (elt.getName().equals("field") && elt.getNamespace().equals(DIM_NS))
            {
                String schema = elt.getAttributeValue("mdschema");
                String element = elt.getAttributeValue("element");
                String qualifier = elt.getAttributeValue("qualifier");
                MetadataSchema ms = ContentServiceFactory.getInstance().getMetadataSchemaService().find(context, schema);
                if (ms == null )
                {
                    System.err.println("DIM Error, Cannot find metadata schema for: schema=\""+schema+
                        "\" (... element=\""+element+"\", qualifier=\""+qualifier+"\")");
                }
                else
                {
                    if (qualifier != null && qualifier.equals(""))
                    {
                        System.err.println("DIM Warning, qualifier is empty string: "+
                              " schema=\""+schema+"\", element=\""+element+"\", qualifier=\""+qualifier+"\"");
                        qualifier = null;
                    }
                    MetadataField mf = ContentServiceFactory.getInstance().getMetadataFieldService().findByElement(context,
                            ms, element, qualifier);
                    if (mf == null)
                    {
                        System.err.println("DIM Error, Cannot find metadata field for: schema=\"" + schema +
                                "\", element=\"" + element + "\", qualifier=\"" + qualifier + "\"");
                    }
                }
            }
            else
            {
                // ("Got unexpected element in DIM list: "+elt.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+elt.toString());
            }
        }
        //complete & close Context
        context.complete();
    }

}

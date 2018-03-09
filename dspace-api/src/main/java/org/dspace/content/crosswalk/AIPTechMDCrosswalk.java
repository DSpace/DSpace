/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.sql.SQLException;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;
import org.dspace.content.packager.PackageUtils;
import org.dspace.eperson.EPerson;
import org.dspace.authorize.AuthorizeException;

import org.apache.log4j.Logger;
import org.dspace.content.packager.DSpaceAIPIngester;
import org.dspace.content.packager.METSManifest;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Crosswalk of technical metadata for DSpace AIP.  This is
 * only intended for use by the METS AIP packager.   It borrows the
 * DIM XML format and DC field names, although it abuses the meaning
 * of Dublin Core terms and qualifiers because this format is
 * ONLY FOR DSPACE INTERNAL USE AND INGESTION.  It is needed to record
 * a complete and accurate image of all of the attributes an object
 * has in the RDBMS.
 *
 * It encodes the following common properties of all archival objects:
 *
 *   identifier.uri -- persistent identifier of object in URI form (e.g. Handle URN)
 *   relation.isPartOf -- persistent identifier of object's parent in URI form (e.g. Handle URN)
 *   relation.isReferencedBy -- if relevant, persistent identifier of other objects that map this one as a child.  May repeat.
 *
 * There may also be other fields, depending on the type of object,
 * which encode attributes that are not part of the descriptive metadata and
 * are not adequately covered by other technical MD formats (i.e. PREMIS).
 *
 *  Configuration entries:
 *    aip.ingest.createEperson -- boolean, create EPerson for Submitter
 *              automatically, on ingest, if it doesn't exist.
 *
 * @author Larry Stone
 * @version $Revision: 1.2 $
 */
public class AIPTechMDCrosswalk implements IngestionCrosswalk, DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AIPTechMDCrosswalk.class);
    protected final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected final SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Get XML namespaces of the elements this crosswalk may return.
     * Returns the XML namespaces (as JDOM objects) of the root element.
     *
     * @return array of namespaces, which may be empty.
     */
    @Override
    public Namespace[] getNamespaces()
    {
        Namespace result[] = new Namespace[1];
        result[0] = XSLTCrosswalk.DIM_NS;
        return result;
    }

    /**
     * Get the XML Schema location(s) of the target metadata format.
     * Returns the string value of the <code>xsi:schemaLocation</code>
     * attribute that should be applied to the generated XML.
     *  <p>
     * It may return the empty string if no schema is known, but crosswalk
     * authors are strongly encouraged to implement this call so their output
     * XML can be validated correctly.
     * @return SchemaLocation string, including URI namespace, followed by
     *  whitespace and URI of XML schema document, or empty string if unknown.
     */
    @Override
    public String getSchemaLocation()
    {
        return "";
    }

    /**
     * Predicate: Can this disseminator crosswalk the given object.
     * Needed by OAI-PMH server implementation.
     *
     * @param dso  dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        //can only Disseminate SITE, COMMUNITY, COLLECTION, ITEM, BITSTREAM
        return (dso.getType()==Constants.SITE
                || dso.getType()==Constants.COMMUNITY
                || dso.getType()==Constants.COLLECTION
                || dso.getType()==Constants.ITEM
                || dso.getType()==Constants.BITSTREAM);
    }

    /**
     * Predicate: Does this disseminator prefer to return a list of Elements,
     * rather than a single root Element?
     * <p>
     * Some metadata formats have an XML schema without a root element,
     * for example, the Dublin Core and Qualified Dublin Core formats.
     * This would be <code>true</code> for a crosswalk into QDC, since
     * it would "prefer" to return a list, since any root element it has
     * to produce would have to be part of a nonstandard schema.  In
     * most cases your implementation will want to return
     * <code>false</code>
     *
     * @return true when disseminator prefers you call disseminateList().
     */
    @Override
    public boolean preferList()
    {
        return false;
    }

    /**
     * Execute crosswalk, returning List of XML elements.
     * Returns a <code>List</code> of JDOM <code>Element</code> objects representing
     * the XML produced by the crosswalk.  This is typically called when
     * a list of fields is desired, e.g. for embedding in a METS document
     * <code>xmlData</code> field.
     * <p>
     * When there are no results, an
     * empty list is returned, but never <code>null</code>.
     *
     * @param context context
     * @param dso the  DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException
    {
        Element dim = disseminateElement(context, dso);
        return dim.getChildren();
    }

    /**
     * Execute crosswalk, returning one XML root element as
     * a JDOM <code>Element</code> object.
     * This is typically the root element of a document.
     * <p>
     *
     * @param context context
     * @param dso the  DSpace Object whose metadata to export.
     * @return root Element of the target metadata, never <code>null</code>
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException
    {
        List<MockMetadataValue> dc = new ArrayList<>();
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            EPerson is = item.getSubmitter();
            if (is != null)
            {
                dc.add(makeDC("creator", null, is.getEmail()));
            }
            dc.add(makeDC("identifier", "uri", "hdl:" + item.getHandle()));
            Collection owningColl = item.getOwningCollection();
            String owner = owningColl.getHandle();
            if (owner != null)
            {
                dc.add(makeDC("relation", "isPartOf", "hdl:" + owner));
            }
            List<Collection> inColl = item.getCollections();
            for (int i = 0; i < inColl.size(); ++i)
            {
                if (!inColl.get(i).getID().equals(owningColl.getID()))
                {
                    String h = inColl.get(i).getHandle();
                    if (h != null)
                    {
                        dc.add(makeDC("relation", "isReferencedBy", "hdl:" + h));
                    }
                }
            }
            if (item.isWithdrawn())
            {
                dc.add(makeDC("rights", "accessRights", "WITHDRAWN"));
            }
        }
        else if (dso.getType() == Constants.BITSTREAM)
        {
            Bitstream bitstream = (Bitstream)dso;
            String bsName = bitstream.getName();
            if (bsName != null)
            {
                dc.add(makeDC("title", null, bsName));
            }
            String bsSource = bitstream.getSource();
            if (bsSource != null)
            {
                dc.add(makeDC("title", "alternative", bsSource));
            }
            String bsDesc = bitstream.getDescription();
            if (bsDesc != null)
            {
                dc.add(makeDC("description", null, bsDesc));
            }
            String bsUfmt = bitstream.getUserFormatDescription();
            if (bsUfmt != null)
            {
                dc.add(makeDC("format", null, bsUfmt));
            }
            BitstreamFormat bsf = bitstream.getFormat(context);
            dc.add(makeDC("format", "medium", bsf.getShortDescription()));
            dc.add(makeDC("format", "mimetype", bsf.getMIMEType()));
            dc.add(makeDC("format", "supportlevel", bitstreamFormatService.getSupportLevelText(bsf)));
            dc.add(makeDC("format", "internal", Boolean.toString(bsf.isInternal())));
        }
        else if (dso.getType() == Constants.COLLECTION)
        {
            Collection collection = (Collection)dso;
            dc.add(makeDC("identifier", "uri", "hdl:" + dso.getHandle()));
            List<Community> owners = collection.getCommunities();
            String ownerHdl = owners.get(0).getHandle();
            if (ownerHdl != null)
            {
                dc.add(makeDC("relation", "isPartOf", "hdl:" + ownerHdl));
            }
            for (int i = 1; i < owners.size(); ++i)
            {
                String h = owners.get(i).getHandle();
                if (h != null)
                {
                    dc.add(makeDC("relation", "isReferencedBy", "hdl:" + h));
                }
            }
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
            Community  community = (Community)dso;
            dc.add(makeDC("identifier", "uri", "hdl:" + dso.getHandle()));
            List<Community> parentCommunities = community.getParentCommunities();
            String ownerHdl = null;
            if (CollectionUtils.isEmpty(parentCommunities))
            {
                ownerHdl = siteService.findSite(context).getHandle();
            }
            else
            {
                ownerHdl = parentCommunities.get(0).getHandle();
            }

            if (ownerHdl != null)
            {
                dc.add(makeDC("relation", "isPartOf", "hdl:" + ownerHdl));
            }
        }
        else if (dso.getType() == Constants.SITE)
        {
            Site site = (Site) dso;
            
            //FIXME: adding two URIs for now (site handle and URL), in case site isn't using handles
            dc.add(makeDC("identifier", "uri", "hdl:" + site.getHandle()));
            dc.add(makeDC("identifier", "uri", site.getURL()));
        }

        return XSLTDisseminationCrosswalk.createDIM(dso, dc);
    }

    private static MockMetadataValue makeDC(String element, String qualifier, String value)
    {
        MockMetadataValue dcv = new MockMetadataValue();
        dcv.setSchema("dc");
        dcv.setLanguage(null);
        dcv.setElement(element);
        dcv.setQualifier(qualifier);
        dcv.setValue(value);
        return dcv;
    }

    /**
     * Ingest a whole document.  Build Document object around root element,
     * and feed that to the transformation, since it may get handled
     * differently than a List of metadata elements.
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        ingest(context, dso, root.getChildren(), createMissingMetadataFields);
    }

    /**
     * Translate metadata with XSL stylesheet and ingest it.
     * Translation produces a list of DIM "field" elements;
     * these correspond directly to Item.addMetadata() calls so
     * they are simply executed.
     * @param createMissingMetadataFields whether to create missing fields
     * @param dimList List of elements
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> dimList, boolean createMissingMetadataFields)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();

        // accumulate values for bitstream format in case we have to make one
        String bsfShortName = null;
        String bsfMIMEType = null;
        int bsfSupport = BitstreamFormat.KNOWN;
        boolean bsfInternal = false;

        for (Element field : dimList)
        {

            // if we get <dim> in a list, recurse.
            if (field.getName().equals("dim") && field.getNamespace().equals(XSLTCrosswalk.DIM_NS))
            {
                ingest(context, dso, field.getChildren(), createMissingMetadataFields);
            }
            else if (field.getName().equals("field") && field.getNamespace().equals(XSLTCrosswalk.DIM_NS))
            {
                String schema = field.getAttributeValue("mdschema");
                if (schema.equals("dc"))
                {
                    String dcField = field.getAttributeValue("element");
                    String qualifier = field.getAttributeValue("qualifier");
                    if (qualifier != null)
                    {
                        dcField += "." + qualifier;
                    }
                    String value = field.getText();

                    if (type == Constants.BITSTREAM)
                    {
                        Bitstream bitstream = (Bitstream)dso;
                        if (dcField.equals("title"))
                        {
                            bitstream.setName(context, value);
                        }
                        else if (dcField.equals("title.alternative"))
                        {
                            bitstream.setSource(context, value);
                        }
                        else if (dcField.equals("description"))
                        {
                            bitstream.setDescription(context, value);
                        }
                        else if (dcField.equals("format"))
                        {
                            bitstream.setUserFormatDescription(context, value);
                        }
                        else if (dcField.equals("format.medium"))
                        {
                            bsfShortName = value;
                        }
                        else if (dcField.equals("format.mimetype"))
                        {
                            bsfMIMEType = value;
                        }
                        else if (dcField.equals("format.supportlevel"))
                        {
                            int sl = bitstreamFormatService.getSupportLevelID(value);
                            if (sl < 0)
                            {
                                throw new MetadataValidationException("Got unrecognized value for bitstream support level: " + value);
                            }
                            else
                            {
                                bsfSupport = sl;
                            }
                        }
                        else if (dcField.equals("format.internal"))
                        {
                            bsfInternal = (Boolean.valueOf(value)).booleanValue();
                        }
                        else
                        {
                            log.warn("Got unrecognized DC field for Bitstream: " + dcField);
                        }
                    }
                    else if (type == Constants.ITEM)
                    {
                        Item item = (Item)dso;

                        // item submitter
                        if (dcField.equals("creator"))
                        {
                            EPerson sub = ePersonService.findByEmail(context, value);

                            // if eperson doesn't exist yet, optionally create it:
                            if (sub == null)
                            {
                                //This class works in conjunction with the DSpaceAIPIngester. 
                                // so, we'll use the configuration settings for that ingester
                                String configName = new DSpaceAIPIngester().getConfigurationName();

                                //Create the EPerson if specified and person doesn't already exit
                                if (ConfigurationManager.getBooleanProperty(METSManifest.CONFIG_METS_PREFIX + configName + ".ingest.createSubmitter"))
                                {
                                    sub = ePersonService.create(context);
                                    sub.setEmail(value);
                                    sub.setCanLogIn(false);
                                    ePersonService.update(context, sub);
                                }
                                else
                                {
                                    log.warn("Ignoring unknown Submitter=" + value + " in AIP Tech MD, no matching EPerson and 'mets.dspaceAIP.ingest.createSubmitter' is false in dspace.cfg.");
                                }
                            }
                            if (sub != null)
                            {
                                item.setSubmitter(sub);
                            }
                        }
                        else if (dcField.equals("rights.accessRights"))
                        {
                            //check if item is withdrawn
                            if (value.equalsIgnoreCase("WITHDRAWN"))
                            {
                                itemService.withdraw(context, item);
                            }
                        }
                        else if(dcField.equals("identifier.uri") ||
                                dcField.equals("relation.isPartOf"))
                        {
                            // Ignore identifier.uri (which specifies object handle)
                            // and relation.isPartOf (which specifies primary parent object)
                            // Both of these should already be set on object, as they
                            // are required/generated when a DSpaceObject is created.
                        }
                        else if (dcField.equals("relation.isReferencedBy"))
                        {
                            // This Item is referenced by other Collections.  This means
                            // it has been mapped into one or more additional collections.

                            // We'll attempt to map it to all referenced collections.
                            // But if this is a recursive ingest, it is possible some of these
                            // collections may not have been created yet. No need to worry,
                            // when each Collection is created it will create any mappings that
                            // we were unable to create now.
                            String parentHandle = value;

                            if(parentHandle!=null && !parentHandle.isEmpty())
                            {
                                //Remove 'hdl:' prefix, if it exists
                                if (parentHandle.startsWith("hdl:"))
                                {
                                    parentHandle = parentHandle.substring(4);
                                }

                                //Get parent object (if it exists)
                                DSpaceObject parentDso = handleService.resolveToObject(context, parentHandle);
                                //For Items, this parent *must* be a Collection
                                if(parentDso!=null && parentDso.getType()==Constants.COLLECTION)
                                {
                                    Collection collection = (Collection) parentDso;

                                    //If this item is not already mapped into this collection, map it!
                                    if (!itemService.isIn(item, collection))
                                    {
                                        collectionService.addItem(context, collection, item);
                                    }
                                }
                            }
                        }
                        else
                        {
                            log.warn("Got unrecognized DC field for Item: " + dcField);
                        }

                    }
                    else if (type == Constants.COMMUNITY || type == Constants.COLLECTION)
                    {
                        if (dcField.equals("identifier.uri") || dcField.equals("relation.isPartOf"))
                        {
                            // Ignore identifier.uri (which specifies object handle)
                            // and relation.isPartOf (which specifies primary parent object)
                            // Both of these should already be set on object, as they
                            // are required/generated when a DSpaceObject is created.
                        }
                        else if (dcField.equals("relation.isReferencedBy"))
                        {
                            // Ignore relation.isReferencedBy since it only
                            // lists _extra_ mapped parents, not the primary one.
                            // DSpace currently doesn't fully support mapping of Collections/Communities
                        }
                        else
                        {
                            log.warn("Got unrecognized DC field for Collection/Community: " + dcField);
                        }
                    } 
                }
                else
                {
                    log.warn("Skipping DIM field with mdschema=\"" + schema + "\".");
                }

            }
            else
            {
                log.error("Got unexpected element in DIM list: "+field.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+field.toString());
            }
        }

        // final step: find or create bitstream format since it
        // takes the accumulation of a few values:
        if (type == Constants.BITSTREAM && bsfShortName != null)
        {
            BitstreamFormat bsf = bitstreamFormatService.findByShortDescription(context, bsfShortName);
            if (bsf == null && bsfMIMEType != null)
            {
                bsf = PackageUtils.findOrCreateBitstreamFormat(context,
                        bsfShortName,
                        bsfMIMEType,
                        bsfShortName,
                        bsfSupport,
                        bsfInternal);
            }
            if (bsf != null)
            {
                ((Bitstream) dso).setFormat(context, bsf);
            }
            else
            {
                log.warn("Failed to find or create bitstream format named \"" + bsfShortName + "\"");
            }
        }
    }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.concept;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;


import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.*;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Display a single concept. This includes a full text search, browse by list,
 * concept display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class ConceptViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");

    public static final Message T_untitled =
            message("xmlui.general.untitled");

    private static final Message T_head_browse =
            message("xmlui.ArtifactBrowser.ConceptViewer.head_browse");

    private static final Message T_browse_titles =
            message("xmlui.ArtifactBrowser.ConceptViewer.browse_titles");

    private static final Message T_browse_authors =
            message("xmlui.ArtifactBrowser.ConceptViewer.browse_authors");

    private static final Message T_browse_dates =
            message("xmlui.ArtifactBrowser.ConceptViewer.browse_dates");

    private static final Message T_head_sub_concepts =
            message("xmlui.ArtifactBrowser.ConceptViewer.head_sub_concepts");


    /** Cached validity object */
    private SourceValidity validity;

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0";  // no item, something is wrong
            }

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * This validity object includes the concept being viewed, all
     * sub-communites (one level deep), all sub-concepts, and
     * recently submitted items.
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
        {
            Concept concept = null;
            try {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso == null)
                {
                    return null;
                }

                if (!(dso instanceof Concept))
                {
                    return null;
                }

                concept = (Concept) dso;

                DSpaceValidity validity = new DSpaceValidity();
                validity.add(concept);

                Term[] terms = concept.getPreferredTerms();
                // Sub concepts
                for (Term term : terms)
                {
                    validity.add(term);

                    // Include the item count in the validity, only if the value is cached.
                    boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
                    if (useCache)
                    {
                        try {
                            int size = new ItemCounter(context).getCount(term);
                            validity.add("size:"+size);
                        } catch(ItemCountException e) { /* ignore */ }
                    }
                }

                this.validity = validity.complete();
            }
            catch (Exception e)
            {
                // Ignore all errors and invalidate the cache.
            }

        }
        return this.validity;
    }


    /**
     * Add the concept's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        Concept concept=null;
        String conceptId = this.parameters.getParameter("concept","-1");
        if (conceptId.equals("-1"))
        {
            return;
        }
        else
        {
            concept = Concept.find(context,Integer.parseInt(conceptId));
            if(concept==null)
            {
                return;
            }
        }
        // Set the page title
        String name = concept.getLabel();
        if (name == null || name.length() == 0)
        {
            pageMeta.addMetadata("title").addContent(T_untitled);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(name);
        }

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/concept/"+concept.getID(),concept.getLabel());
        HandleUtil.buildHandleTrail(concept, pageMeta, contextPath);

        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
        if ( formats != null )
        {
            for (String format : formats.split(","))
            {
                // Remove the protocol number, i.e. just list 'rss' or' atom'
                String[] parts = format.split("_");
                if (parts.length < 1)
                {
                    continue;
                }

                String feedFormat = parts[0].trim()+"+xml";

                String feedURL = contextPath+"/feed/"+format.trim()+"/"+concept.getHandle();
                pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
            }
        }
    }

    /**
     * Display a single concept (and reference any sub communites or
     * concepts)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        String conceptId = this.parameters.getParameter("concept","-1");
        if(conceptId==null)
        {
            return;
        }
        Integer conceptID = Integer.parseInt(conceptId);
        Concept concept = Concept.find(context, conceptID);
        if(concept==null)
        {
            return;
        }

        // Build the concept viewer division.
        Division home = body.addDivision("concept-home", "primary thesaurus concept");


        Scheme parentScheme = concept.getScheme();
        List parentList = home.addList("scheme");
        parentList.setHead("Scheme");
        if(parentScheme!=null)
        {
            parentList.addItem().addXref("/scheme/"+parentScheme.getID(),parentScheme.getIdentifier());
        }

        String name = concept.getLabel();
        if (name == null || name.length() == 0)
        {
            home.setHead(T_untitled);
        }
        else
        {
            home.setHead(name);
        }

        // Add main reference:
        {
            Division viewer = home.addDivision("concept-view","secondary");
            Division attributeSection = viewer.addDivision("attribute-section","thesaurus-section");
            Table attribute = attributeSection.addTable("attribute",3,2,"thesaurus-table");
            attribute.setHead("Attribute");
            Row aRow = attribute.addRow();
            aRow.addCell().addContent("Identifier");
            aRow.addCell().addContent(concept.getIdentifier());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Create Date");
            aRow.addCell().addContent(concept.getCreated().toString());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Status");
            aRow.addCell().addContent(concept.getStatus());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Source");
            if(concept.getSource()!=null) {
                aRow.addCell().addContent(concept.getSource());
            }
            else
            {
                aRow.addCell().addContent("NULL");
            }
            Division preSection = viewer.addDivision("pre-term-section","thesaurus-section");
            preSection.setHead("Preferred Terms");
            Term[] preferredTerms = concept.getPreferredTerms();
            if(preferredTerms!=null && preferredTerms.length >0)
            {


                Table table = preSection.addTable("pre-term", preferredTerms.length + 1, 3,"thesaurus-table");

                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("ID");
                header.addCell().addContent("Identifier");
                header.addCell().addContent("Preferred Label");

                for(Term term : preferredTerms)
                {
                    Row item = table.addRow();
                    item.addCell().addContent(term.getID());
                    item.addCell().addContent(term.getIdentifier());
                    item.addCell().addXref("/term/"+term.getID(),term.getLiteralForm());
                }
            }
            Division altSection = viewer.addDivision("alt-term-section","thesaurus-section");
            altSection.setHead("Alternative Terms");
            Term[] altTerms = concept.getAltTerms();
            if(altTerms!=null && altTerms.length >0)
            {

                Table table = altSection.addTable("alt-term", altTerms.length + 1, 3,"thesaurus-table");

                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("ID");
                header.addCell().addContent("Identifier");
                header.addCell().addContent("Preferred Label");

                for(Term term : altTerms)
                {
                    Row item = table.addRow();
                    item.addCell().addContent(concept.getID());
                    item.addCell().addContent(concept.getIdentifier());
                    item.addCell().addXref("/term/"+term.getID(),term.getLiteralForm());
                }
            }
            Division metadataSection = viewer.addDivision("metadata-section", "thesaurus-section");
            metadataSection.setHead("Metadata Values");
            if(AuthorizeManager.isAdmin(context)){
                //only admin can see metadata
                java.util.List<AuthorityMetadataValue> values = concept.getMetadata();
                int i = 0;

                if(values!=null&&values.size()>0)
                {

                    Table metadataTable = metadataSection.addTable("metadata", values.size() + 1, 2,"detailtable thesaurus-table");

                    Row header = metadataTable.addRow(Row.ROLE_HEADER);
                    header.addCell().addContent("ID");
                    header.addCell().addContent("Field Name");
                    header.addCell().addContent("Value");
                    while (i<values.size()&&values.get(i)!=null)
                    {

                        AuthorityMetadataValue value = (AuthorityMetadataValue)values.get(i);
                        Row mRow = metadataTable.addRow();
                        mRow.addCell().addContent(value.getFieldId());
                        if(value.qualifier!=null&&value.qualifier.length()>0)
                        {
                            mRow.addCell().addContent(value.schema + "." + value.element + "." + value.qualifier);
                        }
                        else
                        {
                            mRow.addCell().addContent(value.schema + "." + value.element);
                        }
                        mRow.addCell().addContent(value.getValue());
                        i++;
                    }

                }
            }


            Concept2Concept[] parentRelations= Concept2Concept.findByChild(context,concept.getID());
            Division aSection = viewer.addDivision("associate-section","thesaurus-section");
            aSection.setHead("Parent Concepts");
            if(parentRelations!=null&&parentRelations.length>0) {



                Table table = aSection.addTable("associate", parentRelations.length + 1, 3,"thesaurus-table");

                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("Parent Concept");
                header.addCell().addContent("Role");
                header.addCell().addContent("Current Concept");


                for(Concept2Concept parentRelation : parentRelations)
                {
                    Concept incomingConcept = Concept.find(context,parentRelation.getIncomingId());
                    Row acRow = table.addRow();
                    acRow.addCell().addXref("/concept/"+incomingConcept.getID(),incomingConcept.getLabel());
                    acRow.addCell().addContent(Concept2ConceptRole.find(context, parentRelation.getRoleId()).getLabel());
                    acRow.addCell().addContent(concept.getLabel());
                }
            }
            Division bSection = viewer.addDivision("hi-section", "thesaurus-section");
            bSection.setHead("Child Concepts");
            Concept2Concept[] childConcepts = Concept2Concept.findByParent(context,concept.getID());
            if(childConcepts!=null&&childConcepts.length>0) {


                Table table = bSection.addTable("hi", childConcepts.length + 1, 3,"thesaurus-table");

                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("current concept");
                header.addCell().addContent("Role");
                header.addCell().addContent("Child Concept");


                for(Concept2Concept childRelation : childConcepts)
                {
                    Row acRow = table.addRow();
                    Concept outgoingConept = Concept.find(context,childRelation.getOutgoingId());

                    acRow.addCell().addContent(concept.getLabel());
                    acRow.addCell().addContent(Concept2ConceptRole.find(context,childRelation.getRoleId()).getLabel());
                    acRow.addCell().addXref("/concept/" + outgoingConept.getID(), outgoingConept.getIdentifier());
                }
            }


        } // main reference
    }



    public void addOptions(org.dspace.app.xmlui.wing.element.Options options) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException, org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
    {


        String conceptId = this.parameters.getParameter("concept","-1");
        if(conceptId==null)
        {
            return;
        }
        Integer conceptID = Integer.parseInt(conceptId);
        Concept concept = Concept.find(context, conceptID);

        options.addList("browse");
        options.addList("account");
        List authority = options.addList("context");
        options.addList("administrative");

        //Check if a system administrator
        boolean isSystemAdmin = AuthorizeManager.isAdmin(this.context);


        // System Administrator options!
        if (isSystemAdmin)
        {
            authority.setHead(T_context_head);
            authority.addItemXref(contextPath+"/admin/concept?conceptID="+concept.getID()+"&edit","Edit Concept Attribute");
            authority.addItemXref(contextPath+"/admin/concept?conceptID="+concept.getID()+"&editMetadata","Edit Concept Metadata Value");
            authority.addItemXref(contextPath+"/admin/concept?conceptID="+concept.getID()+"&addConcept","Add Related Concept");
            authority.addItemXref(contextPath+"/admin/concept?conceptID="+concept.getID()+"&search","Search & Add Terms");
        }
    }



    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }


}

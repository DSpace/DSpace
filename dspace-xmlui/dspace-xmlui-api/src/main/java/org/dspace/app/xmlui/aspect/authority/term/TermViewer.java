/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.term;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;


import org.dspace.content.DSpaceObject;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Display a single term. This includes a full text search, browse by list,
 * term display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class TermViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_administrative_authority 	= message("xmlui.administrative.Navigation.administrative_authority_control");
    private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");
    public static final Message T_untitled =
            message("xmlui.general.untitled");

    private static final Message T_head_browse =
            message("xmlui.ArtifactBrowser.TermViewer.head_browse");

    private static final Message T_browse_titles =
            message("xmlui.ArtifactBrowser.TermViewer.browse_titles");

    private static final Message T_browse_authors =
            message("xmlui.ArtifactBrowser.TermViewer.browse_authors");

    private static final Message T_browse_dates =
            message("xmlui.ArtifactBrowser.TermViewer.browse_dates");


    private static final Message T_head_sub_concepts =
            message("xmlui.ArtifactBrowser.TermViewer.head_sub_concepts");


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
     * This validity object includes the term being viewed, all
     * sub-communites (one level deep), all sub-concepts, and
     * recently submitted items.
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
        {
            Term term = null;
            try {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso == null)
                {
                    return null;
                }

                if (!(dso instanceof Term))
                {
                    return null;
                }

                term = (Term) dso;

                DSpaceValidity validity = new DSpaceValidity();
                validity.add(term);

                Concept[] concepts = term.getConcepts();
                // Sub concepts
                for (Concept concept : concepts)
                {
                    validity.add(concept);

                    // Include the item count in the validity, only if the value is cached.
                    boolean useCache = ConfigurationManager.getBooleanProperty("webui.strengths.cache");
                    if (useCache)
                    {
                        try {
                            int size = new ItemCounter(context).getCount(concept);
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
     * Add the term's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        Term term=null;
        String termId = this.parameters.getParameter("term","-1");
        if (termId.equals("-1"))
        {
            return;
        }
        else
        {
            term = Term.find(context,Integer.parseInt(termId));
            if(term==null)
            {
                return;
            }
        }
        // Set the page title
        String name = term.getLiteralForm();
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
        pageMeta.addTrailLink(contextPath + "/term/"+term.getID(),term.getLiteralForm());
        HandleUtil.buildHandleTrail(term, pageMeta,contextPath);

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

                String feedURL = contextPath+"/feed/"+format.trim()+"/"+term.getHandle();
                pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
            }
        }
    }

    /**
     * Display a single term (and reference any sub communites or
     * concepts)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        String termId = this.parameters.getParameter("term","-1");
        if(termId==null)
        {
            return;
        }
        Integer termID = Integer.parseInt(termId);
        Term term = Term.find(context, termID);

        if(term==null)
        {
            return;
        }

        Concept[] concepts = term.getConcepts();

        // Build the term viewer division.
        Division home = body.addDivision("term-home", "primary repository thesaurus");
        String name = term.getLiteralForm();
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
            Division viewer = home.addDivision("term-view","secondary");

            Division attributeSection = viewer.addDivision("attribute-section","thesaurus-section");
            Table attribute = attributeSection.addTable("attribute",3,2,"thesaurus-table");
            attribute.setHead("Attribute");
            Row aRow = attribute.addRow();
            aRow = attribute.addRow();
            aRow.addCell().addContent("Literal Form");
            aRow.addCell().addContent(term.getLiteralForm());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Identifier");
            aRow.addCell().addContent(term.getIdentifier());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Create Date");
            aRow.addCell().addContent(term.getCreated().toString());
            aRow = attribute.addRow();
            aRow.addCell().addContent("Status");
            aRow.addCell().addContent(term.getStatus());

            if(AuthorizeManager.isAdmin(context)){
                //only admin can see metadata
                ArrayList<AuthorityMetadataValue> values = term.getMetadata();
                Iterator i = values.iterator();
                Division metadataSection = viewer.addDivision("metadata-section","thesaurus-section");
                metadataSection.setHead("Metadata Values");
                Table metadataTable = metadataSection.addTable("metadata", values.size() + 1, 3,"detailtable thesaurus-table");

                Row header = metadataTable.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("ID");
                header.addCell().addContent("Field Name");
                header.addCell().addContent("Value");
                while (i.hasNext())
                {
                    AuthorityMetadataValue value = (AuthorityMetadataValue)i.next();
                    Row mRow = metadataTable.addRow();
                    mRow.addCell().addContent(value.getFieldId());
                    if(value.qualifier!=null&&value.qualifier.length()>0)
                    {
                        mRow.addCell().addContent(value.schema+"."+value.element+"."+value.qualifier);
                    }
                    else
                    {
                        mRow.addCell().addContent(value.schema+"."+value.element);
                    }
                    mRow.addCell().addContent(value.getValue());
                }
            }

            Division conceptSection = viewer.addDivision("concept-section","thesaurus-section");
            conceptSection.setHead("Concepts");
            if(concepts!=null && concepts.length >0)
            {

                Table table = conceptSection.addTable("concepts", concepts.length + 1, 2,"thesaurus-table");

                Row header = table.addRow(Row.ROLE_HEADER);
                header.addCell().addContent("ID");
                header.addCell().addContent("Identifier");
                header.addCell().addContent("Preferred Label");
                for(Concept concept : concepts)
                {
                    Row item = table.addRow();
                    item.addCell().addContent(concept.getID());
                    item.addCell().addXref("/concept/" + concept.getID(), concept.getIdentifier());
                    if(concept.getLabel()==null)
                    {
                        item.addCell().addContent("");
                    }
                    else
                    {
                        item.addCell().addXref("/concept/" + concept.getID(), concept.getLabel());
                    }
                }
            }
        } // main reference
    }



    public void addOptions(org.dspace.app.xmlui.wing.element.Options options) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException, org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
    {


        String termId = this.parameters.getParameter("term","-1");
        if(termId==null)
        {
            return;
        }
        Integer termID = Integer.parseInt(termId);
        Term term = Term.find(context, termID);
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
            authority.addItemXref(contextPath+"/admin/term?termID="+term.getID()+"&edit","Edit Term Attribute");
            authority.addItemXref(contextPath+"/admin/term?termID="+term.getID()+"&editMetadata","Edit Term Metadata Value");
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

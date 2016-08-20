/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.xml.sax.SAXException;

import org.dspace.app.util.CollectionDropDown;

/**
 * Allow the user to select a collection they wish to submit an item to, 
 * this step is sort-of but not officialy part of the item submission 
 * processes. Normally a user will have selected a collection to submit 
 * too by going to the collection's page, but if that was invalid or the 
 * user came directly from the mydspace page then this step is given.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class SelectCollectionStep extends AbstractSubmissionStep
{
   
	/** Language Strings */
    protected static final Message T_head = 
        message("xmlui.Submission.submit.SelectCollection.head");
    protected static final Message T_collection = 
        message("xmlui.Submission.submit.SelectCollection.collection");
    protected static final Message T_collection_help = 
        message("xmlui.Submission.submit.SelectCollection.collection_help");
    protected static final Message T_collection_default = 
        message("xmlui.Submission.submit.SelectCollection.collection_default");
    protected static final Message T_submit_next = 
        message("xmlui.general.next");
	
    public SelectCollectionStep() 
    {
    	this.requireHandle = true;
    }

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
    WingException
    {
        
        pageMeta.addMetadata("title").addContent(T_submission_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_submission_trail);
    }
  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {     
        java.util.List<Collection> collections; // List of possible collections.
        String actionURL = contextPath + "/submit/" + knot.getId() + ".continue";
        DSpaceObject dso = handleService.resolveToObject(context, handle);
        
        if (dso instanceof Community)
        {
            collections = collectionService.findAuthorized(context, ((Community) dso), Constants.ADD);
        } 
        else
        {
            collections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        }
        
        // Basic form with a drop down list of all the collections
        // you can submit too.
        Division div = body.addInteractiveDivision("select-collection",actionURL,Division.METHOD_POST,"primary submission");
        div.setHead(T_submission_head);
        
        List list = div.addList("select-collection", List.TYPE_FORM);
        list.setHead(T_head);       
        Select select = list.addItem().addSelect("handle");
        select.setAutofocus("autofocus");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);
        
        select.addOption("",T_collection_default);
	    CollectionDropDown.CollectionPathEntry[] collectionPaths = CollectionDropDown.annotateWithPaths(context, collections);
        for (CollectionDropDown.CollectionPathEntry entry : collectionPaths)
        {
            select.addOption(entry.collection.getHandle(), entry.path);
        }
        
        Button submit = list.addItem().addButton("submit");
        submit.setValue(T_submit_next);
    }
    
    /** 
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in 
     * List object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first
     * add a sub-List object (with this step's name as the heading),
     * by using a call to reviewList.addList().   This sublist is
     * the list you return from this method!
     * 
     * @param reviewList
     *      The List to which all reviewable information should be added
     * @return 
     *      The new sub-List object created by this step, which contains
     *      all the reviewable information.  If this step has nothing to
     *      review, then return null!   
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //Currently, the selecting a Collection is not reviewable in DSpace,
        //since it cannot be changed easily after creating the item
        return null;
    }
    
    /**
     * Recycle
     */
     public void recycle() 
     {
         this.handle = null;
         super.recycle();
     }
}

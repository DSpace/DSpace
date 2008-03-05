/*
 * SelectCollectionStep.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

/**
 * Allow the user to select a collection they wish to submit an item to, 
 * this step is sort-of but not officialy part of the item submission 
 * processes. Normaly a user will have selected a collection to submit 
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
		Collection[] collections; // List of possible collections.
		DSpaceObject dso = HandleManager.resolveToObject(context, handle);

		if (dso != null && dso instanceof Community)
		{
			collections = Collection.findAuthorized(context, ((Community) dso), Constants.ADD);   
		} 
		else
		{
			collections = Collection.findAuthorized(context, null, Constants.ADD);
		}
        
		// Basic form with a drop down list of all the collections
		// you can submit too.
        Division div = body.addInteractiveDivision("select-collection",contextPath+"/submit",Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
        
        List list = div.addList("select-collection", List.TYPE_FORM);
        list.setHead(T_head);       
        Select select = list.addItem().addSelect("handle");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);
        
        select.addOption("",T_collection_default);
        for (Collection collection : collections) 
        {
        	String name = collection.getMetadata("name");
   		   	if (name.length() > 50)
   		   		name = name.substring(0, 47) + "...";
        	select.addOption(collection.getHandle(),name);
        }
        
        Button submit = list.addItem().addButton("submit");
        submit.setValue(T_submit_next);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
        
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

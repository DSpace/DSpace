/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.xmlworkflow.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.xmlui.aspect.xmlworkflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.content.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

public class ChangeCollectionAction extends AbstractXMLUIAction {

	/** Language Strings**/
	protected static final Message T_submission_head = message("xmlui.Submission.submit.SelectCollection.head");
	protected static final Message T_list_head = message("xmlui.Submission.submit.SelectCollection.head");
	protected static final Message T_submission_paragraph = message("cic.Submission.submit.SelectCollection.paragraph");
	protected static final Message T_collection = message("xmlui.Submission.submit.SelectCollection.collection");
	protected static final Message T_collection_help = message("cic.Submission.submit.SelectCollection.collection_help");
	protected static final Message T_collection_default = message("xmlui.Submission.submit.SelectCollection.collection_default");
	protected static final Message T_submit_next = message("xmlui.general.next");
	protected static final Message T_submit_cancel = message("xmlui.general.cancel");
	@Override
	public void addBody(Body body) throws SAXException, WingException,
			SQLException, IOException, AuthorizeException {
        Collection collection = workflowItem.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";
		
		Collection[] collections; // List of possible collections.
		// Listado de colecciones disponibles
		collections = Collection.findAuthorized(context, null, Constants.ADD);
        
		// Formulario con la lista de colecciones
        Division div = body.addInteractiveDivision("change-collection",actionURL,Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		div.addPara(T_submission_paragraph);
        
		List list = div.addList("select-collection", List.TYPE_FORM);
        list.setHead(T_list_head);       
        Select select = list.addItem().addSelect("collection_handle");
        select.setAutofocus("autofocus");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);
        
        select.addOption("",T_collection_default);
	    CollectionDropDown.CollectionPathEntry[] collectionPaths = CollectionDropDown.annotateWithPaths(collections);
        for (CollectionDropDown.CollectionPathEntry entry : collectionPaths)
        {
            //If the "collection entry" differs from the "collection workflowItem", then add the entry.
        	if(workflowItem.getCollection().getHandle() != entry.collection.getHandle()){
        		select.addOption(entry.collection.getHandle(), entry.path);
            	}
        }
        
        Button submit = list.addItem().addButton("submit");
        submit.setValue(T_submit_next);
        
        list.addItem().addButton("cancel").setValue(T_submit_cancel);

        
        div.addHidden("submission-continue").setValue(knot.getId());

	}

}

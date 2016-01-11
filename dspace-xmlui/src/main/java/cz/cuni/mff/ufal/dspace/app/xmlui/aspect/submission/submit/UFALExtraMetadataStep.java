/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.aspect.submission.submit.DescribeStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Params;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.dspace.app.util.ACL;

/**
 * UFAL/jmisutka
 * @see DescribeStep
 * modified for LINDAT/CLARIN
 */
public class UFALExtraMetadataStep extends DescribeStep
{
        protected static final Message T_head =
            message("xmlui.Submission.submit.ExtraMetadataStep.head");

        public UFALExtraMetadataStep() throws ServletException, DCInputsReaderException
        {
        }

        /**
         * Add information for structural.xsl
         *
         * @see structural.xsl in dri2xhtml theme
         * jmisutka 2011/05/25
         */
        @Override
        public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
            super.addPageMeta(pageMeta);
            pageMeta.addMetadata("include-library", "extrametadata");
            pageMeta.addMetadata("include-library", "datepicker");
        }

        
        @Override
        public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
        {
                // Obtain the inputs (i.e. metadata fields we are going to display)
                Item item = submission.getItem();
                Collection collection = submission.getCollection();
                String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";

                DCInputSet inputSet;
                DCInput[] inputs;
                try {
                    inputSet = getInputsReader().getInputsExtra(submission.getCollection().getHandle());
                    inputs = inputSet.getPageRows(getPage()-1, submission.hasMultipleTitles(), submission.isPublishedBefore());
                }catch (DCInputsReaderException se) {
                        throw new UIException(se);
                }

                Division main_div = body.addInteractiveDivision("submit-extrametadata",actionURL,Division.METHOD_POST,"primary submission");
                main_div.setHead(T_submission_head);
                addSubmissionProgressList(main_div);
                addJumpToInput(main_div);
                

                List form = null;
                String last_repeatable_component = null;
                
                // Fetch the document type (dc.type)
                String documentType = "";
                if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) ) {
                    documentType = item.getMetadataByMetadataString("dc.type")[0].value;
                }

                String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;                
                
                Division div = main_div;
                Division accordionDiv = null;
                String collapsibleHeader = null; 
                
                // Iterate over all inputs and add it to the form.                
                for (int i = 0; i < inputs.length; ++i )
                {
                    DCInput dcInput = inputs[i];
                    
                    if ( dcInput.getCollapsible() != null) 
                    {
                        div = main_div;
                        if(dcInput.getCollapsible().trim().length() > 0 ) 
                        {
                            collapsibleHeader = dcInput.getCollapsible().trim();
                        }
                        else {
                            collapsibleHeader = null;                            
                        }
                    }                    
                    
                    if ( !isInputDisplayable(context, dcInput, scope, documentType ) ) 
                    {
                        continue;
                    }
                    
                    if(!isInputAuthorized(context, dcInput)) {
                        continue;
                    }
                    
                    if(dcInput.hasACL()) 
                    {
                        if(AuthorizeManager.isAdmin(context)) 
                        {
                            dcInput.addRend("admin-field");
                        }
                        else 
                        {
                            dcInput.addRend("specialuser-field");
                        }
                    }
                    
                    // component UI
                    //
                    if(collapsibleHeader != null) {
                        // http://getbootstrap.com/2.3.2/javascript.html#collapse
                        String suffix = "-" + String.valueOf(i);
                        
                        if(accordionDiv == null) {                                
                            accordionDiv = div.addDivision("accordion-main" + suffix , "accordion");
                        }
                        
                        Division accordionGroup  = accordionDiv.addDivision("accordion-group" + suffix, "accordion-group");
                        div = accordionGroup.addDivision("accordion-heading" + suffix, "accordion-heading");
                        div = div.addDivision("accordion-toggle" + suffix, "accordion-toggle");
                        div.setHead(collapsibleHeader);                                                                                                                    
                        // make id/name unique
                        div = accordionGroup.addDivision("accordion-body" + suffix + "-" + collapsibleHeader.replace(" ",  "-"), 
                                        "accordion-body collapse");
                        div = div.addDivision("accordion-inner"  + suffix, "accordion-inner"); 
                        collapsibleHeader = null;
                    }                    
                    
                    // hierarchical repeatable components 
                    // - for each component create special field set
                    // - fill out metadata fields if applicable
                    //
                    
                    // create header/footer and start repeatable components
                    if (dcInput.getComponentLabel() != null ) {
                        form = new_component_set(form, div, dcInput, dcInput.getComponentLabel());
                    }else if ( 0 == i ) {
                        form = new_component_set(form, div, dcInput, "");
                    }
                    assert null != form;
                    
                    handle_repeatable(form, dcInput);

                    
                    // store last value
                    last_repeatable_component = dcInput.getExtraRepeatableComponent();
                    
                    // map dc to other elements
                    Metadatum[] dcValues = item.getMetadata(
                        dcInput.getSchema(), dcInput.getElement(), dcInput.getQualifier(), Item.ANY);
                    dcValues = mapped_values( item, dcInput, dcValues );
                    // render the field
                    render(form, item, dcInput, dcValues, inputSet, collection, scope);
                    
                    // - create controls if the component ended meaning it is the last one
                    //   or the new repeatable is different
                    //
                    String next_repeatable_component = (i + 1 >= inputs.length) ? null : 
                        inputs[i+1].getExtraRepeatableComponent();
                    if ( repeatable_component_ended(dcInput, next_repeatable_component) ) {
                        render_controls_to_repeatable_component(form, last_repeatable_component );
                    }
                }

                // add standard control/paging buttons
                form = main_div.addList( "submit-extrametadata-controls", List.TYPE_FORM );
                addControlButtons(form);
    }
        
    private void handle_repeatable(List form, DCInput dcInput) throws WingException
    {
        String repeatable_component = dcInput.getExtraRepeatableComponent();
        String schema = dcInput.getSchema();
        String element = dcInput.getElement();
        String qualifier = dcInput.getQualifier();

        if ( dcInput.getExtraRepeatableComponent() != null  ) 
        {
            org.dspace.app.xmlui.wing.element.Item item = form.addItem();
            // hierarchical repeatable components #2/2 if we start new repeatable component
            //   - indicate with hidden box
            if ( repeatable_component != null ) {
                repeatable_indicate_start(item, dcInput);
            }     

            if ( !dcInput.isRepeatable(true) ) {
                item.addHidden( "repeatable_" + schema + "_" + element + "_" + qualifier );
            }
        }
    }
        
    private List new_component_set(List form, Division div, DCInput dcInput, String header) 
                        throws WingException
    {
        String element = dcInput.getElement();
        
        form = div.addList( "submit-extrametadata-"+element.replace("#","-"), List.TYPE_FORM );
        if ( header != null && header.trim().length() > 0 ) {
            form.setHead( header );
        }
        return form;
    }
    
    private void repeatable_indicate_start(org.dspace.app.xmlui.wing.element.Item item, DCInput dcInput) throws WingException
    {
        String schema = dcInput.getSchema();
        String element = dcInput.getElement();
        item.addHidden( "start_repeatable_" + schema + "_" + element );
    }
    
    /**
     */
    private boolean repeatable_component_ended(DCInput dcInput, String other_rp )
    {
        String curr_rp = dcInput.getExtraRepeatableComponent();
        return curr_rp != null && ( other_rp == null || !curr_rp.equals(other_rp) );
    }
        
        
    /**
     * if the repeatable component was ended, add the controls (add/delete buttons).
     * @throws WingException 
     */
    private void render_controls_to_repeatable_component( List form, String repatable_component ) throws WingException
    {
        org.dspace.app.xmlui.wing.element.Item _item = form.addItem();

        // add delete/add buttons
        Button delete_component = _item.addButton( "submit_component_remove_" + repatable_component );
        delete_component.setValue( 
            message("xmlui.Submission.submit.ExtraMetadataStep.deleteRepeatabaleComponent") );
        Button add_new_component = _item.addButton( "submit_component_" + repatable_component );
        add_new_component.setValue( 
            message("xmlui.Submission.submit.ExtraMetadataStep.addNewRepeatabaleComponent") );
    }
    

    /**
     * Read the input-forms input (abstracted by DCInput) and if we should take the value
     * from another dc field than use it.
     * 
     * @param dcInput
     * @throws SAXException 
     */
    private Metadatum[] mapped_values( Item item, DCInput dcInput, Metadatum[] dcValues ) throws SAXException
    {
        
        String mapped_to_dc_element = dcInput.getExtraMappedToElement();
            if ( mapped_to_dc_element == null ) {
                return dcValues;
            }
            if ( dcValues != null && 0 < dcValues.length ) {
                return dcValues;
            }
        
        // schema.element.qualifier - 3 parts, if not throw
        String[] dc_uri_arr = mapped_to_dc_element.split( "\\." );
            if (dc_uri_arr.length < 2 || 3 < dc_uri_arr.length) 
            {
                //2,3 OK
                throw new SAXException("Field <mapped-to> (" + mapped_to_dc_element
                    + ") does not contain 2 or 3 parts concatenated with \".\" (schema, element, qualifier).");
            }
        // do we have only schema.element
        String extra_qualifier = (2 == dc_uri_arr.length) ? null : dc_uri_arr[2];
        // get the dc values
        dcValues = item.getMetadata( dc_uri_arr[0], dc_uri_arr[1], extra_qualifier, Item.ANY);
        return dcValues;
    }
    
    
    /**
     * Copied from describe step..
     */
    private void render( 
        List form, Item item, DCInput dcInput, Metadatum[] dcValues, DCInputSet inputSet, Collection collection, String scope ) 
                        throws WingException
    {
        boolean readonly = dcInput.isReadOnly(scope) && !dcInput.isAllowedAction(context, ACL.ACTION_WRITE);        
        
        String schema = dcInput.getSchema();
        String element = dcInput.getElement();
        String qualifier = dcInput.getQualifier();
        
        String fieldName = FlowUtils.getFieldName(dcInput);
        String inputType = dcInput.getInputType();
        
        // 
        boolean hide_if_not_empty = dcInput.hasExtraAttribute("hidenonempty");
        if ( hide_if_not_empty && dcValues != null && dcValues.length == 1 ) {
            form.addItem().addHidden(fieldName, "hide-parent").setValue(dcValues[0].value);
            return;
        }


        // if this field is configured as choice control and its
        // presentation format is SELECT, render it as select field:
        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
        if (cmgr.isChoicesConfigured(fieldKey) &&
            Params.PRESENTATION_SELECT.equals(cmgr.getPresentation(fieldKey)))
        {
                renderChoiceSelectField(form, fieldName, collection, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("name"))
        {
                renderNameField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("date"))
        {
                renderDateField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("series"))
        {
                renderSeriesField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("twobox"))
        {
                // We don't have a twobox field, instead it's just a
                // one box field that the theme can render in two columns.
                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("qualdrop_value"))
        {
                // Determine the real field's values. Since the qualifier is
                // selected we need to search through all the metadata and see
                // if any match for another field, if not we assume that this field
                // should handle it.
                Metadatum[] unfiltered = item.getMetadata(dcInput.getSchema(), dcInput.getElement(), Item.ANY, Item.ANY);
                ArrayList<Metadatum> filtered = new ArrayList<Metadatum>();
                for (Metadatum dcValue : unfiltered)
                {
                        String unfilteredFieldName = dcValue.element + "." + dcValue.qualifier;
                        if ( ! inputSet.isFieldPresent(unfilteredFieldName) )
                        {
                                filtered.add( dcValue );
                        }
                }
                
                renderQualdropField(form, fieldName, dcInput, filtered.toArray(new Metadatum[filtered.size()]), readonly);
        }
        else if (inputType.equals("textarea"))
        {
                renderTextArea(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("dropdown"))
        {
                renderDropdownField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("list"))
        {
                renderSelectFromListField(form, fieldName, dcInput, dcValues, readonly);
        }
        else if (inputType.equals("onebox"))
        {
                renderOneboxField(form, fieldName, dcInput, dcValues, readonly);
        }
        else
        {
                form.addItem(T_unknown_field);
        }
    }

    /**
     * @see DescribeStep.addReviewSection
     */
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //Create a new list section for this step (and set its heading)
        List reviewSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
        reviewSection.setHead(T_head);
        
        //Review the values assigned to all inputs
        //on this page of the Describe step.
        DCInputSet inputSet = null;
        try
        {
            inputSet = getInputsReader().getInputsExtra(submission.getCollection().getHandle());
        }
        catch (DCInputsReaderException se)
        {
            throw new UIException(se);
        }
        
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        DCInput[] inputs = inputSet.getPageRows(getPage()-1, submission.hasMultipleTitles(), submission.isPublishedBefore());

        // Fetch the document type (dc.type)
        Item item = submission.getItem();
        String documentType = "";
        if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
        {
            documentType = item.getMetadataByMetadataString("dc.type")[0].value;
        } 
        
        for (DCInput input : inputs)
        {
            // If the input is invisible in this scope, then skip it.
            String scope = submissionInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
            
            if(!isInputDisplayable(context, input, scope, documentType)) {
                continue;
            }                                   
            
            if(!isInputAuthorized(context, input)) {
                continue;
            }
            
            if(input.hasACL()) 
            {
                if(AuthorizeManager.isAdmin(context)) 
                {
                    input.addRend("admin-field");
                }
                else 
                {
                    input.addRend("specialuser-field");
                }
                //TODO: apply formatting in verify step
            }

            String inputType = input.getInputType();
            String pairsName = input.getPairsType();
            Metadatum[] values;

            if (inputType.equals("qualdrop_value"))
            {
                values = submission.getItem().getMetadata(input.getSchema(), input.getElement(), Item.ANY, Item.ANY);
            }
            else
            {
                values = submission.getItem().getMetadata(input.getSchema(), input.getElement(), input.getQualifier(), Item.ANY);
            }

            
            if (values != null && values.length > 0)
            {
                boolean hide_if_not_empty = input.hasExtraAttribute("hidenonempty");
                if ( hide_if_not_empty && values.length == 1 ) {
                    continue;
                }

                for (Metadatum value : values)
                {
                    String displayValue = null;
                    if (inputType.equals("date"))
                    {
                        DCDate date = new DCDate(value.value);
                        displayValue = date.toString();
                    }
                    else if (inputType.equals("dropdown"))
                    {
                        displayValue = input.getDisplayString(pairsName,value.value);
                    }
                    else if (inputType.equals("qualdrop_value"))
                    {
                        String qualifier = value.qualifier;
                        String displayQual = input.getDisplayString(pairsName,qualifier);
                        if (displayQual!=null && displayQual.length()>0)
                        {
                            displayValue = displayQual + ":" + value.value;
                        }
                    }
                    else
                    {
                        displayValue = value.value;
                    }

                    //Only display this field if we have a value to display
                    if (displayValue!=null && displayValue.length()>0)
                    {                        
                    	reviewSection.addLabel(message(input.getLabel()));
                        if (mam.isAuthorityControlled(value.schema, value.element, value.qualifier))
                        {
                            String confidence = (value.authority != null && value.authority.length() > 0) ?
                                Choices.getConfidenceText(value.confidence).toLowerCase() :
                                "blank";
                            org.dspace.app.xmlui.wing.element.Item authItem =
                            		reviewSection.addItem("submit-review-field-with-authority", "ds-authority-confidence cf-"+confidence);
                            authItem.addContent(displayValue);
                        }
                        else
                        {
                        	reviewSection.addItem(displayValue);
                        }
                    }
                } // For each Metadatum
            } // If values exist
        }// For each input        
        return reviewSection;
    }        
    
}

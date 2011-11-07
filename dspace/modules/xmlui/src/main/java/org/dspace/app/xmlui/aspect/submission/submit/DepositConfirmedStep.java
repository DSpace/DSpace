package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.workflow.DryadWorkflowUtils;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 10/27/11
 * Time: 1:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class DepositConfirmedStep extends AbstractSubmissionStep{
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public DepositConfirmedStep(){
		this.requireHandle = true;
	}

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
    }

    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException{

        String itemID = parameters.getParameter("id", null);
        org.dspace.content.Item item = org.dspace.content.Item.find(context, Integer.valueOf(itemID));
        String doi = getDoiValue(item);

        Division main = body.addInteractiveDivision("deposit-confirmed", contextPath+"/deposit-confirmed", Division.METHOD_POST, "");
        main.setHead("Dryad submission received and assigned a provisional DOI");

        Division actionsDiv = main.addDivision("deposit-confirmed");

        Division dataPacakgeDiv = actionsDiv.addDivision("puboverviewdivision", "odd subdiv");

        dataPacakgeDiv.addPara().addContent("Thank you for your submission! Your submission has been assigned a provisional DOI. This DOI can be placed in your article, though it will not be fully registered with the DOI system until your submission has been approved by the Dryad curation staff.");

	dataPackageDiv.addPara("data-label", "bold").addContent(item.getName());
	dataPackageDiv.addPara("data-label", "bold").addContent("Provisional DOI: " + doi);

        Division dataFileDiv = actionsDiv.addDivision("dataFile", "even subdiv");
        dataFileDiv.addPara("data-label", "bold").addContent("Data Files");
        org.dspace.content.Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, item);

        int i = 0;
        for (org.dspace.content.Item dataFile : dataFiles){
            addPara(dataFileDiv, "Data File: " + dataFile.getName());
        }

    }


    private void addTextBox(Division div, String label, String name, String value, boolean disabled) throws WingException {
        Para p = div.addPara();
        p.addContent(label);
        Text t1 = p.addText(name);
        t1.setValue(value);
        t1.setDisabled(disabled);
    }

     private void addPara(Division div, String label) throws WingException {
        Para p = div.addPara();
        p.addContent(label);
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
        WingException, SQLException, IOException,
        AuthorizeException
    {
        //nothing to review, since submission is now Completed!
        return null;
    }

    private static String getDoiValue(org.dspace.content.Item item) {
        DCValue[] doiVals = item.getMetadata("dc", "identifier", null, org.dspace.content.Item.ANY);
        if (doiVals != null && 0 < doiVals.length) {
            return doiVals[0].value;
        }
        return null;
    }
}

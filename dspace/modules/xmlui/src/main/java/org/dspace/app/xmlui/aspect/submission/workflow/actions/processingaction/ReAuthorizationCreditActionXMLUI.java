package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;


import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.datadryad.anywhere.AssociationAnywhere;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.util.Date;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: lantian @ atmire . com
 * Date: 7/28/14
 * Time: 4:26 PM
 */
public class ReAuthorizationCreditActionXMLUI extends AbstractXMLUIAction {
    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String workflowID = request.getParameter("workflowID");
        String stepID = request.getParameter("stepID");
        String actionID = request.getParameter("actionID");

        Item item = workflowItem.getItem();
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        Collection collection = workflowItem.getCollection();

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";
        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");

        String success = "";
        if(request.getParameter("submit-credit")!=null)
        {
            String credit = request.getParameter("credit");
            if(credit.equals("true"))
            {
                success = submitCredit();

            }
        }
        try{
            PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = payementSystemService.getShoppingCartByItemId(context,item.getID());
            mainDiv.setHead("Re-submit Credit");
            mainDiv.addPara(success);
            if(shoppingCart!=null&&!shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                mainDiv.addPara().addContent("NOTE : click the credit button will tally your credit.");
                mainDiv.addList("submit-credit").addItem().addButton("submit-credit").setValue("Re-submit Credit");
            }
            else
            {
                mainDiv.addPara().addContent("NOTE : credit already tallied, click skip button to submit the item.");
                mainDiv.addList("submit-next").addItem().addButton("submit-credit-next").setValue("Skip resubmit credit");
            }


        }catch (Exception e)
        {
            //TODO: handle the exceptions
            log.error("Exception when entering the checkout step:", e);
        }
        String step = request.getParameter("stepID");
        String action = request.getParameter("actionID");
        mainDiv.addHidden("stepID").setValue(step);
        mainDiv.addHidden("actionID").setValue(action);
        mainDiv.addHidden("submission-continue").setValue(knot.getId());
        mainDiv.addList("submit-cancel").addItem().addButton("submit-cancel").setValue("Cancel");

    }
    private String submitCredit(){
        String success = "";
        try{

            Item item = workflowItem.getItem();
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(context,item.getID());
            // if journal-based subscription is in place, transaction is paid
            if(!shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)&&shoppingCart.hasSubscription()) {
                log.info("processed journal subscription for Item " + item.getHandle() + ", journal = " + shoppingCart.getJournal());
                log.debug("tally credit for journal = "+shoppingCart.getJournal());

                Scheme scheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
                Concept[] concepts = Concept.findByPreferredLabel(context,shoppingCart.getJournal(),scheme.getID());
                if(concepts!=null&&concepts.length!=0){
                    AuthorityMetadataValue[] metadataValues = concepts[0].getMetadata("internal", "journal", "customerID", Item.ANY);
                    if(metadataValues!=null&&metadataValues.length>0){
                        log.debug("marking cart for item " + item.getHandle() + " completed based on journal");
                        shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                        shoppingCart.setPaymentDate(new Date());
                        shoppingCart.update();

                        try{
                            success = AssociationAnywhere.tallyCredit(context, metadataValues[0].value, DOIIdentifierProvider.getDoiValue(item));
                            paymentSystemService.sendPaymentApprovedEmail(context, workflowItem, shoppingCart);
                        }catch (Exception e)
                        {
                            paymentSystemService.sendPaymentErrorEmail(context, workflowItem, shoppingCart,"problem: credit not tallied successfully. \n \n " + e.getMessage());
                            log.error(e.getMessage());
                            return e.getMessage();
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            log.error(e.getMessage(),e);
            return "Error when submitting the credit: " + e.getMessage();
        }
        return success;
    }
}

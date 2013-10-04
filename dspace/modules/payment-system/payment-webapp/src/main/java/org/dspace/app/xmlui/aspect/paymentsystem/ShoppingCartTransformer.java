
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.paymentsystem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.*;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;
import org.dspace.utils.DSpace;


/**
 * Shopping Cart Submision Step Transformer
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCartTransformer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(AbstractDSpaceTransformer.class);

    protected static final Message T_CartHelp=
            message("xmlui.PaymentSystem.shoppingcart.help");
    private static final String DSPACE_SUBMISSION_INFO = "dspace.submission.info";

    public void addOptions(Options options) throws SAXException, org.dspace.app.xmlui.wing.WingException,
            SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        String shoppingCartExist =  request.getParameter("shopping-cart-exist");
        if(shoppingCartExist==null||!shoppingCartExist.equals("true")){
            Map<String,String> messages = new HashMap<String,String>();
            //shoppingcart doesn't exist then add one in the option section
        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
        Enumeration s = request.getParameterNames();
        Enumeration v = request.getAttributeNames();
        SubmissionInfo submissionInfo=(SubmissionInfo)request.getAttribute("dspace.submission.info");

        Item item = null;
        try{
            if(submissionInfo==null)
            {
                //it is in workflow
                String workflowId = request.getParameter("workflowID");
		if(workflowId==null) {
		    // item is no longer in submission OR workflow, probably archived, so we don't need shopping cart info
		    return;
		}
                WorkflowItem workflowItem = WorkflowItem.find(context,Integer.parseInt(workflowId));
                item = workflowItem.getItem();
            }
            else
            {
                item = submissionInfo.getSubmissionItem().getItem();
            }

            //DryadJournalSubmissionUtils.journalProperties.get("");
            PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = null;
            //create new transaction or update transaction id with item
            shoppingCart = payementSystemService.getShoppingCartByItemId(context,item.getID());
            payementSystemService.updateTotal(context,shoppingCart,null);

            //add the order summary form (wrapped in div.ds-option-set for proper sidebar style)
            List info = options.addList("Payment",List.TYPE_FORM,"paymentsystem");
            //todo:find a better way to detect the step we are in
            boolean selectCountry=false;
            DCValue[] values= item.getMetadata("prism.publicationName");
            if(values!=null&&values.length>0)
            {
             selectCountry = true;
            }
            payementSystemService.generateShoppingCart(context,info,shoppingCart,manager,request.getContextPath(),selectCountry,messages);

            org.dspace.app.xmlui.wing.element.Item help = options.addList("need-help").addItem();
            help.addContent(T_CartHelp);
        }catch (Exception pe)
        {
            log.error("Exception: ShoppingCart:", pe);
        }
        }
    }


}

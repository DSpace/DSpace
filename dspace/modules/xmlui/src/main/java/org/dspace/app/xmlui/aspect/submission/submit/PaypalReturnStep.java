/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.paymentsystem.*;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * Provides Return Step from Paypal to present result from processed Paypal transaction
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaypalReturnStep extends AbstractStep {

    private static final Message T_PayPalVerified = message("xmlui.PaymentSystem.shoppingcart.verified");
    private static final Message T_Finalize = message("xmlui.Submission.submit.CheckoutStep.button.finalize");

        private static final Logger log = Logger.getLogger(PaypalReturnStep.class);
        @Override
        public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

            Request request = ObjectModelHelper.getRequest(objectModel);
            String submitUrl = "";
            String secureToken = request.getParameter("SECURETOKEN");
            String result = request.getParameter("RESULT");
            String message = request.getParameter("RESPMSG");

            String reference = request.getParameter("PNREF");
            Response response = ObjectModelHelper.getResponse(objectModel);

	    log.debug("paypal secureToken = " + secureToken);
	    log.debug("paypal result = " + result);
	    log.debug("paypal message = " + message);
	    log.debug("paypal reference = " + reference);
	    
            if(secureToken!=null){
                try{
                    //find the correct shopping cart based on the secrue token
                    ShoppingCart shoppingCart = ShoppingCart.findBySecureToken(context,secureToken);
                    if(shoppingCart!=null){
                        // The transaction is successful if the result is 0 (Approved) OR if the
                        // result is 4 (Invalid amount). Most cards will validate with an 0, but
                        // American Express cards still don't validate correctly, so they return
                        // 4 even when the card is fine.
                        if("0".equals(result) || "4".equals(result))
                        {
                            //successful transaction
                            shoppingCart.setTransactionId(reference);


                            int itemId = shoppingCart.getItem();
                            Item item = Item.find(context,itemId);
                            if(item!=null)
                            {
                                 if(message.startsWith("Verified")){
                                     //authorization
                                    shoppingCart.setStatus(ShoppingCart.STATUS_VERIFIED);
                                     Date now = new Date();
                                     shoppingCart.setOrderDate(now);
                                 } else if ("4".equals(result)) {
                                     //authorization, but paypal isn't supporting our zero-dollar transaction
                                     shoppingCart.setStatus(ShoppingCart.STATUS_VERIFIED);
                                 } else
                                 {
                                     log.debug("marking cart for item " + itemId + " completed, paypal result=" + result + ", message=\"" + message + "\"");
                                     shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                                     Date now = new Date();
                                     shoppingCart.setPaymentDate(now);
                                 }
                                //submitUrl = FlowUtils.processPaypalCheckout(context, request,response,item);
				 body.addDivision("successful").addPara(T_PayPalVerified);
				 body.addDivision("show_button").addHidden("show_button").setValue(T_Finalize);
                            }
                            else
                            {
                                shoppingCart.setStatus(ShoppingCart.STATUS_DENIlED);
                                addErrorLink(body,"Not a valid shopping cart");
                            }
                        }
                        else
                        {
                            shoppingCart.setStatus(ShoppingCart.STATUS_DENIlED);
                            //error in trasaction
                            addErrorLink(body,"We're sorry, but Dryad experienced an error in validating your method of payment. Error code:"+result);
			    log.error("There was an error in PayPal card validation. Code = " + result);

                        }
                        shoppingCart.update();
                    }
                    else
                    {
                        //can't find the shopingcart for this secure token
                        addErrorLink(body,"can't find the shopingcart for this secure token:"+secureToken);
                    }


                }catch (Exception e)
                {
                    //TODO: handle the exceptions
                   // System.out.println("errors in generate the payment form");
                    log.error("Exception when entering the checkout step:", e);
                    addErrorLink(body,"errors in generate the payment form:"+e.getMessage());
                }
            }
            else
            {
                //no secure token returned,reload the page to pay again or cotact admin
                addErrorLink(body,"error response from paypal");
            }

        }

    private void addErrorLink(Body body,String message) throws WingException{
        body.addDivision("error").addPara(message);
        body.addDivision("show_button").addHidden("show_button").setValue("skip payment");

    }

    }

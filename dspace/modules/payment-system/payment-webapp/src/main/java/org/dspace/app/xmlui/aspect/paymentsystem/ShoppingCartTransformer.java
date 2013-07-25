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
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.cocoon.*;

import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;

import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.*;
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
public class ShoppingCartTransformer extends AbstractDSpaceTransformer{

    private static final Logger log = Logger.getLogger(AbstractDSpaceTransformer.class);

    protected static final Message T_Header=
            message("xmlui.PaymentSystem.shoppingcart.order.header");

    protected static final Message T_Payer=
            message("xmlui.PaymentSystem.shoppingcart.order.payer");
    protected static final Message T_Price=
            message("xmlui.PaymentSystem.shoppingcart.order.price");
    protected static final Message T_Surcharge=
            message("xmlui.PaymentSystem.shoppingcart.order.surcharge");
    protected static final Message T_Total=
            message("xmlui.PaymentSystem.shoppingcart.order.total");
    protected static final Message T_noInteg=
            message("xmlui.PaymentSystem.shoppingcart.order.noIntegrateFee");
    protected static final Message T_Country=
            message("xmlui.PaymentSystem.shoppingcart.order.country");
    protected static final Message T_Voucher=
            message("xmlui.PaymentSystem.shoppingcart.order.voucher");
    protected static final Message T_Apply=
            message("xmlui.PaymentSystem.shoppingcart.order.apply");


    public void addOptions(Options options) throws SAXException, org.dspace.app.xmlui.wing.WingException,
            SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);

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
                WorkflowItem workflowItem = WorkflowItem.find(context,Integer.parseInt(workflowId));
                item = workflowItem.getItem();
            }
            else
            {
                item = submissionInfo.getSubmissionItem().getItem();
            }

            //DryadJournalSubmissionUtils.journalProperties.get("");
            PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart transaction = null;
            //create new transaction or update transaction id with item
            transaction = getTransaction(item, payementSystemService);
            payementSystemService.updateTotal(context,transaction,null);

            //add the order summary form
            List info = options.addList("Payment",List.TYPE_FORM,"paymentsystem");

            if(request.getRequestURI().contains("submit-checkout")||request.getRequestURI().contains("workflow")||request.getRequestURI().contains("deposit-confirmed")||request.getParameter("submit-voucher")!=null)
            {
                generateFinalOrderFrom(info,transaction,manager,payementSystemService,request.getContextPath());
            }
            else
            {
                generateOrderFrom(info,transaction,manager,payementSystemService,request.getContextPath());
            };

            org.dspace.app.xmlui.wing.element.Item help = options.addList("need-help").addItem();
            help.addContent("Email us at ");
            help.addXref(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.help.email"), ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.help.email"));
            help.addContent(" or call us at "+ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.help.call"));

        }catch (Exception pe)
        {
            log.error("Exception: ShoppingCart:", pe);
        }
    }

    private ShoppingCart getTransaction(Item item, PaymentSystemService payementSystemService) throws AuthorizeException, SQLException, PaymentSystemException, IOException {
        ShoppingCart shoppingCart=null;
        if(item!=null){
            shoppingCart = payementSystemService.getShoppingCartByItemId(context,item.getID());
        }
        return shoppingCart;
    }

    private void generateOrderFrom(org.dspace.app.xmlui.wing.element.List info,ShoppingCart transaction,PaymentSystemConfigurationManager manager,PaymentSystemService paymentSystemService,String baseUrl) throws WingException,SQLException{
        Item item = Item.find(context,transaction.getItem());
        Long totalSize = new Long(0);


        List hiddenList = info.addList("transaction");
        hiddenList.addItem().addHidden("transactionId").setValue(Integer.toString(transaction.getID()));
        hiddenList.addItem().addHidden("baseUrl").setValue(baseUrl);
        //add selected currency section
        info.addLabel(T_Header);
        org.dspace.app.xmlui.wing.element.Item currency = info.addItem("currency-list", "select-list");
        Select currencyList = currency.addSelect("currency");
        Properties currencyArray = manager.getAllCurrencyProperty();
        Properties countryArray = manager.getAllCountryProperty();
        for(String currencyTemp: currencyArray.stringPropertyNames())
        {
            if(transaction.getCurrency().equals(currencyTemp))
            {
                currencyList.addOption(true, currencyTemp, currencyTemp);
            }
            else
            {
                currencyList.addOption(false, currencyTemp, currencyTemp);
            }
        }
        info.addLabel(T_Payer);
        EPerson e = EPerson.find(context,transaction.getDepositor());
        info.addItem("payer","payer").addContent(e.getFullName());


        info.addLabel(T_Price);
        if(paymentSystemService.hasDiscount(context,transaction,null))
        {
            info.addItem("price","price").addContent("0.0");
        }
        else
        {
            info.addItem("price","price").addContent(Double.toString(transaction.getBasicFee()));
        }



        //add the large file surcharge section
        info.addLabel(T_Surcharge);
        info.addItem("surcharge","surcharge").addContent(Double.toString(paymentSystemService.getSurchargeLargeFileFee(context,transaction)));

        Double noIntegrateFee =  paymentSystemService.getNoIntegrateFee(context,transaction,null);
        //add the no integrate fee if it is not 0
        info.addLabel(T_noInteg);
        if(!paymentSystemService.hasDiscount(context,transaction,null)&&noIntegrateFee>0&&!paymentSystemService.hasDiscount(context,transaction,null))
            {
                info.addItem("no-integret","no-integret").addContent(Double.toString(noIntegrateFee));
            }
        else
        {
            info.addItem("no-integret","no-integret").addContent("0.0");
        }


        //add the total price
        info.addLabel(T_Total);
        info.addItem("total","total").addContent(Double.toString(transaction.getTotal()));

        info.addLabel(T_Country);
        Select countryList = info.addItem("country-list", "select-list").addSelect("country");

        for(String temp:countryArray.stringPropertyNames()){
            if(transaction.getCountry().equals(temp)) {
                countryList.addOption(true,temp,temp);
            }
            else
            {
                countryList.addOption(false,temp,temp);
            }
        }

        info.addLabel(T_Voucher);
        org.dspace.app.xmlui.wing.element.Item voucher = info.addItem("voucher-list","voucher-list");
        Voucher voucher1 = Voucher.findById(context,transaction.getVoucher());
        if(voucher1!=null)
        voucher.addText("voucher","voucher").setValue(voucher1.getCode());
        else
        voucher.addText("voucher","voucher");
        voucher.addButton("apply","apply");

    }

    private void generateFinalOrderFrom(org.dspace.app.xmlui.wing.element.List info,ShoppingCart transaction,PaymentSystemConfigurationManager manager,PaymentSystemService paymentSystemService,String baseUrl) throws WingException,SQLException{
        Properties currencyArray = manager.getAllCurrencyProperty();

        List hiddenList = info.addList("transaction");
        hiddenList.addItem().addHidden("transactionId").setValue(Integer.toString(transaction.getID()));
        hiddenList.addItem().addHidden("baseUrl").setValue(baseUrl);
        //add selected currency section
        info.addLabel(T_Header);
        org.dspace.app.xmlui.wing.element.Item currency = info.addItem("currency-list", "select-list");
        Select currencyList = currency.addSelect("currency");

        for(String currencyTemp: currencyArray.stringPropertyNames())
        {
            if(transaction.getCurrency().equals(currencyTemp))
            {
                currencyList.addOption(true, currencyTemp, currencyTemp);
            }
        }
        info.addLabel(T_Payer);
        EPerson e = EPerson.find(context,transaction.getDepositor());
        info.addItem("payer","payer").addContent(e.getFullName());


        info.addLabel(T_Price);
        String currencyTemp = transaction.getCurrency();
        if(paymentSystemService.hasDiscount(context,transaction,null))
        {
            info.addItem("final-price","price").addContent("0.0");
        }
        else
        {
            info.addItem("final-price","price").addContent(Double.toString(transaction.getBasicFee()));
        }

        //add the large file surcharge section
        info.addLabel(T_Surcharge);
        info.addItem("surcharge","surcharge").addContent(Double.toString(paymentSystemService.getSurchargeLargeFileFee(context,transaction)));


        Double noIntegrateFee = paymentSystemService.getNoIntegrateFee(context,transaction,null);
        //add the no integrate fee if it is not 0
        info.addLabel(T_noInteg);
        if(!paymentSystemService.hasDiscount(context,transaction,null)&&noIntegrateFee>0&&!paymentSystemService.hasDiscount(context,transaction,null)){

            info.addItem("no-integret","no-integret").addContent(Double.toString(noIntegrateFee));
        }

        //add the total price
        info.addLabel(T_Total);
        info.addItem("total","total").addContent(Double.toString(transaction.getTotal()));

        info.addLabel(T_Voucher);
        org.dspace.app.xmlui.wing.element.Item voucher = info.addItem("voucher-list","voucher-list");
        Voucher voucher1 = Voucher.findById(context,transaction.getVoucher());
        if(voucher1!=null)
        voucher.addContent(voucher1.getCode());



    }

}

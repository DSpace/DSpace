/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.paymentsystem;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.*;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Administrative Edit Interface for Shopping Cart Records
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCartUpdateReader extends AbstractReader implements Recyclable {

    private static Logger log = Logger.getLogger(ShoppingCartUpdateReader.class);
    /** These are all our parameters which can be used by this generator **/


    /** The Cocoon response */
    public void generate() throws IOException, SAXException, ProcessingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        PaymentSystemConfigurationManager paymentSystemConfigurationManager = new PaymentSystemConfigurationManager();
        PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

        String transactionId =(String) request.getParameter("transactionId");
        Item item =null;
        try{
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);
            ShoppingCart transaction = payementSystemService.getShoppingCart(context,Integer.parseInt(transactionId));
            if(transaction==null)
            {
                //can't find the transaction
                return;
            }
            String errorMessage = modifyTransaction(context,payementSystemService,transaction,request,dso);

            Response response = ObjectModelHelper.getResponse(objectModel);


            generateJSON(paymentSystemConfigurationManager, transaction, response,payementSystemService,context,request,errorMessage);

        }
        catch (SQLException se)
        {
            log.error("SQL Exception when updating the shopping cart:", se);
             return;
        }
        catch (PaymentSystemException pe)
        {
            log.error("Payment System Exception when updating the shopping cart::", pe);
            return;
        }
        catch (AuthorizeException ae)
        {
            log.error("Authorization Exception when updating the shopping cart::", ae);
            return;
        }
    }

    private void generateJSON(PaymentSystemConfigurationManager paymentSystemConfigurationManager, ShoppingCart shoppingCart, Response response,PaymentSystemService paymentSystemService,Context context,Request request,String errorMessage) throws SQLException,IOException {
        Double total = shoppingCart.getTotal();
        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());
        //{ "firstName":"John" , "lastName":"Doe" }
        String journal =request.getParameter("journal");
        Double basicFee =shoppingCart.getBasicFee();
        Double surcharge = paymentSystemService.getSurchargeLargeFileFee(context,shoppingCart);
        Double noIntegrateFee = paymentSystemService.getNoIntegrateFee(context,shoppingCart,journal);
        Integer voucherId= shoppingCart.getVoucher();
        Voucher voucher = Voucher.findById(context,voucherId);
        String voucherCode = "";
        if(voucher!=null)
        {
            voucherCode = voucher.getCode();
        }
        String waiverMessage = "";
        String payername = paymentSystemService.getPayer(context,shoppingCart,null);
        switch (paymentSystemService.getWaiver(context,shoppingCart,""))
        {
            case ShoppingCart.COUNTRY_WAIVER:waiverMessage= "Country:"+shoppingCart.getCountry()+" paid the basic fee and no integration fee";payername="Country:"+shoppingCart.getCountry();break;
            case ShoppingCart.JOUR_WAIVER: waiverMessage = "Journal paid the basic fee and no integration fee";payername="Journal";break;
            case ShoppingCart.VOUCHER_WAIVER: waiverMessage = "Voucher has been applied";break;
        }

        String result = "{\"total\":\""+symbol+String.valueOf(Double.toString(total))+"\",\"price\":\""+symbol+basicFee+"\",\"surcharge\":\""+symbol+surcharge+"\",\"noIntegrateFee\":\""+symbol+noIntegrateFee+"\",\"voucher\":\""+voucherCode+"\""+",\"errorMessage\":\""+errorMessage+"\",\"waiverMessage\":\""+waiverMessage+"\",\"payer\":\""+payername+"\"}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(result.getBytes("UTF-8"));
        byte[] buffer = new byte[8192];
        response.setHeader("Content-Length", String.valueOf(result.length()));
        int length;
        while ((length = inputStream.read(buffer)) > -1)
        {
            out.write(buffer, 0, length);
        }
        out.flush();
    }

    private Item findItem(String itemId, Context context, ShoppingCart transaction) throws SQLException,AuthorizeException,IOException {
        Item item = null;
        if(itemId==null)
        {
            item=Item.find(context,transaction.getItem());
            if(item!=null){
                itemId=Integer.toString(transaction.getItem());
            }
        }
        else
        {
            item=Item.find(context,Integer.parseInt(itemId));
        }
        return item;
    }

    private String modifyTransaction(Context context,PaymentSystemService payementSystemService, ShoppingCart transaction, Request request,DSpaceObject dso) throws AuthorizeException, SQLException, PaymentSystemException,IOException {
        Item item;
        String errorMessage = "";


        String itemId = request.getParameter("itemId");
        String journal =request.getParameter("journal");

        item =findItem(itemId, context, transaction);
        if(item == null)
        {
            //cant find the item, the transaction is not associate with any item, which should not happen in the submission procedure
            return "Item not found";
        }


        if(dso==null)
        {
            dso=item.getOwningCollection();
        }

        if(request.getParameter("currency")!=null)
        {
            String currency=request.getParameter("currency") .toString();
            payementSystemService.setCurrency(transaction,currency);

        }
        if(request.getParameter("country")!=null)
        {
            String country=request.getParameter("country").toString();
            transaction.setCountry(country);
        }
        if(request.getParameter("voucher")!=null&&request.getParameter("voucher").length()>0&&!request.getParameter("voucher").equals("undefined"))
        {
            String voucherCode=request.getParameter("voucher").toString();
            Voucher voucher = Voucher.findByCode(context,voucherCode);
            VoucherValidationService voucherValidationService =  new DSpace().getSingletonService(VoucherValidationService.class);
            if(voucher!=null&&voucherValidationService.validate(context,voucher.getID(),transaction))
            {
                transaction.setVoucher(voucher.getID());
                errorMessage = "";

            }
            else
            {
                transaction.setVoucher(null);
                if(voucherCode.length()>0)
                    errorMessage = "The voucher code is not valid:can't find the voucher code or the voucher code has been used";

            }

        }
        else
        {
            transaction.setVoucher(null);
        }
        payementSystemService.updateTotal(context,transaction,journal);
        return errorMessage;

    }

}

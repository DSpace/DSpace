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
            ShoppingCart transaction = payementSystemService.getTransaction(context,Integer.parseInt(transactionId));
            if(transaction==null)
            {
                //can't find the transaction
                return;
            }
            modifyTransaction(context,payementSystemService,transaction,request,dso);
            Response response = ObjectModelHelper.getResponse(objectModel);


            generateJSON(paymentSystemConfigurationManager, transaction, response,payementSystemService,context,request);

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

    private void generateJSON(PaymentSystemConfigurationManager paymentSystemConfigurationManager, ShoppingCart transaction, Response response,PaymentSystemService payementSystemService,Context context,Request request) throws SQLException,IOException {
        Double total = transaction.getTotal();
        //{ "firstName":"John" , "lastName":"Doe" }
        String journal =request.getParameter("journal");
        Double basicFee =paymentSystemConfigurationManager.getCurrencyProperty(transaction.getCurrency());
        Double surcharge = payementSystemService.getSurchargeLargeFileFee(context,transaction);
        Double noIntegrateFee = payementSystemService.getNoIntegrateFee(context,transaction,journal);
        String voucherCode = transaction.getVoucher();

        String result = "{\"total\":\""+String.valueOf(Double.toString(total))+"\",\"price\":\""+basicFee+"\",\"surcharge\":\""+surcharge+"\",\"noIntegrateFee\":\""+noIntegrateFee+"\",\"voucher\":\""+voucherCode+"\"}";

        if(payementSystemService.hasDiscount(context,transaction,journal))  {
            result = "{\"total\":\""+String.valueOf(Double.toString(total))+"\",\"price\":\"0.0\",\"surcharge\":\"0.0\",\"noIntegrateFee\":\"0.0\",\"voucher\":\""+voucherCode+"\"}";
        }

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

    private void modifyTransaction(Context context,PaymentSystemService payementSystemService, ShoppingCart transaction, Request request,DSpaceObject dso) throws AuthorizeException, SQLException, PaymentSystemException,IOException {
        Item item;

        String itemId = request.getParameter("itemId");
        String journal =request.getParameter("journal");

        item =findItem(itemId, context, transaction);
        if(item == null)
        {
            //cant find the item, the transaction is not associate with any item, which should not happen in the submission procedure
            return;
        }


        if(dso==null)
        {
            dso=item.getOwningCollection();
        }

        if(request.getParameter("currency")!=null)
        {
            String currency=request.getParameter("currency") .toString();
            transaction.setCurrency(currency);

        }
        if(request.getParameter("country")!=null)
        {
            String country=request.getParameter("country").toString();
            transaction.setCountry(country);
        }
        if(request.getParameter("voucher")!=null&&!request.getParameter("voucher").equals("undefined"))
        {
            String voucher=request.getParameter("voucher").toString();
            VoucherValidationService voucherValidationService =  new DSpace().getSingletonService(VoucherValidationService.class);
//            if(voucherValidationService.validate(context,voucher))
//            {
                transaction.setVoucher(voucher);
//            }
//            else
//            {
//                //try to update the voucher with an invalid voucher code will cause the saved voucher code be deleted
//                transaction.setVoucher(null);
//            }
        }
        //TODO:only setup the price when the old total price is higher than the price right now
        transaction.setTotal(payementSystemService.calculateTransactionTotal(context,transaction,journal));
        transaction.setModified(true);
        payementSystemService.modifyTransaction(context,transaction,dso);
    }
}

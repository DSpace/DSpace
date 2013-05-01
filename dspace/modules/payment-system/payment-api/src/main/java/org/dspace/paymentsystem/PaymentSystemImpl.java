/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * PaymentService provides an interface for the DSpace application to
 * interact with the Payment Service implementation and persist
 * Shopping Cart Changes
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaymentSystemImpl implements PaymentSystemService {


    private int transactionId;
    private ShoppingCart transactions;

    /** log4j log */
    private static Logger log = Logger.getLogger(PaymentSystemImpl.class);

    /** Protected Constructor */
    protected PaymentSystemImpl()
    {
    }



    public ShoppingCart createNewTrasaction(Context context,DSpaceObject dso, Integer itemId, Integer epersonId, String country, String currency, String status) throws SQLException,
            AuthorizeException,IOException {
        ShoppingCart newTransaction = ShoppingCart.create(context, dso);
        newTransaction.setCountry(country);
        newTransaction.setCurrency(currency);
        newTransaction.setDepositor(epersonId);
        newTransaction.setExpiration(null);
        if(itemId !=null){
            //make sure we only create the transaction for data package
            Item item = Item.find(context,itemId);
            org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
            if(dataPackage!=null)
            {
                itemId = dataPackage.getID();
            }
            newTransaction.setItem(itemId);
        }
        newTransaction.setStatus(status);
        newTransaction.setVoucher(null);
        newTransaction.setTransactionId(null);
        Double totalPrice =  calculateTransactionTotal(context,newTransaction,null);
        newTransaction.setTotal(totalPrice);
        newTransaction.update();
        return newTransaction;
    }


    public void modifyTransaction(Context context,ShoppingCart transaction,DSpaceObject dso)throws AuthorizeException, SQLException,PaymentSystemException{
//TODO:add authorization
//        try{
//            AuthorizeManager.authorizeAction(context, dso, Constants.WRITE);
//        }catch (AuthorizeException e)
//        {
//            throw new AuthorizeException(
//                    "You must be an admin to create a Transaction");
//        }
        if(transaction.getModified())
        {
            transaction.update();
            transaction.setModified(false);
        }

    }

    public void deleteTransaction(Context context,Integer transactionId) throws AuthorizeException, SQLException, PaymentSystemException {
           ShoppingCart trasaction = ShoppingCart.findByTransactionId(context, transactionId);
           trasaction.delete();
    }

    public ShoppingCart getTransaction(Context context,Integer transactionId) throws SQLException
    {
        return ShoppingCart.findByTransactionId(context, transactionId);
    }

    public ShoppingCart[] findAllShoppingCart(Context context,Integer itemId)throws SQLException{
        if(itemId==null||itemId==-1){
            return ShoppingCart.findAll(context);
        }
        else
        {
            ShoppingCart[] shoppingCarts = new ShoppingCart[1];
            shoppingCarts[0] = getTransactionByItemId(context,itemId);
            return shoppingCarts;
        }
    }

    public ShoppingCart getTransactionByItemId(Context context,Integer itemId) throws SQLException
    {
        //make sure we get correct transaction for data package
        Item item = Item.find(context,itemId);
        org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        if(dataPackage!=null)
        {
            itemId = dataPackage.getID();
        }
        List<ShoppingCart> transactionList= ShoppingCart.findAllByItem(context, itemId);
        if(transactionList!=null && transactionList.size()>0)
            return transactionList.get(0);
        return null;
    }

    public Double calculateTransactionTotal(Context context,ShoppingCart transaction,String journal) throws SQLException{


        Double price = new Double(0);
        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();

        if(!hasDiscount(context,transaction,journal))
        {
            //no journal,voucher,country discount
            Double basicFee =  manager.getCurrencyProperty(transaction.getCurrency());
            double fileSizeFee=getSurchargeLargeFileFee(context, transaction);
            price = basicFee+fileSizeFee;
            price = price+getNoIntegrateFee(context,transaction,journal);

        }
        return price;
    }

    public double getSurchargeLargeFileFee(Context context, ShoppingCart transaction) throws SQLException {

        Item item =Item.find(context, transaction.getItem());
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, item);
        Long allowedSizeT=PaymentSystemConfigurationManager.getMaxFileSize();
        long allowedSize = allowedSizeT;

        double totalSurcharge=0;
        long totalSizeDataFile=0;
        for(Item dataFile : dataFiles){

            Bundle bundles[] = dataFile.getBundles();
            for(Bundle bundle:bundles)
            {
                Bitstream bitstreams[]=bundle.getBitstreams();
                for(Bitstream bitstream:bitstreams)
                {
                    totalSizeDataFile=totalSizeDataFile+bitstream.getSize();
                }
            }

        }

        if(totalSizeDataFile > allowedSize){
            totalSurcharge+=PaymentSystemConfigurationManager.getSizeFileFeeProperty(transaction.getCurrency());
            int unit =0;
            Long UNITSIZE=PaymentSystemConfigurationManager.getUnitSize();  //1 GB
            //eg. $10 after every 1 gb
            if(UNITSIZE!=null&&UNITSIZE>0) {
                Long overSize = (totalSizeDataFile-allowedSize)/UNITSIZE;
                unit = overSize.intValue();
            }
            totalSurcharge = totalSurcharge+Double.parseDouble(PaymentSystemConfigurationManager.getSizeFileFeeAfterProperty(transaction.getCurrency()))*unit;

        }


        return totalSurcharge;
    }

    public boolean getJournalSubscription(Context context, ShoppingCart transaction, String journal) throws SQLException {
        if(journal==null){
            Item item = Item.find(context,transaction.getItem()) ;
            if(item!=null)
            {
                try{
                    //only take the first journal
                    DCValue[] values = item.getMetadata("prism.publicationName");
                    if(values!=null && values.length > 0){
                        journal=values[0].value;
                    }
                }catch (Exception e)
                {
                    log.error("Exception when get journal in journal subscription:", e);
                }
            }

        }
        if(journal!=null)
        {
            try{
                Map<String, String> properties = DryadJournalSubmissionUtils.journalProperties.get(journal);
                if(properties==null) return false;

                String subscription = properties.get("subscriptionPaid");
                if(StringUtils.equals(subscription, ShoppingCart.FREE))
                    return true;

            }catch(Exception e){
                log.error("Exception when get journal subscription:", e);
                return false;
            }
        }
        return false;
    }

    public double getNoIntegrateFee(Context context, ShoppingCart transaction, String journal) throws SQLException {

        Double totalPrice = new Double(0);
        PaymentSystemConfigurationManager paymentSystemConfigurationManager =new PaymentSystemConfigurationManager();
        if(journal==null){
            Item item = Item.find(context,transaction.getItem()) ;
            if(item!=null)
            {
                try{
                    DCValue[] values = item.getMetadata("prism.publicationName");
                    if(values!=null && values.length > 0){
                        journal=values[0].value;
                    }
                }catch (Exception e)
                {
                    log.error("Exception when get journal name in geting no integration fee:", e);
                }
            }

        }
        if(journal!=null)
        {
            try{
                DryadJournalSubmissionUtils util = new DryadJournalSubmissionUtils();
                Map<String, String> properties = util.journalProperties.get(journal);
                if(properties!=null){
                String subscription = properties.get("integrated");
                if(subscription==null || !subscription.equals(ShoppingCart.FREE))
                {

                    totalPrice= paymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(transaction.getCurrency());
                }


            }
            else
            {
                totalPrice= paymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(transaction.getCurrency());
            }
            }catch(Exception e){
                log.error("Exception when get no integration fee:", e);
            }
        }
        else
        {
            totalPrice= paymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(transaction.getCurrency());
        }
        return totalPrice;
    }

    private boolean voucherValidate(Context context,ShoppingCart transaction){
        VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
        return voucherValidationService.validate(context,transaction.getVoucher());
    }

        public boolean hasDiscount(Context context,ShoppingCart transaction,String journal)throws SQLException{
        //this method check all the discount: journal,country,voucher
            PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
            Properties currencyArray = manager.getAllCurrencyProperty();
            Properties countryArray = manager.getAllCountryProperty();
            Boolean journalSubscription =  getJournalSubscription(context, transaction, journal);
            Boolean countryDiscount = countryArray.get(transaction.getCountry()).equals(ShoppingCart.COUNTRYFREE);
            Boolean voucherDiscount = voucherValidate(context,transaction);

            if(journalSubscription||countryDiscount||voucherDiscount){
                return true;
            }
            return false;
        }

}

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

    /** log4j log */
    private static Logger log = Logger.getLogger(PaymentSystemImpl.class);

    /** Protected Constructor */
    protected PaymentSystemImpl()
    {
    }



    public ShoppingCart createNewShoppingCart(Context context, Integer itemId, Integer epersonId, String country, String currency, String status) throws SQLException,
            PaymentSystemException {
        ShoppingCart newShoppingcart = ShoppingCart.create(context);
        newShoppingcart.setCountry(country);
        newShoppingcart.setCurrency(currency);
        newShoppingcart.setDepositor(epersonId);
        newShoppingcart.setExpiration(null);
        if(itemId !=null){
            //make sure we only create the shoppingcart for data package
            Item item = Item.find(context,itemId);
            org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
            if(dataPackage!=null)
            {
                itemId = dataPackage.getID();
            }
            newShoppingcart.setItem(itemId);
        }
        newShoppingcart.setStatus(status);
        newShoppingcart.setVoucher(null);
        newShoppingcart.setTransactionId(null);

        newShoppingcart.setBasicFee(PaymentSystemConfigurationManager.getCurrencyProperty(currency));
        newShoppingcart.setNoInteg(PaymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(currency));
        newShoppingcart.setSurcharge(PaymentSystemConfigurationManager.getSizeFileFeeProperty(currency));
        Double totalPrice =  calculateShoppingCartTotal(context, newShoppingcart, null);
        newShoppingcart.setTotal(totalPrice);
        newShoppingcart.update();
        return newShoppingcart;
    }


    public void modifyShoppingCart(Context context,ShoppingCart shoppingcart,DSpaceObject dso)throws AuthorizeException, SQLException,PaymentSystemException{

        if(shoppingcart.getModified())
        {
            shoppingcart.update();
            shoppingcart.setModified(false);
        }

    }

    public void setCurrency(ShoppingCart shoppingCart,String currency)throws SQLException{
        shoppingCart.setCurrency(currency);
        shoppingCart.setBasicFee(PaymentSystemConfigurationManager.getCurrencyProperty(currency));
        shoppingCart.setNoInteg(PaymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(currency));
        shoppingCart.setSurcharge(PaymentSystemConfigurationManager.getSizeFileFeeProperty(currency));
        shoppingCart.update();
        shoppingCart.setModified(false);

    }

    public void deleteShoppingCart(Context context,Integer shoppingcartId) throws AuthorizeException, SQLException, PaymentSystemException {
           ShoppingCart trasaction = ShoppingCart.findByTransactionId(context, shoppingcartId);
           trasaction.delete();
    }

    public ShoppingCart getShoppingCart(Context context,Integer shoppingcartId) throws SQLException
    {
        return ShoppingCart.findByTransactionId(context, shoppingcartId);
    }

    public ShoppingCart[] findAllShoppingCart(Context context,Integer itemId)throws SQLException,PaymentSystemException{
        if(itemId==null||itemId==-1){
            return ShoppingCart.findAll(context);
        }
        else
        {
            ShoppingCart[] shoppingCarts = new ShoppingCart[1];
            shoppingCarts[0] = getShoppingCartByItemId(context, itemId);
            return shoppingCarts;
        }
    }

    public ShoppingCart getShoppingCartByItemId(Context context,Integer itemId) throws SQLException,PaymentSystemException
    {
        //make sure we get correct shoppingcart for data package
        Item item = Item.find(context,itemId);
        org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        if(dataPackage!=null)
        {
            itemId = dataPackage.getID();
        }
        List<ShoppingCart> shoppingcartList= ShoppingCart.findAllByItem(context, itemId);
        if(shoppingcartList!=null && shoppingcartList.size()>0)
            return shoppingcartList.get(0);
        else
        {
            //if no shopping cart , create a new one
            return createNewShoppingCart(context,itemId,context.getCurrentUser().getID(),ShoppingCart.COUNTRY_US,ShoppingCart.CURRENCY_US,ShoppingCart.STATUS_OPEN);
        }

    }

    public Double calculateShoppingCartTotal(Context context,ShoppingCart shoppingcart,String journal) throws SQLException{


        Double price = new Double(0);

        if(hasDiscount(context,shoppingcart,journal))
        {
            //has discount , only caculate the file surcharge fee
            price =getSurchargeLargeFileFee(context, shoppingcart);
        }
        else
        {
            //no journal,voucher,country discount
            Double basicFee = shoppingcart.getBasicFee();
            double fileSizeFee=getSurchargeLargeFileFee(context, shoppingcart);
            price = basicFee+fileSizeFee;
            price = price+getNoIntegrateFee(context,shoppingcart,journal);
        }
        return price;
    }

    public double getSurchargeLargeFileFee(Context context, ShoppingCart shoppingcart) throws SQLException {

        Item item =Item.find(context, shoppingcart.getItem());
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
            totalSurcharge+=shoppingcart.getSurcharge();
            int unit =0;
            Long UNITSIZE=PaymentSystemConfigurationManager.getUnitSize();  //1 GB
            //eg. $10 after every 1 gb
            if(UNITSIZE!=null&&UNITSIZE>0) {
                Long overSize = (totalSizeDataFile-allowedSize)/UNITSIZE;
                unit = overSize.intValue();
            }
            totalSurcharge = totalSurcharge+shoppingcart.getSurcharge()*unit;

        }


        return totalSurcharge;
    }

    public boolean getJournalSubscription(Context context, ShoppingCart shoppingcart, String journal) throws SQLException {
        if(journal==null){
            Item item = Item.find(context,shoppingcart.getItem()) ;
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

    public double getNoIntegrateFee(Context context, ShoppingCart shoppingcart, String journal) throws SQLException {

        Double totalPrice = new Double(0);
        if(journal==null){
            Item item = Item.find(context,shoppingcart.getItem()) ;
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

                    totalPrice= shoppingcart.getNoInteg();
                }


            }
            else
            {
                totalPrice= shoppingcart.getNoInteg();
            }
            }catch(Exception e){
                log.error("Exception when get no integration fee:", e);
            }
        }
        else
        {
            totalPrice= shoppingcart.getNoInteg();
        }
        return totalPrice;
    }

    private boolean voucherValidate(Context context,ShoppingCart shoppingcart){
        VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
        return voucherValidationService.validate(context,shoppingcart.getVoucher(),shoppingcart);
    }

    public boolean hasDiscount(Context context,ShoppingCart shoppingcart,String journal)throws SQLException{
        //this method check all the discount: journal,country,voucher
            PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
            Properties countryArray = manager.getAllCountryProperty();
            Boolean journalSubscription =  getJournalSubscription(context, shoppingcart, journal);
            Boolean countryDiscount = countryArray.get(shoppingcart.getCountry()).equals(ShoppingCart.COUNTRYFREE);
            Boolean voucherDiscount = voucherValidate(context,shoppingcart);

            if(journalSubscription||countryDiscount||voucherDiscount){
                return true;
            }
            return false;
        }

    public void updateTotal(Context context, ShoppingCart shoppingCart, String journal) throws SQLException{
        Double newPrice = calculateShoppingCartTotal(context,shoppingCart,journal);
        //TODO:only setup the price when the old total price is higher than the price right now
        shoppingCart.setTotal(newPrice);
        shoppingCart.update();
        shoppingCart.setModified(false);
    }

}

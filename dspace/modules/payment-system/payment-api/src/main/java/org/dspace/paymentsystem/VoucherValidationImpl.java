/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.commons.lang.RandomStringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * This is a stub interface to support interaction with a voucher
 * database during submission and workflow steps.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class VoucherValidationImpl implements VoucherValidationService
{

    public boolean validate(Context context, Integer voucherId,ShoppingCart shoppingCart)
    {
        if (voucherId != null)
        {
           try{
                Voucher voucher = findById(context,voucherId);
                if(voucher!=null)
                {
                    //if the voucher haven't been used than return true
                    ShoppingCart newShoppingCart = ShoppingCart.findByVoucher(context,voucherId);
                    if(newShoppingCart==null||(shoppingCart.equals(newShoppingCart)&&!shoppingCart.getStatus().equals(Voucher.STATUS_USED)))
                    {
                        //no shopping cart is using the voucher or the voucher is used by the shopping cart it self
                        return true;
                    }
                }

           }catch(Exception e){

           }
        }
        return false;
    }

    public boolean voucherUsed(Context context, String voucherCode)
    {
        if (voucherCode != null)
        {
            try{
                Voucher voucher = findByCode(context,voucherCode);
                if(voucher!=null)
                {
                    Integer voucherId = voucher.getID();
                    //if the voucher haven't been used than return true
                    ShoppingCart newShoppingCart = ShoppingCart.findByVoucher(context,voucherId);
                    if(newShoppingCart==null||newShoppingCart.getStatus().equals(Voucher.STATUS_USED))

                    {
                        //no shopping cart is using the voucher
                        return false;
                    }
                }

            }catch(Exception e){

            }
        }
        return true;
    }

    @Override
    public Voucher create(Context context,String code,String status,Date creation, String explanation,String customer, Integer generator) throws SQLException,AuthorizeException {


        Voucher newVoucher = Voucher.create(context);
        newVoucher.setCode(code);
        newVoucher.setStatus(status);
        newVoucher.setCreation(creation);
        newVoucher.setExplanation(explanation);
        newVoucher.setGenerator(generator);
        newVoucher.setCustomer(customer);
        newVoucher.update();
        return newVoucher;
    }

    @Override
    public Voucher findById(Context context, Integer voucherId) throws SQLException {
        return Voucher.findById(context,voucherId);
    }

    @Override
    public void delete(Context context, Integer voucherId) throws SQLException,AuthorizeException {
        Voucher voucher = findById(context,voucherId);
        if(voucher!=null){
            voucher.delete();
        }
    }

    @Override
    public Voucher findByCode(Context context, String code) throws SQLException {
       return Voucher.findByCode(context,code);
    }

    @Override
    public ArrayList<Voucher> createVouchers(Context context,String status,Date creation,int totalNumber,String explanation,String customer, Integer generator) throws SQLException,AuthorizeException {
        ArrayList<Voucher> vouchers = new ArrayList<Voucher>();
        int i =0;
        while(i < totalNumber)
        {
            Random random = new Random();
            String code = RandomStringUtils.random(8, true, true);
            vouchers.add(create(context,code,status, creation, explanation,customer,generator));
            i=i+1;
        }
       return vouchers;

    }
}

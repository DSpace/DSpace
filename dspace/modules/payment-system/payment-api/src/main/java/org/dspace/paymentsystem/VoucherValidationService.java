/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 *  This is a stub interface to support interaction with a voucher
 *  database during submission and workflow steps.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public interface VoucherValidationService {

      public boolean validate(Context context,Integer voucherId,ShoppingCart shoppingCart);

      public boolean voucherUsed(Context context,String voucherCode);

      public Voucher create(Context context,String code,String status,Date creation,String explanation,String customer,String customerName, Integer generator, String batchId)throws SQLException,AuthorizeException;

      public Voucher findById(Context context,Integer voucherId)throws SQLException;

      public void delete(Context context, Integer voucherId) throws SQLException,AuthorizeException;

      public Voucher findByCode(Context context, String code) throws SQLException;

      public ArrayList<Voucher> createVouchers(Context context,String status,Date creation,int totalNumber,String explanation,String customer,String customerName, Integer generator) throws SQLException,AuthorizeException;
}

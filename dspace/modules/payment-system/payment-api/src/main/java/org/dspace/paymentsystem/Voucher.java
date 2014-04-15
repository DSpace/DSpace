package org.dspace.paymentsystem;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: lantian @ atmire . com
 * Date: 7/11/13
 * Time: 10:30 AM
 */
public class Voucher {
    public static final int VOUCHER_ID = 1;

    public static final int CREATION = 2;

    public static final int STATUS = 3;

    public static final int CODE = 4;

    public static final int BATCH_ID = 5;

    public static final int CUSTOMER_NAME = 6;

    public static final String STATUS_USED = "used";
    public static final String STATUS_OPEN = "open";
    private static Logger log = Logger.getLogger(Voucher.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this transaction */
    private TableRow myRow;

    /** Flag set when data is modified, for events */
    private boolean modified;

    Voucher(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
        // Cache ourselves
        context.cache(this, row.getIntColumn("voucher_id"));
        modified = false;
    }

    /**
     * Get the voucher internal identifier
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("voucher_id");
    }

    /**
     * Get the code
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getCode()
    {
        return myRow.getStringColumn("code");
    }

    /**
     * Get the batch_id
     *
     * @return integer code (or null if the column is an SQL NULL)
     */
    public String getBatchId()
    {
        return myRow.getStringColumn("batch_id");
    }


    /**
     * Get the status
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getStatus()
    {
        return myRow.getStringColumn("status");
    }


    /**
     * Get the creation
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public Date getCreation()
    {
        return myRow.getDateColumn("creation");
    }




    /**
     * Set the code
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCode(String code)
    {

        if(code==null)
        {
            myRow.setColumnNull("code");
        }
        else{
            myRow.setColumn("code",code);
        }
        modified = true;
    }


    /**
     * Set the batch_id
     *
     * @return int batch_id (or null if the column is an SQL NULL)
     */
    public void setBatchId(String batchId)
    {

        myRow.setColumn("batch_id",batchId);
        modified = true;
    }


    /**
     * Set the code
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setStatus(String status)
    {

        if(status==null)
        {
            myRow.setColumnNull("status");
        }
        else{
            myRow.setColumn("status",status);
        }
        modified = true;
    }


    /**
     * Set the creation date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCreation(Date creation)
    {

        if(creation==null)
        {
            myRow.setColumnNull("creation");
        }
        else{
            myRow.setColumn("creation",creation);
        }
        modified = true;
    }


    public static Voucher create(Context context) throws SQLException,AuthorizeException

    {
        if(!AuthorizeManager.isCuratorOrAdmin(context)){
            throw new AuthorizeException("you have to be a admin or senior curator to add a new voucher");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "voucher");

        Voucher e = new Voucher(context, row);

        log.info(LogManager.getHeader(context, "create_voucher", "voucher_id="
                + e.getID()));


        return e;
    }

    public static ArrayList<Voucher> findAll(Context context) throws SQLException{
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM voucher ORDER BY voucher_id DESC");

        try
        {
            List<TableRow> voucherRows = rows.toList();

            ArrayList<Voucher> vouchers = new ArrayList<Voucher>();

            for (int i = 0; i < voucherRows.size(); i++)
            {
                TableRow row = (TableRow) voucherRows.get(i);

                // First check the cache
                Voucher fromCache = (Voucher) context.fromCache(Voucher.class, row
                        .getIntColumn("voucher_id"));

                if (fromCache != null)
                {
                    vouchers.add(fromCache);
                }
                else
                {
                    Voucher newProperty = new Voucher(context, row);
                    vouchers.add(newProperty);
                }
            }

            return vouchers;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Update the payment
     */
    public void update() throws SQLException
    {

        DatabaseManager.update(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "update_voucher",
                "voucher_id=" + getID()));

        if (modified)
        {
            modified = false;
        }
    }

    public static Voucher findById(Context context,Integer id) throws SQLException{

        TableRowIterator rows = DatabaseManager.queryTable(context, "voucher", "SELECT * FROM voucher WHERE voucher_id = "+ id+ "limit 1");

        try
        {
            List<TableRow> voucherRows = rows.toList();

            ArrayList<Voucher> vouchers = new ArrayList<Voucher>();

            for (int i = 0; i < voucherRows.size(); i++)
            {
                TableRow row = (TableRow) voucherRows.get(i);

                // First check the cache
                Voucher fromCache = (Voucher) context.fromCache(Voucher.class, row
                        .getIntColumn("voucher_id"));

                if (fromCache != null)
                {
                    vouchers.add(fromCache);
                }
                else
                {
                    Voucher newProperty = new Voucher(context, row);
                    vouchers.add(newProperty);
                }
            }
            if(vouchers.size()>0){
                return vouchers.get(0);
            }
            else
            {
                return null;
            }

        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }
    public static Voucher findByCode(Context context,String code) throws SQLException{

        TableRowIterator rows = DatabaseManager.queryTable(context, "voucher", "SELECT * FROM voucher WHERE code = '"+ code+ "' limit 1");

        try
        {
            List<TableRow> voucherRows = rows.toList();

            ArrayList<Voucher> vouchers = new ArrayList<Voucher>();

            for (int i = 0; i < voucherRows.size(); i++)
            {
                TableRow row = (TableRow) voucherRows.get(i);

                // First check the cache
                Voucher fromCache = (Voucher) context.fromCache(Voucher.class, row
                        .getIntColumn("voucher_id"));

                if (fromCache != null)
                {
                    vouchers.add(fromCache);
                }
                else
                {
                    Voucher newProperty = new Voucher(context, row);
                    vouchers.add(newProperty);
                }
            }
            if(vouchers.size()>0){
                return vouchers.get(0);
            }
            else
            {
                return null;
            }

        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public static Voucher[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }

    public static Voucher[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM voucher WHERE voucher_id = ? OR ");
        queryBuf.append("LOWER(status) LIKE LOWER(?) OR LOWER(code) LIKE LOWER(?) OR LOWER(batch_id) LIKE LOWER(?) OR LOWER(customer) LIKE LOWER(?) OR LOWER(customer_name) LIKE LOWER(?) OR LOWER(code) LIKE LOWER(?) OR  LOWER(explanation) LIKE LOWER(?) OR to_char(creation, 'YYYY-MM-DD') LIKE ? ORDER BY voucher_id DESC ");

        // Add offset and limit restrictions - Oracle requires special code
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // First prepare the query to generate row numbers
            if (limit > 0 || offset > 0)
            {
                queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") ");
            }

            // Restrict the number of rows returned based on the limit
            if (limit > 0)
            {
                queryBuf.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0)
                {
                    limit += offset;
                }
            }

            // Return only the records after the specified offset (row number)
            if (offset > 0)
            {
                queryBuf.insert(0, "SELECT * FROM (");
                queryBuf.append(") WHERE rnum>?");
            }
        }
        else
        {
            if (limit > 0)
            {
                queryBuf.append(" LIMIT ? ");
            }

            if (offset > 0)
            {
                queryBuf.append(" OFFSET ? ");
            }
        }

        String dbquery = queryBuf.toString();

        // When checking against the voucher-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {int_param,params,params,params,params,params,params,params,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params,params, params,params,params,params, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param, params, params, params,params, params,params,params,params, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param, params, params, params,params, params,params,params,params, offset};
        }

        // Get all the voucher that match the query
        TableRowIterator rows = DatabaseManager.query(context,
                dbquery, paramArr);
        try
        {
            List<TableRow> voucherRows = rows.toList();
            Voucher[] voucher = new Voucher[voucherRows.size()];

            for (int i = 0; i < voucherRows.size(); i++)
            {
                TableRow row = (TableRow) voucherRows.get(i);

                // First check the cache
                Voucher fromCache = (Voucher) context.fromCache(Voucher.class, row
                        .getIntColumn("voucher_id"));

                if (fromCache != null)
                {
                    voucher[i] = fromCache;
                }
                else
                {
                    voucher[i] = new Voucher(context, row);
                }
            }

            return voucher;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public void delete()throws SQLException,AuthorizeException{
        if(!AuthorizeManager.isCuratorOrAdmin(myContext)){
            throw new AuthorizeException("you have to be a admin to delete a new voucher");
        }
        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_voucher",
                "voucher_id=" + getID()));        // authorized?


    }

    public String getExplanation(){
        return myRow.getStringColumn("explanation");
    }

    public void setExplanation(String explanation){
        if(explanation==null)
        {
            myRow.setColumnNull("explanation");
        }
        else{
            myRow.setColumn("explanation",explanation);
        }
        modified = true;
    }

    public Integer getGenerator(){
        return myRow.getIntColumn("generator");
    }

    public void setGenerator(Integer enerator){
        if(enerator==null)
        {
            myRow.setColumnNull("generator");
        }
        else{
            myRow.setColumn("generator",enerator);
        }
        modified = true;
    }

    public String getCustomer(){
        return myRow.getStringColumn("customer");
    }

    public void setCustomer(String customer){
        if(customer==null)
        {
            myRow.setColumnNull("customer");
        }
        else{
            myRow.setColumn("customer",customer);
        }
        modified = true;
    }


    /**
     * Get the voucher customer name
     *
     * @return the customer name
     */
    public String getCustomerName()
    {
        return myRow.getStringColumn("customer_name");
    }

    public void setCustomerName(String name)
    {
        myRow.setColumn("customer_name",name);
        modified=true;
    }

}

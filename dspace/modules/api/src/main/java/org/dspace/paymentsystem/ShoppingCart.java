/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadOrganizationConcept;
import org.datadryad.rest.models.ResultSet;
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
 * This is the core Shopping Cart Domain Class
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCart {
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_OPEN = "open";
    public static final String STATUS_DENIED = "denied";
    public static final String STATUS_VERIFIED = "verified";

    public static final String COUNTRY_US = "US";
    public static final String CURRENCY_US = "USD";

    public static final String FREE = "true";

    public static final String COUNTRYFREE = "free";
    public static final String COUNTRYNOTFREE = "not_free";
    public static final int NO_WAIVER =0;

    public static final int COUNTRY_WAIVER =1;
    public static final int JOUR_WAIVER =2;
    public static final int VOUCHER_WAIVER =3;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ShoppingCart.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this transaction */
    private TableRow myRow;

    /** Flag set when data is modified, for events */
    private boolean modified;

    private DryadOrganizationConcept sponsorConcept;


    ShoppingCart(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
        setSponsorID(row.getIntColumn("sponsor_id"));
        sponsorConcept = getSponsoringOrganization(myContext);


        // Cache ourselves
        context.cache(this, row.getIntColumn("cart_id"));
        modified = false;
    }

    public String getHandle(){
        // No Handles for
        return null;
    }

    public String getName()
    {
        return null;
    }

    /**
     * Get the shoppingcart internal identifier
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("cart_id");
    }


    /**
     * Get the depositor
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public int getDepositor()
    {
        return myRow.getIntColumn("depositor");
    }

    /**
     * Get the item
     *
     * @return int code (or null if the column is an SQL NULL)
     */
    public int getItem()
    {
        return myRow.getIntColumn("item");
    }


    /**
     * Get the expiration date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getExpiration()
    {
        return myRow.getStringColumn("expiration");
    }

    /**
     * Get the CURRENCY
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getCurrency()
    {
        return myRow.getStringColumn("currency");
    }

    /**
     * Get the Country
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getCountry()
    {
        return myRow.getStringColumn("country");
    }

    /**
     * Get the Status
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getStatus()
    {
        return myRow.getStringColumn("status");
    }

    /**
     * Get the VOUCHER
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public Integer getVoucher()
    {
        return myRow.getIntColumn("voucher");
    }

    /**
     * Get the payflow_id
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getTransactionId()
    {
        return myRow.getStringColumn("transaction_id");
    }

    /**
     * Set the payflow_id
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setTransactionId(String id)
    {

        if(id==null)
        {
           myRow.setColumnNull("transaction_id");
        }
        else{
        myRow.setColumn("transaction_id",id);
        }
        modified = true;
    }
    /**
     * Set the depositor
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setDepositor(Integer eperson)
    {
        if(eperson==null)
        {
            myRow.setColumnNull("depositor");
        }
        else{
        myRow.setColumn("depositor",eperson);
        }
        modified = true;
    }

    /**
     * Set the item
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setItem(Integer itemId)
    {
        if(itemId==null)
        {
            myRow.setColumnNull("item");
        }
        else{
        myRow.setColumn("item", itemId);
        }
        modified = true;
    }


    /**
     * Set the expiration date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setExpiration(String expiration)
    {
        if(expiration==null)
        {
            myRow.setColumnNull("expiration");
        }
        else{
        myRow.setColumn("expiration",expiration);
        }
        modified = true;

    }

    /**
     * Set the CURRENCY
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCurrency(String currency)
    {
        if(currency==null)
        {
            myRow.setColumnNull("currency");
        }
        else{
        myRow.setColumn("currency",currency);
        }
        modified = true;
    }

    /**
     * Set the Country
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setCountry(String country)
    {
        if(country==null)
        {
            myRow.setColumnNull("country");
        }
        else{
        myRow.setColumn("country",country);
        }
        modified = true;
    }

    /**
     * Set the Status
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
     * Set the VOUCHER
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setVoucher(Integer voucher)
    {
        if(voucher==null)
        {
            myRow.setColumnNull("voucher");
        }
        else{
        myRow.setColumn("voucher",voucher);
        }
        modified = true;
    }


    /**
     * Update the payment
     */
    public void update() throws SQLException
    {

        DatabaseManager.update(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "update_shoppingcart",
                "shoppingcart_id=" + getID()));

        if (modified)
        {
            modified = false;
        }
    }

    public void setModified(Boolean i){

        this.modified=i;

    }

    /**
     * Create a new shoppingcart
     *
     * @param context
     *            DSpace context object
     */
    public static ShoppingCart create(Context context) throws SQLException
    {

        // Create a table row
        TableRow row = DatabaseManager.create(context, "shoppingcart");

        ShoppingCart e = new ShoppingCart(context, row);

        log.info(LogManager.getHeader(context, "create_shoppingcart", "cart_id="
                + e.getID()));


        return e;
    }

    /**
     * Delete an shoppingcart
     *
     */
    public void delete() throws SQLException,
            PaymentSystemException
    {

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_shoppingcart",
                "cart_id=" + getID()));
    }

    private static ArrayList<ShoppingCart> getCartsForTableRows(Context context, TableRowIterator rows) throws SQLException {
        try
        {
            List<TableRow> propertyRows = rows.toList();

            ArrayList<ShoppingCart> shoppingCarts = new ArrayList<ShoppingCart>();

            for (TableRow row : propertyRows) {
                ShoppingCart cart = getCartForTableRow(context, row);
                if (cart != null) {
                    shoppingCarts.add(cart);
                }
            }

            return shoppingCarts;
        } finally {
            if (rows != null) {
                rows.close();
            }
        }
    }

    private static ShoppingCart getCartForTableRow(Context context, TableRow row) {
        if (row == null) {
            return null;
        } else {
            // First check the cache
            ShoppingCart fromCache = (ShoppingCart) context.fromCache(ShoppingCart.class, row.getIntColumn("cart_id"));

            if (fromCache != null) {
                return fromCache;
            } else {
                return new ShoppingCart(context, row);
            }
        }
    }

    public static ArrayList<ShoppingCart> findAllByEpeople(Context context, int epeopleId)
            throws SQLException
    {
        TableRowIterator rows = DatabaseManager.query(context,
                "SELECT * FROM shoppingcart WHERE depositor = "+ epeopleId+ " ORDER BY cart_id DESC");
        return getCartsForTableRows(context, rows);
    }

    public static ArrayList<ShoppingCart> findAllByItem(Context context, int itemId)
            throws SQLException
    {

        TableRowIterator rows = DatabaseManager.queryTable(context, "shoppingcart", "SELECT * FROM shoppingcart WHERE item = "+ itemId+ " ORDER BY cart_id DESC");

        return getCartsForTableRows(context, rows);
    }

    public static ShoppingCart find(Context context, int cartId)
            throws SQLException {
        TableRowIterator rows = DatabaseManager.queryTable(context, "shoppingcart", "SELECT * FROM shoppingcart WHERE cart_id = "+ cartId+ "limit 1");
        return getCartForTableRow(context, rows.next());
    }

    /**
     * Find the shoppingCarts by its id.
     *
     * @return Transaction, or {@code null} if none such exists.
     */
    public static ShoppingCart findByTransactionId(Context context, Integer id)
            throws SQLException
    {
        if (id == null)
        {
            return null;
        }

        // All name addresses are stored as lowercase, so ensure that the name address is lowercased for the lookup
        TableRow row = DatabaseManager.findByUnique(context, "shoppingcart",
                "cart_id", id);
        return getCartForTableRow(context, row);
    }

    public boolean getModified(){
        return this.modified;
    }

    public double getTotal(){
        return myRow.getDoubleColumn("total");
    }
    public void setTotal(double total){
        myRow.setColumn("total",total);
        modified = true;
    }



    public String getSecureToken()
    {
        return myRow.getStringColumn("securetoken");
    }


    /**
     * return type found in Constants
     */
    public void setSecureToken(String secureToken)
    {
        if(secureToken==null)
        {
            myRow.setColumnNull("securetoken");
        }
        else{
            myRow.setColumn("securetoken",secureToken);
        }
        modified = true;
    }


    /**
     * return type found in Constants
     */
    public static ShoppingCart findBySecureToken(Context context,String secureToken) throws SQLException
    {
        if(secureToken==null)
        {
            return null;
        }
        else
        {
            // All name addresses are stored as lowercase, so ensure that the name address is lowercased for the lookup
            TableRow row = DatabaseManager.findByUnique(context, "shoppingcart",
                    "securetoken", secureToken);
            return getCartForTableRow(context, row);
        }
    }

    public static ShoppingCart findByVoucher(Context context,Integer voucherId) throws SQLException {
        if (voucherId==null) {
            return null;
        } else {
            // All name addresses are stored as lowercase, so ensure that the name address is lowercased for the lookup
            TableRow row = DatabaseManager.findByUnique(context, "shoppingcart", "voucher", voucherId);
            return getCartForTableRow(context, row);
        }
    }

    public static ShoppingCart[] findAll(Context context) throws SQLException {

        TableRowIterator rows = DatabaseManager.query(context,"SELECT * FROM shoppingcart order by cart_id DESC");

        ArrayList<ShoppingCart> carts = getCartsForTableRows(context, rows);
        return carts.toArray(new ShoppingCart[carts.size()]);
    }

    public static ResultSet findAllCarts(Context context) throws SQLException {
        TableRowIterator rows = DatabaseManager.query(context,"SELECT cart_id FROM shoppingcart order by cart_id ASC");
        ArrayList<Integer> cartList = new ArrayList<Integer>();
        for (TableRow row : rows.toList()) {
            cartList.add(row.getIntColumn("cart_id"));
        }
        return new ResultSet(cartList, 20, 0);
    }

    public static ResultSet search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }

    public static ResultSet search(Context context, String query, int cursor, int limit)
            throws SQLException {
        String params = "%"+query.toLowerCase()+"%";
        StringBuilder queryBuf = new StringBuilder();
        queryBuf.append("SELECT cart_id FROM shoppingcart WHERE ");
        queryBuf.append("item = ? OR LOWER(status) LIKE LOWER(?) OR LOWER(transaction_id) LIKE LOWER(?) ORDER BY item DESC ");
        TableRowIterator rows = DatabaseManager.query(context, queryBuf.toString(), params, params, params);
        ArrayList<Integer> cartList = new ArrayList<Integer>();
        for (TableRow row : rows.toList()) {
            cartList.add(row.getIntColumn("cart_id"));
        }
        return new ResultSet(cartList, limit, cursor);
    }

    public static List<ShoppingCart> getCartsForIDs(Context context, List<Integer> cartIDs) throws SQLException {
        ArrayList<ShoppingCart> carts = new ArrayList<ShoppingCart>();

        for (Integer i : cartIDs) {
            carts.add(find(context, i));
        }

        return carts;
    }

//    public static final int BASIC_FEE =12;
//    public static final int SURCHARGE =14;
    public void setBasicFee(double basicFee){
        myRow.setColumn("basic_fee",basicFee);
        modified = true;
    }
    public double getBasicFee(){
       return myRow.getDoubleColumn("basic_fee");
    }

    public void setSurcharge(double surcharge){
        myRow.setColumn("surcharge",surcharge);
        modified = true;
    }
    public double getSurcharge(){
        return myRow.getDoubleColumn("surcharge");
    }

    public void setSponsoringOrganization(DryadOrganizationConcept organizationConcept) {
        if (organizationConcept != null) {
            setJournal(organizationConcept.getFullName());
            setJournalSub(organizationConcept.getSubscriptionPaid());
            setSponsorID(organizationConcept.getConceptID());
            sponsorConcept = organizationConcept;
        } else {
            setJournal(null);
            setJournalSub(false);
            setSponsorID(-1);
        }
    }

    public DryadOrganizationConcept getSponsoringOrganization(Context context) {
        if (sponsorConcept == null) {
            int concept_id = getSponsorID();
            if (concept_id > 0) {
                sponsorConcept = DryadOrganizationConcept.getOrganizationConceptMatchingConceptID(context, concept_id);
            }
        }
        return sponsorConcept;
    }

    private void setSponsorID(int concept_id) {
        myRow.setColumn("sponsor_id", concept_id);
        modified = true;
    }

    private int getSponsorID() {
        return myRow.getIntColumn("sponsor_id");
    }

    /**
     * Set the JOURNAL
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    private void setJournal(String journal)
    {
        if(journal==null)
        {
            myRow.setColumnNull("journal");
        }
        else{
            myRow.setColumn("journal",journal);
        }
        modified = true;
    }

    /**
     * Get the JOURNAL
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public String getJournal()
    {
        return myRow.getStringColumn("journal");
    }

    /**
     * Set the JOURNAL
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    private void setJournalSub(boolean journal_sub)
    {
        myRow.setColumn("journal_sub",journal_sub);
        modified = true;
    }

    /**
     * Get the JOURNAL
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */

    private Boolean getJournalSub() {
        return myRow.getBooleanColumn("journal_sub");
    }

    public Boolean hasSubscription()
    {
        return getJournalSub();
    }



    /**
     * Set the order date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setOrderDate(java.util.Date date)
    {
        if(date==null)
        {
            myRow.setColumnNull("order_date");
        }
        else{
            myRow.setColumn("order_date",date);
        }
        modified = true;

    }

    /**
     * Set the payment date
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setPaymentDate(java.util.Date date)
    {
        if(date==null)
        {
            myRow.setColumnNull("payment_date");
        }
        else{
            myRow.setColumn("payment_date",date);
        }
        modified = true;

    }

    /**
     * Set the Note
     *
     * @return text_lang code (or null if the column is an SQL NULL)
     */
    public void setNote(String note)
    {
        if(note==null)
        {
            myRow.setColumnNull("notes");
        }
        else{
            myRow.setColumn("notes",note);
        }
        modified = true;

    }

    public String getNote()
    {
        return myRow.getStringColumn("notes");

    }

    public Date getOrderDate()
    {
        return myRow.getDateColumn("order_date");

    }
    public Date getPaymentDate()
    {
        return myRow.getDateColumn("payment_date");

    }
}

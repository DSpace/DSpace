/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p/>
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadFunderConcept;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.JournalUtils;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.DryadWorkflowUtils;
import org.datadryad.api.DryadJournalConcept;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

import org.dspace.workflow.WorkflowItem;
import org.dspace.core.I18nUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;

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
    protected static final Message T_Header =

            message("xmlui.PaymentSystem.shoppingcart.order.header");


    protected static final Message T_Payer =

            message("xmlui.PaymentSystem.shoppingcart.order.payer");

    protected static final Message T_Price =

            message("xmlui.PaymentSystem.shoppingcart.order.price");

    protected static final Message T_Surcharge =

            message("xmlui.PaymentSystem.shoppingcart.order.surcharge");

    protected static final Message T_Total =

            message("xmlui.PaymentSystem.shoppingcart.order.total");

    protected static final Message T_Country =

            message("xmlui.PaymentSystem.shoppingcart.order.country");

    protected static final Message T_Voucher =

            message("xmlui.PaymentSystem.shoppingcart.order.voucher");

    protected static final Message T_Apply =

            message("xmlui.PaymentSystem.shoppingcart.order.apply");


    protected static final String Country_Help_Text = "Submitters requesting a waiver must be employees of an institution in an eligible country or be independent researchers in residence in an eligible country. Eligible countries are those classified by the World Bank as low-income or lower-middle-income economies.";

    protected static final String Voucher_Help_Text = "Organizations may sponsor submissions to Dryad by purchasing and distributing voucher codes redeemable for one Data Publishing Charge.  Submitters redeeming vouchers may only use them with the permission of the purchaser.";

    protected static final String Currency_Help_Text = "Select Currency";


    /** Protected Constructor */
    protected PaymentSystemImpl() {
    }


    public ShoppingCart createNewShoppingCart(Context context, Integer itemId, Integer epersonId, String country, String currency, String status) throws SQLException,
            PaymentSystemException {
        ShoppingCart newShoppingcart = ShoppingCart.create(context);
        newShoppingcart.setCountry(country);
        newShoppingcart.setCurrency(currency);
        newShoppingcart.setDepositor(epersonId);
        newShoppingcart.setExpiration(null);
        if (itemId != null) {
            //make sure we only create the shoppingcart for data package
            Item item = Item.find(context, itemId);
            org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
            if (dataPackage != null) {
                itemId = dataPackage.getID();
            }
            newShoppingcart.setItem(itemId);
        }
        newShoppingcart.setStatus(status);
        newShoppingcart.setVoucher(null);
        newShoppingcart.setTransactionId(null);
        newShoppingcart.setSponsoringOrganization(null);
        newShoppingcart.setBasicFee(PaymentSystemConfigurationManager.getCurrencyProperty(currency));
        newShoppingcart.setSurcharge(PaymentSystemConfigurationManager.getSizeFileFeeProperty(currency));
        Double totalPrice = calculateShoppingCartTotal(context, newShoppingcart);
        newShoppingcart.setTotal(totalPrice);
        newShoppingcart.update();
        return newShoppingcart;
    }


    public void modifyShoppingCart(Context context, ShoppingCart shoppingcart, DSpaceObject dso) throws AuthorizeException, SQLException, PaymentSystemException {

        if (shoppingcart.getModified()) {
            shoppingcart.update();
            shoppingcart.setModified(false);
        }

    }

    public void setCurrency(ShoppingCart shoppingCart, String currency) throws SQLException {
        shoppingCart.setCurrency(currency);
        shoppingCart.setBasicFee(PaymentSystemConfigurationManager.getCurrencyProperty(currency));
        shoppingCart.setSurcharge(PaymentSystemConfigurationManager.getSizeFileFeeProperty(currency));
        shoppingCart.update();
        shoppingCart.setModified(false);

    }

    public void deleteShoppingCart(Context context, Integer shoppingcartId) throws AuthorizeException, SQLException, PaymentSystemException {
        ShoppingCart trasaction = ShoppingCart.findByTransactionId(context, shoppingcartId);
        trasaction.delete();
    }

    public ShoppingCart getShoppingCart(Context context, Integer shoppingcartId) throws SQLException {
        return ShoppingCart.findByTransactionId(context, shoppingcartId);
    }

    public ShoppingCart[] findAllShoppingCart(Context context, Integer itemId) throws SQLException, PaymentSystemException {
        if (itemId == null || itemId == -1) {
            return ShoppingCart.findAll(context);
        } else {

            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            VersionHistory history = versioningService.findVersionHistory(context, itemId);
            if (history != null) {
                Item originalItem = history.getFirstVersion().getItem();
                itemId = originalItem.getID();
            }
            ShoppingCart[] shoppingCarts = new ShoppingCart[1];
            shoppingCarts[0] = getShoppingCartByItemId(context, itemId);
            return shoppingCarts;
        }
    }

    public ShoppingCart getShoppingCartByItemId(Context context, Integer itemId) throws SQLException, PaymentSystemException {
        //make sure we get correct shoppingcart for data package
        Item item = Item.find(context, itemId);
        org.dspace.content.Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        if (dataPackage != null) {
            itemId = dataPackage.getID();
        }

        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(context, itemId);
        if (history != null) {
            Item originalItem = history.getFirstVersion().getItem();
            itemId = originalItem.getID();
            ArrayList<ShoppingCart> shoppingCarts = ShoppingCart.findAllByItem(context, itemId);
            if (shoppingCarts.size() > 0) {
                return shoppingCarts.get(0);
            } else {
                // if the original item doesn't have a shopping cart,
                // this item must've been created before the payment system was in place.
                // We should create a completed placeholder cart for the original item, but make sure it is marked
                // that it was created for versioning and should not be re-charged.
                log.debug("creating a completed shopping cart for new version of item " + itemId);
                ShoppingCart versionCart = createNewShoppingCart(context, itemId, context.getCurrentUser().getID(), "", ShoppingCart.CURRENCY_US, ShoppingCart.STATUS_COMPLETED);
                versionCart.setNote("cart created for versioning; do not charge");
                versionCart.update();
                return versionCart;
            }

        }

        List<ShoppingCart> shoppingcartList = ShoppingCart.findAllByItem(context, itemId);
        if (shoppingcartList != null && shoppingcartList.size() > 0)
            return shoppingcartList.get(0);
        else {
            //if no shopping cart , create a new one
            return createNewShoppingCart(context, itemId, context.getCurrentUser().getID(), "", ShoppingCart.CURRENCY_US, ShoppingCart.STATUS_OPEN);
        }

    }

    /**
     * Calculate the shopping cart total
     * @param discount if true, basicFee is ignored
     * @param fileSizeSurcharge surcharge to add for large files
     * @param basicFee basic submission fee
     * @return the shopping cart total, based on the input parameters
     */
    static double calculateTotal(boolean discount,
                                 double fileSizeSurcharge,
                                 double basicFee) {
        double price;
        if (discount) {
            price = fileSizeSurcharge;
        } else {
            // no sponsor, voucher, or country discount
            price = basicFee + fileSizeSurcharge;
        }
        return price;
    }

    public Double calculateShoppingCartTotal(Context context, ShoppingCart shoppingcart) throws SQLException {
        log.debug("recalculating shopping cart total");
        boolean discount = dpcIsCovered(context, shoppingcart);
        double fileSizeSurcharge = getSurchargeLargeFileFee(context, shoppingcart);
        double basicFee = shoppingcart.getBasicFee();
        double price = calculateTotal(discount, fileSizeSurcharge, basicFee);
        return price;
    }

    /**
     * Calculate the surcharge for a data package, based on its size in bytes
     * @param allowedSize the maximum size allowed before large file surcharge
     * @param totalDataFileSize the total size of data files in bytes
     * @param fileSizeFeeAfter the fee per unit to assess after exceeding allowedSize
     * @param initialSurcharge Initial surcharge, e.g. 15 for the first 1GB exceeding 10GB.
     * @param surchargeUnitSize amount of data to assess a fileSizeFeeAfter on, e.g. 1GB = 1*1024*1024
     * @return The total surcharge to assess.
     */
    static double calculateFileSizeSurcharge(
            long allowedSize,
            long totalDataFileSize,
            double fileSizeFeeAfter,
            double initialSurcharge,
            long surchargeUnitSize) {
        double totalSurcharge = 0.0;
        if (totalDataFileSize > allowedSize) {
            totalSurcharge = initialSurcharge;
            int unit = 0;
            //eg. $10 after every 1 gb
            if (surchargeUnitSize > 0) {
                Long overSize = (totalDataFileSize - allowedSize) / surchargeUnitSize;
                unit = overSize.intValue();
            }
            totalSurcharge = totalSurcharge + fileSizeFeeAfter * unit;
        }
        return totalSurcharge;
    }

    /**
     * Get the total size in bytes of all bitstreams within a data package.
     * Assumes item is a data package and DryadWorkFlowUtils.getDataFiles returns
     * data file items with bundles, bitstreams.
     * @param context
     * @param dataPackage
     * @return
     * @throws SQLException
     */
    public long getTotalDataFileSize(Context context, Item dataPackage) throws SQLException {
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, dataPackage);
        long size = 0;
        for (Item dataFile : dataFiles) {
            Bundle bundles[] = dataFile.getBundles();
            for (Bundle bundle : bundles) {
                Bitstream bitstreams[] = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    size += bitstream.getSize();
                }
            }
        }
        return size;
    }

    public double getSurchargeLargeFileFee(Context context, ShoppingCart shoppingcart) throws SQLException {
        // Extract values from database objects and configuration to pass to calculator
        String currency = shoppingcart.getCurrency();

        long allowedSize = PaymentSystemConfigurationManager.getMaxFileSize().longValue();
        double fileSizeFeeAfter = PaymentSystemConfigurationManager.getAllSizeFileFeeAfterProperty(currency);
        Long unitSize = PaymentSystemConfigurationManager.getUnitSize();  //1 GB

        Item item = Item.find(context, shoppingcart.getItem());
        long totalSizeDataFile = getTotalDataFileSize(context, item);
        double totalSurcharge = calculateFileSizeSurcharge(allowedSize, totalSizeDataFile, fileSizeFeeAfter, shoppingcart.getSurcharge(), unitSize);
        return totalSurcharge;
    }

    public boolean isSponsored(Context context, ShoppingCart shoppingcart) throws SQLException {
        if (shoppingcart.getSponsoringOrganization(context) == null) {
            // if this is an older cart that hasn't set a SponsoringOrganization yet, set one based on its journal.
            if (!shoppingcart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
                String journal = "";
                String funder = "";
                Item item = Item.find(context, shoppingcart.getItem());
                if (item != null) {
                    try {
                        // Look for the journal
                        DCValue[] values = item.getMetadata("prism.publicationName");
                        if (values != null && values.length > 0) {
                            journal = values[0].value;
                        }
                        // Look for any valid funding entities
                        // (for now, there should only be one; there could be more later
                        DCValue[] fundingEntities = item.getMetadata("dryad.fundingEntity");
                        if (fundingEntities != null && fundingEntities.length > 0) {
                            if (fundingEntities[0].confidence == Choices.CF_ACCEPTED) {
                                funder = fundingEntities[0].authority;
                            }
                        }

                    } catch (Exception e) {
                        log.error("Exception getting journal from item " + item.getID() + ":", e);
                    }
                }
                if (journal != null && journal.length() > 0) {
                    DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalName(journal);
                    if (journalConcept != null) {
                        shoppingcart.setSponsoringOrganization(journalConcept);
                    }
                }

                // funder of last resort:
                log.info("checking to see if " + funder + " is a sponsor");
                if (!shoppingcart.hasSubscription()) {
                    if (!"".equals(funder)) {
                        DryadFunderConcept funderConcept = DryadFunderConcept.getFunderConceptMatchingFunderID(context, funder);
                        if (funderConcept != null && funderConcept.getSubscriptionPaid()) {
                            log.info("funder is a sponsor");
                            shoppingcart.setSponsoringOrganization(funderConcept);
                        }
                    }
                }
            }
        }
        shoppingcart.update();
        return shoppingcart.hasSubscription();
    }


    private boolean voucherValidate(Context context, ShoppingCart shoppingcart) {
        VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
        return voucherValidationService.validate(context, shoppingcart.getVoucher(), shoppingcart);
    }

    public boolean dpcIsCovered(Context context, ShoppingCart shoppingcart) throws SQLException {
        int waiver = getWaiver(context, shoppingcart);
        if (waiver == ShoppingCart.NO_WAIVER) {
            return false;
        }
        return true;
    }

    public int getWaiver(Context context, ShoppingCart shoppingcart) throws SQLException {
        // check for payment by sponsor, waiver, voucher
        Boolean isSponsored = isSponsored(context, shoppingcart);
        Boolean countryDiscount = getCountryWaiver(context, shoppingcart);
        Boolean voucherDiscount = voucherValidate(context, shoppingcart);

        if (countryDiscount) {
            return ShoppingCart.COUNTRY_WAIVER;
        } else if (isSponsored) {
            return ShoppingCart.JOUR_WAIVER;
        } else if (voucherDiscount) {
            return ShoppingCart.VOUCHER_WAIVER;
        }
        return ShoppingCart.NO_WAIVER;
    }

    public String getWaiverMessage(Context context, ShoppingCart shoppingCart) {
        String result = "";
        try {
            switch (getWaiver(context, shoppingCart)) {
                case ShoppingCart.COUNTRY_WAIVER:
                    result = "Data Publishing Charge has been waived due to submitter's association with " + shoppingCart.getCountry() + ".";
                    break;
                case ShoppingCart.JOUR_WAIVER:
                    result = shoppingCart.getJournal() + " will cover the Data Publishing Charge for this associated submission.";
                    break;
                case ShoppingCart.VOUCHER_WAIVER:
                    result = "Voucher code applied to Data Publishing Charge.";
                    break;
            }
        } catch (SQLException e) {
            log.error("Exception getting waiver for cart " + shoppingCart.getID());
        }
        return result;
    }

    public boolean getCountryWaiver(Context context, ShoppingCart shoppingCart) throws SQLException {
        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
        Properties countryArray = manager.getAllCountryProperty();

        if (shoppingCart.getCountry() != null && shoppingCart.getCountry().length() > 0) {
            return countryArray.get(shoppingCart.getCountry()).equals(ShoppingCart.COUNTRYFREE);
        } else {
            return false;
        }
    }


    public void updateTotal(Context context, ShoppingCart shoppingCart) throws SQLException {
        if (!shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
            Double newPrice = calculateShoppingCartTotal(context, shoppingCart);
            //TODO:only setup the price when the old total price is higher than the price right now
            shoppingCart.setTotal(newPrice);
            shoppingCart.update();
            shoppingCart.setModified(false);
        }
    }

    public String getPayer(Context context, ShoppingCart shoppingcart) throws SQLException {
        String payerName = "";
        EPerson e = EPerson.find(context, shoppingcart.getDepositor());
        switch (getWaiver(context, shoppingcart)) {
            case ShoppingCart.COUNTRY_WAIVER:
                payerName = "Country";
                break;
            case ShoppingCart.JOUR_WAIVER:
                payerName = "Sponsor";
                break;
            case ShoppingCart.VOUCHER_WAIVER:
                payerName = "Voucher";
                break;
            case ShoppingCart.NO_WAIVER:
                payerName = e.getFullName();
                break;
        }
        return payerName;
    }

    private String format(String label, String value) {
        return label + ": " + value + "\n";
    }

    public String printShoppingCart(Context c, ShoppingCart shoppingCart) {

        String result = "";

        try {

            result += format("Payer", getPayer(c, shoppingCart));

            String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());

            if (dpcIsCovered(c, shoppingCart)) {
                result += format("Price", symbol + "0.0");
            } else {
                result += format("Price", symbol + Double.toString(shoppingCart.getBasicFee()));
            }

            //add the large file surcharge section
            format("Excess data storage", symbol + Double.toString(getSurchargeLargeFileFee(c, shoppingCart)));

            try {
                Voucher v = Voucher.findById(c, shoppingCart.getVoucher());
                if (v != null) {
                    result += format("Voucher applied", v.getCode());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            //add the final total price
            result += format("Total", symbol + Double.toString(shoppingCart.getTotal()));

            // add waiver information
            result += format("Waiver Details", getWaiverMessage(c, shoppingCart));

            if (shoppingCart.getTransactionId() != null && "".equals(shoppingCart.getTransactionId().trim())) {
                format("Transaction ID", shoppingCart.getTransactionId());
            }

        } catch (Exception e) {
            result += format("Error", e.getMessage());
            log.error(e.getMessage(), e);
        }

        return result;
    }


    public void generateShoppingCart(Context context, org.dspace.app.xmlui.wing.element.List info, ShoppingCart shoppingCart, PaymentSystemConfigurationManager manager, String baseUrl, Map<String, String> messages) throws WingException, SQLException {

        Item item = Item.find(context, shoppingCart.getItem());
        Long totalSize = new Long(0);
        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());

        org.dspace.app.xmlui.wing.element.List hiddenList = info.addList("transaction");
        hiddenList.addItem().addHidden("transactionId").setValue(Integer.toString(shoppingCart.getID()));
        hiddenList.addItem().addHidden("baseUrl").setValue(baseUrl);
        try {

            //add selected currency section

            generateCurrencyList(info, manager, shoppingCart);
            generatePayer(context, info, shoppingCart, item);


            generatePrice(context, info, manager, shoppingCart);
            generateCountryList(info, manager, shoppingCart);
            generateVoucherForm(context, info, manager, shoppingCart, messages);
        } catch (Exception e) {

            info.addLabel("Errors when generate the shopping cart form:" + e.getMessage());
        }


    }

    public void generateNoEditableShoppingCart(Context context, org.dspace.app.xmlui.wing.element.List info, ShoppingCart transaction, PaymentSystemConfigurationManager manager, String baseUrl, Map<String, String> messages) throws WingException, SQLException

    {

        Item item = Item.find(context, transaction.getItem());

        Long totalSize = new Long(0);

        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(transaction.getCurrency());
        org.dspace.app.xmlui.wing.element.List hiddenList = info.addList("transaction");
        hiddenList.addItem().addHidden("transactionId").setValue(Integer.toString(transaction.getID()));
        hiddenList.addItem().addHidden("baseUrl").setValue(baseUrl);

        try {
            //add selected currency section

            info.addLabel(T_Header);

            info.addItem().addContent(transaction.getCurrency());

            generatePayer(context, info, transaction, item);

            generatePrice(context, info, manager, transaction);

            info.addItem().addContent(transaction.getCountry());

            generateNoEditableVoucherForm(context, info, transaction, messages);

        } catch (Exception e)

        {
            info.addLabel("Errors when generate the shopping cart form");
        }
    }


    private void generateNoEditableVoucherForm(Context context, org.dspace.app.xmlui.wing.element.List info, ShoppingCart shoppingCart, Map<String, String> messages) throws WingException, SQLException {

        Voucher voucher1 = Voucher.findById(context, shoppingCart.getVoucher());

        if (messages.get("voucher") != null)

        {
            info.addItem("errorMessage", "errorMessage").addContent(messages.get("voucher"));

        } else

        {
            info.addItem("errorMessage", "errorMessage").addContent("");

        }
        info.addLabel(T_Voucher);
        info.addItem().addContent(voucher1.getCode());
    }

    private void generateCountryList(org.dspace.app.xmlui.wing.element.List info, PaymentSystemConfigurationManager manager, ShoppingCart shoppingCart) throws WingException {
        //only generate country selection list when it is not on the publication select page, to do this we need to check the publication is not empty

        java.util.List<String> countryArray = manager.getSortedCountry();

        Select countryList = info.addItem("country-list", "select-list").addSelect("country");

        countryList.setLabel(T_Country);
        countryList.setHelp(Country_Help_Text);
        countryList.addOption("", "Select your country");
        for (String temp : countryArray) {
            String[] countryTemp = temp.split(":");
            if (shoppingCart.getCountry() != null && shoppingCart.getCountry().length() > 0 && shoppingCart.getCountry().equals(countryTemp[0])) {
                countryList.addOption(true, countryTemp[0], countryTemp[0]);
            } else {
                countryList.addOption(false, countryTemp[0], countryTemp[0]);
            }
        }


        if (shoppingCart.getCountry().length() > 0) {
            info.addItem("remove-country", "remove-country").addXref("#", "Remove country : " + shoppingCart.getCountry());
        } else {
            info.addItem("remove-country", "remove-country").addXref("#", "");
        }

    }

    private void generateSurchargeFeeForm(Context context, org.dspace.app.xmlui.wing.element.List info, PaymentSystemConfigurationManager manager, ShoppingCart shoppingCart) throws WingException, SQLException {
        //add the large file surcharge section
        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());
        info.addLabel(T_Surcharge);
        info.addItem("surcharge", "surcharge").addContent(String.format("%s%.0f", symbol, this.getSurchargeLargeFileFee(context, shoppingCart)));

    }

    private void generateCurrencyList(org.dspace.app.xmlui.wing.element.List info, PaymentSystemConfigurationManager manager, ShoppingCart shoppingCart) throws WingException, SQLException {
        org.dspace.app.xmlui.wing.element.Item currency = info.addItem("currency-list", "select-list");
        Select currencyList = currency.addSelect("currency");
        currencyList.setLabel(T_Header);
        //currencyList.setHelp(Currency_Help_Text);
        Properties currencyArray = manager.getAllCurrencyProperty();

        for (String currencyTemp : currencyArray.stringPropertyNames()) {
            if (shoppingCart.getCurrency().equals(currencyTemp)) {
                currencyList.addOption(true, currencyTemp, currencyTemp);
            } else {
                currencyList.addOption(false, currencyTemp, currencyTemp);
            }
        }

    }

    private void generateVoucherForm(Context context, org.dspace.app.xmlui.wing.element.List info, PaymentSystemConfigurationManager manager, ShoppingCart shoppingCart, Map<String, String> messages) throws WingException, SQLException {
        Voucher voucher1 = Voucher.findById(context, shoppingCart.getVoucher());
        if (messages.get("voucher") != null)

        {

            info.addItem("errorMessage", "errorMessage").addContent(messages.get("voucher"));

        } else

        {

            info.addItem("errorMessage", "errorMessage").addContent("");

        }

        org.dspace.app.xmlui.wing.element.Item voucher = info.addItem("voucher-list", "voucher-list");

        Text voucherText = voucher.addText("voucher", "voucher");
        voucherText.setLabel(T_Voucher);
        voucherText.setHelp(Voucher_Help_Text);
        voucher.addButton("apply", "apply");
        if (voucher1 != null) {
            voucherText.setValue(voucher1.getCode());
            info.addItem("remove-voucher", "remove-voucher").addXref("#", "Remove voucher : " + voucher1.getCode(), "remove-voucher", "remove-voucher");
        } else {
            info.addItem("remove-voucher", "remove-voucher").addXref("#", "", "remove-voucher", "remove-voucher");
        }


    }

    private void generatePrice(Context context, org.dspace.app.xmlui.wing.element.List info, PaymentSystemConfigurationManager manager, ShoppingCart shoppingCart) throws WingException, SQLException {
        String waiverMessage = getWaiverMessage(context, shoppingCart);
        String symbol = PaymentSystemConfigurationManager.getCurrencySymbol(shoppingCart.getCurrency());
        info.addLabel(T_Price);
        if (this.dpcIsCovered(context, shoppingCart)) {
            info.addItem("price", "price").addContent(symbol + "0");
        } else {
            info.addItem("price", "price").addContent(String.format("%s%.0f", symbol, shoppingCart.getBasicFee()));
        }

        generateSurchargeFeeForm(context, info, manager, shoppingCart);


        //add the final total price
        info.addLabel(T_Total);
        info.addItem("total", "total").addContent(String.format("%s%.0f", symbol, shoppingCart.getTotal()));
        info.addItem("waiver-info", "waiver-info").addContent(waiverMessage);
    }

    private void generatePayer(Context context, org.dspace.app.xmlui.wing.element.List info, ShoppingCart shoppingCart, Item item) throws WingException, SQLException {
        info.addLabel(T_Payer);
        String payerName = this.getPayer(context, shoppingCart);
        DCValue[] values = item.getMetadata("prism.publicationName");
        if (values != null && values.length > 0) {
            //on the first page don't generate the payer name, wait until user choose country or sponsor
            info.addItem("payer", "payer").addContent(payerName);
        } else {
            info.addItem("payer", "payer").addContent("");
        }


    }


    public void sendPaymentApprovedEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart) {
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_approved"));
            email.addRecipient(wfi.getSubmitter().getEmail());
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.alert.recipient"));
            email.addArgument(wfi.getItem().getName());
            email.addArgument(wfi.getSubmitter().getFullName() + " (" + wfi.getSubmitter().getEmail() + ")");

            if (shoppingCart != null) {
                /** add details of shopping cart */
                email.addArgument(printShoppingCart(c, shoppingCart));
            }

            email.send();
        } catch (Exception e) {
            log.error("Error sending payment approved submission email: WorkflowItemId: " + wfi.getID());
        }

    }

    public void sendPaymentErrorEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart, String error) {
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_error"));
            // only send result of shopping cart errors to administrators
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.help.email"));
            email.addArgument(wfi.getItem().getName());
            email.addArgument(wfi.getItem().getID());
            email.addArgument(error);

            if (shoppingCart != null) {
                /** add details of shopping cart */
                email.addArgument(printShoppingCart(c, shoppingCart));
            }

            email.send();
        } catch (Exception e) {
            log.error("Error sending payment rejected submission email: WorkflowItemId: " + wfi.getID());
        }

    }

    public void sendPaymentWaivedEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart) {
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_waived"));
            email.addRecipient(wfi.getSubmitter().getEmail());
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.alert.recipient"));

            email.addArgument(wfi.getItem().getName());

            email.addArgument(wfi.getSubmitter().getFullName() + " (" + wfi.getSubmitter().getEmail() + ")");
            if (shoppingCart != null) {
                /** add details of shopping cart */
                email.addArgument(printShoppingCart(c, shoppingCart));
            }
            email.send();
        } catch (Exception e) {
            log.error("Error sending payment approved submission email: WorkflowItemId: " + wfi.getID());
        }

    }

    public void sendPaymentRejectedEmail(Context c, WorkflowItem wfi, ShoppingCart shoppingCart) {
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_rejected"));
            // temporarily only send result of shopping cart errors to administrators
            email.addRecipient(wfi.getSubmitter().getEmail());
            email.addRecipient(ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.help.email"));
            email.addArgument(wfi.getItem().getName());

            email.addArgument(wfi.getSubmitter().getFullName() + " (" + wfi.getSubmitter().getEmail() + ")");

            if (shoppingCart != null) {
                /** add details of shopping cart */
                email.addArgument(printShoppingCart(c, shoppingCart));
            }
            email.send();
        } catch (Exception e) {
            log.error("Error sending payment rejected submission email: WorkflowItemId: " + wfi.getID());
        }

    }
}

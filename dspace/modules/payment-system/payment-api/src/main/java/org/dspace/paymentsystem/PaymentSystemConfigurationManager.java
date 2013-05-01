/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;

import java.util.Properties;

/**
 * PaymentSystem Configuration Manager simplifies parsing and setup
 * of dspace configuration properties for the payment service.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaymentSystemConfigurationManager {

    private static Properties country = null;
    private static Properties currency = null;
    private static Properties sizeFileFee = null;
    private static Properties notIntegratedJournalFee = null;
    private static Properties sizeFileFeeAfter = null;

    private static String maxFileSize = ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.maxFileSize");

    private static String currencyConfig = ConfigurationManager.getProperty("payment-system", "dryad.paymentsystem.currency");


    private static String priceList =   ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.sizeFileFee");


    private static String countryList = ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.countries");


    private static String notIntegratedJournalFeeList = ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.notIntegratedJournalFee");

    private static String sizeFileFeeAfterList = ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.sizeFileFeeAfter");

    private static String UnitSize = ConfigurationManager.getProperty("payment-system","dryad.paymentsystem.unitSize");

    public PaymentSystemConfigurationManager()
    {
        country = new Properties();
        currency = new Properties();
        sizeFileFee = new Properties();
        notIntegratedJournalFee = new Properties();
        sizeFileFeeAfter = new Properties();
        String[] currencyArray = currencyConfig.split(";");
        String[] prices = priceList.split(";");
        String[] countryArray = countryList.split(";");
        String[] notIntegratedJournalFees = notIntegratedJournalFeeList.split(";");
        String[] sizeFileFeeAfterArray = sizeFileFeeAfterList.split(";");

        for(String countryTemp:countryArray)
        {
            String[] countryTempArray = countryTemp.split(":");
            country.setProperty(countryTempArray[0],countryTempArray[1]);
        }

        for(String currencyTemp:currencyArray)
        {
            String[] currencyTempArray = currencyTemp.split(":");
            currency.setProperty(currencyTempArray[0],currencyTempArray[1]);
        }

        for(String priceTemp:prices)
        {
            String[] priceTempArray = priceTemp.split(":");
            sizeFileFee.setProperty(priceTempArray[0],priceTempArray[1]);
        }

        for(String temp:notIntegratedJournalFees)
        {
            String[] tempArray = temp.split(":");
            notIntegratedJournalFee.setProperty(tempArray[0],tempArray[1]);
        }
        for(String sizeFileFeeAfterTemp:sizeFileFeeAfterArray)
        {
            String[] temp = sizeFileFeeAfterTemp.split(":");
            sizeFileFeeAfter.setProperty(temp[0],temp[1]);
        }

    }

    /**
     * Get a configuration property
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property, or <code>null</code> if the property
     *         does not exist.
     */
    public static String getCountryProperty(String property)
    {
        String value = country.getProperty(property);
        return (value != null) ? value.trim() : null;
    }

    public static Double getCurrencyProperty(String property)
    {
        Double value = Double.parseDouble(currency.getProperty(property));
        return value;
    }

    public static Double getSizeFileFeeProperty(String property)
    {
        Double value = Double.parseDouble(sizeFileFee.getProperty(property));
        return value;
    }

    public static Double getNotIntegratedJournalFeeProperty(String property)
    {
        Double value = Double.parseDouble(notIntegratedJournalFee.getProperty(property));
        return value;
    }


    public static Properties getAllCountryProperty()
    {
       return country;
    }

    public static Properties getAllCurrencyProperty()
    {
        return currency;
    }

    public static Properties getAllSizeFileFeeProperty()
    {
        return sizeFileFee;
    }

    public static Properties getAllNotIntegratedJournalFeeProperty()
    {
        return notIntegratedJournalFee;
    }

    public static Long getMaxFileSize()
    {
        if(StringUtils.equals(maxFileSize,null))
        {
            return null;
        }
        else
        {
            return Long.parseLong(maxFileSize);
        }

    }

    public static Properties getAllSizeFileFeeAfter()
    {
        return sizeFileFeeAfter;
    }

    public static String getSizeFileFeeAfterProperty(String property)
    {
        String value = sizeFileFeeAfter.getProperty(property);
        return (value != null) ? value.trim() : null;
    }

    public static Long getUnitSize()
    {
        if(StringUtils.equals(UnitSize,null))
        {
            return null;
        }
        else
        {
            return Long.parseLong(UnitSize);
        }
    }

}

/*
 * I18nUtil.java
 *
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.core;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.eperson.EPerson;

import java.io.File;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;




/**
 * I18nUtil.java
 * 
 * Some Utilities for i18n Support. 
 * - getting the default Locale for this DSpace Instance
 * - getting all supported Locales for this DSpace Instance
 * - getting email template, help file, input forms for a given Locale
 * 
 * 
 * @author Bernadette Schlonsok and Claudia Juergen
 *  
 * @version 1.0
 */

public class I18nUtil
{
    private static final Logger log = Logger.getLogger(I18nUtil.class);
    
    // the default Locale of this DSpace Instance
    public static final Locale DEFAULTLOCALE = getDefaultLocale();

    /**
     * Gets the default locale as defined in dspace.cfg If no default locale is
     * defined, the Locale of the JVM is used
     * 
     * @return defaultLocale
     *         the default Locale for this DSpace instance
     */
    public static Locale getDefaultLocale()
    {
        Locale defaultLocale = null;
        if ((ConfigurationManager.getProperty("default.locale") != null)
                && (ConfigurationManager.getProperty("default.locale") != ""))
        {
            StringTokenizer configDefaultLocale = new StringTokenizer(
                    ConfigurationManager.getProperty("default.locale"));
            int countTokens = configDefaultLocale.countTokens();
            switch (countTokens)
            {

            case 1:
                defaultLocale = new Locale(configDefaultLocale.nextToken()
                        .trim());
                break;

            case 2:
                defaultLocale = new Locale(configDefaultLocale.nextToken()
                        .trim(), configDefaultLocale.nextToken().trim());
                break;
            case 3:
                defaultLocale = new Locale(configDefaultLocale.nextToken()
                        .trim(), configDefaultLocale.nextToken().trim(),
                        configDefaultLocale.nextToken().trim());
                break;
            }

        }
        if (defaultLocale == null)
        {
            // use the Locale of the JVM
            defaultLocale = Locale.getDefault();
        }

        return defaultLocale;
    }

    /**
     * Get the Locale for a specified EPerson. If the language is missing,
     * return the default Locale for the repository.
     * 
     * @param ep
     * @return
     */
    public static Locale getEPersonLocale(EPerson ep)
    {
        if (ep == null)
        {
            log.error("No EPerson specified, returning default locale");
            return I18nUtil.getDefaultLocale();
        }

        String lang = ep.getLanguage();
        
        if (StringUtils.isBlank(lang))
        {
            log.error("No language specified for EPerson " + ep.getID());
            return I18nUtil.getDefaultLocale();
        }

        return I18nUtil.getSupportedLocale(new Locale(lang));
    }

    /**
     * get the available Locales for the User Interface as defined in dspace.cfg
     * returns an array of Locales or null
     * 
     * @return an array of supported Locales or null
     */
    public static Locale[] getSupportedLocales()
    {
        Locale[] availableLocales;
        
        if (ConfigurationManager.getProperty("webui.supported.locales") != null)
        {

            StringTokenizer configuredLocales = new StringTokenizer(
                    ConfigurationManager.getProperty("webui.supported.locales"),
                    ",");
            availableLocales = new Locale[configuredLocales
                    .countTokens()];

            while (configuredLocales.hasMoreTokens())
            {
                StringTokenizer localeElements = new StringTokenizer(
                        configuredLocales.nextToken().trim(), "_");
                int countTokens = localeElements.countTokens();
                switch (countTokens)
                {

                case 1:
                    availableLocales[configuredLocales.countTokens()] = new Locale(
                            localeElements.nextToken().trim());
                    break;

                case 2:
                    availableLocales[configuredLocales.countTokens()] = new Locale(
                            localeElements.nextToken().trim(), localeElements
                                    .nextToken().trim());
                    break;
                case 3:
                    availableLocales[configuredLocales.countTokens()] = new Locale(
                            localeElements.nextToken().trim(), localeElements
                                    .nextToken().trim(), localeElements
                                    .nextToken().trim());
                    break;
                }
            }
        }
        else
        {
            availableLocales = new Locale[1];
            availableLocales[0] =  DEFAULTLOCALE;
        }
        return availableLocales;
    }

    /**
     * Gets the appropriate supported Locale according for a given Locale If
     * no appropriate supported locale is found, the DEFAULTLOCALE is used
     * 
     * @param locale
     *        Locale to find the corresponding Locale
     * @return supportedLocale 
     *         Locale for session according to locales supported by this DSpace instance as set in dspace.cfg
     */
    
    public static Locale getSupportedLocale(Locale locale)
    {

        Locale[] availableLocales = getSupportedLocales();
        boolean isSupported = false;
        Locale supportedLocale = null;
        String testLocale = "";
        if (availableLocales == null)
        {
            supportedLocale = DEFAULTLOCALE;
        }
        else
        {
            if (!locale.getVariant().equals(""))
            {
                testLocale = locale.toString();
                for (int i = 0; i < availableLocales.length; i++)
                {
                    if (testLocale.equalsIgnoreCase(availableLocales[i]
                            .toString()))
                    {
                        isSupported = true;
                        supportedLocale = availableLocales[i];
                    }

                }
            }

            if (!(isSupported && locale.getCountry().equals("")))
            {
                testLocale = locale.getLanguage() + "_"
                        + locale.getCountry();

                for (int i = 0; i < availableLocales.length; i++)
                {
                    if (testLocale.equalsIgnoreCase(availableLocales[i]
                            .toString()))
                    {
                        isSupported = true;
                        supportedLocale = availableLocales[i];
                    }
                }

            }
            if (!isSupported)
            {
                testLocale = locale.getLanguage();

                for (int i = 0; i < availableLocales.length; i++)
                {
                    if (testLocale.equalsIgnoreCase(availableLocales[i]
                            .toString()))
                    {
                        isSupported = true;
                        supportedLocale = availableLocales[i];
                    }

                }
            }
            if (!isSupported)
            {
                supportedLocale = DEFAULTLOCALE;
            }
        }
        return supportedLocale; 
    }





    /**
     * Get the appropriate localized version of input-forms.xml according to language settings
     * 
     * @param locale
     *        Locale, the local to get the input-forms.xml for
     * @return String - localized filename for input-forms.xml
     */
    public static String getInputFormsFileName(Locale locale)
    {
        /** Name of the form definition XML file */
        String fileName = "";
        final String FORM_DEF_FILE = "input-forms";
        final String FILE_TYPE = ".xml";
        String defsFilename = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + FORM_DEF_FILE;
        fileName =  getFilename(locale, defsFilename, FILE_TYPE);
        return fileName;
    }
    /**
     * et the i18n message string for a given key and use the default Locale
     * 
     * @param key
     *        String - name of the key to get the message for
     *        
     * @return message
     *         String of the message
     * 
     * 
     */
    public static String getMessage(String key) throws MissingResourceException
    {
        
        String message = getMessage(key.trim(), DEFAULTLOCALE);
      
        return message;
    }
    
    /**
     * Get the i18n message string for a given key and locale
     * 
     * @param key
     *        String - name of the key to get the message for
     * @param locale
     *        Locale, to get the message for
     *        
     * @return message
     *         String of the message
     * 
     * 
     */
    public static String getMessage(String key, Locale locale) throws MissingResourceException
    {
        String message = "";
        if (locale == null)
        {
            locale = DEFAULTLOCALE;
        }
        ResourceBundle messages = ResourceBundle.getBundle("Messages", locale);
        message = messages.getString(key.trim());
        
        return message;
    }
    
    /**
     * Get the i18n message string for a given key and locale
     * 
     * @param key
     *        String - name of the key to get the message for
     * @param locale
     *        Locale, to get the message for
     *        
     * @return message
     *         String of the message
     * 
     * 
     */
    public static String getMessage(String key, Context c) throws MissingResourceException
    {
        String message = getMessage(key.trim(), c.getCurrentLocale());
        return message;
    }
    


    /**
     * Get the appropriate localized version of the default.license according to language settings
     * 
     * @param context
     *        the current DSpace context
     * @return fileName 
     *         String - localized filename for default.license
     */
    public static String getDefaultLicense(Context context)
    {
        Locale locale = context.getCurrentLocale();
        String fileName = "";
        /** Name of the default license */
        final String DEF_LIC_FILE = "default";
        final String FILE_TYPE = ".license";
        String defsFilename = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + DEF_LIC_FILE;
        
        fileName = getFilename(locale, defsFilename, FILE_TYPE);
        
        return fileName;
    }
    /**
     * Get the appropriate localized version of a file according to language settings
     * e. g. help files in jsp/help/
     * 
     * @param locale
     *        Locale to get the file for
     * @param fileName
     *        String fileName, to get the localized file for
     * @param fileType
     *        String file extension
     * @return localizedFileName
     *          String - localized filename
     */
    private static String getFilename(Locale locale, String fileName, String fileType)
    {
        String localizedFileName = null;
        boolean fileFound = false;
        // with Language, Country, Variant
        String fileNameLCV = null;
        // with Language, Country
        String fileNameLC = null;
        // with Language
        String fileNameL = null;
        fileNameL = fileName + "_" + locale.getLanguage();

        if (fileType == null)
        {
            fileType = "";
        }

        if (!("".equals(locale.getCountry())))
        {
            fileNameLC = fileName + "_" + locale.getLanguage() + "_"
                    + locale.getCountry();

            if (!("".equals(locale.getVariant())))
            {
                fileNameLCV = fileName + "_" + locale.getLanguage() + "_"
                        + locale.getCountry() + "_" + locale.getVariant();
            }
        }

        if (fileNameLCV != null && !fileFound)
        {
            File fileTmp = new File(fileNameLCV + fileType);
            if (fileTmp.exists())
            {
                fileFound = true;
                localizedFileName =  fileNameLCV + fileType;
            }
        }

        if (fileNameLC != null && !fileFound)
        {
            File fileTmp = new File(fileNameLC + fileType);
            if (fileTmp.exists())
            {
                fileFound = true;
                localizedFileName = fileNameLC + fileType;
            }
        }

        if (fileNameL != null && !fileFound)
        {
            File fileTmp = new File(fileNameL + fileType);
            if (fileTmp.exists())
            {
                fileFound = true;
                localizedFileName =  fileNameL + fileType;
            }
        }
        if (!fileFound)
        {
            localizedFileName = fileName + fileType;
        }
        return localizedFileName;
    }
    

    
    /**
     * Get the appropriate localized version of an email template according to language settings
     * 
     * @param locale
     *        Locale for this request
     * @param name
     *        String - base name of the email template 
     * @return templateName 
     *         String - localized filename of an email template
     */
    public static String getEmailFilename(Locale locale, String name)
    {
        String templateName = "";
        String templateFile = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "emails"
                + File.separator + name;

        templateName = getFilename(locale, templateFile, "");
        return templateName;
    }

}

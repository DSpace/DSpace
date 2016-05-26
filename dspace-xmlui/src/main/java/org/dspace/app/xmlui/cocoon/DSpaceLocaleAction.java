/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.parameters.Parameters;

import org.apache.cocoon.acting.ServiceableAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.i18n.I18nUtils;
import org.apache.cocoon.i18n.I18nUtils.LocaleValidator;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This action looks at several places to determine what locale should be used for 
 * this request. We use cocoon's i18nUtils find local method which will look in 
 * several places continuing to the next step if no local is found.:
 * 
 * 1. HTTP Request parameter 'locale' 
 * 2. Session attribute 'locale'
 * 3. First matching cookie parameter 'locale' within each cookie sent
 * 4. Sitemap parameter "locale"
 * 5. Locale setting of the requesting browser or server default
 * 6. Default
 * 7. Blank
 * 8. Fail
 * 
 * Only those locales which are listed in webui.supported.locales will be identified,
 * if no acceptable locales are found then the default locale will be used.
 * 
 * @author Scott Phillips
 */
public class DSpaceLocaleAction extends ServiceableAction implements Configurable {

   
	/** A validator class which tests if a local is a supported locale */
	private static DSpaceLocaleValidator localeValidator;
	
	/** The default locale if no acceptable locales are identified */
	private static Locale defaultLocale;
	
	
	/** 
	 * Configure the action.
	 */
	 public void configure(Configuration config)
	 {
		 if (localeValidator == null)
         {
             localeValidator = new DSpaceLocaleValidator();
         }
		 
		 if (defaultLocale == null)
         {
             defaultLocale = I18nUtil.getDefaultLocale();
         }
	 }
	
	
    /**
     * Action which obtains the current environments locale information, and
     * places it in the objectModel (and optionally in a session/cookie).
     */
    public Map act(Redirector redirector,
                   SourceResolver resolver,
                   Map objectModel,
                   String source,
                   Parameters parameters)
    throws Exception {
        
        Locale locale = I18nUtils.findLocale(objectModel, "locale-attribute", parameters, defaultLocale, false, true, false, localeValidator);

        if (locale == null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("No locale found, using default");
            }
            locale = I18nUtil.getDefaultLocale();
        }

        String localeStr = locale.toString();
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found locale: " + localeStr);
        }

        I18nUtils.storeLocale(objectModel,
                "locale-attribute",
                localeStr,
                false,
                true,
                false,
                false);

        Context context = ContextUtil.obtainContext(objectModel);
        context.setCurrentLocale(locale);

        // Set up a map for sitemap parameters
        Map<String, String> map = new HashMap<String, String>();
        map.put("language", locale.getLanguage());
        map.put("country", locale.getCountry());
        map.put("variant", locale.getVariant());
        map.put("locale", localeStr);
        return map;
    }

    
    /**
     * This validator class works with cocoon's i18nutils class to test if locales are valid. 
     * For dspace we define a locale as valid if it is listed in webui.supported.locales config 
     * parameter.
     */
    public static class DSpaceLocaleValidator implements LocaleValidator {

    	/** the list of supported locales that may be used. */
    	private List<Locale> supportedLocales;
    	
    	/**
    	 * Build a list supported locales to validate against upon object construction.
    	 */
    	public DSpaceLocaleValidator()
    	{
            if (DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.supported.locales") != null)
            {
            	supportedLocales = new ArrayList<Locale>();
            	
                String[] supportedLocalesConfig = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.supported.locales");

                for (String part : supportedLocalesConfig)
                {	
                	Locale supportedLocale = I18nUtils.parseLocale(part.trim(), null);
                	if (supportedLocale != null)
                	{
                		supportedLocales.add(supportedLocale);
                	}
                }
            }
    	}
    	
    	
    	/**
         * @param name name of the locale (for debugging)
         * @param test locale to test
         * @return true if locale satisfies validator's criteria
         */
		public boolean test(String name, Locale test) 
		{
			// If there are no configured locales the accept them all.
			if (supportedLocales == null)
            {
                return true;
            }
			
			// Otherwise check if they are listed
			for (Locale locale : supportedLocales)
            {
				if (locale.equals(test))
                {
					return true;
                }
            }
			
			// Fail if not found
			return false;
			
		}
    	
    }
    
    
}

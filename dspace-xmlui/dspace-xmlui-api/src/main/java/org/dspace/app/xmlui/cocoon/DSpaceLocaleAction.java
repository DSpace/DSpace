/*
 * DSpaceLocaleAction.java
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

package org.dspace.app.xmlui.cocoon;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.parameters.Parameters;

import org.apache.cocoon.acting.ServiceableAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.i18n.I18nUtils;
import org.apache.cocoon.i18n.I18nUtils.LocaleValidator;
import org.dspace.core.ConfigurationManager;
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
 * Only those locales which are listed in xmlui.supported.locales will be identified,
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
			 localeValidator = new DSpaceLocaleValidator();
		 
		 if (defaultLocale == null)
			 defaultLocale = I18nUtil.getDefaultLocale();
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
                              false,
                              false,
                              false);

        // Set up a map for sitemap parameters
        Map map = new HashMap();
        map.put("language", locale.getLanguage());
        map.put("country", locale.getCountry());
        map.put("variant", locale.getVariant());
        map.put("locale", localeStr);
        return map;
    }

    
    /**
     * This validator class workes with cocoon's i18nutils class to test if locales are valid. 
     * For dspace we define a locale as valid if it is listed in xmlui.supported.locales config 
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
            if (ConfigurationManager.getProperty("xmlui.supported.locales") != null)
            {
            	supportedLocales = new ArrayList<Locale>();
            	
                String supportedLocalesConfig = ConfigurationManager.getProperty("xmlui.supported.locales");
                
                String[] parts = supportedLocalesConfig.split(",");
                
                for (String part : parts)
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
         * @param name of the locale (for debugging)
         * @param locale to test
         * @return true if locale satisfies validator's criteria
         */
		public boolean test(String name, Locale test) 
		{
			// If there are no configured locales the accept them all.
			if (supportedLocales == null)
				return true;
			
			// Otherwise check if they are listed
			for (Locale locale : supportedLocales)
				if (locale.equals(test))
					return true;
			
			// Fail if not found
			return false;
			
		}
    	
    }
    
    
}

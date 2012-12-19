/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.aspect.extraSubmission;


import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Metadata;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import ar.edu.unlp.sedici.util.ConfigurationUtil;


/**
 * Aspect para la generacion de metas de autoarchivo
 *
 * @author Nicolás Romagnoli
 * @author Nestor Oviedo

 */
public class GenerateHomeLinksPageMeta  extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    /** What page metadata to add to the document */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
    {
    	// Guardo las communities que serán links
    	Map<String,String> communityLinks = ConfigurationUtil.readMultipleProperties("sedici-dspace", "xmlui.community-list.home-link");
    	String orderedHomeLinks = ConfigurationManager.getProperty("sedici-dspace","xmlui.community-list.ordered-home-links");
    	
    	for(String key : orderedHomeLinks.split(",")) {
    		pageMeta.addMetadata("home-link", key.trim()).addContent(communityLinks.get(key.trim()));
    	}
		
    }

	@Override
	public Serializable getKey() {
	    return "1";
	}
    

}


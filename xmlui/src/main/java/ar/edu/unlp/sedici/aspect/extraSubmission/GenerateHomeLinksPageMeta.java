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
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Metadata;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * Aspect para la generacion de metas de autoarchivo
 *
 * @author Nicolás Romagnoli
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
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	//Guardo las communities que serán links
    	String communitiesLinks = ConfigurationManager.getProperty("sedici-dspace", "xmlui.community-list.home-links");
    	Metadata meta;
    	int posicionInicial = 0;
    	boolean continuar=true;
    	int posicionFinal=communitiesLinks.indexOf("|", posicionInicial);
    	if (posicionFinal==-1){
    		continuar=false;
    	}
    	String elemento, nombre, valor="";
    	while (continuar){   
    		posicionFinal=communitiesLinks.indexOf("|", posicionInicial);
        	if (posicionFinal==-1){
        		posicionFinal=communitiesLinks.length();
        		continuar=false;
        	}
    		elemento=communitiesLinks.substring(posicionInicial, posicionFinal);
    		posicionInicial=posicionFinal+1;    		
    		try{
	    		nombre=elemento.substring(0, elemento.indexOf(":"));
	    		valor=elemento.substring(elemento.indexOf(":")+1, elemento.length());
	    		meta=pageMeta.addMetadata("home-link", nombre);
	    		meta.addContent(valor);
    		} catch (Exception e) {
    			System.out.println("erorr");
			}
    	}
    	

    	
		
    }

	@Override
	public Serializable getKey() {
	    return "1";
	}
    

}


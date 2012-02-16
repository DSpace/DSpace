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
public class GenerateAutoArchivePageMeta  extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.FrontPageSearch.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.FrontPageSearch.para1");
    
    private static final Message T_go =
        message("xmlui.general.go");
    
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() 
    {
       return "1";
    }

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
    	//Recupero el id de la coleccion de autoarchivo y lo agrego al pageMeta
    	String handleConfig = ConfigurationManager.getProperty("sedici-dspace", "autoArchiveCollectionId");
    	Metadata meta=pageMeta.addMetadata("autoArchive", "handle");
    	meta.addContent(handleConfig);
    	
    	//Verifico que el usuario solo tenga permiso de escritura solamente en autoarchivo
    	Boolean onlyAutoArchiveSubmit=false;
    	Collection[] collections; // List of possible collections.
		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		collections = Collection.findAuthorized(context, null, Constants.ADD);
		if (collections.length==1){
			for (Collection collection : collections) 
	        {
				String handle=collection.getHandle();
				if (handle.equals(handleConfig)){
					onlyAutoArchiveSubmit=true;
				}	        	
	        }
		}
		meta=pageMeta.addMetadata("autoArchive", "submit");
    	meta.addContent(onlyAutoArchiveSubmit.toString());
		
		
    }
    

}


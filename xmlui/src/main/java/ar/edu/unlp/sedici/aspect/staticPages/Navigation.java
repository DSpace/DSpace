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
package ar.edu.unlp.sedici.aspect.staticPages;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Navigation that adds code needed for the browse features from dspace
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    /** Language Strings */
    private static final Message T_head_all_of_dspace =
        message("xmlui.ArtifactBrowser.Navigation.head_all_of_dspace");

    private static final Message T_head_links =
        message("xmlui.ArtifactBrowser.Navigation.links_browse");

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key = request.getScheme() + request.getServerName() + request.getServerPort() + request.getSitemapURI() + request.getQueryString();

            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                key += "-" + dso.getHandle();
            }

            return HashUtil.hash(key);
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

        /**
     * Generate the cache validity object.
     *
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }

    /**
     * Add the basic navigational options:
     *
     * browse - browse by Titles - browse by Authors - browse by Dates
     *
     * language FIXME: add languages
     *
     * context no context options are added.
     *
     * action no action options are added.
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        /* Create skeleton menu structure to ensure consistent order between aspects,
         * even if they are never used
         */

        options.addList("browse");
        options.addList("account");
        options.addList("context");
        options.addList("administrative");
        List links = options.addList("links");


        links.setHead(T_head_links);
        
        Map<String, String> urls=this.generarUrls();
        for (String nombre : urls.keySet()) {
        	links.addItemXref(contextPath + urls.get(nombre),nombre);
		};


    }
    
	private static Map<String, String> generarUrls() {
		int inicial=1;
		Map<String, String> retorno=new HashMap<String, String>();
		String valor=ConfigurationManager.getProperty("sedici.pages", "map.url"+inicial);
		String nombre=ConfigurationManager.getProperty("sedici.pages", "map.url"+inicial+".nombre");
		while (valor!=null){
			retorno.put(nombre, valor);
			inicial+=1;
			valor=ConfigurationManager.getProperty("sedici.pages", "map.url"+inicial);
			nombre=ConfigurationManager.getProperty("sedici.pages", "map.url"+inicial+".nombre");
			
		}
		return retorno;
		
	}

}


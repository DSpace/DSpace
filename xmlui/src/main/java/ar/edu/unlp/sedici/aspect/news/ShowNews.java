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
package ar.edu.unlp.sedici.aspect.news;


import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * Aspect para el manejo de las noticias desde un feed
 *
 * @author Nicolás Romagnoli
 */
public class ShowNews  extends AbstractDSpaceTransformer implements CacheableProcessingComponent
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
    

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	String url = ConfigurationManager.getProperty("sedici-dspace", "feed");
    	//String url="http://feeds.feedburner.com/luauf";
        SyndFeed feed;
        try {
        	//recupero el feed de noticias e inicializo las variables para su manejo
            URL feedUrl = new URL(url);

            SyndFeedInput input = new SyndFeedInput();
            feed = input.build(new XmlReader(feedUrl));
            
            //Trabajo para feed de noticias

            Division newsDiv = 	body.addDivision("feed"); 
            newsDiv.setHead("Noticias");
            List browseNewsList = newsDiv.addList("news", List.TYPE_SIMPLE,
                    "news");
            browseNewsList.setHead("Lista de noticias");
            int cont=0;
            for (Iterator iterator = feed.getEntries().iterator(); iterator.hasNext();) {
            	SyndEntry entrada = (SyndEntry) iterator.next();
            	List noticia = browseNewsList.addList("noticia_"+cont);
            	Item titulo=noticia.addItem("titulo", "titulo");
            	titulo.addContent(entrada.getTitle());
            	Item descripcion=noticia.addItem("descripcion", "descripcion");
            	descripcion.addContent(entrada.getDescription().getValue());
            	Item link=noticia.addItem("link", "link");
            	link.addXref(entrada.getLink());
            	cont=cont+1;

    		}

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("ERROR: "+ex.getMessage());
        }


        

    }
}


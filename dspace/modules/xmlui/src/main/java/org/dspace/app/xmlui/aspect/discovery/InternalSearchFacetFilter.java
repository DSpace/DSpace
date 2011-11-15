package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 16-sep-2011
 * Time: 9:28:35
 */
public class InternalSearchFacetFilter extends SearchFacetFilter{

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        if(!AuthorizeManager.isAdmin(ContextUtil.obtainContext(objectModel))){
            throw new AuthorizeException();
        }
    }

    public String getView(){
        return "nonarchived";
    }

    public String getSearchFilterUrl(){
        return "non-archived-search-filter";
    }

    public String getDiscoverUrl(){
        return "non-archived-discovery";
    }
    
}

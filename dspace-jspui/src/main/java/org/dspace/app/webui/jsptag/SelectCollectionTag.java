/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.util.CollectionUtils;
import org.dspace.app.util.CollectionsTree;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;

/**
 * Renders select element to select collection with parent community
 * object.
 * 
 * @author Keiji Suzuki
 */
public class SelectCollectionTag extends TagSupport
{

    /** the class description */
    private String klass;

    /** the name description */
    private String name;

    /** the id description */
    private String id;

    /** the collection id */
    private int collection = -1;

    

    public SelectCollectionTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        StringBuffer sb = new StringBuffer();

        try
        {
            HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
            Context context = UIUtil.obtainContext(hrq);
            Collection[] collections = (Collection[]) hrq.getAttribute("collections");
            
            CollectionsTree tree= CollectionUtils.getCollectionsTree(collections, false);

            sb.append("<select");
            if (name != null)
            {
                sb.append(" name=\"").append(name).append("\"");
            }
            if (klass != null)
            {
                sb.append(" class=\"").append(klass).append("\"");
            }
            if (id != null)
            {
                sb.append(" id=\"").append(id).append("\"");
            }
            sb.append(">\n");

            ResourceBundle msgs = ResourceBundle.getBundle("Messages", context.getCurrentLocale());
            String firstOption = msgs.getString("jsp.submit.start-lookup-submission.select.collection.defaultoption");
         
            if (collection == -1) sb.append(" selected=\"selected\"");
            sb.append("<option value=\"-1\"");
            sb.append(">").append(firstOption).append("</option>\n");

            out.print(sb.toString());
            collectionSelect(out, tree);
   
            out.print("</select>\n");

        }
        catch (IOException e)
        {
            throw new JspException(e);
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
        
        return SKIP_BODY;
    }
    
    
	private void collectionSelect(JspWriter out, CollectionsTree tree ) throws IOException 
	{
		
		if(tree==null){
			return;
		}
		if (tree.getCurrent() != null)
		{
			out.print("<optgroup label=\""+tree.getCurrent().getName()+"\">");
		}
		if (tree.getCollections() != null){
			for (Collection col : tree.getCollections())
			{
				String selected= col.getID()==collection ? "selected":"";
				out.print("<option value=\""+col.getID()+"\" "+ selected + ">"+col.getName()+"</option>");	
			}
		}
		if (tree.getSubTree() != null)
		{
			for (CollectionsTree subTree: tree.getSubTree())
			{
				collectionSelect(out, subTree);
			}
		}
		if (tree.getCurrent() != null)
		{
			out.print("</optgroup>");
		}
		
	}    

    public String getKlass()
    {
        return klass;
    }

    public void setKlass(String klass)
    {
        this.klass = klass;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public int getCollection()
    {
        return collection;
    }

    public void setCollection(int collection)
    {
        this.collection = collection;
    }


    public void release()
    {
        klass = null;
        name = null;
        id = null;
        collection = -1;
    }
}


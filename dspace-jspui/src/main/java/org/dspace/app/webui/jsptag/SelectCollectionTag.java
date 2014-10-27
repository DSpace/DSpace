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
            Map<Community, List<Collection>> commCollList = new LinkedHashMap<Community, List<Collection>>();

            for (Community topcommunity : Community.findAllTop(context))
            {
                for (Collection collection : topcommunity.getCollections())
                {
                    List<Collection> clist = null;
                    if (commCollList.containsKey(topcommunity))
                    {
                        clist = commCollList.get(topcommunity);
                    }
                    else
                    {
                        clist = new ArrayList<Collection>();
                    }
                    clist.add(collection);
                    commCollList.put(topcommunity, clist);
                }

                for (Community subcommunity : topcommunity.getSubcommunities())
                {
                    addCommCollList(subcommunity, commCollList);
                }
            }

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
            sb.append("<option value=\"-1\"");
            if (collection == -1) sb.append(" selected=\"selected\"");
            sb.append(">").append(firstOption).append("</option>\n");

            Iterator<Community> iter = commCollList.keySet().iterator();
            while(iter.hasNext())
            {
                Community comm = iter.next();
                //sb.append("<optgroup label=\"").append(getCommName(comm)).append("\">\n");
                for (Collection coll : commCollList.get(comm))
                {
                    sb.append("<option value=\"").append(coll.getID()).append("\"");
                    if (collection == coll.getID())
                    {
                        sb.append(" selected=\"selected\"");
                    }
                    sb.append(">").append(CollectionDropDown.collectionPath(coll)).append("</option>\n");
                }
                //sb.append("</optgroup>\n");
            }
            sb.append("</select>\n");

            out.print(sb.toString());
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

    private void addCommCollList(Community community, Map<Community, 
        List<Collection>> commCollList) throws SQLException
    {
        for (Collection collection : community.getCollections())
        {
            List<Collection> clist = null;
            if (commCollList.containsKey(community))
            {
                clist = commCollList.get(community);
            }
            else
            {
                clist = new ArrayList<Collection>();
            }
            clist.add(collection);
            commCollList.put(community, clist);
        }

        for (Community subcommunity : community.getSubcommunities())
        {
            addCommCollList(subcommunity, commCollList);
        }

    }

    private String getCommName(Community community) throws SQLException
    {
        StringBuffer sb = new StringBuffer("");
        Community[] parents = community.getAllParents();
        for (Community parent : parents)
        {
            sb.insert(0, parent.getMetadata("name")+"/");
        }
        sb.append(community.getMetadata("name"));

        return sb.toString().substring(1);
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


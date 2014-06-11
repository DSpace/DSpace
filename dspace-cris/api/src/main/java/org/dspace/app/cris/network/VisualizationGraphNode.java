/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import java.util.LinkedList;
import java.util.List;

public class VisualizationGraphNode
{

    private String type;

    private String a;
    
    private String a_auth;

    private String a_dept;
    
    private String b;
    
    private String b_auth;

    private String b_dept;
    
    private String favalue;

    private String fbvalue;

    private List<String> focus;

    private List<String> extra;

    private List<String> value;

    private Integer entity;
    
    public Integer getEntity()
    {
        return entity;
    }

    public void setEntity(Integer entity)
    {
        this.entity = entity;
    }

    public List<String> getFocus()
    {
        if(this.focus==null) {
            this.focus = new LinkedList<String>();
        }
        return focus;
    }

    public void setFocus(List<String> focus)
    {
        this.focus = focus;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getA_auth()
    {
        return a_auth;
    }

    public void setA_auth(String node1)
    {
        this.a_auth = node1;
    }

    public String getB_auth()
    {
        return b_auth;
    }

    public void setB_auth(String node2)
    {
        this.b_auth = node2;
    }

    public List<String> getExtra()
    {
        if (this.extra == null)
        {
            this.extra = new LinkedList<String>();
        }
        return extra;
    }

    public void setExtra(List<String> extra)
    {
        this.extra = extra;
    }

    public List<String> getValue()
    {
        if (this.value == null)
        {
            this.value = new LinkedList<String>();
        }
        return value;
    }

    public void setValue(List<String> value)
    {
        this.value = value;
    }

    public void setFavalue(String fullNameNode1)
    {
        this.favalue = fullNameNode1;
    }

    public String getFavalue()
    {
        return favalue;
    }

    public void setFbvalue(String fullNameNode2)
    {
        this.fbvalue = fullNameNode2;
    }

    public String getFbvalue()
    {
        return fbvalue;
    }

	public String getB() {
		return b;
	}

	public void setB(String b) {
		this.b = b;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

    public String getA_dept()
    {
        return a_dept;
    }

    public void setA_dept(String a_dept)
    {
        this.a_dept = a_dept;
    }

    public String getB_dept()
    {
        return b_dept;
    }

    public void setB_dept(String b_dept)
    {
        this.b_dept = b_dept;
    }

}

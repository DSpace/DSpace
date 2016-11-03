/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.deduplication.utils.DedupUtils;
import org.dspace.app.cris.deduplication.utils.DuplicateItemInfo;
import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import flexjson.JSONSerializer;

public class DuplicateItemInfoJSONRequest extends JSONRequest
{

    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws IOException, AuthorizeException
    {
        DedupUtils dedupUtils = new DSpace().getServiceManager()
                .getServiceByName("dedupUtils", DedupUtils.class);
        
        DuplicateItemInfoJSONResponse jsonresp = new DuplicateItemInfoJSONResponse();
        int itemID = UIUtil.getIntParameter(req, "itemid");
        boolean check = UIUtil.getBoolParameter(req, "check");
        int typeID = UIUtil.getIntParameter(req, "typeid");
        boolean admin = UIUtil.getBoolParameter(req, "admin");
        try
        {
            if(admin) {
                jsonresp.potentialDuplicates = dedupUtils
                        .getAdminDuplicateByIdAndType(context, itemID, typeID);
            }
            else {
                jsonresp.potentialDuplicates = dedupUtils
                    .getDuplicateByIDandType(context, itemID, typeID, check);
            }
            if (jsonresp.potentialDuplicates != null)
            {
                for (DuplicateItemInfo pd : jsonresp.potentialDuplicates)
                {
                    jsonresp.aaData.add(pd.getDuplicateItem());
                }
            }
        }
        catch (SQLException e)
        {
            throw new IOException(e);
        }
        catch (SearchServiceException e1)
        {
            throw new IOException(e1);
        }

        jsonresp.setiTotalDisplayRecords(jsonresp.getAaData().size());
        jsonresp.setiTotalRecords(jsonresp.getAaData().size());
        jsonresp.setsEcho(UIUtil.getIntParameter(req, "sEcho"));

        JSONSerializer serializer = new JSONSerializer();
        serializer.deepSerialize(jsonresp, resp.getWriter());
    }
}

class DuplicateItemInfoJSONResponse
{
    List<DuplicateItemInfo> potentialDuplicates;

    List<SimpleViewEntityDTO> aaData = new ArrayList<SimpleViewEntityDTO>();

    private int offset = 0;

    private long iTotalRecords = 0;

    private long iTotalDisplayRecords = 0;

    private boolean error = false;

    private int sEcho = 0;

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public long getiTotalRecords()
    {
        return iTotalRecords;
    }

    public void setiTotalRecords(long iTotalRecords)
    {
        this.iTotalRecords = iTotalRecords;
    }

    public long getiTotalDisplayRecords()
    {
        return iTotalDisplayRecords;
    }

    public void setiTotalDisplayRecords(long iTotalDisplayRecords)
    {
        this.iTotalDisplayRecords = iTotalDisplayRecords;
    }

    public boolean isError()
    {
        return error;
    }

    public void setError(boolean error)
    {
        this.error = error;
    }

    public int getsEcho()
    {
        return sEcho;
    }

    public void setsEcho(int sEcho)
    {
        this.sEcho = sEcho;
    }

    public List<DuplicateItemInfo> getPotentialDuplicates()
    {
        return potentialDuplicates;
    }

    public List<SimpleViewEntityDTO> getAaData()
    {
        return aaData;
    }
}
package org.dspace.app.cris.deduplication.utils;

import java.util.List;

public class DuplicateInfoList
{
    
    private long size;
    
    private List<DuplicateInfo> dsi;

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public List<DuplicateInfo> getDsi()
    {
        return dsi;
    }

    public void setDsi(List<DuplicateInfo> dsi)
    {
        this.dsi = dsi;
    }
    
    
}

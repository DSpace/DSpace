package org.dspace.sword2;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.InputStream;

public interface SwordContentDisseminator
{
    public InputStream disseminate(Context context, Item item)
        throws DSpaceSwordException, SwordError, SwordServerException;

    public boolean disseminatesContentType(String contentType)
        throws DSpaceSwordException, SwordError, SwordServerException;

    public boolean disseminatesPackage(String contentType)
        throws DSpaceSwordException, SwordError, SwordServerException;

    public void setContentType(String contentType);

    public void setPackaging(String packaging);

    public String getContentType();

    public String getPackaging();
}

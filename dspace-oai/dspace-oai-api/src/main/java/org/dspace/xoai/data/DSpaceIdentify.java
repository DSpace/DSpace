/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.util.DateUtils;
import org.dspace.xoai.util.MetadataFieldManager;

import com.lyncode.xoai.dataprovider.core.DeleteMethod;
import com.lyncode.xoai.dataprovider.core.Granularity;
import com.lyncode.xoai.dataprovider.data.AbstractIdentify;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceIdentify extends AbstractIdentify
{
    private static Logger log = LogManager.getLogger(DSpaceIdentify.class);

    private static List<String> _emails = null;

    private static String _name = null;

    private static String _baseUrl = null;

    private Context _context;

    private HttpServletRequest _request;

    public DSpaceIdentify(Context context, HttpServletRequest request)
    {
        _context = context;
        _request = request;
    }

    @Override
    public List<String> getAdminEmails()
    {
        if (_emails == null)
        {
            _emails = new ArrayList<String>();
            String result = ConfigurationManager.getProperty("mail.admin");
            if (result == null)
            {
                log.warn("{ OAI 2.0 :: DSpace } Not able to retrieve the mail.admin property from the configuration file");
            }
            else
                _emails.add(result);
        }
        return _emails;
    }

    @Override
    public String getBaseUrl()
    {
        if (_baseUrl == null)
        {
            _baseUrl = _request.getRequestURL().toString()
                    .replace(_request.getPathInfo(), "");
        }
        return _baseUrl + _request.getPathInfo();
    }

    @Override
    public DeleteMethod getDeleteMethod()
    {
        return DeleteMethod.PERSISTENT;
    }

    @Override
    public Date getEarliestDate()
    {
        // Look at the database!
        try
        {
            TableRowIterator iterator = DatabaseManager
                    .query(_context,
                            "SELECT MIN(text_value) as value FROM metadatavalue WHERE metadata_field_id = ?",
                            MetadataFieldManager.getFieldID(_context,
                                    "dc.date.available"));

            if (iterator.hasNext())
            {
                String str = iterator.next().getStringColumn("value");
                try
                {
                    Date d = DateUtils.parseDate(str);
                    if (d != null)
                        return d;
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (InvalidMetadataFieldException e)
        {
            log.error(e.getMessage(), e);
        }
        return new Date();
    }

    @Override
    public Granularity getGranularity()
    {
        return Granularity.Second;
    }

    @Override
    public String getRepositoryName()
    {
        if (_name == null)
        {
            _name = ConfigurationManager.getProperty("dspace.name");
            if (_name == null)
            {
                log.warn("{ OAI 2.0 :: DSpace } Not able to retrieve the dspace.name property from the configuration file");
                _name = "OAI Repository";
            }
        }
        return _name;
    }

}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class extends the {@link ExternalSourceEntryItemUriListHandler} abstract class and implements it specifically
 * for the List<PoolTask> objects.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class ExternalSourceEntryPoolTaskUriListHandler extends ExternalSourceEntryItemUriListHandler<PoolTask> {

    @Autowired
    private PoolTaskService poolTaskService;

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (clazz != PoolTask.class) {
            return false;
        }
        return true;
    }

    @Override
    public PoolTask handle(Context context, HttpServletRequest request, List<String> uriList)
            throws SQLException, AuthorizeException {
        return getObjectFromUriList(context, uriList);
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException {
        if (uriList.size() > 1) {
            return false;
        }
        return true;
    }


    private PoolTask getObjectFromUriList(Context context, List<String> uriList) {
        PoolTask poolTask = null;
        String url = uriList.get(0);
        Pattern pattern = Pattern.compile("\\/api\\/workflow\\/pooltasks\\/(.*)");
        Matcher matcher = pattern.matcher(url);

        matcher.find();
        String id = matcher.group(1);
        int poolTaskId = Integer.parseInt(id);
        try {
            poolTask = poolTaskService.find(context, poolTaskId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return poolTask;
    }
}
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
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.service.OrcidQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class extends the {@link ExternalSourceEntryItemUriListHandler} abstract class and implements it specifically
 * for the List<OrcidQueue> objects.
 * 
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class ExternalSourceEntryOrcidQueueUriListHandler
    extends ExternalSourceEntryItemUriListHandler<OrcidQueue> {

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (clazz != OrcidQueue.class) {
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException {
        if (uriList.size() > 1) {
            return false;
        }
        return true;
    }

    @Override
    public OrcidQueue handle(Context context, HttpServletRequest request, List<String> uriList)
        throws SQLException, AuthorizeException {
        return getObjectFromUriList(context, uriList);
    }

    private OrcidQueue getObjectFromUriList(Context context, List<String> uriList) {
        OrcidQueue orcidQueue = null;
        String url = uriList.get(0);
        Pattern pattern = Pattern.compile("\\/api\\/eperson\\/orcidqueues\\/(.*)");
        Matcher matcher = pattern.matcher(url);

        matcher.find();
        String id = matcher.group(1);
        int queueId = Integer.parseInt(id);
        try {
            orcidQueue = orcidQueueService.find(context, queueId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return orcidQueue;
    }
}

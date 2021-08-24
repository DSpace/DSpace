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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class extends the {@link ExternalSourceEntryItemUriListHandler} abstract class and implements it specifically
 * for the List<Item> objects.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class ExternalSourceItemUriListHandler extends ExternalSourceEntryItemUriListHandler<Item> {

    @Autowired
    private ItemService itemService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean supports(List<String> uriList, String method,Class clazz) {
        if (clazz != Item.class) {
            return false;
        }
        return true;
    }

    @Override
    public Item handle(Context context, HttpServletRequest request, List<String> uriList)
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


    private Item getObjectFromUriList(Context context, List<String> uriList) {
        Item item = null;
        String url = uriList.get(0);
        Pattern pattern = Pattern.compile("\\/api\\/core\\/items\\/(.*)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            throw new DSpaceBadRequestException("The uri: " + url + " doesn't resolve to an item");
        }
        String id = matcher.group(1);
        UUID itemId = UUID.fromString(id);
        try {
            item = itemService.find(context, itemId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return item;
    }

}
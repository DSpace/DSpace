/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.sql.SQLException;
import java.util.UUID;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.ldn.dao.LDNMessageDao;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.model.Service;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link LDNMessageService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class LDNMessageServiceImpl implements LDNMessageService {

    @Autowired(required = true)
    private LDNMessageDao ldnMessageDao;
    @Autowired(required = true)
    private NotifyService notifyService;
    @Autowired(required = true)
    private ConfigurationService configurationService;
    @Autowired(required = true)
    private HandleService handleService;
    @Autowired(required = true)
    private ItemService itemService;

    protected LDNMessageServiceImpl() {

    }

    @Override
    public LDNMessage find(Context context, String id) throws SQLException {
        return ldnMessageDao.findByID(context, LDNMessage.class, id);
    }

    @Override
    public LDNMessage create(Context context, String id) throws SQLException {
        return ldnMessageDao.create(context, new LDNMessage(id));
    }

    @Override
    public LDNMessage create(Context context, Notification notification) throws SQLException {
        LDNMessage ldnMessage = create(context, notification.getId());

        ldnMessage.setObject(findDspaceObjectByUrl(context, notification.getId()));

        if (null != notification.getContext()) {
            ldnMessage.setContext(findDspaceObjectByUrl(context, notification.getContext().getId()));
        }

        ldnMessage.setOrigin(findNotifyService(context, notification.getOrigin()));
        ldnMessage.setTarget(findNotifyService(context, notification.getTarget()));
        ldnMessage.setInReplyTo(find(context, notification.getInReplyTo()));
        ldnMessage.setMessage(new Gson().toJson(notification));
        ldnMessage.setType(StringUtils.joinWith(",", notification.getType()));

        update(context, ldnMessage);
        return ldnMessage;
    }

    @Override
    public void update(Context context, LDNMessage ldnMessage) throws SQLException {
        ldnMessageDao.save(context, ldnMessage);
    }

    private DSpaceObject findDspaceObjectByUrl(Context context, String url) throws SQLException {
        String dspaceUrl = configurationService.getProperty("dspace.ui.url") + "/handle/";

        if (url.startsWith(dspaceUrl)) {
            return handleService.resolveToObject(context, url.substring(dspaceUrl.length()));
        }

        String handleResolver = configurationService.getProperty("handle.canonical.prefix", "https://hdl.handle.net/");
        if (url.startsWith(handleResolver)) {
            return handleService.resolveToObject(context, url.substring(handleResolver.length()));
        }

        dspaceUrl = configurationService.getProperty("dspace.ui.url") + "/items/";
        if (url.startsWith(dspaceUrl)) {
            return itemService.find(context, UUID.fromString(url.substring(dspaceUrl.length())));
        }

        return null;
    }

    private NotifyServiceEntity findNotifyService(Context context, Service service) throws SQLException {
        return notifyService.findByLdnUrl(context, service.getInbox());
    }

}

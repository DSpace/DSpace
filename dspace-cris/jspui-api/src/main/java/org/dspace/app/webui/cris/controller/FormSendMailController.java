/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.cris.dto.MailDTO;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is responsible to send request change email to
 * backoffice staff.
 * 
 * @author cilea
 * 
 */
public class FormSendMailController extends BaseFormController
{
    /*
     * constants to define the scope of the send mail page
     */
    /**
     * constant to request changes for media
     */
    public final int MODE_MEDIA = 1;

    /**
     * constant to request changes for bibliometric data
     */
    public final int MODE_BIBLIOMETRIC = 2;

    /**
     * constant to request changes for item
     */
    public final int MODE_ITEMS = 3;

    /**
     * email template name constant
     */
    public final String TEMPLATE_MEDIA = "rp_request_changes_media";

    /**
     * email template name constant
     */
    public final String TEMPLATE_BIBLIOMETRIC = "rp_request_changes_bibliometric";

    /**
     * email template name constant
     */
    public final String TEMPLATE_ITEMS = "rp_request_changes_items";

    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {

        String mode_s = request.getParameter("mode");
        Integer mode = Integer.parseInt(mode_s);
        String id_s = request.getParameter("id");
        Integer id = null;
        if (id_s != null)
        {
            id = Integer.parseInt(id_s);
        }
        ResearcherPage researcher = applicationService.get(
                ResearcherPage.class, id);
        MailDTO dto = new MailDTO();
        dto.setSubmitter(researcher.getId());
        dto.setRp(ResearcherPageUtils.getPersistentIdentifier(researcher));
        Locale supportedLocale = request.getLocale();
        ResourceBundle labels = ResourceBundle.getBundle("Messages",
                supportedLocale);
        switch (mode)
        {

        case MODE_BIBLIOMETRIC:
            dto.setTemplate(TEMPLATE_BIBLIOMETRIC);
            dto.setSubject(labels
                    .getString("subject.mail.hku.request-changes.two"));
            break;

        case MODE_ITEMS:
            dto.setTemplate(TEMPLATE_ITEMS);
            dto.setSubject(labels
                    .getString("subject.mail.hku.request-changes.three"));
            break;

        default:
            dto.setTemplate(TEMPLATE_MEDIA);
            dto.setSubject(labels
                    .getString("subject.mail.hku.request-changes.one"));
            break;
        }
        return dto;
    }

    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {

        MailDTO dto = (MailDTO) command;

        // send mail
        Email email = Email.getEmail(ConfigurationManager
                .getProperty("dspace.dir")
                + File.separatorChar
                + "config"
                + File.separatorChar
                + "emails"
                + File.separatorChar + dto.getTemplate());
        email.addArgument(dto.getRp());
        email.addArgument(dto.getText());
        email.addRecipient(ConfigurationManager
                .getProperty("feedback.recipient"));
        email.setReplyTo(UIUtil.obtainContext(request).getCurrentUser()
                .getEmail());
        email.send();
        return new ModelAndView(getSuccessView() + dto.getRp());
    }

}

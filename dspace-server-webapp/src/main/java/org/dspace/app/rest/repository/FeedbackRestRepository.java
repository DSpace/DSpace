/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.DSpaceFeedbackNotFoundException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.FeedbackRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the Repository that takes care of the operations on the {@link FeedbackRest} objects
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
@Component(FeedbackRest.CATEGORY + "." + FeedbackRest.NAME)
public class FeedbackRestRepository extends DSpaceRestRepository<FeedbackRest, Integer> {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<FeedbackRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(FeedbackRest.NAME, "findAll");
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public FeedbackRest findOne(Context context, Integer id) {
        throw new RepositoryMethodNotImplementedException(FeedbackRest.NAME, "findOne");
    }

    @Override
    @PreAuthorize("permitAll()")
    protected FeedbackRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        FeedbackRest feedbackRest = null;

        String recipientEmail = configurationService.getProperty("feedback.recipient");
        if (StringUtils.isBlank(recipientEmail)) {
            throw new DSpaceFeedbackNotFoundException("Recipient's email was not found!");
        }

        try {
            feedbackRest = mapper.readValue(req.getInputStream(), FeedbackRest.class);
        } catch (IOException exIO) {
            throw new UnprocessableEntityException("error parsing the body " + exIO.getMessage(), exIO);
        }

        String session = req.getSession().getId();
        String agent = req.getHeader("User-Agent");
        String currentUserEmail = StringUtils.EMPTY;

        if (Objects.nonNull(context.getCurrentUser())) {
            currentUserEmail = context.getCurrentUser().getEmail();
        }

        String senderEmail = feedbackRest.getEmail();
        String message = feedbackRest.getMessage();
        String page = feedbackRest.getPage();

        if (StringUtils.isBlank(senderEmail) || StringUtils.isBlank(message)) {
            throw new DSpaceBadRequestException("Filds as e-mail and message are mandatory!");
        }

        try {
            Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "feedback"));
            email.addArgument(new Date());         //  Date
            email.addArgument(senderEmail);       //  Email
            email.addArgument(currentUserEmail); //  Logged in as
            email.addArgument(page);            //  Referring page
            email.addArgument(agent);          //  User agent
            email.addArgument(session);       //  Session ID
            email.addArgument(message);      //  The feedback itself
            email.send();
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Class<FeedbackRest> getDomainClass() {
        return FeedbackRest.class;
    }

}
package org.ssu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.ChairEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.FacultyEntity;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.FacultyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.AuthorLocalization;
import org.ssu.service.AuthorsService;
import org.ssu.service.EpersonService;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ProfileController {
    protected transient EPersonService personService
            = EPersonServiceFactory.getInstance().getEPersonService();

    @Resource
    private AuthorsService authorsService;

    @Resource
    private EpersonService epersonService;

    private FacultyService facultyService = EPersonServiceFactory.getInstance().getFacultyService();

    @RequestMapping("/profile")
    public ModelAndView profilePage(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException, JsonProcessingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson eperson = dspaceContext.getCurrentUser();

        boolean missingFields = Optional.ofNullable((Boolean) request.getAttribute("missing.fields")).orElse(Boolean.FALSE);
        boolean passwordProblem = Optional.ofNullable((Boolean) request.getAttribute("password.problem")).orElse(Boolean.FALSE);

        EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        String lastName = Optional.ofNullable(eperson.getLastName()).orElse("");
        String firstName = Optional.ofNullable(eperson.getFirstName()).orElse("");
        String phone = Optional.ofNullable(epersonService.getMetadata(eperson, "phone")).orElse("");
        String language = Optional.ofNullable(epersonService.getMetadata(eperson, "language")).orElse("");

        Map<Integer, List<ChairEntity>> chairList = facultyService.findAll(dspaceContext).stream().collect(Collectors.toMap(FacultyEntity::getId, FacultyEntity::getChairs));
        model.addObject("lastName", lastName);
        model.addObject("firstName", firstName);
        model.addObject("isAuthorLocalized", authorsService.isAuthorLocalizationPresent(String.format("%s, %s", lastName, firstName)));
        model.addObject("orcid", authorsService.getAuthorLocalization(String.format("%s, %s", lastName, firstName)).getOrcid());
        model.addObject("phone", phone);
        model.addObject("language", language);
        model.addObject("position", eperson.getPosition());
        model.addObject("chair", eperson.getChair());
        model.addObject("facultyList", facultyService.findAll(dspaceContext));
        model.addObject("chairListJson", new ObjectMapper().writeValueAsString(chairList));

        model.addObject("supportedLocales", I18nUtil.getSupportedLocales());
        model.addObject("sessionLocale", UIUtil.getSessionLocale(request));
        model.addObject("passwordProblem", passwordProblem);
        model.addObject("missingFields", missingFields);
        model.setViewName("profile");
        return model;
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ModelAndView updateProfile(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException, AuthorizeException {
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson eperson = dspaceContext.getCurrentUser();
        boolean settingPassword = (!eperson.getRequireCertificate() && !StringUtils.isEmpty(request.getParameter("password")));

        boolean checkUserData = epersonService.updateUserProfile(dspaceContext, eperson, request);

        if (!checkUserData) {
            request.setAttribute("missing.fields", Boolean.TRUE);
        }

        if (checkUserData && settingPassword) {
            checkUserData = epersonService.confirmAndSetPassword(eperson, request);
            if (!checkUserData) {
                request.setAttribute("password.problem", Boolean.TRUE);
            }
        }

        if (checkUserData) {
            Optional<String> orcid = Optional.ofNullable(request.getParameter("orcid")).map(param -> param.replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", ""));
            if (orcid.isPresent()) {
                AuthorLocalization authorLocalization = authorsService.getAuthorLocalization(String.format("%s, %s", eperson.getLastName(), eperson.getFirstName()));
                authorLocalization.setOrcid(orcid.get());
                authorsService.updateAuthorOrcid(authorLocalization);
            }
            personService.update(dspaceContext, eperson);
            request.setAttribute("password.updated", settingPassword);
            JSPManager.showJSP(request, response, "/register/profile-updated.jsp");

            dspaceContext.complete();
        } else {
            request.setAttribute("eperson", eperson);
            JSPManager.showJSP(request, response, "/register/edit-profile.jsp");
        }

        return model;
    }
}

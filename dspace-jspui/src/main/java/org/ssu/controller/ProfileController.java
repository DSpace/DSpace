package org.ssu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.AuthorLocalization;
import org.ssu.entity.ChairEntity;
import org.ssu.entity.EssuirEperson;
import org.ssu.entity.FacultyEntity;
import org.ssu.service.AuthorsService;
import org.ssu.service.EpersonService;
import org.ssu.service.FacultyService;
import org.ssu.service.localization.AuthorsCache;

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
    private AuthorsCache authorsCache;

    @Resource
    private EpersonService ePersonService;

    @Resource
    private FacultyService facultyService;

    @RequestMapping("/profile")
    public ModelAndView profilePage(ModelAndView model , HttpServletRequest request, HttpServletResponse response) throws SQLException, JsonProcessingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson eperson = dspaceContext.getCurrentUser();
        EssuirEperson currentUser = ePersonService.extendEpersonInformation(eperson);

        boolean missingFields = Optional.ofNullable((Boolean) request.getAttribute("missing.fields")).orElse(Boolean.FALSE);
        boolean passwordProblem = Optional.ofNullable((Boolean) request.getAttribute("password.problem")).orElse(Boolean.FALSE);

        EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        String lastName = Optional.ofNullable(currentUser.getLastName()).orElse("");
        String firstName = Optional.ofNullable(currentUser.getFirstName()).orElse("");
        String phone = Optional.ofNullable(epersonService.getMetadata(eperson, "phone")).orElse("");
        String language = Optional.ofNullable(epersonService.getMetadata(eperson, "language")).orElse("");

        Map<Integer, List<ChairEntity>> chairList = facultyService.getFacultyList().stream().collect(Collectors.toMap(FacultyEntity::getId, FacultyEntity::getChairs));
        model.addObject("lastName", lastName);
        model.addObject("firstName", firstName);
        model.addObject("isAuthorLocalized", authorsCache.isAuthorLocalizationPresent(String.format("%s, %s", lastName, firstName)));
        model.addObject("orcid", authorsCache.getAuthorLocalization(String.format("%s, %s", lastName, firstName)).getOrcid());
        model.addObject("phone", phone);
        model.addObject("language", language);
        model.addObject("position", currentUser.getPosition());
        model.addObject("chair", currentUser.getChairEntity());
        model.addObject("facultyList", facultyService.getFacultyList());
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

        boolean checkUserData = updateUserProfile(dspaceContext, eperson, request);

        if (!checkUserData) {
            request.setAttribute("missing.fields", Boolean.TRUE);
        }

        if (checkUserData && settingPassword) {
            checkUserData = confirmAndSetPassword(eperson, request);
            if (!checkUserData) {
                request.setAttribute("password.problem", Boolean.TRUE);
            }
        }

        if (checkUserData) {
            Optional<String> orcid = Optional.ofNullable(request.getParameter("orcid")).map(param -> param.replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", ""));
            if(orcid.isPresent()) {
                AuthorLocalization authorLocalization = authorsCache.getAuthorLocalization(String.format("%s, %s", eperson.getLastName(), eperson.getFirstName()));
                authorLocalization.setOrcid(orcid.get());
                authorsService.updateAuthorOrcid(authorLocalization);
            }
            personService.update(dspaceContext, eperson);
            request.setAttribute("password.updated", settingPassword);
            JSPManager.showJSP(request, response,"/register/profile-updated.jsp");

            dspaceContext.complete();
        }
        else {
            request.setAttribute("eperson", eperson);
            JSPManager.showJSP(request, response, "/register/edit-profile.jsp");
        }

        return model;
    }

    private boolean updateUserProfile(Context context, EPerson eperson, HttpServletRequest request) throws SQLException {
        String lastName = request.getParameter("last_name");
        String firstName = request.getParameter("first_name");
        String phone = request.getParameter("phone");
        String language = request.getParameter("language");
        eperson.setFirstName(context, firstName);
        eperson.setLastName(context, lastName);
        personService.setMetadataSingleValue(context, eperson, "eperson" , "phone", null, null, phone);
        eperson.setLanguage(context, language);

        return (!StringUtils.isEmpty(lastName) && !StringUtils.isEmpty(firstName));
    }

    private boolean confirmAndSetPassword(EPerson eperson, HttpServletRequest request) {
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("password_confirm");

        if ((password == null) || (password.length() < 6) || !password.equals(passwordConfirm))
        {
            return false;
        } else {
            personService.setPassword(eperson, password);
            return true;
        }
    }
}

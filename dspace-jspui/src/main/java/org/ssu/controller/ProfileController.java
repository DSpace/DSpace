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
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.FacultyService;
import org.dspace.eperson.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
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
    @Resource
    private AuthorsService authorsService;
    @Resource
    private EpersonService epersonService;

    private final transient GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private final transient EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();
    private final transient FacultyService facultyService = EPersonServiceFactory.getInstance().getFacultyService();

    @RequestMapping(value = "/dspace-admin/edit-epeople", method = RequestMethod.GET)
    public ModelAndView editUserProfileByAdministrator(HttpServletRequest request) {
        return redirectUserToDspaceHandler(request);
    }

    @RequestMapping(value = "/dspace-admin/edit-epeople", method = RequestMethod.POST)
    public ModelAndView editUserProfileByAdministratorPostEndpoint(HttpServletRequest request) throws SQLException, JsonProcessingException, AuthorizeException {
        String button = UIUtil.getSubmitButton(request, "submit");
        ModelAndView model;
        switch (button) {
            case "submit_add":
                model = handleNewEpersonRequest(request);
                break;
            case "submit_edit":
                model = handleEditEpersonRequest(request);
                break;
            case "submit_save":
                model = handleSaveEpersonRequest(request);
                break;
            default:
                model = redirectUserToDspaceHandler(request);
        }
        return model;
    }

    private ModelAndView redirectUserToDspaceHandler(HttpServletRequest request) {
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT);
        return new ModelAndView("redirect:/dspace-admin/edit-epeople-dspace");
    }

    private ModelAndView handleNewEpersonRequest(HttpServletRequest request) throws SQLException, JsonProcessingException, AuthorizeException {
        ModelAndView model = new ModelAndView();
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson e = personService.create(dspaceContext);
        e.setEmail("newuser" + e.getID());
        personService.update(dspaceContext, e);
        model = fillEditUserForm(request, model, e);
        model.addObject("isNewUser", true);
        model.setViewName("edit-user");
        dspaceContext.complete();
        return model;
    }

    private ModelAndView handleEditEpersonRequest(HttpServletRequest request) throws SQLException, JsonProcessingException, AuthorizeException {
        ModelAndView model = new ModelAndView();
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson eperson = personService.find(dspaceContext, UIUtil.getUUIDParameter(request, "eperson_id"));
        if (eperson != null) {
            fillEditUserForm(request, model, eperson);
            List<Group> groupMemberships = groupService.allMemberGroups(dspaceContext, eperson);
            model.addObject("groupMemberships", groupMemberships);
            model.setViewName("edit-user");
        }
        dspaceContext.complete();
        return model;
    }

    private ModelAndView handleSaveEpersonRequest(HttpServletRequest request) throws SQLException, JsonProcessingException, AuthorizeException {
        ModelAndView model = new ModelAndView();
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson ePerson = personService.find(dspaceContext, UIUtil.getUUIDParameter(request, "eperson_id"));
        if (!saveUser(dspaceContext, request, ePerson)) {
            model.addObject("emailExists", true);
            model = fillEditUserForm(request, model, ePerson);
            model.setViewName("edit-user");
        } else {
            model = new ModelAndView("redirect:/dspace-admin/edit-epeople");
        }
        dspaceContext.complete();
        return model;
    }

    private boolean saveUser(Context context, HttpServletRequest request, EPerson eperson) throws SQLException, AuthorizeException {
        boolean checkUserData = epersonService.updateUserProfile(context, eperson, request);
        String oldEmail = eperson.getEmail();
        String newEmail = request.getParameter("email").trim();
        if (!newEmail.equals(oldEmail) && personService.findByEmail(context, newEmail) != null) {
            return false;
        }
        if (!newEmail.equals(oldEmail)) {
            eperson.setEmail(newEmail);
        }
        if (checkUserData) {
            personService.update(context, eperson);
        }
        return true;
    }

    private ModelAndView fillEditUserForm(HttpServletRequest request, ModelAndView model, EPerson eperson) throws SQLException, JsonProcessingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        String lastName = Optional.ofNullable(eperson.getLastName()).orElse("");
        String firstName = Optional.ofNullable(eperson.getFirstName()).orElse("");
        String phone = Optional.ofNullable(epersonService.getMetadata(eperson, "phone")).orElse("");
        String language = Optional.ofNullable(epersonService.getMetadata(eperson, "language")).orElse("");

        model.addObject("email", eperson.getEmail());
        model.addObject("epersonId", eperson.getID());
        model.addObject("lastName", lastName);
        model.addObject("firstName", firstName);
        model.addObject("phone", phone);
        model.addObject("language", language);
        model.addObject("position", eperson.getPosition());
        model.addObject("chair", eperson.getChair());

        model.addObject("sessionLocale", UIUtil.getSessionLocale(request));
        Map<Integer, List<ChairEntity>> chairList = facultyService.findAll(dspaceContext).stream().collect(Collectors.toMap(FacultyEntity::getId, FacultyEntity::getChairs));
        model.addObject("facultyList", facultyService.findAll(dspaceContext));
        model.addObject("chairListJson", new ObjectMapper().writeValueAsString(chairList));

        model.addObject("supportedLocales", I18nUtil.getSupportedLocales());
        return model;
    }

    @RequestMapping("/profile")
    public ModelAndView profilePage(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException, JsonProcessingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson eperson = dspaceContext.getCurrentUser();

        boolean missingFields = Optional.ofNullable((Boolean) request.getAttribute("missing.fields")).orElse(Boolean.FALSE);
        boolean passwordProblem = Optional.ofNullable((Boolean) request.getAttribute("password.problem")).orElse(Boolean.FALSE);
        model = fillEditUserForm(request, model, eperson);
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

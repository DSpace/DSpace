package org.ssu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.ChairEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.FacultyEntity;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.FacultyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class RegisterController {
    private static final org.apache.log4j.Logger log = Logger.getLogger(RegisterController.class);
    private final transient AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    private final transient AccountService accountService = EPersonServiceFactory.getInstance().getAccountService();
    protected transient EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();
    private FacultyService facultyService = EPersonServiceFactory.getInstance().getFacultyService();

    @Resource
    private EpersonService epersonService;

    private Function<Context, Map<Integer, List<ChairEntity>>> getChairList = (context) -> {
        try {
            return facultyService.findAll(context).stream().collect(Collectors.toMap(FacultyEntity::getId, FacultyEntity::getChairs));
        } catch (SQLException e) {
            log.error("Error during collecting chairlist in RegistrationConrtroller!");
            e.printStackTrace();
        }
        return null;
    };

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerPost(HttpServletRequest request) throws SQLException, IOException, AuthorizeException {
        int step = UIUtil.getIntParameter(request, "step");
        Context context = UIUtil.obtainContext(request);
        if (step == 2) {
            return processPersonalInfo(context, request);
        } else {
            request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT);
            return new ModelAndView("redirect:/register-dspace");
        }
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView register(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        Context context = UIUtil.obtainContext(request);
        String token = request.getParameter("token");
        if (token == null) {
            log.info("Empty token! Make redirect to dspace servlet.");
            return new ModelAndView("redirect:/register-dspace");
        } else {
            String email = accountService.getEmail(context, token);

            if (email != null) {
                ModelAndView model = new ModelAndView();
                model.addObject("facultyList", facultyService.findAll(context));
                model.addObject("chairListJson", new ObjectMapper().writeValueAsString(getChairList.apply(context)));
                model.addObject("isAllFieldsFilled", true);
                model.addObject("isPasswordOk", true);
                model.addObject("supportedLocales", I18nUtil.getSupportedLocales());
                model.addObject("sessionLocale", UIUtil.getSessionLocale(request));
                model.addObject("token", token);
                model.addObject("chairId", -1);
                model.addObject("facultyId", -1);
                model.setViewName("register");
                return model;
            } else {
                return new ModelAndView("redirect:/register-dspace");
            }
        }
    }

    private Optional<EPerson> fetchEpersonDataFromRequest(Context context, HttpServletRequest request) throws SQLException, AuthorizeException {
        String token = request.getParameter("token");
        String email = Optional.ofNullable(accountService.getEmail(context, token)).orElse(request.getParameter("email"));

        if (email == null) {
            return Optional.empty();
        }

        EPerson eperson;
        context.turnOffAuthorisationSystem();
        eperson = personService.create(context);
        eperson.setEmail(email);
        personService.update(context, eperson);
        context.restoreAuthSystemState();
        return Optional.of(eperson);
    }

    private ModelAndView processPersonalInfo(Context context, HttpServletRequest request) throws IOException, SQLException, AuthorizeException {
        ModelAndView model = new ModelAndView();
        String token = request.getParameter("token");
        Optional<EPerson> epersonFetchedData = fetchEpersonDataFromRequest(context, request);
        if(!epersonFetchedData.isPresent()) {
            log.info(LogManager.getHeader(context, "invalid_token", "token=" + token));
            model.setViewName("invalid-token");
            return model;
        }
        EPerson eperson = epersonFetchedData.get();
        context.setCurrentUser(eperson);
        boolean infoOK = epersonService.updateUserProfile(context, eperson, request);
        eperson.setCanLogIn(true);
        eperson.setSelfRegistered(true);
        authenticationService.initEPerson(context, request, eperson);

        boolean passwordOK = true;
        if (!eperson.getRequireCertificate() && authenticationService.allowSetPassword(context, request, eperson.getEmail())) {
            passwordOK = epersonService.confirmAndSetPassword(eperson, request);
        }

        if (infoOK && passwordOK) {
            log.info(LogManager.getHeader(context, "usedtoken_register", "email=" + eperson.getEmail()));
            if (token != null) {
                accountService.deleteToken(context, token);
            }
            personService.update(context, eperson);
            model.addObject("name", eperson.getFirstName());
            model.setViewName("registered");
            context.complete();
        } else {
            Integer facultyId = Optional.ofNullable(request.getParameter("faculty")).map(Integer::valueOf).orElse(-1);
            String phone = Optional.ofNullable(request.getParameter("phone")).orElse("");

            model.addObject("facultyList", facultyService.findAll(context));
            model.addObject("chairListJson", new ObjectMapper().writeValueAsString(getChairList.apply(context)));

            model.addObject("supportedLocales", I18nUtil.getSupportedLocales());
            model.addObject("sessionLocale", UIUtil.getSessionLocale(request));
            model.addObject("token", token);
            model.addObject("isAllFieldsFilled", infoOK);
            model.addObject("isPasswordOk", passwordOK);
            model.addObject("lastName", eperson.getLastName());
            model.addObject("firstName", eperson.getFirstName());
            model.addObject("chairId", eperson.getChairId());
            model.addObject("language", eperson.getLanguage());
            model.addObject("position", eperson.getPosition());
            model.addObject("chair", eperson.getChair());
            model.addObject("phone", phone);
            model.addObject("facultyId", facultyId);
            model.setViewName("register");

            context.abort();
        }
        return model;
    }
}

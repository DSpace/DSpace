package org.ssu.controller;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.AuthorLocalization;
import org.ssu.service.AuthorsService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class AdminController {
    @Resource
    private AuthorsService authorsService;

    private final transient EPersonService personService = EPersonServiceFactory.getInstance().getEPersonService();

    @RequestMapping(value = "/autocomplete", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String autocompleteAuthors(HttpServletRequest request) {
        Locale currentLocale = Locale.forLanguageTag(Optional.ofNullable(request.getParameter("locale")).orElse("uk"));
        List<Locale> locales = Arrays.asList(currentLocale, Locale.ENGLISH, Locale.forLanguageTag("ru"), Locale.forLanguageTag("uk"));

        List<AuthorLocalization> authorsData = authorsService.getAllAuthors(Optional.ofNullable(request.getParameter("q")));
        Function<AuthorLocalization, String> authorLocalizationMapping = (author) -> locales.stream()
                .map(locale -> String.format("%s|%s", author.getSurname(locale), author.getInitials(locale)))
                .collect(Collectors.joining("|"));
        return authorsData.stream()
                .map(authorLocalizationMapping)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @RequestMapping("/authors/list")
    public ModelAndView autofillPage(ModelAndView model, HttpServletRequest request) {
        Optional<String> startsWith = Optional.ofNullable(request.getParameter("startsWith"));
        model.addObject("authors", authorsService.getAllAuthors(startsWith));
        model.setViewName("autofill");
        return model;
    }

    @RequestMapping(value = "/authors/edit", method = RequestMethod.GET)
    public ModelAndView authorEditPage(ModelAndView model, HttpServletRequest request) throws SQLException, AuthorizeException {
        Optional<UUID> authorUuid = Optional.ofNullable(request.getParameter("author_uuid")).map(UUID::fromString);
        Context dspaceContext = UIUtil.obtainContext(request);
        if(authorUuid.isPresent()) {
            Optional<AuthorLocalization> author = authorsService.getAuthor(authorUuid.get());
            if(author.isPresent())
                model.addObject("author", author.get());
            EPerson ePerson = personService.find(dspaceContext, authorUuid.get());
            model.addObject("eperson_attached", ePerson != null);
            if(ePerson != null)
                model.addObject("eperson_string", ePerson.getLastName() + " " + ePerson.getFirstName() + " (" + ePerson.getEmail() + ")");
        }
        model.setViewName("author-edit");
        return model;
    }

    @RequestMapping(value = "/authors/edit", method = RequestMethod.POST)
    public ModelAndView saveAuthorData(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException {
        AuthorLocalization authorLocalization = new AuthorLocalization();
        String surnameEnglish = request.getParameter("surnameEn");
        String initialsEnglish = request.getParameter("initialsEn");
        String surnameRussian = request.getParameter("surnameRu");
        String initialsRussian = request.getParameter("initialsRu");
        String surnameUkrainian = request.getParameter("surnameUk");
        String initialsUkrainian = request.getParameter("initialsUk");
        String orcid = Optional.ofNullable(request.getParameter("orcid")).map(param -> param.replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", "")).orElse("");
        String epersonId = request.getParameter("eperson_id");
        String uuid = request.getParameter("uuid");
        UUID authorUuid;
        if (StringUtils.isNotEmpty(epersonId)){
            if (uuid != null && !uuid.equals(""))
                authorsService.removeAuthor(UUID.fromString(uuid));
            Optional<AuthorLocalization> authorUuidOldEpersonOwner = authorsService.getAuthor(UUID.fromString(epersonId));
            if (authorUuidOldEpersonOwner.isPresent() && !(epersonId.equals(uuid))) {
                authorsService.removeAuthor(UUID.fromString(epersonId));
                AuthorLocalization author = authorUuidOldEpersonOwner.get();
                author.setUuid(UUID.randomUUID());
                authorsService.updateAuthorData(author);
            }
            authorUuid = UUID.fromString(epersonId);
        } else {
            authorUuid = StringUtils.isNotEmpty(uuid) ? UUID.fromString(uuid) : UUID.randomUUID();
        }
        authorLocalization.addAuthorData(surnameEnglish, initialsEnglish, Locale.ENGLISH);
        authorLocalization.addAuthorData(surnameRussian, initialsRussian, Locale.forLanguageTag("ru"));
        authorLocalization.addAuthorData(surnameUkrainian, initialsUkrainian, Locale.forLanguageTag("uk"));
        authorLocalization.setOrcid(orcid);
        authorLocalization.setUuid(authorUuid);

        boolean allFieldsFilled = StringUtils.isNotEmpty(surnameEnglish) &&
                StringUtils.isNotEmpty(surnameRussian) &&
                StringUtils.isNotEmpty(surnameUkrainian) &&
                StringUtils.isNotEmpty(initialsEnglish) &&
                StringUtils.isNotEmpty(initialsRussian) &&
                StringUtils.isNotEmpty(initialsUkrainian);

        if (allFieldsFilled) {
            authorsService.updateAuthorData(authorLocalization);
            model.addObject("message", "Author data successfully updated.");
            model.addObject("messageType", "success");
        } else {
            model.addObject("message", "Please fill all fields.");
            model.addObject("messageType", "danger");
        }
        Context dspaceContext = UIUtil.obtainContext(request);
        EPerson ePerson = personService.find(dspaceContext, authorUuid);
        model.addObject("eperson_attached", ePerson != null);
        if(ePerson != null)
            model.addObject("eperson_string", ePerson.getLastName() + " " + ePerson.getFirstName() + " (" + ePerson.getEmail() + ")");
        model.addObject("author", authorLocalization);
        model.addObject("hasMessage", true);
        model.setViewName("author-edit");
        return model;
    }

    @RequestMapping(value = "/authors/delete", method = RequestMethod.GET)
    public String deleteAuthorLocalization(HttpServletRequest request) {
        Optional<UUID> authorUuid = Optional.ofNullable(request.getParameter("uuid")).map(UUID::fromString);
        authorUuid.ifPresent(uuid -> authorsService.removeAuthor(uuid));
        return "redirect:/authors/list";
    }
}

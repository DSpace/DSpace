package org.ssu.controller;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class AdminController {
    @Resource
    private AuthorsService authorsService;

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
    public ModelAndView authorEditPage(ModelAndView model, HttpServletRequest request) {
        Optional<UUID> authorUuid = Optional.ofNullable(request.getParameter("author_uuid")).map(UUID::fromString);
        if(authorUuid.isPresent()) {
            Optional<AuthorLocalization> author = authorsService.getAuthor(authorUuid.get());
            if(author.isPresent())
                model.addObject("author", author.get());
        }
        model.setViewName("author-edit");
        return model;
    }

    @RequestMapping(value = "/authors/edit", method = RequestMethod.POST)
    public ModelAndView saveAuthorData(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {
        AuthorLocalization authorLocalization = new AuthorLocalization();
        String surnameEnglish = request.getParameter("surnameEn");
        String initialsEnglish = request.getParameter("initialsEn");
        String surnameRussian = request.getParameter("surnameRu");
        String initialsRussian = request.getParameter("initialsRu");
        String surnameUkrainian = request.getParameter("surnameUk");
        String initialsUkrainian = request.getParameter("initialsUk");
        String orcid = Optional.ofNullable(request.getParameter("orcid")).map(param -> param.replaceAll("https://", "").replaceAll("http://", "").replaceAll("orcid.org/", "")).orElse("");
        UUID authorUuid = Optional.ofNullable(request.getParameter("uuid")).filter(uuid -> !uuid.isEmpty()).map(UUID::fromString).orElse(UUID.randomUUID());

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

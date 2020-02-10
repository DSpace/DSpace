package org.ssu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.jooq.lambda.Seq;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.AuthorLocalization;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.AuthorsService;
import org.ssu.service.ItemService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping
public class AdminController {
    @Resource
    private AuthorsService authorsService;
    @Resource
    private ItemService itemService;

    @RequestMapping(value = "/export-report", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String generateReportForExport(HttpServletRequest request) throws SQLException, JsonProcessingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        System.out.println("started");
        Long start = System.currentTimeMillis();
        List<ItemResponse> items = Seq.seq(Lists.newArrayList(itemService.findAll(dspaceContext)))
                .parallel()
                .map(item -> itemService.fetchItemresponseDataForItem(item, Locale.forLanguageTag("uk")))
                .toList();


        System.out.println("===============================");
        System.out.println("executed!");
        System.out.println(System.currentTimeMillis() - start);
        System.out.println("===============================");

        return items.toString();
    }

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
    public ModelAndView autofillPage(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {
        Optional<String> startsWith = Optional.ofNullable(request.getParameter("startsWith"));
        model.addObject("authors", authorsService.getAllAuthors(startsWith));
        model.setViewName("autofill");
        return model;
    }

    @RequestMapping(value = "/authors/edit", method = RequestMethod.GET)
    public ModelAndView authorEditPage(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {
        Optional<String> author = Optional.ofNullable(request.getParameter("author"));
        model.addObject("author", authorsService.getAuthorLocalization(author));
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

        authorLocalization.addAuthorData(surnameEnglish, initialsEnglish, Locale.ENGLISH);
        authorLocalization.addAuthorData(surnameRussian, initialsRussian, Locale.forLanguageTag("ru"));
        authorLocalization.addAuthorData(surnameUkrainian, initialsUkrainian, Locale.forLanguageTag("uk"));
        authorLocalization.setOrcid(orcid);

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
    public String deleteAuthorLocalization(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {
        String authorData = request.getParameter("author");
        authorsService.removeAuthorData(authorData);
        return "redirect:/authors/list";
    }
}

package org.ssu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.EpersonService;
import org.ssu.service.ReportService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class ReportController {
    @Resource
    private EpersonService ePersonService;

    @Resource
    private ReportService reportService;
    private DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @RequestMapping("/report")
    public ModelAndView homepage(ModelAndView model) {
        model.setViewName("report-homepage");
        return model;
    }

    @RequestMapping("/report/recent-person")
    public ModelAndView recentRegistrationsPersons(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException {
        Context context = UIUtil.obtainContext(request);
        Integer limit = Optional.ofNullable(request.getParameter("limit")).map(Integer::valueOf).orElse(20);

        model.addObject("users", ePersonService.getLatestRegisteredUsers(context, limit));
        model.addObject("limit", limit);
        model.setViewName("report-recent-registrations");
        return model;
    }

    @RequestMapping("/report/general")
    public ModelAndView generalStatistics(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException {
        model.setViewName("report-general-statistics");
        return model;
    }

    @RequestMapping(value = "/report/speciality", method = RequestMethod.GET)
    public ModelAndView getSpecialityStatistics(ModelAndView model, HttpServletRequest request, HttpServletResponse response) {
        model.setViewName("report-speciality");
        return model;
    }

    @RequestMapping(value = "/report/person", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getPersonList(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) throws SQLException, IOException {
        Context context = UIUtil.obtainContext(request);
        LocalDate fromDate = LocalDate.parse(from, format);
        LocalDate toDate = LocalDate.parse(to, format);
        return new ObjectMapper().writeValueAsString(reportService.getUsersSubmissionCountBetweenDates(context, fromDate, toDate));
    }

    @RequestMapping(value = "/report/speciality-data", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getSpecialityStatisticsData(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) throws IOException, SQLException {
        Context context = UIUtil.obtainContext(request);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate fromDate = LocalDate.parse(from, format);
        LocalDate toDate = LocalDate.parse(to, format);
        return new ObjectMapper().writeValueAsString(reportService.getSpecialitySubmissionCountBetweenDates(context, fromDate, toDate));
    }

    @RequestMapping(value = "/report/itemUploadingReport", method = RequestMethod.GET)
    public ModelAndView getUploadingStatisticsByDepositor(@RequestParam(value = "from", defaultValue = "01.01.2010") String from,
                                                          @RequestParam(value = "to", defaultValue = "01.01.2100") String to,
                                                          @RequestParam("faculty") Optional<String> faculty,
                                                          @RequestParam("chair") Optional<String> chair,
                                                          @RequestParam("person") Optional<String> person,
                                                          HttpServletRequest request,
                                                          ModelAndView model) throws SQLException, IOException {
        Context context = UIUtil.obtainContext(request);
        List<ItemResponse> itemsInSpeciality = new ArrayList<>();
        LocalDate fromDate = LocalDate.parse(from, format);
        LocalDate toDate = LocalDate.parse(to, format);


        if(faculty.isPresent())
            itemsInSpeciality = reportService.getUploadedItemsByFacultyName(context, faculty.get(), fromDate, toDate);

        if(chair.isPresent())
            itemsInSpeciality = reportService.getUploadedItemsByChairName(context, chair.get(), fromDate, toDate);

        if(person.isPresent()) {
            String name = person.get();
            String email = name;
            if(name.contains("(")) {
                email = name.substring(name.indexOf('(') + 1, name.indexOf(')'));
            }
            itemsInSpeciality = reportService.getUploadedItemsByPersonEmail(context, email, fromDate, toDate);
        }


        List<String> itemLinks = itemsInSpeciality
                .stream()
                .sorted(Comparator.comparing(ItemResponse::getTitle))
                .map(item -> String.format("<a href = \"%s\">%s</a>", item.getHandle(), item.getTitle()))
                .collect(Collectors.toList());

        model.setViewName("detailed-report");
        model.addObject("data", itemLinks);
        model.addObject("depositor", Stream.of(request.getParameter("depositor").split("//")).reduce((a, b) -> b).orElse("--"));
        return model;
    }

    @RequestMapping(value = "/report/detailedReport", method = RequestMethod.GET)
    public ModelAndView getDetailedReportForDepositor(@RequestParam(value = "from", defaultValue = "01.01.2010") String from, @RequestParam(value = "to", defaultValue = "01.01.2100") String to,@RequestParam("depositor") String depositor, ModelAndView model, HttpServletRequest request) throws SQLException, IOException {
        Context context = UIUtil.obtainContext(request);
        List<Item> itemsInSpeciality;
        LocalDate fromDate = LocalDate.parse(from, format);
        LocalDate toDate = LocalDate.parse(to, format);
        if(depositor.equals("-")) {
            itemsInSpeciality = reportService.getBacheoursWithoutSpeciality(context);
        } else {
            itemsInSpeciality = reportService.getItemsInSpeciality(context, depositor, fromDate, toDate);
        }

        List<String> itemLinks = itemsInSpeciality
                .stream()
                .filter(item -> Objects.nonNull(item.getName()))
                .sorted(Comparator.comparing(Item::getName))
                .map(item -> String.format("<a href = \"/handle/%s\">%s</a>", item.getHandle(), item.getName()))
                .collect(Collectors.toList());

        model.setViewName("detailed-report");

        model.addObject("data", itemLinks);
        return model;
    }
}

package ua.edu.sumdu.essuir.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ua.edu.sumdu.essuir.entity.Faculty;
import ua.edu.sumdu.essuir.entity.Metadatavalue;
import ua.edu.sumdu.essuir.repository.FacultyRepository;
import ua.edu.sumdu.essuir.service.ReportService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/statistics")
public class ReportController {
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    @Resource
    private ReportService reportService;
    @Resource
    private FacultyRepository facultyRepository;


    @RequestMapping(value = "/person", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getPersonList(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) {
        try {
            if (AuthorizeManager.isAdmin(UIUtil.obtainContext(request))) {
                LocalDate fromDate = LocalDate.parse(from, format);
                LocalDate toDate = LocalDate.parse(to, format);
                return generateResponse(reportService.getUsersSubmissionCountBetweenDates(fromDate, toDate));
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return new JSONArray().toString();
    }

    @RequestMapping(value = "/speciality", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getSpecialityStatistics(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) {
        try {
            if (AuthorizeManager.isAdmin(UIUtil.obtainContext(request))) {
                LocalDate fromDate = LocalDate.parse(from, format);
                LocalDate toDate = LocalDate.parse(to, format);
                return generateResponse(reportService.getSpecialitySubmissionCountBetweenDates(fromDate, toDate));
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return new JSONArray().toString();
    }

    private String generateResponse(Map<String, Faculty>  submissions) throws JsonProcessingException {
        ArrayList<Faculty> faculties = new ArrayList<>(submissions.values());
        Collections.sort(faculties, new Comparator<Faculty>() {
            @Override
            public int compare(Faculty o1, Faculty o2) {
                return o1.getFacultyName().compareTo(o2.getFacultyName());
            }
        });
        return new ObjectMapper().writeValueAsString(faculties);
    }

    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public String getTotalReport(ModelMap model) {
        return "report";
    }

    @RequestMapping(value = "/facultylist", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getFacultyList() throws JsonProcessingException {
        try {
            return new ObjectMapper().writeValueAsString(facultyRepository.findAll());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(value = "/detailedReport", method = RequestMethod.GET)
    public ModelAndView getDetailedReportForDepositor(@RequestParam("depositor") String depositor, ModelAndView model) {
        Collection<Map<Integer, List<Metadatavalue>>> itemsInSpeciality;

        if(depositor.equals("-")) {
            itemsInSpeciality = reportService.getBacheoursWithoutSpeciality().values();
        } else {
            itemsInSpeciality = reportService.getItemsInSpeciality(depositor).values();
        }


        BiFunction<Map<Integer, List<Metadatavalue>>, Integer, String> extractItemDataByFieldId = (metadata, fieldId) -> metadata
                .getOrDefault(fieldId, new ArrayList<>())
                .stream()
                .filter(item -> item.getPlace().equals(1))
                .map(Metadatavalue::getTextValue)
                .findAny()
                .orElse("");


        Function<Map<Integer, List<Metadatavalue>>, Pair<String, String>> extractItemNameAndLink = (metadata) ->
                Pair.of(extractItemDataByFieldId.apply(metadata, 64), extractItemDataByFieldId.apply(metadata, 25));

        List<String> collect = itemsInSpeciality
                .stream()
                .map(item -> extractItemNameAndLink.apply(item))
                .map(item -> String.format("<a href = \"%s\">%s</a>", item.getRight(), item.getLeft()))
                .collect(Collectors.toList());

        model.setViewName("detailed-report");

        model.addObject("data", collect);
        return model;
    }
}


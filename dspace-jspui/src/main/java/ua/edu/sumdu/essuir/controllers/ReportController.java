package ua.edu.sumdu.essuir.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ua.edu.sumdu.essuir.entity.Faculty;
import ua.edu.sumdu.essuir.service.ReportService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

@Controller
@RequestMapping(value = "/statistics")
public class ReportController {
    private static final DateTimeFormatter format = DateTimeFormat.forPattern("dd.MM.YYYY");
    @Resource
    private ReportService reportService;

    @RequestMapping(value = "/person", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getPersonList(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) {
        try {
            if (AuthorizeManager.isAdmin(UIUtil.obtainContext(request))) {
                return generateResponseByDates(LocalDate.parse(from, format), LocalDate.parse(to, format));
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return new JSONArray().toString();
    }

    private String generateResponseByDates(LocalDate from, LocalDate to) throws JsonProcessingException {
        ArrayList<Faculty> faculties = new ArrayList<>(reportService.getUsersSubmissionCountBetweenDates(from, to).values());
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
}

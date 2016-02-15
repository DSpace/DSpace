package ua.edu.sumdu.essuir.controllers;


import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ua.edu.sumdu.essuir.entity.Faculty;
import ua.edu.sumdu.essuir.entity.Person;
import ua.edu.sumdu.essuir.service.ReportService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping(value = "/statistics")
public class ReportController {
    private static final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    @Resource
    private ReportService reportService;

    @RequestMapping(value = "/person", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getPersonList(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) {
        try {
            if (AuthorizeManager.isAdmin(UIUtil.obtainContext(request))) {
                return generateResponceByDates(parseDate(from), parseDate(to)).toString();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new JSONArray().toString();
    }

    private Date parseDate(String date) {
        Date result = new Date();
        try {
            result = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private JSONArray generateResponceByDates(Date from, Date to) {
        List<Person> persons = reportService.getPersonList();
        HashMap<String, Faculty> faculties = new HashMap<>();

        JSONArray result = new JSONArray();
        for (Person person : persons) {
            if (!faculties.containsKey(person.getFaculty())) {
                faculties.put(person.getFaculty(), new Faculty(person.getFaculty()));
            }
            faculties.get(person.getFaculty()).addPerson(person);
        }

        for (Faculty faculty : faculties.values()) {
            result.add(faculty.generateJSONbyDate(from, to));
        }
        return result;
    }

    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public String getTotalReport(ModelMap model) {
        return "report";
    }
}

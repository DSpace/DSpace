package org.ssu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.FacultyEntity;
import org.ssu.entity.jooq.Faculty;
import org.ssu.entity.response.ItemDepositorResponse;
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

@Controller
public class ReportController {
    @Resource
    private EpersonService ePersonService;

    @Resource
    private ReportService reportService;

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

    @RequestMapping(value = "/report/person", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String getPersonList(@RequestParam("from") String from, @RequestParam("to") String to, HttpServletRequest request) throws SQLException, IOException {
        Context context = UIUtil.obtainContext(request);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate fromDate = LocalDate.parse(from, format);
        LocalDate toDate = LocalDate.parse(to, format);
        return new ObjectMapper().writeValueAsString(reportService.getUsersSubmissionCountBetweenDates(context, fromDate, toDate));
    }

}

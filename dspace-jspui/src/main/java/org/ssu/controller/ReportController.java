package org.ssu.controller;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.service.EpersonService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.Optional;

@Controller
public class ReportController {
    @Resource
    private EpersonService ePersonService;

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
}

package org.ssu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ReportController {
    @RequestMapping("/report")
    public ModelAndView homepage(ModelAndView model) {
        model.setViewName("report-homepage");
        return model;
    }
}

package ua.edu.sumdu.essuir.controllers;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import ua.edu.sumdu.essuir.service.GeneralStatisticsService;
import ua.edu.sumdu.essuir.service.ScheduledTasks;
import ua.edu.sumdu.essuir.statistics.EssuirStatistics;
import ua.edu.sumdu.essuir.statistics.StatisticData;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/stat")
public class TotalStatisticsController {
    private static Logger log = Logger.getLogger(TotalStatisticsController.class);
    @Autowired
    private ScheduledTasks scheduledTasks;
    @Autowired
    private GeneralStatisticsService generalStatisticsService;

    @RequestMapping(value = "/current", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Integer> getTotalStatistics(HttpServletRequest request) {
        Map<String, Integer> stat = new HashMap<String, Integer>();
        StatisticData sd;
        try {
            org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
            sd = EssuirStatistics.getTotalStatistic(context);
            stat.put("TotalCount", Long.valueOf(sd.getTotalCount()).intValue());
            stat.put("TotalViews", Long.valueOf(sd.getTotalViews()).intValue());
            stat.put("TotalDownloads", Long.valueOf(sd.getTotalDownloads()).intValue());
            stat.put("CurrentMonthStatisticsViews", generalStatisticsService.getCurrentMonthStatisticsViews(sd.getTotalViews()));
            stat.put("CurrentMonthStatisticsDownloads", generalStatisticsService.getCurrentMonthStatisticsDownloads(sd.getTotalDownloads()));
            stat.put("CurrentYearStatisticsViews", generalStatisticsService.getCurrentYearStatisticsViews(sd.getTotalViews()));
            stat.put("CurrentYearStatisticsDownloads", generalStatisticsService.getCurrentYearStatisticsDownloads(sd.getTotalDownloads()));
            context.complete();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return stat;
    }

    @RequestMapping(value = "/pubstat", method = RequestMethod.GET)
    public String getGeneralStatistics(ModelMap model) {
        model.addAttribute("listYearStatistics", generalStatisticsService.getListYearsStatistics());
        return "pub_stat";
    }

    @RequestMapping(value = "/update-month-statistics", method = RequestMethod.GET)
    @ResponseBody
    public String updateStatistics() {
        if (scheduledTasks.finalizeMonthStatistics()) {
            return "Statistics updated successfully";
        } else {
            return "Errors occurred";
        }
    }
}

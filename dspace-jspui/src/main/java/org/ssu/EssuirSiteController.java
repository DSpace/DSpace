package org.ssu;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.NewsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.ItemTypeResponse;
import org.ssu.statistics.EssuirStatistics;
import org.ssu.statistics.StatisticsData;
import org.ssu.types.TypeLocalization;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequestMapping("/")
@Controller
public class EssuirSiteController {

    @Resource
    private TypeLocalization typeLocalization;

    @Resource
    private EssuirStatistics essuirStatistics;

    @RequestMapping("/")
    public ModelAndView homePage(ModelAndView model, HttpServletRequest request) throws SQLException, ItemCountException {
        Context dspaceContext = UIUtil.obtainContext(request);
        ItemCounter ic = new ItemCounter(dspaceContext);
        Locale locale = dspaceContext.getCurrentLocale();

        NewsService newsService = CoreServiceFactory.getInstance().getNewsService();
        String topNews = newsService.readNewsFile(I18nUtil.getMessage("news-top.html", locale));
        String sideNews = newsService.readNewsFile(I18nUtil.getMessage("news-side.html", locale));
        List<Community> communities = ContentServiceFactory.getInstance().getCommunityService().findAll(dspaceContext);

        Map<Community, Integer> communityResponse = communities
                .stream()
                .filter(item -> item.getParentCommunities().isEmpty())
                .collect(Collectors.toMap(item -> item, community -> {
                    try {
                        return ic.getCount(community);
                    } catch (ItemCountException e) {

                    }
                    return 0;
                }));

        StatisticsData totalStatistic = essuirStatistics.getTotalStatistic();
        List<ItemTypeResponse> submissionStatisticsByType = typeLocalization.getSubmissionStatisticsByType(locale.getLanguage());
        model.addObject("topNews", String.format(topNews, totalStatistic.getTotalCount(), totalStatistic.getLastUpdate()));
        model.addObject("sideNews", sideNews);
        model.addObject("submissions", submissionStatisticsByType);
        model.addObject("communities", communityResponse);

        model.setViewName("home");
        return model;
    }
}

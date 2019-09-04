package org.ssu;

import org.dspace.app.webui.components.RecentSubmissionsException;
import org.dspace.app.webui.components.RecentSubmissionsManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.NewsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.ItemTypeResponse;
import org.ssu.entity.response.RecentItem;
import org.ssu.localization.TypeLocalization;
import org.ssu.statistics.EssuirStatistics;
import org.ssu.statistics.StatisticsData;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        List<ItemTypeResponse> submissionStatisticsByType = typeLocalization.getSubmissionStatisticsByType(locale);
        model.addObject("topNews", String.format(topNews, totalStatistic.getTotalCount(), totalStatistic.getLastUpdate()));
        model.addObject("sideNews", sideNews);
        model.addObject("submissions", submissionStatisticsByType);
        model.addObject("communities", communityResponse);

        model.setViewName("home");
        return model;
    }

    @RequestMapping("/provision")
    public String provisionPage() {
        return "position";
    }

    @RequestMapping("/about")
    public String aboutPage() {
        return "about";
    }

    @RequestMapping("/instruction")
    public String instructionPage() {
        return "instruction";
    }

    @RequestMapping("/contacts")
    public String contactsPage() {
        return "contacts";
    }

    @RequestMapping("/application1")
    public String application1Page() {
        return "application1";
    }

    @RequestMapping("/application2")
    public String application2Page() {
        return "application2";
    }

    @RequestMapping("/structure")
    public String structurePage() {
        return "structure";
    }


    @RequestMapping("/recent-items")
    public ModelAndView recentItemsPage(ModelAndView model, HttpServletRequest request) throws SQLException, RecentSubmissionsException {
        Context context = UIUtil.obtainContext(request);
        Locale locale = context.getCurrentLocale();
        List<Item> items = new RecentSubmissionsManager(context).getRecentSubmissions(null).getRecentSubmissions();

        List<RecentItem> recentItems = items.stream()
                .map(item -> new RecentItem.Builder()
                        .withTitle(item.getName())
                        .withType(typeLocalization.getTypeLocalized(item.getItemService().getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "type", null, Item.ANY), locale))
                        .withHandle(item.getHandle())
                        .build())
                .collect(Collectors.toList());
        model.addObject("recentItems", recentItems);
        model.setViewName("recent-items");
        return model;
    }

    @RequestMapping("/faq")
    public ModelAndView faqPage(ModelAndView model, HttpServletRequest request) throws SQLException {
        Context dspaceContext = UIUtil.obtainContext(request);
        Locale locale = dspaceContext.getCurrentLocale();
        NewsService newsService = CoreServiceFactory.getInstance().getNewsService();
        String faqFilePath = String.format("faq%s.html", locale.getLanguage().equals("en") ? "" : "_" + locale.getLanguage());
        model.addObject("faq", newsService.readNewsFile(faqFilePath));
        model.setViewName("faq");

        return model;
    }

    @RequestMapping("/top-publications")
    public ModelAndView top10authorsPage(ModelAndView model, HttpServletRequest request) throws SQLException {
        List<org.ssu.entity.Item> collect = essuirStatistics.topPublications(10);
        model.addObject("publicationList", collect);
        model.setViewName("top-publications");
        return model;
    }
}

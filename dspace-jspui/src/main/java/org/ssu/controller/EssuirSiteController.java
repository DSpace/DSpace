package org.ssu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
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
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.NewsService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.FacultyService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.response.CommunityResponse;
import org.ssu.entity.response.ItemTypeResponse;
import org.ssu.entity.response.RecentItem;
import org.ssu.entity.statistics.StatisticsData;
import org.ssu.service.CommunityService;
import org.ssu.service.GeneralStatisticsService;
import org.ssu.service.localization.TypeLocalization;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/")
@Controller
public class EssuirSiteController {
    private static Logger log = Logger.getLogger(EssuirSiteController.class);
    private FacultyService facultyService = EPersonServiceFactory.getInstance().getFacultyService();
    @Resource
    private TypeLocalization typeLocalization;

    @Resource
    private CommunityService communityService;

    @Resource
    private EssuirStatistics essuirStatistics;

    @Resource
    private GeneralStatisticsService generalStatisticsService;

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

        StatisticsData totalStatistic = essuirStatistics.getTotalStatistic(dspaceContext);
        List<ItemTypeResponse> submissionStatisticsByType = typeLocalization.getSubmissionStatisticsByType(locale).stream().sorted(Comparator.comparing(ItemTypeResponse::getTitle)).collect(Collectors.toList());
        model.addObject("topNews", String.format(topNews, totalStatistic.getTotalCount(), totalStatistic.getLastUpdate()));
        model.addObject("sideNews", sideNews);
        model.addObject("submissions", submissionStatisticsByType);
        model.addObject("communities", communityResponse);
        request.setAttribute("dspace.context", dspaceContext);
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
        context.complete();
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
    public ModelAndView topPublicationsPage(ModelAndView model, HttpServletRequest request) throws SQLException {
        List<org.ssu.entity.Item> publications = essuirStatistics.topPublications(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("jsp.view.top_publications_count"));
        model.addObject("publicationList", publications);
        model.setViewName("top-publications");
        model.addObject("listSize", publications.size());
        return model;
    }

    @RequestMapping("/top-authors")
    public ModelAndView topAuthorsPage(ModelAndView model, HttpServletRequest request) throws SQLException {
        Context dspaceContext = UIUtil.obtainContext(request);
        Locale locale = dspaceContext.getCurrentLocale();
        List<Pair<String, Integer>> authors = essuirStatistics.topAuthors(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("jsp.view.top_authors_count"))
                .stream()
                .map(author -> Pair.of(author.getKey().getFormattedAuthorData("%s, %s", locale), author.getValue()))
                .collect(Collectors.toList());
        model.addObject("authorList", authors);
        model.addObject("listSize", authors.size());
        model.setViewName("top-authors");
        return model;
    }

    @RequestMapping(value = "/general-statistics", method = RequestMethod.GET)
    public String getGeneralStatistics(ModelMap model, HttpServletRequest request) throws SQLException {
        Context context = UIUtil.obtainContext(request);
        StatisticsData statisticsData = essuirStatistics.getTotalStatistic(context);
        model.addAttribute("totalItemCount", statisticsData.getTotalCount());
        model.addAttribute("totalDownloads", statisticsData.getTotalDownloads());
        model.addAttribute("totalViews", statisticsData.getTotalViews());
        model.addAttribute("listYearStatistics", generalStatisticsService.getListYearsStatistics());
        context.complete();
        return "pub_stat";
    }

    @RequestMapping("community-list")
    public ModelAndView getCommunityList(ModelAndView model, HttpServletRequest request, HttpServletResponse response) throws SQLException, ItemCountException {
        Context dspaceContext = UIUtil.obtainContext(request);
        CommunityResponse communityResponse = communityService.build(dspaceContext);

        model.addObject("communities", communityResponse.getCommunities());
        model.addObject("innerCommunities", communityResponse.getCommMap());
        model.addObject("isAdmin", communityResponse.getIsAdmin());
        model.addObject("itemCounter", new ItemCounter(dspaceContext));
        request.setAttribute("dspace.context", dspaceContext);
        model.setViewName("community-list");
        return model;
    }


    @RequestMapping(value = "/feedback", method = RequestMethod.GET)
    public ModelAndView feedbackPage(ModelAndView model,
                                     HttpServletRequest request,
                                     @RequestParam(value = "email", required = false) String email,
                                     @RequestParam(value = "feedback", required = false) String feedback,
                                     @RequestParam(value = "fakeVariable", required = false, defaultValue = "") String message,
                                     @RequestParam(value = "fakeVariable", required = false, defaultValue = "true") String messageType) {
        String recaptchaPublicKey = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("recaptcha.public");

        model.addObject("recaptchaPublicKey", recaptchaPublicKey);
        model.addObject("message", message);
        model.addObject("messageClass", messageType);
        model.addObject("email", StringEscapeUtils.escapeHtml(email));
        model.addObject("feedback", StringEscapeUtils.escapeHtml(feedback));

        model.setViewName("feedback");
        return model;
    }

    private boolean checkGoogleRecaptcha(HttpServletRequest request) {
        String host = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname");
        Map<String, String> googleRequestParameters = new HashMap<>();
        googleRequestParameters.put("secret", DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("recaptcha.private"));
        googleRequestParameters.put("response", request.getParameter("g-recaptcha-response"));
        googleRequestParameters.put("remoteip", host);

        ResponseEntity<Map> recaptchaResponseEntity = new RestTemplate()
                .postForEntity("https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={response}&remoteip={remoteip}", googleRequestParameters, Map.class, googleRequestParameters);
        Map<String, Object> googleCaptchaVerifyRepsonse = recaptchaResponseEntity.getBody();
        return (Boolean) googleCaptchaVerifyRepsonse.get("success");
    }

    @RequestMapping(value = "/feedback", method = RequestMethod.POST)
    public ModelAndView sendFeedback(ModelAndView model,
                                     HttpServletRequest request,
                                     @RequestParam(value = "feedback", required = false) String feedback,
                                     @RequestParam(value = "email", required = false) String email) throws SQLException, IOException {
        Context dspaceContext = UIUtil.obtainContext(request);
        Locale locale = dspaceContext.getCurrentLocale();

        boolean verifyStatus = checkGoogleRecaptcha(request);
        boolean isFeedbackTextFilled = !StringUtils.isEmpty(feedback);
        boolean isEmailCorrect = EmailValidator.getInstance().isValid(email);

        String message = "";
        String messageType = "success";

        if (!isEmailCorrect) {
            message = I18nUtil.getMessage("feedback.email.incorrect", locale);
            messageType = "warning";
        }

        if (!isFeedbackTextFilled) {
            message = I18nUtil.getMessage("feedback.feedback.empty", locale);
            messageType = "warning";
        }

        if (!verifyStatus) {
            message = I18nUtil.getMessage("feedback.captcha.fail", locale);
            messageType = "warning";
        }

        if (isFeedbackTextFilled && isEmailCorrect && verifyStatus) {
            EPerson currentUser = dspaceContext.getCurrentUser();
            Email emailTemplate = Email.getEmail(I18nUtil.getEmailFilename(dspaceContext.getCurrentLocale(), "feedback"));
            emailTemplate.addRecipient(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("feedback.recipient"));

            emailTemplate.addArgument(new Date());
            emailTemplate.addArgument(email);
            emailTemplate.addArgument(Optional.ofNullable(currentUser).map(EPerson::getEmail).orElse(""));
            emailTemplate.addArgument(request.getHeader("Referer"));
            emailTemplate.addArgument(request.getHeader("User-Agent"));
            emailTemplate.addArgument(request.getSession().getId());
            emailTemplate.addArgument(feedback);
            emailTemplate.setReplyTo(email);

            message = I18nUtil.getMessage("feedback.sent.success", locale);
            try {
                emailTemplate.send();
            } catch (MessagingException | IOException e) {
                log.error(e);
                message = I18nUtil.getMessage("jsp.error.integrity.list4", locale);
                messageType = "danger";
            }

        }
        return feedbackPage(model, request, email, feedback, message, messageType);
    }

    @RequestMapping(value = "/api/facultylist", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String getFacultyList(HttpServletRequest request) throws JsonProcessingException, SQLException {
        Context dspaceContext = UIUtil.obtainContext(request);
        try {
            return new ObjectMapper().writeValueAsString(facultyService.findAll(dspaceContext));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
}

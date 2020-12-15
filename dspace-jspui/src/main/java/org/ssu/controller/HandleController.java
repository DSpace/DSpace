package org.ssu.controller;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.*;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.plugin.CollectionHomeProcessor;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.sort.SortException;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.ssu.entity.GoogleMetadataTagGenerator;
import org.ssu.entity.response.BitstreamResponse;
import org.ssu.entity.response.CountedCommunityResponse;
import org.ssu.entity.response.CountryStatisticsResponse;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.*;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/")
public class HandleController {
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private org.dspace.content.service.ItemService dspaceItemService = ContentServiceFactory.getInstance().getItemService();
    private final transient PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    private final transient DisseminationCrosswalk xHTMLHeadCrosswalk = (DisseminationCrosswalk) pluginService.getNamedPlugin(DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");
    private final transient org.dspace.content.service.CommunityService dspaceCommunityService = ContentServiceFactory.getInstance().getCommunityService();
    private final transient CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Resource
    private ItemService itemService;

    @Resource
    private EssuirStatistics essuirStatistics;

    @Resource
    private CommunityService communityService;

    @Resource
    private BrowseRequestProcessor browseRequestProcessor;

    @Resource
    private AuthorsService authorsService;

    @Resource
    private GeoIpService geoIpService;

    @RequestMapping(value = "/123456789/{itemId}")
    public ModelAndView entrypoint(HttpServletRequest request, HttpServletResponse response,  @PathVariable("itemId") String itemId, ModelAndView model) throws SQLException, ItemCountException, PluginException, AuthorizeException, ServletException, BrowseException, IOException, SortException, CrosswalkException {
        Context dspaceContext = UIUtil.obtainContext(request);
        DSpaceObject dSpaceObject = handleService.resolveToObject(dspaceContext, "123456789/" + itemId);
        Locale locale = dspaceContext.getCurrentLocale();
        ModelAndView result = new ModelAndView();
        request.setAttribute("dspace.context", dspaceContext);
        if(authorizeService.authorizeActionBoolean(dspaceContext, dSpaceObject, Constants.READ)) {
            Community parentCommunity = null;
            boolean includeCurrentCommunityInResult = false;
            if (dSpaceObject.getType() == Constants.ITEM && authorizeService.authorizeActionBoolean(dspaceContext, ((Item)dSpaceObject).getOwningCollection(), Constants.READ)) {
                Item item = (Item) dSpaceObject;
                request.setAttribute("dspace.collection", item.getOwningCollection());
                parentCommunity = item.getOwningCollection().getCommunities().get(0);
                includeCurrentCommunityInResult = true;
                result = displayItem(request, model, item, locale, dspaceContext);
            }
            if (dSpaceObject.getType() == Constants.COMMUNITY) {
                Community community = (Community) dSpaceObject;
                parentCommunity = community;
                includeCurrentCommunityInResult = false;
                result = displayCommunity(request, response, model, community, locale, dspaceContext);
            }
            if (dSpaceObject.getType() == Constants.COLLECTION) {
                parentCommunity = ((Collection)dSpaceObject).getCommunities().get(0);
                includeCurrentCommunityInResult = true;
                result = displayCollection(request, response, model, (Collection)dSpaceObject, locale, dspaceContext);
            }
            if(parentCommunity != null) {
                request.setAttribute("dspace.community", parentCommunity);
                request.setAttribute("dspace.communities", getCommunityParents(dspaceContext, parentCommunity, includeCurrentCommunityInResult));
                return result;
            }
        }

        request.setAttribute("dspace.original.url", "/handle/123456789/" + itemId);
        Authenticate.startAuthentication(dspaceContext, request, response);
        result = new ModelAndView();
        result.setViewName("home");

        return result;
    }


    private ModelAndView displayCollection(HttpServletRequest request, HttpServletResponse response, ModelAndView model, Collection collection, Locale locale, Context dspaceContext) throws SQLException, ServletException, IOException, AuthorizeException, BrowseException, SortException, ItemCountException, PluginException {

        request.setAttribute("dspace.collection", collection);
        ItemCounter ic = new ItemCounter(dspaceContext);
        BrowseInfo browseInfo = new BrowseContext().getBrowseInfo(dspaceContext, request, response);
        List<ItemResponse> items = communityService.getItems(dspaceContext, browseInfo);
        browseRequestProcessor.fillModelWithData(model, items, browseInfo, request, true);

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        CollectionHomeProcessor[] chp = (CollectionHomeProcessor[]) pluginService.getPluginSequence(CollectionHomeProcessor.class);
        for (CollectionHomeProcessor collectionHomeProcessor : chp) {
            collectionHomeProcessor.process(dspaceContext, request, response, collection);
        }

        request.setAttribute("collection", collection);
        request.setAttribute("community", collection.getCommunities().get(0));
        model.addObject("title", collection.getName());
        model.addObject("collection", collection);
        model.addObject("community", collection.getCommunities().get(0));
        model.addObject("submitters", collection.getSubmitters());
        model.addObject("editorButton", collectionService.canEditBoolean(dspaceContext, collection, true));
        model.addObject("adminButton", authorizeService.authorizeActionBoolean(dspaceContext, collection, Constants.ADMIN));
        model.addObject("handle", collection.getHandle());
        model.addObject("itemCount", ic.getCount(collection));
        model.addObject("browseIndices", Arrays.asList(BrowseIndex.getBrowseIndices()));

        model.setViewName("collection-display");
        return model;
    }
    private ModelAndView displayCommunity(HttpServletRequest request, HttpServletResponse response, ModelAndView model, Community community, Locale locale, Context dspaceContext) throws SQLException, ItemCountException, PluginException, AuthorizeException, BrowseException {
        ItemCounter ic = new ItemCounter(dspaceContext);
        Function<DSpaceObject, CountedCommunityResponse> extractDataForCountedItem = (collection) -> {
            try {
                return new CountedCommunityResponse.Builder()
                        .withTitle(collection.getName())
                        .withHandle(collection.getHandle())
                        .withItemCount(ic.getCount(collection))
                        .withId(collection.getID())
                        .build();
            } catch (ItemCountException e) {
                e.printStackTrace();
            }
            return null;
        };

        List<CountedCommunityResponse> subCommunities = community.getSubcommunities()
                .stream()
                .map(extractDataForCountedItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<CountedCommunityResponse> collections = community.getCollections()
                .stream()
                .map(extractDataForCountedItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        CommunityHomeProcessor[] chp = (CommunityHomeProcessor[]) pluginService.getPluginSequence(CommunityHomeProcessor.class);
        for (CommunityHomeProcessor communityHomeProcessor : chp) {
            communityHomeProcessor.process(dspaceContext, request, response, community);
        }

        model.setViewName("community-display");
        request.setAttribute("community", community);
        model.addObject("title", community.getName());
        model.addObject("editorButton", dspaceCommunityService.canEditBoolean(dspaceContext, community));
        model.addObject("addButton", authorizeService.authorizeActionBoolean(dspaceContext, community, Constants.ADD));
        model.addObject("removeButton", authorizeService.authorizeActionBoolean(dspaceContext, community, Constants.REMOVE));
        model.addObject("community", community);
        model.addObject("handle", community.getHandle());
        model.addObject("subCommunities", subCommunities);
        model.addObject("collections", collections);
        model.addObject("itemCount", ic.getCount(community));
        model.addObject("browseIndices", Arrays.asList(BrowseIndex.getBrowseIndices()));
        return model;
    }

    @RequestMapping("/123456789/{handle}/{sequenceId}/{bitstreamName:.+}")
    public RedirectView downloadBitstream(HttpServletRequest request, @PathVariable("handle") Integer handle, @PathVariable("sequenceId") Integer sequenceId, @PathVariable("bitstreamName") String bitstreamName) throws SQLException, UnsupportedEncodingException {
        Context dspaceContext = UIUtil.obtainContext(request);
        Item item = (Item)handleService.resolveToObject(dspaceContext, String.format("123456789/%d", handle));
        boolean isSpiderBot = SpiderDetector.isSpider(request);
        if(!isSpiderBot && !geoIpService.isLocalhost(request)) {
            essuirStatistics.incrementGlobalItemDownloads();
            essuirStatistics.updateItemDownloads(request, item.getID());
        }
        String downloadString = String.format("%s/bitstream-download/%s/%s/%s", request.getContextPath(), item.getHandle(), sequenceId, UIUtil.encodeBitstreamName(bitstreamName, Constants.DEFAULT_ENCODING));

        RedirectView redirectView = new RedirectView(downloadString);
        dspaceContext.complete();
        return redirectView;
    }

    private ModelAndView displayItem(HttpServletRequest request, ModelAndView model, Item item, Locale locale, Context dspaceContext) throws SQLException, IOException, CrosswalkException, AuthorizeException {
        List<Element> metaTags = xHTMLHeadCrosswalk.disseminateList(dspaceContext, item);

        boolean googleEnabled = ConfigurationManager.getBooleanProperty("google-metadata.enable", false);
        if (googleEnabled)
        {
            GoogleMetadataTagGenerator googleMetadata = new GoogleMetadataTagGenerator(dspaceContext, item);
            String language = googleMetadata.getLanguage().stream().findFirst().orElse("en");
            Locale authorLocalizationLocale = Locale.forLanguageTag(language);
            googleMetadata.setAuthors(googleMetadata.getAuthors().stream()
                    .map(author -> authorsService.getAuthorLocalization(author))
                    .distinct()
                    .map(authorLocalized -> authorLocalized.getFormattedAuthorData("%s, %s", authorLocalizationLocale))
                    .collect(Collectors.toList()));
            metaTags.addAll(googleMetadata.disseminateList());
        }

        StringWriter headMetadata  = new StringWriter();
        XMLOutputter outputXmlWritter = new XMLOutputter();
        outputXmlWritter.output(new Text("\n"), headMetadata );

        List<Element> outputTags = metaTags.stream()
                .peek(element -> element.setNamespace(null))
                .map(element -> element.addContent(new Text("\n")))
                .collect(Collectors.toList());
        outputXmlWritter.output(outputTags, headMetadata );
        request.setAttribute("dspace.layout.head", headMetadata.toString());

        boolean isSpiderBot = SpiderDetector.isSpider(request);
        if(!isSpiderBot && !geoIpService.isLocalhost(request)) {
            essuirStatistics.updateItemViews(request, item.getID());
            essuirStatistics.incrementGlobalItemViews();
        }
        List<CountryStatisticsResponse> itemViewsByCountry = essuirStatistics.getItemViewsByCountry(item.getID())
                .entrySet()
                .stream()
                .map(country -> new CountryStatisticsResponse.Builder()
                        .withCountryCode(country.getKey())
                        .withCountryName(LocationUtils.getCountryName("--".equals(country.getKey()) ? "" : country.getKey(), locale))
                        .withCount(country.getValue())
                        .build()
                )
                .sorted(Comparator.comparing(CountryStatisticsResponse::getCountryName))
                .collect(Collectors.toList());

        List<CountryStatisticsResponse> itemDownloadsByCountry = essuirStatistics.getItemDownloadsByCountry(item.getID())
                .entrySet()
                .stream()
                .map(country -> new CountryStatisticsResponse.Builder()
                        .withCountryCode(country.getKey())
                        .withCountryName(LocationUtils.getCountryName("--".equals(country.getKey()) ? "" : country.getKey(), locale))
                        .withCount(country.getValue())
                        .build()
                )
                .sorted(Comparator.comparing(CountryStatisticsResponse::getCountryName))
                .collect(Collectors.toList());

        List<Pair<String, String>> authors = itemService.extractAuthorListForItem(item).stream()
                .map(author -> Pair.of(author.getFormattedAuthorData("%s, %s", locale), author.getOrcid()))
                .collect(Collectors.toList());
        Function<Bitstream, String> getBitstreamFormat = (bitstream) -> {
            try {
                return bitstream.getFormatDescription(dspaceContext);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "";
        };

        List<Bundle> bundles = dspaceItemService.getBundles(item, "ORIGINAL");
        List<BitstreamResponse> bitstreams = bundles.stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .map(bitstream -> new BitstreamResponse.Builder()
                        .withDownloadCount(itemDownloadsByCountry.stream().mapToInt(CountryStatisticsResponse::getCount).sum())
                        .withFormat(getBitstreamFormat.apply(bitstream))
                        .withFilename(bitstream.getName())
                        .withHandle(bitstream.getHandle())
                        .withLink(String.format("%s/bitstream/%s/%d/%s", request.getContextPath(), item.getHandle(), bitstream.getSequenceID(), bitstream.getName()))
                        .withSize(UIUtil.formatFileSize(bitstream.getSizeBytes()))
                        .build())
                .collect(Collectors.toList());
        model.addObject("title", item.getName());
        model.addObject("titlesAlternative", itemService.getAlternativeTitleForItem(item));
        model.addObject("owningCollections", item.getCollections());
        model.addObject("type", itemService.getItemTypeLocalized(item, locale));
        model.addObject("authors", authors);
        model.addObject("keywords", itemService.getKeywordsForItem(item));
        model.addObject("year", itemService.extractIssuedYearForItem(item));
        model.addObject("uri", itemService.getURIForItem(item));
        model.addObject("publisher", itemService.getPublisherForItem(item));
        model.addObject("citation", itemService.getCitationForItem(item));
        model.addObject("abstracts", itemService.getAbstractsForItem(item));
        model.addObject("rights", itemService.getRightsForItem(item, locale));
        model.addObject("views", itemViewsByCountry);
        model.addObject("downloads", itemDownloadsByCountry);
        model.addObject("bundles", bitstreams);
        model.addObject("handle", item.getHandle());
        model.addObject("itemId", item.getID());
        model.addObject("canEdit", dspaceItemService.canEdit(dspaceContext, item));

        model.setViewName("item-display");
        return model;
    }

    private List<Community> getCommunityParents(Context context, Community currentCommunity, boolean includeCurrentCommunityInResult) throws SQLException {
        Stream<Optional<Community>> previousCommunities = Lists.reverse(dspaceCommunityService.getAllParents(context, currentCommunity)).stream().map(Optional::ofNullable);
        return Stream.concat(previousCommunities, Stream.of(Optional.ofNullable(currentCommunity).filter(com -> includeCurrentCommunityInResult)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}

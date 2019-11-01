package org.ssu.controller;

import com.google.common.collect.Lists;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.*;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
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
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.ssu.entity.GoogleMetadataTagGenerator;
import org.ssu.entity.response.BitstreamResponse;
import org.ssu.entity.response.CountedCommunityResponse;
import org.ssu.entity.response.CountryStatisticsResponse;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.BrowseContext;
import org.ssu.service.CommunityService;
import org.ssu.service.ItemService;
import org.ssu.service.BrowseRequestProcessor;
import org.ssu.service.localization.AuthorsCache;
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

@Controller
@RequestMapping("/")
public class HandleController {
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private org.dspace.content.service.ItemService dspaceItemService = ContentServiceFactory.getInstance().getItemService();
    private final transient PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    private final transient DisseminationCrosswalk xHTMLHeadCrosswalk = (DisseminationCrosswalk) pluginService.getNamedPlugin(DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");
    private final transient org.dspace.content.service.CommunityService dspaceCommunityService = ContentServiceFactory.getInstance().getCommunityService();
    @Resource
    private ItemService itemService;

    @Resource
    private EssuirStatistics essuirStatistics;

    @Resource
    private CommunityService communityService;

    @Resource
    private BrowseRequestProcessor browseRequestProcessor;

    @Resource
    private AuthorsCache authorsCache;

    @RequestMapping(value = "/123456789/{itemId}")
    public ModelAndView entrypoint(HttpServletRequest request, HttpServletResponse response,  @PathVariable("itemId") String itemId, ModelAndView model) throws SQLException, ItemCountException, PluginException, AuthorizeException, ServletException, BrowseException, IOException, SortException, CrosswalkException {
        Context dspaceContext = UIUtil.obtainContext(request);
        DSpaceObject dSpaceObject = handleService.resolveToObject(dspaceContext, "123456789/" + itemId);
        Locale locale = dspaceContext.getCurrentLocale();
        ModelAndView result = new ModelAndView();
//        authorizeService.authorizeAction(dspaceContext, dSpaceObject, Constants.READ);
        if(authorizeService.authorizeActionBoolean(dspaceContext, dSpaceObject, Constants.READ)) {
            Community parentCommunity = null;
            boolean includeCurrentCommunityInResult = false;
            if (dSpaceObject.getType() == Constants.ITEM) {
                Item item = (Item) dSpaceObject;
                request.setAttribute("dspace.collection", item.getOwningCollection());
                parentCommunity = item.getOwningCollection().getCommunities().get(0);
                includeCurrentCommunityInResult = true;
                result = displayItem(request, model, item, locale);
            }
            if (dSpaceObject.getType() == Constants.COMMUNITY) {
                Community community = (Community) dSpaceObject;
                parentCommunity = community;
                includeCurrentCommunityInResult = false;
                result = displayCommunity(request, response, model, community, locale);
            }
            if (dSpaceObject.getType() == Constants.COLLECTION) {
                parentCommunity = ((Collection)dSpaceObject).getCommunities().get(0);
                includeCurrentCommunityInResult = true;
                result = displayCollection(request, response, model, (Collection)dSpaceObject, locale);
            }
            request.setAttribute("dspace.community", parentCommunity);
            request.setAttribute("dspace.communities", getParents(dspaceContext, parentCommunity, includeCurrentCommunityInResult));
        } else {
            request.setAttribute("dspace.original.url", "/handle/123456789/" + itemId);
            Authenticate.startAuthentication(dspaceContext, request, response);
            result = new ModelAndView();
            result.setViewName("home");
        }

        return result;
    }


    private ModelAndView displayCollection(HttpServletRequest request, HttpServletResponse response, ModelAndView model, Collection collection, Locale locale) throws SQLException, ServletException, IOException, AuthorizeException, BrowseException, SortException, ItemCountException, PluginException {

        request.setAttribute("dspace.collection", collection);
        Context dspaceContext = UIUtil.obtainContext(request);
        ItemCounter ic = new ItemCounter(dspaceContext);
        BrowseInfo browseInfo = new BrowseContext().getBrowseInfo(dspaceContext, request, response);
        List<ItemResponse> items = communityService.getItems(dspaceContext, browseInfo);
        browseRequestProcessor.fillModelWithData(model, items, browseInfo, request, true);

        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        CollectionHomeProcessor[] chp = (CollectionHomeProcessor[]) pluginService.getPluginSequence(CollectionHomeProcessor.class);
        for (int i = 0; i < chp.length; i++)
        {
            chp[i].process(dspaceContext, request, response, collection);
        }

        request.setAttribute("collection", collection);
        request.setAttribute("community", collection.getCommunities().get(0));
        model.addObject("title", collection.getName());
        model.addObject("handle", collection.getHandle());
        model.addObject("itemCount", ic.getCount(collection));
        model.addObject("browseIndices", Arrays.asList(BrowseIndex.getBrowseIndices()));

        model.setViewName("collection-display");
        return model;
    }
    private ModelAndView displayCommunity(HttpServletRequest request, HttpServletResponse response, ModelAndView model, Community community, Locale locale) throws SQLException, ItemCountException, PluginException, AuthorizeException, BrowseException {
        Context dspaceContext = UIUtil.obtainContext(request);
        ItemCounter ic = new ItemCounter(dspaceContext);

        List<CountedCommunityResponse> subCommunities = community.getSubcommunities()
                .stream()
                .map(subCommunity -> {
                    try {
                        return new CountedCommunityResponse.Builder()
                                .withTitle(subCommunity.getName())
                                .withHandle(subCommunity.getHandle())
                                .withItemCount(ic.getCount(subCommunity))
                                .build();
                    } catch (ItemCountException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<CountedCommunityResponse> collections = community.getCollections()
                .stream()
                .map(subCommunity -> {
                    try {
                        return new CountedCommunityResponse.Builder()
                                .withTitle(subCommunity.getName())
                                .withHandle(subCommunity.getHandle())
                                .withItemCount(ic.getCount(subCommunity))
                                .build();
                    } catch (ItemCountException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        CommunityHomeProcessor[] chp = (CommunityHomeProcessor[]) pluginService.getPluginSequence(CommunityHomeProcessor.class);
        for (int i = 0; i < chp.length; i++)
        {
            chp[i].process(dspaceContext, request, response, community);
        }

        model.setViewName("community-display");
        request.setAttribute("community", community);
        model.addObject("title", community.getName());
        model.addObject("handle", community.getHandle());
        model.addObject("subCommunities", subCommunities);
        model.addObject("collections", collections);
        model.addObject("itemCount", ic.getCount(community));
        model.addObject("browseIndices", Arrays.asList(BrowseIndex.getBrowseIndices()));
        return model;
    }

    @RequestMapping("/item-download/{itemId}/{bitstreamId}")
    public ModelAndView downloadBitstream(HttpServletRequest request, @PathVariable("itemId") UUID itemId, @PathVariable("bitstreamId") UUID bitstreamId) throws SQLException {
        Context dspaceContext = UIUtil.obtainContext(request);
        Function<Bitstream, String> getLinkForBitstream = (bitstream) -> {
            try {
                Item item = dspaceItemService.find(dspaceContext, itemId);
                essuirStatistics.updateItemDownloads(request, item.getLegacyId());
                return String.format("%s/bitstream/%s/%s/%s", request.getContextPath(), item.getHandle(), bitstream.getSequenceID(), UIUtil.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException | SQLException e) {
                e.printStackTrace();
            }
            return bitstream.getHandle();
        };

        Bitstream bitstream = ContentServiceFactory.getInstance().getBitstreamService().find(dspaceContext, bitstreamId);
        return new ModelAndView("redirect:" + getLinkForBitstream.apply(bitstream));
    }

    private ModelAndView displayItem(HttpServletRequest request, ModelAndView model, Item item, Locale locale) throws SQLException, IOException, CrosswalkException, AuthorizeException {
        Context dspaceContext = UIUtil.obtainContext(request);

        List<Element> metaTags = xHTMLHeadCrosswalk.disseminateList(dspaceContext, item);

        boolean googleEnabled = ConfigurationManager.getBooleanProperty("google-metadata.enable", false);
        if (googleEnabled)
        {
            GoogleMetadataTagGenerator googleMetadata = new GoogleMetadataTagGenerator(dspaceContext, item);
            String language = googleMetadata.getLanguage().stream().findFirst().orElse("en");
            Locale authorLocalizationLocale = Locale.forLanguageTag(language);
            googleMetadata.setAuthors(googleMetadata.getAuthors().stream()
                    .map(author -> authorsCache.getAuthorLocalization(author))
                    .distinct()
                    .map(authorLocalized -> String.format("%s, %s", authorLocalized.getSurname(authorLocalizationLocale), authorLocalized.getInitials(authorLocalizationLocale)))
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

        essuirStatistics.updateItemViews(request, item.getLegacyId());
        List<CountryStatisticsResponse> itemViewsByCountry = essuirStatistics.getItemViewsByCountry(item.getLegacyId())
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

        List<CountryStatisticsResponse> itemDownloadsByCountry = essuirStatistics.getItemDownloadsByCountry(item.getLegacyId())
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

        List<String> authors = itemService.extractAuthorListForItem(item).stream()
                .map(author -> String.format("%s, %s", author.getSurname(locale), author.getInitials(locale)))
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
                        .withLink(String.format("%s/handle/item-download/%s/%s", request.getContextPath(), item.getID(), bitstream.getID()))
                        .withSize(UIUtil.formatFileSize(bitstream.getSizeBytes()))
                        .build())
                .collect(Collectors.toList());
        model.addObject("title", item.getName());
        model.addObject("titleAlternative", itemService.getAlternativeTitleForItem(item));
        model.addObject("owningCollections", item.getCollections());
        model.addObject("type", itemService.getItemTypeLocalized(item, locale));
        model.addObject("authors", authors);
        model.addObject("keywords", itemService.getKeywordsForItem(item));
        model.addObject("year", itemService.extractIssuedYearForItem(item));
        model.addObject("uri", itemService.getURIForItem(item));
        model.addObject("publisher", itemService.getPublisherForItem(item));
        model.addObject("citation", itemService.getCitationForItem(item));
        model.addObject("abstracts", itemService.getAbstractsForItem(item));
        model.addObject("views", itemViewsByCountry);
        model.addObject("downloads", itemDownloadsByCountry);
        model.addObject("bundles", bitstreams);
        model.addObject("handle", item.getHandle());
        model.addObject("itemId", item.getID());
        model.addObject("canEdit", dspaceItemService.canEdit(dspaceContext, item));

        model.setViewName("item-display");
        return model;
    }

    private List<Community> getParents(Context context, Community c, boolean include)
            throws SQLException
    {
        // Find all the "parent" communities for the community
        List<Community> parents = dspaceCommunityService .getAllParents(context, c);
        parents = Lists.reverse(parents);
        if (include)
        {
            parents.add(c);
        }
        return parents;
    }
}

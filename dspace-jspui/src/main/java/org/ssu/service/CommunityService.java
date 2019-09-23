package org.ssu.service;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseInfo;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Service;
import org.ssu.entity.response.CommunityResponse;
import org.ssu.entity.response.ItemResponse;
import org.ssu.service.localization.AuthorsCache;
import org.ssu.service.localization.TypeLocalization;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    @Resource
    private EssuirStatistics essuirStatistics;

    @Resource
    private ItemService itemService;

    private final transient org.dspace.content.service.CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private final transient AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public CommunityResponse build(Context context) throws SQLException {
        Map<String, List<Community>> subCommunities;
        subCommunities = new HashMap<>();
        List<Community> communities = communityService.findAllTop(context)
                .stream()
                .sorted(Comparator.comparing(Community::getName))
                .collect(Collectors.toList());
        for (Community community : communities) {
            build(community, subCommunities, context);
        }
        return new CommunityResponse.Builder()
                .withCommMap(subCommunities)
                .withIsAdmin(authorizeService.isAdmin(context))
                .withCommunities(communities)
                .build();
    }

    private void build(Community community, Map<String, List<Community>> commMap, Context context) throws SQLException {
        if(authorizeService.authorizeActionBoolean(context, community, Constants.READ)) {
            String comID = community.getID().toString();
            List<Community> communities = community.getSubcommunities()
                    .stream()
                    .sorted(Comparator.comparing(Community::getName))
                    .collect(Collectors.toList());

            for(Collection collection : community.getCollections()) {
                if(!authorizeService.authorizeActionBoolean(context, collection, Constants.READ)) {
                    community.removeCollection(collection);
                }
            }

            if (communities.size() > 0) {
                commMap.put(comID, communities);
                for (Community sub : communities) {
                    build(sub, commMap, context);
                }
            }
        }
    }


    public List<ItemResponse> getItems(Context context, BrowseInfo browserInfo) throws BrowseException {
        Locale locale = context.getCurrentLocale();

        Function<Item, String> extractAuthorListForItem = (item) ->
            itemService.extractAuthorListForItem(item)
                    .stream()
                    .map(author -> String.format("%s, %s", author.getSurname(locale), author.getInitials(locale)))
                    .map(author -> String.format("<a href=\"/browse?type=author&value=%s\">%s</a>", author, author))
                    .collect(Collectors.joining("; "));

        return browserInfo.getBrowseItemResults()
                .stream()
                .map(item -> new ItemResponse.Builder()
                .withTitle(item.getName())
                        .withYear(itemService.extractIssuedYearForItem(item))
                        .withHandle(item.getHandle())
                        .withAuthors(extractAuthorListForItem.apply(item))
                        .withType(itemService.getItemTypeLocalized(item, locale))
                        .withViews(essuirStatistics.getViewsForItem(item.getLegacyId()))
                        .withDownloads(essuirStatistics.getDownloadsForItem(item.getLegacyId()))
                        .build())
                .filter(item -> item.getYear() != null)
                .collect(Collectors.toList());
    }
}

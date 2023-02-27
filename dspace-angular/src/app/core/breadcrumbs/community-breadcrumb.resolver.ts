import { Injectable } from '@angular/core';
import { DSOBreadcrumbsService } from './dso-breadcrumbs.service';
import { DSOBreadcrumbResolver } from './dso-breadcrumb.resolver';
import { CommunityDataService } from '../data/community-data.service';
import { Community } from '../shared/community.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { COMMUNITY_PAGE_LINKS_TO_FOLLOW } from '../../community-page/community-page.resolver';

/**
 * The class that resolves the BreadcrumbConfig object for a Community
 */
@Injectable({
  providedIn: 'root'
})
export class CommunityBreadcrumbResolver extends DSOBreadcrumbResolver<Community> {
  constructor(protected breadcrumbService: DSOBreadcrumbsService, protected dataService: CommunityDataService) {
    super(breadcrumbService, dataService);
  }

  /**
   * Method that returns the follow links to already resolve
   * The self links defined in this list are expected to be requested somewhere in the near future
   * Requesting them as embeds will limit the number of requests
   */
  get followLinks(): FollowLinkConfig<Community>[] {
    return COMMUNITY_PAGE_LINKS_TO_FOLLOW;
  }
}

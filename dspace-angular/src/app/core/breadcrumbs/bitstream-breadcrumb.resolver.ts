import { Injectable } from '@angular/core';

import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { Bitstream } from '../shared/bitstream.model';
import { BitstreamDataService } from '../data/bitstream-data.service';
import { BITSTREAM_PAGE_LINKS_TO_FOLLOW } from '../../bitstream-page/bitstream-page.resolver';
import { DSOBreadcrumbResolver } from './dso-breadcrumb.resolver';
import { BitstreamBreadcrumbsService } from './bitstream-breadcrumbs.service';

/**
 * The class that resolves the BreadcrumbConfig object for an Item
 */
@Injectable({
  providedIn: 'root'
})
export class BitstreamBreadcrumbResolver extends DSOBreadcrumbResolver<Bitstream> {
  constructor(
    protected breadcrumbService: BitstreamBreadcrumbsService, protected dataService: BitstreamDataService) {
    super(breadcrumbService, dataService);
  }

  /**
   * Method that returns the follow links to already resolve
   * The self links defined in this list are expected to be requested somewhere in the near future
   * Requesting them as embeds will limit the number of requests
   */
  get followLinks(): FollowLinkConfig<Bitstream>[] {
    return BITSTREAM_PAGE_LINKS_TO_FOLLOW;
  }

}

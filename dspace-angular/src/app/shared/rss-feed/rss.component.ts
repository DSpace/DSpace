import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { LinkHeadService } from '../../core/services/link-head.service';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';
import { environment } from '../../../../src/environments/environment';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { PaginationService } from '../../core/pagination/pagination.service';
import { Router } from '@angular/router';
import { map, switchMap } from 'rxjs/operators';
import { PaginatedSearchOptions } from '../search/models/paginated-search-options.model';
import { RemoteData } from '../../core/data/remote-data';


/**
 * The Rss feed button componenet.
 */
@Component({
  exportAs: 'rssComponent',
  selector: 'ds-rss',
  styleUrls: ['rss.component.scss'],
  templateUrl: 'rss.component.html',
  changeDetection: ChangeDetectionStrategy.Default,
  encapsulation: ViewEncapsulation.Emulated
})
export class RSSComponent implements OnInit, OnDestroy  {

  route$: BehaviorSubject<string>;

  isEnabled$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(null);

  uuid: string;
  configuration$: Observable<string>;

  subs: Subscription[] = [];

  constructor(private groupDataService: GroupDataService,
              private linkHeadService: LinkHeadService,
              private configurationService: ConfigurationDataService,
              private searchConfigurationService: SearchConfigurationService,
              private router: Router,
              protected paginationService: PaginationService) {
  }
  /**
   * Removes the linktag created when the component gets removed from the page.
   */
  ngOnDestroy(): void {
    this.linkHeadService.removeTag("rel='alternate'");
    this.subs.forEach(sub => {
      sub.unsubscribe();
    });
  }


  /**
   * Generates the link tags and the url to opensearch when the component is loaded.
   */
  ngOnInit(): void {
    this.configuration$ = this.searchConfigurationService.getCurrentConfiguration('default');

    this.subs.push(this.configurationService.findByPropertyName('websvc.opensearch.enable').pipe(
      getFirstCompletedRemoteData(),
    ).subscribe((result) => {
      if (result.hasSucceeded) {
        const enabled = (result.payload.values[0] === 'true');
        this.isEnabled$.next(enabled);
      }
    }));
    this.subs.push(this.configurationService.findByPropertyName('websvc.opensearch.svccontext').pipe(
      getFirstCompletedRemoteData(),
      map((result: RemoteData<any>) => {
        if (result.hasSucceeded) {
          return result.payload.values[0];
        }
        return null;
      }),
      switchMap((openSearchUri: string) =>
        this.searchConfigurationService.paginatedSearchOptions.pipe(
          map((searchOptions: PaginatedSearchOptions) => ({ openSearchUri,  searchOptions }))
        )
      ),
    ).subscribe(({ openSearchUri,  searchOptions }) => {
      if (!openSearchUri) {
        return null;
      }
      this.uuid = this.groupDataService.getUUIDFromString(this.router.url);
      const route = environment.rest.baseUrl + this.formulateRoute(this.uuid, openSearchUri, searchOptions.query);
      this.addLinks(route);
      this.linkHeadService.addTag({
        href: environment.rest.baseUrl + '/' + openSearchUri + '/service',
        type: 'application/atom+xml',
        rel: 'search',
        title: 'Dspace'
      });
      this.route$ = new BehaviorSubject<string>(route);
    }));
  }

  /**
   * Function created a route given the different params available to opensearch
   * @param uuid The uuid if a scope is present
   * @param opensearch openSearch uri
   * @param query The query string that was provided in the search
   * @returns The combine URL to opensearch
   */
  formulateRoute(uuid: string, opensearch: string, query: string): string {
    let route = '?format=atom';
    if (uuid) {
      route += `&scope=${uuid}`;
    }
    if (query) {
      route += `&query=${query}`;
    } else {
      route += `&query=*`;
    }
    route = '/' + opensearch + route;
    return route;
  }

  /**
   * Check if the router url contains the specified route
   *
   * @param {string} route
   * @returns
   * @memberof MyComponent
   */
  hasRoute(route: string) {
    return this.router.url.includes(route);
  }

  /**
   * Creates <link> tags in the header of the page
   * @param route The composed url to opensearch
   */
  addLinks(route: string): void {
    this.linkHeadService.addTag({
      href: route,
      type: 'application/atom+xml',
      rel: 'alternate',
      title: 'Sitewide Atom feed'
    });
    route = route.replace('format=atom', 'format=rss');
    this.linkHeadService.addTag({
      href: route,
      type: 'application/rss+xml',
      rel: 'alternate',
      title: 'Sitewide RSS feed'
    });
  }
}

import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { getCommunityPageRoute } from '../../../community-page/community-page-routing-paths';
import { getCollectionPageRoute } from '../../../collection-page/collection-page-routing-paths';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { BrowseDefinition } from '../../../core/shared/browse-definition.model';
import { RemoteData } from '../../../core/data/remote-data';
import { BrowseService } from '../../../core/browse/browse.service';

export interface ComColPageNavOption {
  id: string;
  label: string;
  routerLink: string;
  params?: any;
}

/**
 * A component to display the "Browse By" section of a Community or Collection page
 * It expects the ID of the Community or Collection as input to be passed on as a scope
 */
@Component({
  selector: 'ds-comcol-page-browse-by',
  styleUrls: ['./comcol-page-browse-by.component.scss'],
  templateUrl: './comcol-page-browse-by.component.html'
})
export class ComcolPageBrowseByComponent implements OnInit {
  /**
   * The ID of the Community or Collection
   */
  @Input() id: string;
  @Input() contentType: string;

  allOptions: ComColPageNavOption[];

  currentOptionId$: Observable<string>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private browseService: BrowseService
  ) {
  }

  ngOnInit(): void {
    this.browseService.getBrowseDefinitions()
      .pipe(getFirstCompletedRemoteData<PaginatedList<BrowseDefinition>>())
      .subscribe((browseDefListRD: RemoteData<PaginatedList<BrowseDefinition>>) => {
        if (browseDefListRD.hasSucceeded) {
          this.allOptions = browseDefListRD.payload.page
            .map((config: BrowseDefinition) => ({
              id: config.id,
              label: `browse.comcol.by.${config.id}`,
              routerLink: `/browse/${config.id}`,
              params: { scope: this.id }
            }));

          if (this.contentType === 'collection') {
            this.allOptions = [{
              id: this.id,
              label: 'collection.page.browse.recent.head',
              routerLink: getCollectionPageRoute(this.id)
            }, ...this.allOptions];
          } else if (this.contentType === 'community') {
            this.allOptions = [{
              id: this.id,
              label: 'community.all-lists.head',
              routerLink: getCommunityPageRoute(this.id)
            }, ...this.allOptions];
          }
        }
      });

    this.currentOptionId$ = this.route.params.pipe(
      map((params: Params) => params.id)
    );
  }

  onSelectChange(newId: string) {
    const selectedOption = this.allOptions
      .find((option: ComColPageNavOption) => option.id === newId);

    this.router.navigate([selectedOption.routerLink], { queryParams: selectedOption.params });
  }
}

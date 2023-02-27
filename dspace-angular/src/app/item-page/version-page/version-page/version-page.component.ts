import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { RemoteData } from '../../../core/data/remote-data';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { map, switchMap } from 'rxjs/operators';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { VersionDataService } from '../../../core/data/version-data.service';
import { Version } from '../../../core/shared/version.model';
import { Item } from '../../../core/shared/item.model';
import { getItemPageRoute } from '../../item-page-routing-paths';
import { getPageNotFoundRoute } from '../../../app-routing-paths';
import { redirectOn4xx } from '../../../core/shared/authorized.operators';

@Component({
  selector: 'ds-version-page',
  templateUrl: './version-page.component.html',
  styleUrls: ['./version-page.component.scss']
})
export class VersionPageComponent implements OnInit {

  versionRD$: Observable<RemoteData<Version>>;
  itemRD$: Observable<RemoteData<Item>>;

  constructor(
    protected route: ActivatedRoute,
    private router: Router,
    private versionService: VersionDataService,
    private authService: AuthService,
  ) {
  }

  ngOnInit(): void {
    /* Retrieve version from resolver or redirect on 4xx */
    this.versionRD$ = this.route.data.pipe(
      map((data) => data.dso as RemoteData<Version>),
      redirectOn4xx(this.router, this.authService),
    );

    /* Retrieve item from version and reroute to item's page or handle missing item */
    this.versionRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((version) => version.item),
      redirectOn4xx(this.router, this.authService),
      getFirstCompletedRemoteData(),
    ).subscribe((itemRD: RemoteData<Item>) => {
      if (itemRD.hasNoContent) {
        this.router.navigateByUrl(getPageNotFoundRoute(), { skipLocationChange: true });
      } else {
        const itemUrl = getItemPageRoute(itemRD.payload);
        this.router.navigateByUrl(itemUrl);
      }
    });

  }

}

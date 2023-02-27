import { Component, Input, OnInit } from '@angular/core';
import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { ScriptDataService } from '../../../core/data/processes/script-data.service';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { map } from 'rxjs/operators';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { hasValue, isNotEmpty } from '../../empty.util';
import { RemoteData } from '../../../core/data/remote-data';
import { Process } from '../../../process-page/processes/process.model';
import { getProcessDetailRoute } from '../../../process-page/process-page-routing.paths';
import { NotificationsService } from '../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { PaginatedSearchOptions } from '../models/paginated-search-options.model';

@Component({
  selector: 'ds-search-export-csv',
  styleUrls: ['./search-export-csv.component.scss'],
  templateUrl: './search-export-csv.component.html',
})
/**
 * Display a button to export the current search results as csv
 */
export class SearchExportCsvComponent implements OnInit {

  /**
   * The current configuration of the search
   */
  @Input() searchConfig: PaginatedSearchOptions;

  /**
   * Observable used to determine whether the button should be shown
   */
  shouldShowButton$: Observable<boolean>;

  /**
   * The message key used for the tooltip of the button
   */
  tooltipMsg = 'metadata-export-search.tooltip';

  constructor(private scriptDataService: ScriptDataService,
              private authorizationDataService: AuthorizationDataService,
              private notificationsService: NotificationsService,
              private translateService: TranslateService,
              private router: Router
  ) {
  }

  ngOnInit(): void {
    const scriptExists$ = this.scriptDataService.findById('metadata-export-search').pipe(
      getFirstCompletedRemoteData(),
      map((rd) => rd.isSuccess && hasValue(rd.payload))
    );

    const isAuthorized$ = this.authorizationDataService.isAuthorized(FeatureID.AdministratorOf);

    this.shouldShowButton$ = observableCombineLatest([scriptExists$, isAuthorized$]).pipe(
      map(([scriptExists, isAuthorized]: [boolean, boolean]) => scriptExists && isAuthorized)
    );
  }

  /**
   * Start the export of the items based on the current search configuration
   */
  export() {
    const parameters = [];
    if (hasValue(this.searchConfig)) {
      if (isNotEmpty(this.searchConfig.query)) {
        parameters.push({name: '-q', value: this.searchConfig.query});
      }
      if (isNotEmpty(this.searchConfig.scope)) {
        parameters.push({name: '-s', value: this.searchConfig.scope});
      }
      if (isNotEmpty(this.searchConfig.configuration)) {
        parameters.push({name: '-c', value: this.searchConfig.configuration});
      }
      if (isNotEmpty(this.searchConfig.filters)) {
        this.searchConfig.filters.forEach((filter) => {
          if (hasValue(filter.values)) {
            filter.values.forEach((value) => {
              let operator;
              let filterValue;
              if (hasValue(filter.operator)) {
                operator = filter.operator;
                filterValue = value;
              } else {
                operator = value.substring(value.lastIndexOf(',') + 1);
                filterValue = value.substring(0, value.lastIndexOf(','));
              }
              const valueToAdd = `${filter.key.substring(2)},${operator}=${filterValue}`;
              parameters.push({name: '-f', value: valueToAdd});
            });
          }
        });
      }
    }

    this.scriptDataService.invoke('metadata-export-search', parameters, []).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((rd: RemoteData<Process>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get('metadata-export-search.submit.success'));
        this.router.navigateByUrl(getProcessDetailRoute(rd.payload.processId));
      } else {
        this.notificationsService.error(this.translateService.get('metadata-export-search.submit.error'));
      }
    });
  }
}

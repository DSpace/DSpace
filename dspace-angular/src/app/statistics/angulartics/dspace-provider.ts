import { Injectable } from '@angular/core';
import { Angulartics2 } from 'angulartics2';
import { StatisticsService } from '../statistics.service';

/**
 * Angulartics2DSpace is a angulartics2 plugin that provides DSpace with the events.
 */
@Injectable({providedIn: 'root'})
export class Angulartics2DSpace {

  constructor(
    private angulartics2: Angulartics2,
    private statisticsService: StatisticsService,
  ) {
  }

  /**
   * Activates this plugin
   */
  startTracking(): void {
    this.angulartics2.eventTrack
      .pipe(this.angulartics2.filterDeveloperMode())
      .subscribe((event) => this.eventTrack(event));
  }

  private eventTrack(event) {
    if (event.action === 'page_view') {
      this.statisticsService.trackViewEvent(event.properties.object);
    } else if (event.action === 'search') {
      this.statisticsService.trackSearchEvent(
        event.properties.searchOptions,
        event.properties.page,
        event.properties.sort,
        event.properties.filters
      );
    }
  }
}

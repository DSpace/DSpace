import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { SystemWideAlertDataService } from '../../core/data/system-wide-alert-data.service';
import {
  getAllSucceededRemoteDataPayload
} from '../../core/shared/operators';
import { filter, map, switchMap } from 'rxjs/operators';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { SystemWideAlert } from '../system-wide-alert.model';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { BehaviorSubject, EMPTY, interval, Subscription } from 'rxjs';
import { zonedTimeToUtc } from 'date-fns-tz';
import { isPlatformBrowser } from '@angular/common';
import { NotificationsService } from '../../shared/notifications/notifications.service';

/**
 * Component responsible for rendering a banner and the countdown for an active system-wide alert
 */
@Component({
  selector: 'ds-system-wide-alert-banner',
  styleUrls: ['./system-wide-alert-banner.component.scss'],
  templateUrl: './system-wide-alert-banner.component.html'
})
export class SystemWideAlertBannerComponent implements OnInit, OnDestroy {

  /**
   * BehaviorSubject that keeps track of the currently configured system-wide alert
   */
  systemWideAlert$ = new BehaviorSubject<SystemWideAlert>(undefined);

  /**
   * BehaviorSubject that keeps track of the amount of minutes left to count down to
   */
  countDownMinutes = new BehaviorSubject<number>(0);

  /**
   * BehaviorSubject that keeps track of the amount of hours left to count down to
   */
  countDownHours = new BehaviorSubject<number>(0);

  /**
   * BehaviorSubject that keeps track of the amount of days left to count down to
   */
  countDownDays = new BehaviorSubject<number>(0);

  /**
   * List of subscriptions
   */
  subscriptions: Subscription[] = [];

  constructor(
    @Inject(PLATFORM_ID) protected platformId: Object,
    protected systemWideAlertDataService: SystemWideAlertDataService,
    protected notificationsService: NotificationsService,
  ) {
  }

  ngOnInit() {
    this.subscriptions.push(this.systemWideAlertDataService.searchBy('active').pipe(
      getAllSucceededRemoteDataPayload(),
      map((payload: PaginatedList<SystemWideAlert>) => payload.page),
      filter((page) => isNotEmpty(page)),
      map((page) => page[0])
    ).subscribe((alert: SystemWideAlert) => {
      this.systemWideAlert$.next(alert);
    }));

    this.subscriptions.push(this.systemWideAlert$.pipe(
      switchMap((alert: SystemWideAlert) => {
        if (hasValue(alert) && hasValue(alert.countdownTo)) {
          const date = zonedTimeToUtc(alert.countdownTo, 'UTC');
          const timeDifference = date.getTime() - new Date().getTime();
          if (timeDifference > 0) {
            this.allocateTimeUnits(timeDifference);
            if (isPlatformBrowser(this.platformId)) {
              return interval(1000);
            } else {
              return EMPTY;
            }

          }
        }
        // Reset the countDown times to 0 and return EMPTY to prevent unnecessary countdown calculations
        this.countDownDays.next(0);
        this.countDownHours.next(0);
        this.countDownMinutes.next(0);
        return EMPTY;
      })
    ).subscribe(() => {
      this.setTimeDifference(this.systemWideAlert$.getValue().countdownTo);
    }));
  }

  /**
   * Helper method to calculate the time difference between the countdown date from the system-wide alert and "now"
   * @param countdownTo - The date to count down to
   */
  private setTimeDifference(countdownTo: string) {
    const date = zonedTimeToUtc(countdownTo, 'UTC');

    const timeDifference = date.getTime() - new Date().getTime();
    this.allocateTimeUnits(timeDifference);
  }

  /**
   * Helper method to push how many days, hours and minutes are left in the countdown to their respective behaviour subject
   * @param timeDifference  - The time difference to calculate and push the time units for
   */
  private allocateTimeUnits(timeDifference) {
    const minutes = Math.floor((timeDifference) / (1000 * 60) % 60);
    const hours = Math.floor((timeDifference) / (1000 * 60 * 60) % 24);
    const days = Math.floor((timeDifference) / (1000 * 60 * 60 * 24));

    this.countDownMinutes.next(minutes);
    this.countDownHours.next(hours);
    this.countDownDays.next(days);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((sub: Subscription) => {
      if (hasValue(sub)) {
        sub.unsubscribe();
      }
    });
  }
}

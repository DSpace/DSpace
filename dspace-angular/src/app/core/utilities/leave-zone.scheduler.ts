import { SchedulerLike, Subscription } from 'rxjs';
import { NgZone } from '@angular/core';

/**
 * An RXJS scheduler that will run what's scheduled outside of the Angular zone
 */
export class LeaveZoneScheduler implements SchedulerLike {
  constructor(private zone: NgZone, private scheduler: SchedulerLike) { }

  schedule(...args: any[]): Subscription {
    return this.zone.runOutsideAngular(() =>
      this.scheduler.schedule.apply(this.scheduler, args)
    );
  }

  now (): number {
    return this.scheduler.now();
  }
}

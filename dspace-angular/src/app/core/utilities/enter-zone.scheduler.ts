import { SchedulerLike, Subscription } from 'rxjs';
import { NgZone } from '@angular/core';

/**
 *  An RXJS scheduler that will re-enter the Angular zone to run what's scheduled
 */
export class EnterZoneScheduler implements SchedulerLike {
  constructor(private zone: NgZone, private scheduler: SchedulerLike) { }

  schedule(...args: any[]): Subscription {
    return this.zone.run(() =>
      this.scheduler.schedule.apply(this.scheduler, args)
    );
  }

  now (): number {
    return this.scheduler.now();
  }
}

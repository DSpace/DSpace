import { Observable, of as observableOf } from 'rxjs';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewEncapsulation
} from '@angular/core';
import { trigger } from '@angular/animations';
import { DomSanitizer } from '@angular/platform-browser';
import { NotificationsService } from '../notifications.service';
import { scaleEnter, scaleInState, scaleLeave, scaleOutState } from '../../animations/scale';
import { rotateEnter, rotateInState, rotateLeave, rotateOutState } from '../../animations/rotate';
import { fromBottomEnter, fromBottomInState, fromBottomLeave, fromBottomOutState } from '../../animations/fromBottom';
import { fromRightEnter, fromRightInState, fromRightLeave, fromRightOutState } from '../../animations/fromRight';
import { fromLeftEnter, fromLeftInState, fromLeftLeave, fromLeftOutState } from '../../animations/fromLeft';
import { fromTopEnter, fromTopInState, fromTopLeave, fromTopOutState } from '../../animations/fromTop';
import { fadeInEnter, fadeInState, fadeOutLeave, fadeOutState } from '../../animations/fade';
import { NotificationAnimationsStatus } from '../models/notification-animations-type';
import { isNotEmpty } from '../../empty.util';
import { INotification } from '../models/notification.model';
import { filter, first } from 'rxjs/operators';

@Component({
  selector: 'ds-notification',
  encapsulation: ViewEncapsulation.None,
  animations: [
    trigger('enterLeave', [
      fadeInEnter, fadeInState, fadeOutLeave, fadeOutState,
      fromBottomEnter, fromBottomInState, fromBottomLeave, fromBottomOutState,
      fromRightEnter, fromRightInState, fromRightLeave, fromRightOutState,
      fromLeftEnter, fromLeftInState, fromLeftLeave, fromLeftOutState,
      fromTopEnter, fromTopInState, fromTopLeave, fromTopOutState,
      rotateInState, rotateEnter, rotateOutState, rotateLeave,
      scaleInState, scaleEnter, scaleOutState, scaleLeave
    ])
  ],
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class NotificationComponent implements OnInit, OnDestroy {

  @Input() public notification = null as INotification;

  /**
   * Whether this notification's countdown should be paused
   */
  @Input() public isPaused$: Observable<boolean> = observableOf(false);

  // Progress bar variables
  public title: Observable<string>;
  public content: Observable<string>;
  public html: any;
  public showProgressBar = false;
  public titleIsTemplate = false;
  public contentIsTemplate = false;
  public htmlIsTemplate = false;

  public progressWidth = 0;

  private stopTime = false;
  private timer: any;
  private steps: number;
  private speed: number;
  private count = 0;
  private start: any;
  private diff: any;
  public animate: string;

  constructor(private notificationService: NotificationsService,
              private domSanitizer: DomSanitizer,
              private cdr: ChangeDetectorRef,
              private zone: NgZone) {
  }

  ngOnInit(): void {
    this.animate = this.notification.options.animate + NotificationAnimationsStatus.In;

    if (this.notification.options.timeOut !== 0) {
      this.startTimeOut();
      this.showProgressBar = true;
    }
    this.html = this.notification.html;
    this.contentType(this.notification.title, 'title');
    this.contentType(this.notification.content, 'content');
  }

  private startTimeOut(): void {
    this.steps = this.notification.options.timeOut / 10;
    this.speed = this.notification.options.timeOut / this.steps;
    this.start = new Date().getTime();
    this.zone.runOutsideAngular(() => this.timer = setTimeout(this.instance, this.speed));
  }

  ngOnDestroy(): void {
    clearTimeout(this.timer);
  }

  private instance = () => {
    this.diff = (new Date().getTime() - this.start) - (this.count * this.speed);

    this.isPaused$.pipe(
      filter(paused => !paused),
      first(),
    ).subscribe(() => {
      if (this.count++ === this.steps) {
        this.remove();
      } else if (!this.stopTime) {
        if (this.showProgressBar) {
          this.progressWidth += 100 / this.steps;
        }

        this.timer = setTimeout(this.instance, (this.speed - this.diff));
      }
      this.zone.run(() => this.cdr.detectChanges());
    });
  };

  public remove() {
    if (this.animate) {
      this.setAnimationOut();
      setTimeout(() => {
        this.notificationService.remove(this.notification);
      }, 1000);
    } else {
      this.notificationService.remove(this.notification);
    }
  }

  private contentType(item: any, key: string) {
    if (item instanceof TemplateRef) {
      this[key] = item;
    } else if (key === 'title' || (key === 'content' && !this.html)) {
      let value = null;
      if (isNotEmpty(item)) {
        if (typeof item === 'string') {
          value = observableOf(item);
        } else if (item instanceof Observable) {
          value = item;
        } else if (typeof item === 'object' && isNotEmpty(item.value)) {
          // when notifications state is transferred from SSR to CSR,
          // Observables Object loses the instance type and become simply object,
          // so converts it again to Observable
          value = observableOf(item.value);
        }
      }
      this[key] = value;
    } else {
      this[key] = this.domSanitizer.bypassSecurityTrustHtml(item);
    }

    this[key + 'IsTemplate'] = item instanceof TemplateRef;
  }

  private setAnimationOut() {
    this.animate = this.notification.options.animate + NotificationAnimationsStatus.Out;
    this.cdr.detectChanges();
  }
}

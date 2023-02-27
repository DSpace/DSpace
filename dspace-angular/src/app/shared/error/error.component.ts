import { Component, Input } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { Subscription } from 'rxjs';
import { AlertType } from '../alert/aletr-type';

@Component({
  selector: 'ds-error',
  styleUrls: ['./error.component.scss'],
  templateUrl: './error.component.html'
})
export class ErrorComponent {

  @Input() message = 'Error...';

  /**
   * The AlertType enumeration
   * @type {AlertType}
   */
  public AlertTypeEnum = AlertType;

  private subscription: Subscription;

  constructor(private translate: TranslateService) {

  }

  ngOnInit() {
    if (this.message === undefined) {
      this.subscription = this.translate.get('error.default').subscribe((message: string) => {
        this.message = message;
      });
    }
  }

  ngOnDestroy() {
    if (this.subscription !== undefined) {
      this.subscription.unsubscribe();
    }
  }
}

import { Component, EventEmitter, Inject, Input, OnInit, Output } from '@angular/core';

import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { getFirstSucceededRemoteDataPayload } from '../../core/shared/operators';
import { Observable } from 'rxjs';
import { NativeWindowRef, NativeWindowService } from 'src/app/core/services/window.service';
import { isNotEmpty } from '../empty.util';

@Component({
  selector: 'ds-google-recaptcha',
  templateUrl: './google-recaptcha.component.html',
  styleUrls: ['./google-recaptcha.component.scss'],
})
export class GoogleRecaptchaComponent implements OnInit {

  @Input() captchaMode: string;

  /**
   * An EventEmitter that's fired whenever the form is being submitted
   */
  @Output() executeRecaptcha: EventEmitter<any> = new EventEmitter();

  @Output() checkboxChecked: EventEmitter<boolean> = new EventEmitter();

  @Output() showNotification: EventEmitter<any> = new EventEmitter();

  recaptchaKey$: Observable<any>;

  constructor(
    @Inject(NativeWindowService) private _window: NativeWindowRef,
    private configService: ConfigurationDataService,
  ) {
  }

  /**
   * Retrieve the google recaptcha site key
   */
  ngOnInit() {
    this.recaptchaKey$ = this.configService.findByPropertyName('google.recaptcha.key.site').pipe(
      getFirstSucceededRemoteDataPayload(),
    );
    this._window.nativeWindow.dataCallback = this.dataCallbackFcn;
    this._window.nativeWindow.expiredCallback = this.expiredCallbackFcn;
    this._window.nativeWindow.errorCallback = this.errorCallbackFcn;
  }

  dataCallbackFcn = ($event) => {
    switch (this.captchaMode) {
      case 'invisible':
        this.executeRecaptcha.emit($event);
        break;
      case 'checkbox':
        this.checkboxChecked.emit(isNotEmpty($event));
        break;
      default:
        console.error(`Invalid reCaptcha mode '${this.captchaMode}`);
        this.showNotification.emit('error');
    }
  };

  expiredCallbackFcn = () => {
    this.checkboxChecked.emit(false);
    this.showNotification.emit('expired');
  };

  errorCallbackFcn = () => {
    this.showNotification.emit('error');
  };

}

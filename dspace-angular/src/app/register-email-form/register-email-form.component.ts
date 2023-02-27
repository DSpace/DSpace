import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Optional } from '@angular/core';
import {EpersonRegistrationService} from '../core/data/eperson-registration.service';
import {NotificationsService} from '../shared/notifications/notifications.service';
import {TranslateService} from '@ngx-translate/core';
import {Router} from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators, ValidatorFn } from '@angular/forms';
import {Registration} from '../core/shared/registration.model';
import {RemoteData} from '../core/data/remote-data';
import {ConfigurationDataService} from '../core/data/configuration-data.service';
import { getAllSucceededRemoteDataPayload, getFirstSucceededRemoteDataPayload } from '../core/shared/operators';
import {ConfigurationProperty} from '../core/shared/configuration-property.model';
import {isNotEmpty} from '../shared/empty.util';
import {BehaviorSubject, combineLatest, Observable, of, switchMap} from 'rxjs';
import {map, startWith, take} from 'rxjs/operators';
import {CAPTCHA_NAME, GoogleRecaptchaService} from '../core/google-recaptcha/google-recaptcha.service';
import {AlertType} from '../shared/alert/aletr-type';
import {KlaroService} from '../shared/cookies/klaro.service';
import {CookieService} from '../core/services/cookie.service';
import { Subscription } from 'rxjs';

export const TYPE_REQUEST_FORGOT = 'forgot';
export const TYPE_REQUEST_REGISTER = 'register';

@Component({
  selector: 'ds-register-email-form',
  templateUrl: './register-email-form.component.html'
})
/**
 * Component responsible to render an email registration form.
 */
export class RegisterEmailFormComponent implements OnDestroy, OnInit {

  /**
   * The form containing the mail address
   */
  form: FormGroup;

  /**
   * The message prefix
   */
  @Input()
  MESSAGE_PREFIX: string;

  /**
   * Type of register request to be done, register new email or forgot password (same endpoint)
   */
  @Input()
  typeRequest: string = null;

  public AlertTypeEnum = AlertType;

  /**
   * registration verification configuration
   */
  registrationVerification = false;

  /**
   * Return true if the user completed the reCaptcha verification (checkbox mode)
   */
  checkboxCheckedSubject$ = new BehaviorSubject<boolean>(false);

  disableUntilChecked = true;

  validMailDomains: string[];
  TYPE_REQUEST_REGISTER = TYPE_REQUEST_REGISTER;

  subscriptions: Subscription[] = [];

  captchaVersion(): Observable<string> {
    return this.googleRecaptchaService.captchaVersion();
  }

  captchaMode(): Observable<string> {
    return this.googleRecaptchaService.captchaMode();
  }

  constructor(
    private epersonRegistrationService: EpersonRegistrationService,
    private notificationService: NotificationsService,
    private translateService: TranslateService,
    private router: Router,
    private formBuilder: FormBuilder,
    private configService: ConfigurationDataService,
    public googleRecaptchaService: GoogleRecaptchaService,
    public cookieService: CookieService,
    @Optional() public klaroService: KlaroService,
    private changeDetectorRef: ChangeDetectorRef,
    private notificationsService: NotificationsService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((sub: Subscription) => sub.unsubscribe());
  }

  ngOnInit(): void {
    const validators: ValidatorFn[] = [
      Validators.required,
      Validators.email,
      // Regex pattern borrowed from HTML5 specs for a valid email address:
      // https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
      Validators.pattern('^[a-zA-Z0-9.!#$%&\'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$')
    ];
    this.form = this.formBuilder.group({
      email: new FormControl('', {
        validators: validators,
      })
    });
    this.validMailDomains = [];
    if (this.typeRequest === TYPE_REQUEST_REGISTER) {
      this.subscriptions.push(this.configService.findByPropertyName('authentication-password.domain.valid')
        .pipe(getAllSucceededRemoteDataPayload())
        .subscribe((remoteData: ConfigurationProperty) => {
          this.validMailDomains = remoteData.values;
          for (const remoteValue of remoteData.values) {
            if (this.validMailDomains.length !== 0) {
              this.form.get('email').setValidators([
                ...validators,
                Validators.pattern(this.validMailDomains.map((domain: string) => '(^.*' + domain.replace(new RegExp('\\.', 'g'), '\\.') + '$)').join('|')),
              ]);
              this.form.updateValueAndValidity();
            }
          }
          this.changeDetectorRef.detectChanges();
        }));
    }
    this.subscriptions.push(this.configService.findByPropertyName('registration.verification.enabled').pipe(
      getFirstSucceededRemoteDataPayload(),
      map((res: ConfigurationProperty) => res?.values[0].toLowerCase() === 'true')
    ).subscribe((res: boolean) => {
      this.registrationVerification = res;
    }));

    this.subscriptions.push(this.disableUntilCheckedFcn().subscribe((res) => {
      this.disableUntilChecked = res;
      this.changeDetectorRef.detectChanges();
    }));
  }

  /**
   * execute the captcha function for v2 invisible
   */
  executeRecaptcha() {
    this.googleRecaptchaService.executeRecaptcha();
  }

  /**
   * Register an email address
   */
  register(tokenV2?) {
    if (!this.form.invalid) {
      if (this.registrationVerification) {
        this.subscriptions.push(combineLatest([this.captchaVersion(), this.captchaMode()]).pipe(
          switchMap(([captchaVersion, captchaMode])  => {
            if (captchaVersion === 'v3') {
              return this.googleRecaptchaService.getRecaptchaToken('register_email');
            } else if (captchaVersion === 'v2' && captchaMode === 'checkbox') {
              return of(this.googleRecaptchaService.getRecaptchaTokenResponse());
            } else if (captchaVersion === 'v2' && captchaMode === 'invisible') {
              return of(tokenV2);
            } else {
              console.error(`Invalid reCaptcha configuration: version = ${captchaVersion}, mode = ${captchaMode}`);
              this.showNotification('error');
            }
          }),
          take(1),
        ).subscribe((token) => {
            if (isNotEmpty(token)) {
              this.registration(token);
            } else {
              console.error('reCaptcha error');
              this.showNotification('error');
            }
          }
        ));
      } else {
        this.registration();
      }
    }
  }

  /**
   * Registration of an email address
   */
  registration(captchaToken = null) {
    let registerEmail$ = captchaToken ?
      this.epersonRegistrationService.registerEmail(this.email.value, captchaToken, this.typeRequest) :
      this.epersonRegistrationService.registerEmail(this.email.value, null, this.typeRequest);
    this.subscriptions.push(registerEmail$.subscribe((response: RemoteData<Registration>) => {
      if (response.hasSucceeded) {
        this.notificationService.success(this.translateService.get(`${this.MESSAGE_PREFIX}.success.head`),
          this.translateService.get(`${this.MESSAGE_PREFIX}.success.content`, {email: this.email.value}));
        this.router.navigate(['/home']);
        } else if (response.statusCode === 422) {
        this.notificationService.error(this.translateService.get(`${this.MESSAGE_PREFIX}.error.head`), this.translateService.get(`${this.MESSAGE_PREFIX}.error.maildomain`, {domains: this.validMailDomains.join(', ')}));
      } else {
        this.notificationService.error(this.translateService.get(`${this.MESSAGE_PREFIX}.error.head`),
          this.translateService.get(`${this.MESSAGE_PREFIX}.error.content`, {email: this.email.value}));
      }
    }));
  }

  /**
   * Return true if the user has accepted the required cookies for reCaptcha
   */
  isRecaptchaCookieAccepted(): boolean {
    const klaroAnonymousCookie = this.cookieService.get('klaro-anonymous');
    return isNotEmpty(klaroAnonymousCookie) ? klaroAnonymousCookie[CAPTCHA_NAME] : false;
  }

  /**
   * Return true if the user has not completed the reCaptcha verification (checkbox mode)
   */
  disableUntilCheckedFcn(): Observable<boolean> {
    const checked$ = this.checkboxCheckedSubject$.asObservable();
    return combineLatest([this.captchaVersion(), this.captchaMode(), checked$]).pipe(
      // disable if checkbox is not checked or if reCaptcha is not in v2 checkbox mode
      switchMap(([captchaVersion, captchaMode, checked])  => captchaVersion === 'v2' && captchaMode === 'checkbox' ? of(!checked) : of(false)),
      startWith(true),
    );
  }

  get email() {
    return this.form.get('email');
  }

  onCheckboxChecked(checked: boolean) {
    this.checkboxCheckedSubject$.next(checked);
  }

  /**
   * Show a notification to the user
   * @param key
   */
  showNotification(key) {
    const notificationTitle = this.translateService.get(this.MESSAGE_PREFIX + '.google-recaptcha.notification.title');
    const notificationErrorMsg = this.translateService.get(this.MESSAGE_PREFIX + '.google-recaptcha.notification.message.error');
    const notificationExpiredMsg = this.translateService.get(this.MESSAGE_PREFIX + '.google-recaptcha.notification.message.expired');
    switch (key) {
      case 'expired':
        this.notificationsService.warning(notificationTitle, notificationExpiredMsg);
        break;
      case 'error':
        this.notificationsService.error(notificationTitle, notificationErrorMsg);
        break;
      default:
        console.warn(`Unimplemented notification '${key}' from reCaptcha service`);
    }
  }

}

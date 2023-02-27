import { Component } from '@angular/core';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { Observable } from 'rxjs';
import { Registration } from '../../core/shared/registration.model';
import { map } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticateAction } from '../../core/auth/auth.actions';
import { Store } from '@ngrx/store';
import { RemoteData } from '../../core/data/remote-data';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload, } from '../../core/shared/operators';
import { CoreState } from '../../core/core-state.model';

@Component({
  selector: 'ds-forgot-password-form',
  styleUrls: ['./forgot-password-form.component.scss'],
  templateUrl: './forgot-password-form.component.html'
})
/**
 * Component for a user to enter a new password for a forgot token.
 */
export class ForgotPasswordFormComponent {

  registration$: Observable<Registration>;

  token: string;
  email: string;
  user: string;

  isInValid = true;
  password: string;

  /**
   * Prefix for the notification messages of this component
   */
  NOTIFICATIONS_PREFIX = 'forgot-password.form.notification';

  constructor(private ePersonDataService: EPersonDataService,
              private translateService: TranslateService,
              private notificationsService: NotificationsService,
              private store: Store<CoreState>,
              private router: Router,
              private route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
    this.registration$ = this.route.data.pipe(
      map((data) => data.registration as RemoteData<Registration>),
      getFirstSucceededRemoteDataPayload(),
    );
    this.registration$.subscribe((registration: Registration) => {
      this.email = registration.email;
      this.token = registration.token;
      this.user = registration.user;
    });
  }

  setInValid($event: boolean) {
    this.isInValid = $event;
  }

  setPasswordValue($event: string) {
    this.password = $event;
  }

  /**
   * Submits the password to the eperson service to be updated.
   * The submission will not be made when the form is not valid.
   */
  submit() {
    if (!this.isInValid) {
      this.ePersonDataService.patchPasswordWithToken(this.user, this.token, this.password).pipe(
        getFirstCompletedRemoteData()
      ).subscribe((response: RemoteData<EPerson>) => {
        if (response.hasSucceeded) {
          this.notificationsService.success(
            this.translateService.instant(this.NOTIFICATIONS_PREFIX + '.success.title'),
            this.translateService.instant(this.NOTIFICATIONS_PREFIX + '.success.content')
          );
          this.store.dispatch(new AuthenticateAction(this.email, this.password));
          this.router.navigate(['/home']);
        } else {
          this.notificationsService.error(
            this.translateService.instant(this.NOTIFICATIONS_PREFIX + '.error.title'), response.errorMessage
          );
        }
      });
    }
  }
}

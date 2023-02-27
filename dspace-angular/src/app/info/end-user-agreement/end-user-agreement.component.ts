import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { map, switchMap, take } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '../../app.reducer';
import { LogOutAction } from '../../core/auth/auth.actions';
import { EndUserAgreementService } from '../../core/end-user-agreement/end-user-agreement.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { isNotEmpty } from '../../shared/empty.util';

@Component({
  selector: 'ds-end-user-agreement',
  templateUrl: './end-user-agreement.component.html',
  styleUrls: ['./end-user-agreement.component.scss']
})
/**
 * Component displaying the End User Agreement and an option to accept it
 */
export class EndUserAgreementComponent implements OnInit {

  /**
   * Whether or not the user agreement has been accepted
   */
  accepted = false;

  constructor(protected endUserAgreementService: EndUserAgreementService,
              protected notificationsService: NotificationsService,
              protected translate: TranslateService,
              protected authService: AuthService,
              protected store: Store<AppState>,
              protected router: Router,
              protected route: ActivatedRoute) {
  }

  /**
   * Initialize the component
   */
  ngOnInit(): void {
    this.initAccepted();
  }

  /**
   * Initialize the "accepted" property of this component by checking if the current user has accepted it before
   */
  initAccepted() {
    this.endUserAgreementService.hasCurrentUserOrCookieAcceptedAgreement(false).subscribe((accepted) => {
      this.accepted = accepted;
    });
  }

  /**
   * Submit the form
   * Set the End User Agreement, display a notification and (optionally) redirect the user back to their original destination
   */
  submit() {
    this.endUserAgreementService.setUserAcceptedAgreement(this.accepted).pipe(
      switchMap((success) => {
        if (success) {
          this.notificationsService.success(this.translate.instant('info.end-user-agreement.accept.success'));
          return this.route.queryParams.pipe(map((params) => params.redirect));
        } else {
          this.notificationsService.error(this.translate.instant('info.end-user-agreement.accept.error'));
          return observableOf(undefined);
        }
      }),
      take(1)
    ).subscribe((redirectUrl) => {
      if (isNotEmpty(redirectUrl)) {
        this.router.navigateByUrl(decodeURIComponent(redirectUrl));
      }
    });
  }

  /**
   * Cancel the agreement
   * If the user is logged in, this will log them out
   * If the user is not logged in, they will be redirected to the homepage
   */
  cancel() {
    this.authService.isAuthenticated().pipe(take(1)).subscribe((authenticated) => {
      if (authenticated) {
        this.store.dispatch(new LogOutAction());
      } else {
        this.router.navigate(['home']);
      }
    });
  }

}

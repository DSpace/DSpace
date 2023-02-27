import { Injectable } from '@angular/core';
import { Observable, of as observableOf } from 'rxjs';
import { AbstractEndUserAgreementGuard } from './abstract-end-user-agreement.guard';
import { EndUserAgreementService } from './end-user-agreement.service';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

/**
 * A guard redirecting logged in users to the end agreement page when they haven't accepted the latest user agreement
 */
@Injectable()
export class EndUserAgreementCurrentUserGuard extends AbstractEndUserAgreementGuard {

  constructor(protected endUserAgreementService: EndUserAgreementService,
              protected router: Router) {
    super(router);
  }

  /**
   * True when the currently logged in user has accepted the agreements or when the user is not currently authenticated
   */
  hasAccepted(): Observable<boolean> {
    if (!environment.info.enableEndUserAgreement) {
      return observableOf(true);
    }

    return this.endUserAgreementService.hasCurrentUserAcceptedAgreement(true);
  }

}

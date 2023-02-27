import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { RemoteData } from '../../../../core/data/remote-data';
import { first, map } from 'rxjs/operators';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { getFirstCompletedRemoteData } from '../../../../core/shared/operators';
import { NoContent } from '../../../../core/shared/NoContent.model';
import { ComColDataService } from '../../../../core/data/comcol-data.service';
import { Community } from '../../../../core/shared/community.model';
import { Collection } from '../../../../core/shared/collection.model';

/**
 * Component representing the delete page for communities and collections
 */
@Component({
  selector: 'ds-delete-comcol',
  template: ''
})
export class DeleteComColPageComponent<TDomain extends Community | Collection> implements OnInit {
  /**
   * Frontend endpoint for this type of DSO
   */
  protected frontendURL: string;
  /**
   * The initial DSO object
   */
  public dsoRD$: Observable<RemoteData<TDomain>>;

  /**
   * A boolean representing if a delete operation is pending
   * @type {BehaviorSubject<boolean>}
   */
  public processing$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  public constructor(
    protected dsoDataService: ComColDataService<TDomain>,
    protected router: Router,
    protected route: ActivatedRoute,
    protected notifications: NotificationsService,
    protected translate: TranslateService,
  ) {
  }

  ngOnInit(): void {
    this.dsoRD$ = this.route.data.pipe(first(), map((data) => data.dso));
  }

  /**
   * @param {TDomain} dso The DSO to delete
   * Deletes an existing DSO and redirects to the home page afterwards, showing a notification that states whether or not the deletion was successful
   */
  onConfirm(dso: TDomain) {
    this.processing$.next(true);
    this.dsoDataService.delete(dso.id)
      .pipe(getFirstCompletedRemoteData())
      .subscribe((response: RemoteData<NoContent>) => {
        if (response.hasSucceeded) {
          const successMessage = this.translate.instant((dso as any).type + '.delete.notification.success');
          this.notifications.success(successMessage);
        } else {
          const errorMessage = this.translate.instant((dso as any).type + '.delete.notification.fail');
          this.notifications.error(errorMessage);
        }
        this.router.navigate(['/']);
      });
  }

  /**
   * @param {TDomain} dso The DSO for which the delete action was canceled
   * When a delete is canceled, the user is redirected to the DSO's edit page
   */
  onCancel(dso: TDomain) {
    this.router.navigate([this.frontendURL + '/' + dso.uuid + '/edit']);
  }
}

import { Router } from '@angular/router';
import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';

import { take, tap } from 'rxjs/operators';

import { MyDSpaceActionsServiceFactory } from './mydspace-actions-service.factory';
import { RemoteData } from '../../core/data/remote-data';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { ResourceType } from '../../core/shared/resource-type';
import { NotificationOptions } from '../notifications/models/notification-options.model';
import { NotificationsService } from '../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { RequestService } from '../../core/data/request.service';
import { BehaviorSubject, Subscription } from 'rxjs';
import { SearchService } from '../../core/shared/search/search.service';
import { getFirstSucceededRemoteData } from '../../core/shared/operators';
import { IdentifiableDataService } from '../../core/data/base/identifiable-data.service';

export interface MyDSpaceActionsResult {
  result: boolean;
  reloadedObject: DSpaceObject;
}

/**
 * Abstract class for all different representations of mydspace actions
 */
@Component({
  selector: 'ds-mydspace-actions-abstract',
  template: '',
})
export abstract class MyDSpaceActionsComponent<T extends DSpaceObject, TService extends IdentifiableDataService<T>> {

  /**
   * The target mydspace object
   */
  @Input() abstract object: T;

  /**
   * Emit to notify the instantiator when the action has been performed.
   */
  @Output() processCompleted = new EventEmitter<MyDSpaceActionsResult>();

  /**
   * A boolean representing if an operation is pending
   * @type {BehaviorSubject<boolean>}
   */
  public processing$ = new BehaviorSubject<boolean>(false);

  /**
   * Instance of DataService related to mydspace object
   */
  protected objectDataService: TService;

  protected subscription: Subscription;

  /**
   * Initialize instance variables
   *
   * @param {ResourceType} objectType
   * @param {Injector} injector
   * @param {Router} router
   * @param {NotificationsService} notificationsService
   * @param {TranslateService} translate
   * @param {SearchService} searchService
   * @param {RequestService} requestService
   */
  constructor(
    protected objectType: ResourceType,
    protected injector: Injector,
    protected router: Router,
    protected notificationsService: NotificationsService,
    protected translate: TranslateService,
    protected searchService: SearchService,
    protected requestService: RequestService,
  ) {
    const factory = new MyDSpaceActionsServiceFactory<T, TService>();
    this.objectDataService = injector.get(factory.getConstructor(objectType));
  }

  /**
   * Abstract method called to init the target object
   *
   * @param {T} object
   */
  abstract initObjects(object: T): void;

  /**
   * Refresh current page
   */
  reload(): void {

    this.router.navigated = false;
    const url = decodeURIComponent(this.router.url);
    // override the route reuse strategy
    this.router.routeReuseStrategy.shouldReuseRoute = () => {
      return false;
    };
    // This assures that the search cache is empty before reloading mydspace.
    // See https://github.com/DSpace/dspace-angular/pull/468
    this.searchService.getEndpoint().pipe(
      take(1),
      tap((cachedHref: string) => this.requestService.removeByHrefSubstring(cachedHref))
    ).subscribe(() => this.router.navigateByUrl(url));
  }

  /**
   * Override the target object with a refreshed one
   */
  refresh(): void {

    // find object by id
    this.objectDataService.findById(this.object.id, false).pipe(
      getFirstSucceededRemoteData(),
    ).subscribe((rd: RemoteData<T>) => {
      this.initObjects(rd.payload as T);
    });
  }

  /**
   * Handle action response and show properly notification
   *
   * @param result
   *    true on success, false otherwise
   */
  handleActionResponse(result: boolean): void {
    if (result) {
      this.reload();
      this.notificationsService.success(null,
        this.translate.get('submission.workflow.tasks.generic.success'),
        new NotificationOptions(5000, false));
    } else {
      this.notificationsService.error(null,
        this.translate.get('submission.workflow.tasks.generic.error'),
        new NotificationOptions(20000, true));
    }
  }
}

import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { hasValue } from '../../shared/empty.util';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { EpersonDtoModel } from '../../core/eperson/models/eperson-dto.model';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { getAllSucceededRemoteData, getFirstCompletedRemoteData } from '../../core/shared/operators';
import { ConfirmationModalComponent } from '../../shared/confirmation-modal/confirmation-modal.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RequestService } from '../../core/data/request.service';
import { PageInfo } from '../../core/shared/page-info.model';
import { NoContent } from '../../core/shared/NoContent.model';
import { PaginationService } from '../../core/pagination/pagination.service';

@Component({
  selector: 'ds-epeople-registry',
  templateUrl: './epeople-registry.component.html',
})
/**
 * A component used for managing all existing epeople within the repository.
 * The admin can create, edit or delete epeople here.
 */
export class EPeopleRegistryComponent implements OnInit, OnDestroy {

  labelPrefix = 'admin.access-control.epeople.';

  /**
   * A list of all the current EPeople within the repository or the result of the search
   */
  ePeople$: BehaviorSubject<PaginatedList<EPerson>> = new BehaviorSubject(buildPaginatedList<EPerson>(new PageInfo(), []));
  /**
   * A BehaviorSubject with the list of EpersonDtoModel objects made from the EPeople in the repository or
   * as the result of the search
   */
  ePeopleDto$: BehaviorSubject<PaginatedList<EpersonDtoModel>> = new BehaviorSubject<PaginatedList<EpersonDtoModel>>({} as any);

  /**
   * An observable for the pageInfo, needed to pass to the pagination component
   */
  pageInfoState$: BehaviorSubject<PageInfo> = new BehaviorSubject<PageInfo>(undefined);

  /**
   * A boolean representing if a search is pending
   */
  searching$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * Pagination config used to display the list of epeople
   */
  config: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'elp',
    pageSize: 5,
    currentPage: 1
  });

  /**
   * Whether or not to show the EPerson form
   */
  isEPersonFormShown: boolean;

  // The search form
  searchForm;

  // Current search in epersons registry
  currentSearchQuery: string;
  currentSearchScope: string;

  /**
   * FindListOptions
   */
  findListOptionsSub: Subscription;

  /**
   * List of subscriptions
   */
  subs: Subscription[] = [];

  constructor(private epersonService: EPersonDataService,
              private translateService: TranslateService,
              private notificationsService: NotificationsService,
              private authorizationService: AuthorizationDataService,
              private formBuilder: FormBuilder,
              private router: Router,
              private modalService: NgbModal,
              private paginationService: PaginationService,
              public requestService: RequestService) {
    this.currentSearchQuery = '';
    this.currentSearchScope = 'metadata';
    this.searchForm = this.formBuilder.group(({
      scope: 'metadata',
      query: '',
    }));
  }

  ngOnInit() {
    this.initialisePage();
  }

  /**
   * This method will initialise the page
   */
  initialisePage() {
    this.searching$.next(true);
    this.isEPersonFormShown = false;
    this.search({scope: this.currentSearchScope, query: this.currentSearchQuery});
    this.subs.push(this.epersonService.getActiveEPerson().subscribe((eperson: EPerson) => {
      if (eperson != null && eperson.id) {
        this.isEPersonFormShown = true;
      }
    }));
    this.subs.push(this.ePeople$.pipe(
      switchMap((epeople: PaginatedList<EPerson>) => {
        if (epeople.pageInfo.totalElements > 0) {
          return combineLatest(...epeople.page.map((eperson) => {
            return this.authorizationService.isAuthorized(FeatureID.CanDelete, hasValue(eperson) ? eperson.self : undefined).pipe(
              map((authorized) => {
                const epersonDtoModel: EpersonDtoModel = new EpersonDtoModel();
                epersonDtoModel.ableToDelete = authorized;
                epersonDtoModel.eperson = eperson;
                return epersonDtoModel;
              })
            );
          })).pipe(map((dtos: EpersonDtoModel[]) => {
            return buildPaginatedList(epeople.pageInfo, dtos);
          }));
        } else {
          // if it's empty, simply forward the empty list
          return [epeople];
        }
      })).subscribe((value: PaginatedList<EpersonDtoModel>) => {
      this.searching$.next(false);this.ePeopleDto$.next(value);
      this.pageInfoState$.next(value.pageInfo);
    }));
  }

  /**
   * Search in the EPeople by metadata (default) or email
   * @param data  Contains scope and query param
   */
  search(data: any) {
    this.searching$.next(true);
    if (hasValue(this.findListOptionsSub)) {
      this.findListOptionsSub.unsubscribe();
    }
    this.findListOptionsSub = this.paginationService.getCurrentPagination(this.config.id, this.config).pipe(
      switchMap((findListOptions) => {
          const query: string = data.query;
          const scope: string = data.scope;
          if (query != null && this.currentSearchQuery !== query) {
            this.router.navigate([this.epersonService.getEPeoplePageRouterLink()], {
              queryParamsHandling: 'merge'
            });
            this.currentSearchQuery = query;
            this.paginationService.resetPage(this.config.id);
          }
          if (scope != null && this.currentSearchScope !== scope) {
            this.router.navigate([this.epersonService.getEPeoplePageRouterLink()], {
              queryParamsHandling: 'merge'
            });
            this.currentSearchScope = scope;
            this.paginationService.resetPage(this.config.id);

          }
          return this.epersonService.searchByScope(this.currentSearchScope, this.currentSearchQuery, {
            currentPage: findListOptions.currentPage,
            elementsPerPage: findListOptions.pageSize
          });
        }
      ),
      getAllSucceededRemoteData(),
    ).subscribe((peopleRD) => {
        this.ePeople$.next(peopleRD.payload);
        this.pageInfoState$.next(peopleRD.payload.pageInfo);
      }
    );
  }

  /**
   * Checks whether the given EPerson is active (being edited)
   * @param eperson
   */
  isActive(eperson: EPerson): Observable<boolean> {
    return this.getActiveEPerson().pipe(
      map((activeEPerson) => eperson === activeEPerson)
    );
  }

  /**
   * Gets the active eperson (being edited)
   */
  getActiveEPerson(): Observable<EPerson> {
    return this.epersonService.getActiveEPerson();
  }

  /**
   * Start editing the selected EPerson
   * @param ePerson
   */
  toggleEditEPerson(ePerson: EPerson) {
    this.getActiveEPerson().pipe(take(1)).subscribe((activeEPerson: EPerson) => {
      if (ePerson === activeEPerson) {
        this.epersonService.cancelEditEPerson();
        this.isEPersonFormShown = false;
      } else {
        this.epersonService.editEPerson(ePerson);
        this.isEPersonFormShown = true;
      }
    });
    this.scrollToTop();
  }

  /**
   * Deletes EPerson, show notification on success/failure & updates EPeople list
   */
  deleteEPerson(ePerson: EPerson) {
    if (hasValue(ePerson.id)) {
      const modalRef = this.modalService.open(ConfirmationModalComponent);
      modalRef.componentInstance.dso = ePerson;
      modalRef.componentInstance.headerLabel = 'confirmation-modal.delete-eperson.header';
      modalRef.componentInstance.infoLabel = 'confirmation-modal.delete-eperson.info';
      modalRef.componentInstance.cancelLabel = 'confirmation-modal.delete-eperson.cancel';
      modalRef.componentInstance.confirmLabel = 'confirmation-modal.delete-eperson.confirm';
      modalRef.componentInstance.brandColor = 'danger';
      modalRef.componentInstance.confirmIcon = 'fas fa-trash';
      modalRef.componentInstance.response.pipe(take(1)).subscribe((confirm: boolean) => {
        if (confirm) {
          if (hasValue(ePerson.id)) {
            this.epersonService.deleteEPerson(ePerson).pipe(getFirstCompletedRemoteData()).subscribe((restResponse: RemoteData<NoContent>) => {
              if (restResponse.hasSucceeded) {
                this.notificationsService.success(this.translateService.get(this.labelPrefix + 'notification.deleted.success', {name: ePerson.name}));
              } else {
                this.notificationsService.error('Error occured when trying to delete EPerson with id: ' + ePerson.id + ' with code: ' + restResponse.statusCode + ' and message: ' + restResponse.errorMessage);
              }
            });
          }
        }
      });
    }
  }

  /**
   * Unsub all subscriptions
   */
  ngOnDestroy(): void {
    this.cleanupSubscribes();
    this.paginationService.clearPagination(this.config.id);
  }


  cleanupSubscribes() {
    this.subs.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
  }

  scrollToTop() {
    (function smoothscroll() {
      const currentScroll = document.documentElement.scrollTop || document.body.scrollTop;
      if (currentScroll > 0) {
        window.requestAnimationFrame(smoothscroll);
        window.scrollTo(0, currentScroll - (currentScroll / 8));
      }
    })();
  }

  /**
   * Reset all input-fields to be empty and search all search
   */
  clearFormAndResetResult() {
    this.searchForm.patchValue({
      query: '',
    });
    this.search({query: ''});
  }

  /**
   * This method will set everything to stale, which will cause the lists on this page to update.
   */
  reset() {
    this.epersonService.getBrowseEndpoint().pipe(
      take(1)
    ).subscribe((href: string) => {
      this.requestService.setStaleByHrefSubstring(href).pipe(take(1)).subscribe(() => {
        this.epersonService.cancelEditEPerson();
        this.isEPersonFormShown = false;
      });
    });
  }
}

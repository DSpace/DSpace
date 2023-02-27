import {ChangeDetectionStrategy, Component, Inject } from '@angular/core';

import { Observable, of as observableOf, Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { SectionsType } from '../sections-type';
import { SectionModelComponent } from '../models/section.model';
import { renderSectionFor } from '../sections-decorator';
import { SectionDataObject } from '../models/section-data.model';
import { SubmissionService } from '../../submission.service';
import { AlertType } from '../../../shared/alert/aletr-type';
import { SectionsService } from '../sections.service';
import { WorkspaceitemSectionIdentifiersObject } from '../../../core/submission/models/workspaceitem-section-identifiers.model';

/**
 * This simple component displays DOI, handle and other identifiers that are already minted for the item in
 * a workflow / submission section.
 * ShowMintIdentifierStep will attempt to reserve an identifier before injecting result data for this component.
 *
 * @author Kim Shepherd
 */
@Component({
  selector: 'ds-submission-section-identifiers',
  templateUrl: './section-identifiers.component.html',
  changeDetection: ChangeDetectionStrategy.Default
})

@renderSectionFor(SectionsType.Identifiers)
export class SubmissionSectionIdentifiersComponent extends SectionModelComponent {
  /**
   * The Alert categories.
   * @type {AlertType}
   */
  public AlertTypeEnum = AlertType;

  /**
   * Variable to track if the section is loading.
   * @type {boolean}
   */
  public isLoading = true;

  /**
   * Observable identifierData subject
   * @type {Observable<WorkspaceitemSectionIdentifiersObject>}
   */
  public identifierData$: Observable<WorkspaceitemSectionIdentifiersObject> = new Observable<WorkspaceitemSectionIdentifiersObject>();

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  protected subs: Subscription[] = [];
  public subbedIdentifierData: WorkspaceitemSectionIdentifiersObject;

  /**
   * Initialize instance variables.
   *
   * @param {PaginationService} paginationService
   * @param {TranslateService} translate
   * @param {SectionsService} sectionService
   * @param {SubmissionService} submissionService
   * @param {string} injectedCollectionId
   * @param {SectionDataObject} injectedSectionData
   * @param {string} injectedSubmissionId
   */
  constructor(protected translate: TranslateService,
              protected sectionService: SectionsService,
              protected submissionService: SubmissionService,
              @Inject('collectionIdProvider') public injectedCollectionId: string,
              @Inject('sectionDataProvider') public injectedSectionData: SectionDataObject,
              @Inject('submissionIdProvider') public injectedSubmissionId: string) {
    super(injectedCollectionId, injectedSectionData, injectedSubmissionId);
  }

  ngOnInit() {
      super.ngOnInit();
  }

  /**
   * Initialize all instance variables and retrieve configuration.
   */
  onSectionInit() {
    this.isLoading = false;
    this.identifierData$ = this.getIdentifierData();
  }

  /**
   * Check if identifier section has read-only visibility
   */
  isReadOnly(): boolean {
    return true;
  }

  /**
   * Unsubscribe from all subscriptions, if needed.
   */
  onSectionDestroy(): void {
    return;
  }

  /**
   * Get section status. Because this simple component never requires human interaction, this is basically
   * always going to be the opposite of "is this section still loading". This is not the place for API response
   * error checking but determining whether the step can 'proceed'.
   *
   * @return Observable<boolean>
   *     the section status
   */
  public getSectionStatus(): Observable<boolean> {
    return observableOf(!this.isLoading);
  }

  /**
   * Get identifier data (from the REST service) as a simple object with doi, handle, otherIdentifiers variables
   * and as an observable so it can update in real-time.
   */
  getIdentifierData() {
    return this.sectionService.getSectionData(this.submissionId, this.sectionData.id, this.sectionData.sectionType) as
      Observable<WorkspaceitemSectionIdentifiersObject>;
  }

}

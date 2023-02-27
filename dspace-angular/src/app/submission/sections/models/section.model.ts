import { Component, Inject, OnDestroy, OnInit } from '@angular/core';

import { Observable, Subscription } from 'rxjs';
import { filter, startWith } from 'rxjs/operators';

import { SectionDataObject } from './section-data.model';
import { SectionsService } from '../sections.service';
import { hasValue, isNotUndefined } from '../../../shared/empty.util';

export interface SectionDataModel {
  sectionData: SectionDataObject;
}

/**
 * An abstract model class for a submission edit form section.
 */
@Component({
  selector: 'ds-section-model',
  template: ''
})
export abstract class SectionModelComponent implements OnDestroy, OnInit, SectionDataModel {
  protected abstract sectionService: SectionsService;

  /**
   * The collection id this submission belonging to
   * @type {string}
   */
  collectionId: string;

  /**
   * The section data
   * @type {SectionDataObject}
   */
  sectionData: SectionDataObject;

  /**
   * The submission id
   * @type {string}
   */
  submissionId: string;

  /**
   * A boolean representing if this section is valid
   * @type {boolean}
   */
  protected valid: boolean;

  /**
   * The Subscription to section status observable
   * @type {Subscription}
   */
  private sectionStatusSub: Subscription;

  /**
   * Initialize instance variables
   *
   * @param {string} injectedCollectionId
   * @param {SectionDataObject} injectedSectionData
   * @param {string} injectedSubmissionId
   */
  public constructor(@Inject('collectionIdProvider') public injectedCollectionId: string,
    @Inject('sectionDataProvider') public injectedSectionData: SectionDataObject,
    @Inject('submissionIdProvider') public injectedSubmissionId: string) {
    this.collectionId = injectedCollectionId;
    this.sectionData = injectedSectionData;
    this.submissionId = injectedSubmissionId;
  }

  /**
   * Call abstract methods on component init
   */
  ngOnInit(): void {
    this.onSectionInit();
    this.updateSectionStatus();
  }

  /**
   * Abstract method to implement to get section status
   *
   * @return Observable<boolean>
   *     the section status
   */
  protected abstract getSectionStatus(): Observable<boolean>;

  /**
   * Abstract method called on component init.
   * It must be used instead of ngOnInit on the component that extend this abstract class
   *
   * @return Observable<boolean>
   *     the section status
   */
  protected abstract onSectionInit(): void;

  /**
   * Abstract method called on component destroy.
   * It must be used instead of ngOnDestroy on the component that extend this abstract class
   *
   * @return Observable<boolean>
   *     the section status
   */
  protected abstract onSectionDestroy(): void;

  /**
   * Subscribe to section status
   */
  protected updateSectionStatus(): void {
    this.sectionStatusSub = this.getSectionStatus().pipe(
      filter((sectionStatus: boolean) => isNotUndefined(sectionStatus)),
      startWith(true))
      .subscribe((sectionStatus: boolean) => {
        this.sectionService.setSectionStatus(this.submissionId, this.sectionData.id, sectionStatus);
      });
  }

  /**
   * Unsubscribe from all subscriptions and Call abstract methods on component destroy
   */
  ngOnDestroy(): void {
    if (hasValue(this.sectionStatusSub)) {
      this.sectionStatusSub.unsubscribe();
    }
    this.onSectionDestroy();
  }
}

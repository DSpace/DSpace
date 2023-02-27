import { Component, Input, OnInit, } from '@angular/core';

import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { SectionsService } from '../../sections/sections.service';
import { HostWindowService } from '../../../shared/host-window.service';
import { SubmissionService } from '../../submission.service';
import { SectionDataObject } from '../../sections/models/section-data.model';

/**
 * This component allow to add any new section to submission form
 */
@Component({
  selector: 'ds-submission-form-section-add',
  styleUrls: [ './submission-form-section-add.component.scss' ],
  templateUrl: './submission-form-section-add.component.html'
})
export class SubmissionFormSectionAddComponent implements OnInit {

  /**
   * The collection id this submission belonging to
   * @type {string}
   */
  @Input() collectionId: string;

  /**
   * The submission id
   * @type {string}
   */
  @Input() submissionId: string;

  /**
   * The possible section list to add
   * @type {Observable<SectionDataObject[]>}
   */
  public sectionList$: Observable<SectionDataObject[]>;

  /**
   * A boolean representing if there are available sections to add
   * @type {Observable<boolean>}
   */
  public hasSections$: Observable<boolean>;

  /**
   * Initialize instance variables
   *
   * @param {SectionsService} sectionService
   * @param {SubmissionService} submissionService
   * @param {HostWindowService} windowService
   */
  constructor(private sectionService: SectionsService,
              private submissionService: SubmissionService,
              public windowService: HostWindowService) {
  }

  /**
   * Initialize all instance variables
   */
  ngOnInit() {
    this.sectionList$ = this.submissionService.getDisabledSectionsList(this.submissionId);
    this.hasSections$ = this.sectionList$.pipe(
      map((list: SectionDataObject[]) => list.length > 0)
    );
  }

  /**
   * Dispatch an action to add a new section
   */
  addSection(sectionId) {
    this.sectionService.addSection(this.submissionId, sectionId);
  }
}

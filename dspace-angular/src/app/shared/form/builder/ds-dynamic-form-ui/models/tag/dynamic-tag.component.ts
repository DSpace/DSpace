import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { DynamicFormLayoutService, DynamicFormValidationService } from '@ng-dynamic-forms/core';
import { Observable, of as observableOf } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, merge, switchMap, tap } from 'rxjs/operators';
import { NgbTypeahead, NgbTypeaheadSelectItemEvent } from '@ng-bootstrap/ng-bootstrap';
import isEqual from 'lodash/isEqual';

import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { DynamicTagModel } from './dynamic-tag.model';
import { Chips } from '../../../../chips/models/chips.model';
import { hasValue, isNotEmpty } from '../../../../../empty.util';
import { environment } from '../../../../../../../environments/environment';
import { getFirstSucceededRemoteDataPayload } from '../../../../../../core/shared/operators';
import {
  PaginatedList,
  buildPaginatedList
} from '../../../../../../core/data/paginated-list.model';
import { VocabularyEntry } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { PageInfo } from '../../../../../../core/shared/page-info.model';
import { DsDynamicVocabularyComponent } from '../dynamic-vocabulary.component';

/**
 * Component representing a tag input field
 */
@Component({
  selector: 'ds-dynamic-tag',
  styleUrls: ['./dynamic-tag.component.scss'],
  templateUrl: './dynamic-tag.component.html'
})
export class DsDynamicTagComponent extends DsDynamicVocabularyComponent implements OnInit {

  @Input() bindId = true;
  @Input() group: FormGroup;
  @Input() model: DynamicTagModel;

  @Output() blur: EventEmitter<any> = new EventEmitter<any>();
  @Output() change: EventEmitter<any> = new EventEmitter<any>();
  @Output() focus: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild('instance') instance: NgbTypeahead;

  chips: Chips;
  hasAuthority: boolean;

  searching = false;
  searchFailed = false;
  hideSearchingWhenUnsubscribed = new Observable(() => () => this.changeSearchingStatus(false));
  currentValue: any;
  public pageInfo: PageInfo;

  constructor(protected vocabularyService: VocabularyService,
              private cdr: ChangeDetectorRef,
              protected layoutService: DynamicFormLayoutService,
              protected validationService: DynamicFormValidationService
  ) {
    super(vocabularyService, layoutService, validationService);
  }

  /**
   * Converts an item from the result list to a `string` to display in the `<input>` field.
   */
  formatter = (x: { display: string }) => x.display;

  /**
   * Converts a stream of text values from the `<input>` element to the stream of the array of items
   * to display in the typeahead popup.
   */
  search = (text$: Observable<string>) =>
    text$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.changeSearchingStatus(true)),
      switchMap((term) => {
        if (term === '' || term.length < this.model.minChars) {
          return observableOf({ list: [] });
        } else {
          return this.vocabularyService.getVocabularyEntriesByValue(term, false, this.model.vocabularyOptions, new PageInfo()).pipe(
            getFirstSucceededRemoteDataPayload(),
            tap(() => this.searchFailed = false),
            catchError(() => {
              this.searchFailed = true;
              return observableOf(buildPaginatedList(
                new PageInfo(),
                []
              ));
            }));
        }
      }),
      map((list: PaginatedList<VocabularyEntry>) => list.page),
      tap(() => this.changeSearchingStatus(false)),
      merge(this.hideSearchingWhenUnsubscribed));

  /**
   * Initialize the component, setting up the init form value
   */
  ngOnInit() {
    this.hasAuthority = this.model.vocabularyOptions && hasValue(this.model.vocabularyOptions.name);

    this.chips = new Chips(
      this.model.value as any[],
      'display',
      null,
      environment.submission.icons.metadata);

    this.chips.chipsItems
      .subscribe((subItems: any[]) => {
        const items = this.chips.getChipsItems();
        // Does not emit change if model value is equal to the current value
        if (!isEqual(items, this.model.value)) {
          this.dispatchUpdate(items);
        }
      });
  }

  /**
   * Changes the searching status
   * @param status
   */
  changeSearchingStatus(status: boolean) {
    this.searching = status;
    this.cdr.detectChanges();
  }

  /**
   * Mark form group as dirty on input
   * @param event
   */
  onInput(event) {
    if (event.data) {
      this.group.markAsDirty();
    }
    this.cdr.detectChanges();
  }

  /**
   * Emits a blur event containing a given value and add all tags to chips.
   * @param event The value to emit.
   */
  onBlur(event: Event) {
    if (isNotEmpty(this.currentValue) && !this.instance.isPopupOpen()) {
      this.addTagsToChips();
    }
    this.blur.emit(event);
  }

  /**
   * Updates model value with the selected value and add a new tag to chips.
   * @param event The value to set.
   */
  onSelectItem(event: NgbTypeaheadSelectItemEvent) {
    this.chips.add(event.item);
    // this.group.controls[this.model.id].setValue(this.model.value);
    this.updateModel(event);

    setTimeout(() => {
      // Reset the input text after x ms, mandatory or the formatter overwrite it
      this.setCurrentValue(null);
      this.cdr.detectChanges();
    }, 50);
  }

  updateModel(event) {
    /*    this.model.valueUpdates.next(this.chips.getChipsItems());
        this.change.emit(event);*/
    this.dispatchUpdate(this.chips.getChipsItems());
  }

  /**
   * Add a new tag with typed text when typing 'Enter' or ',' or ';'
   * @param event the keyUp event
   */
  onKeyUp(event) {
    if (event.keyCode === 13 || event.keyCode === 188) {
      event.preventDefault();
      // Key: 'Enter' or ',' or ';'
      this.addTagsToChips();
      event.stopPropagation();
    }
  }

  /**
   * Prevent propagation of a key event in case of return key is pressed
   * @param event the key event
   */
  preventEventsPropagation(event) {
    event.stopPropagation();
    if (event.keyCode === 13) {
      event.preventDefault();
    }
  }

  /**
   * Sets the current value with the given value.
   * @param value The value to set.
   * @param init Representing if is init value or not.
   */
  public setCurrentValue(value: any, init = false) {
    this.currentValue = value;
  }

  private addTagsToChips() {
    if (hasValue(this.currentValue) && (!this.hasAuthority || !this.model.vocabularyOptions.closed)) {
      let res: string[] = [];
      res = this.currentValue.split(',');

      const res1 = [];
      res.forEach((item) => {
        item.split(';').forEach((i) => {
          res1.push(i);
        });
      });

      res1.forEach((c) => {
        c = c.trim();
        if (c.length > 0) {
          this.chips.add(c);
        }
      });

      // this.currentValue = '';
      setTimeout(() => {
        // Reset the input text after x ms, mandatory or the formatter overwrite it
        this.setCurrentValue(null);
        this.cdr.detectChanges();
      }, 50);
      this.updateModel(event);
    }
  }
}

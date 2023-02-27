import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder } from '@angular/forms';

import { Subscription } from 'rxjs';

import { SearchEvent } from '../eperson-group-list.component';
import { isNotNull } from '../../empty.util';

/**
 * A component used to show a search box for groups.
 */
@Component({
  selector: 'ds-group-search-box',
  templateUrl: './group-search-box.component.html',
})
export class GroupSearchBoxComponent {

  labelPrefix = 'admin.access-control.groups.';

  /**
   * The search form
   */
  searchForm;

  /**
   * List of subscriptions
   */
  subs: Subscription[] = [];

  /**
   * An event fired when a search is triggred.
   * Event's payload is a SearchEvent.
   */
  @Output() search: EventEmitter<SearchEvent> = new EventEmitter<SearchEvent>();

  constructor(private formBuilder: FormBuilder) {
    this.searchForm = this.formBuilder.group(({
      query: '',
    }));
  }

  /**
   * Reset the search form
   */
  reset() {
    this.searchForm = this.formBuilder.group(({
      query: '',
    }));
  }

  /**
   * Emit a new search event
   * @param data  Form data
   */
  submit(data: any) {
    const event: SearchEvent = {
      scope: '',
      query: isNotNull(data) ? data.query : ''
    };
    this.search.emit(event);
  }
}

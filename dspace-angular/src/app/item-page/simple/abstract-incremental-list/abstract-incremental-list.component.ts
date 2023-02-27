import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { hasValue, isNotEmpty } from '../../../shared/empty.util';

@Component({
  selector: 'ds-abstract-incremental-list',
  template: ``,
})
/**
 * An abstract component for displaying an incremental list of objects
 */
export class AbstractIncrementalListComponent<T> implements OnInit, OnDestroy {

  /**
   * The amount to increment the list by
   * Define this amount in the child component overriding this component
   */
  incrementBy: number;

  /**
   * All pages of objects to display as an array
   */
  objects: T[];

  /**
   * Placeholder css class (defined in global-styles)
   */
  placeholderFontClass: string;

  /**
   * A list of open subscriptions
   */
  subscriptions: Subscription[];

  ngOnInit(): void {
    this.objects = [];
    this.subscriptions = [];
    this.increase();
  }

  /**
   * Get a specific page
   * > Override this method to return a specific page
   * @param page  The page to fetch
   */
  getPage(page: number): T {
    return undefined;
  }

  /**
   * Increase the amount displayed
   */
  increase() {
    const page = this.getPage(this.objects.length + 1);
    if (hasValue(page)) {
      this.objects.push(page);
    }
  }

  /**
   * Decrease the amount displayed
   */
  decrease() {
    if (this.objects.length > 1) {
      this.objects.pop();
    }
  }

  /**
   * Unsubscribe from any open subscriptions
   */
  ngOnDestroy(): void {
    if (isNotEmpty(this.subscriptions)) {
      this.subscriptions.forEach((sub: Subscription) => {
        sub.unsubscribe();
      });
    }
  }
}

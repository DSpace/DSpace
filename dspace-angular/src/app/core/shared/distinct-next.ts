/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { BehaviorSubject } from 'rxjs';

/**
 * Use nextValue to update a given BehaviorSubject, only if it differs from its current value
 *
 * @param bs a BehaviorSubject
 * @param nextValue the next value for that BehaviorSubject
 * @protected
 */
export function distinctNext<T>(bs: BehaviorSubject<T>, nextValue: T): void {
  if (bs.getValue() !== nextValue) {
    bs.next(nextValue);
  }
}

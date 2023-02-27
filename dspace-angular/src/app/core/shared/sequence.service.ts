/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { Injectable } from '@angular/core';

@Injectable()
/**
 * Provides unique sequential numbers
 */
export class SequenceService {
  private value: number;

  constructor() {
    this.value = 0;
  }

  public next(): number {
    return ++this.value;
  }
}

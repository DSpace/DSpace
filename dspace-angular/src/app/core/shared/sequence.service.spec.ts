/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { SequenceService } from './sequence.service';

let service: SequenceService;

describe('SequenceService', () => {
  beforeEach(() => {
    service = new SequenceService();
  });

  it('should return sequential numbers on next(), starting with 1', () => {
    const NUMBERS = [1,2,3,4,5];
    const sequence = NUMBERS.map(() => service.next());
    expect(sequence).toEqual(NUMBERS);
  });
});

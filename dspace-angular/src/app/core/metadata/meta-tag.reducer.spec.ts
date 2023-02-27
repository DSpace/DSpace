/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { metaTagReducer } from './meta-tag.reducer';
import { AddMetaTagAction, ClearMetaTagAction } from './meta-tag.actions';

const nullAction = { type: null };

describe('metaTagReducer', () => {
  it('should start with an empty array', () => {
    const state0 = metaTagReducer(undefined, nullAction);
    expect(state0.tagsInUse).toEqual([]);
  });

  it('should return the current state on invalid action', () => {
    const state0 = {
      tagsInUse: ['foo', 'bar'],
    };

    const state1 = metaTagReducer(state0, nullAction);
    expect(state1).toEqual(state0);
  });

  it('should add tags on AddMetaTagAction', () => {
    const state0 = {
      tagsInUse: ['foo'],
    };

    const state1 = metaTagReducer(state0, new AddMetaTagAction('bar'));
    const state2 = metaTagReducer(state1, new AddMetaTagAction('baz'));

    expect(state1.tagsInUse).toEqual(['foo', 'bar']);
    expect(state2.tagsInUse).toEqual(['foo', 'bar', 'baz']);
  });

  it('should clear tags on ClearMetaTagAction', () => {
    const state0 = {
      tagsInUse: ['foo', 'bar'],
    };

    const state1 = metaTagReducer(state0, new ClearMetaTagAction());

    expect(state1.tagsInUse).toEqual([]);
  });
});

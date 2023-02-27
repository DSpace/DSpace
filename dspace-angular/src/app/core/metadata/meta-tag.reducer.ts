import {
  MetaTagAction,
  MetaTagTypes,
  AddMetaTagAction,
  ClearMetaTagAction,
} from './meta-tag.actions';

export interface MetaTagState {
  tagsInUse: string[];
}

const initialstate: MetaTagState = {
  tagsInUse: []
};

export const metaTagReducer = (state: MetaTagState = initialstate, action: MetaTagAction): MetaTagState => {
  switch (action.type) {
    case MetaTagTypes.ADD: {
      return addMetaTag(state, action as AddMetaTagAction);
    }
    case MetaTagTypes.CLEAR: {
      return clearMetaTags(state, action as ClearMetaTagAction);
    }
    default: {
      return state;
    }
  }
};

const addMetaTag = (state: MetaTagState, action: AddMetaTagAction): MetaTagState => {
  return {
    tagsInUse: [...state.tagsInUse, action.payload]
  };
};

const clearMetaTags = (state: MetaTagState, action: ClearMetaTagAction): MetaTagState => {
  return Object.assign({}, initialstate);
};

import { isNotUndefined } from '../../../shared/empty.util';
import { URLCombiner } from '../../url-combiner/url-combiner';

/**
 * Interface used to represent a JSON-PATCH path member
 * in JsonPatchOperationsState
 */
export interface JsonPatchOperationPathObject {
  rootElement: string;
  subRootElement: string;
  path: string;
}

/**
 * Combines a variable number of strings representing parts
 * of a JSON-PATCH path
 */
export class JsonPatchOperationPathCombiner extends URLCombiner {
  private _rootElement: string;
  private _subRootElement: string;

  constructor(rootElement, ...subRootElements: string[]) {
    super(rootElement, ...subRootElements);
    this._rootElement = rootElement;
    this._subRootElement = subRootElements.join('/');
  }

  get rootElement(): string {
    return this._rootElement;
  }

  get subRootElement(): string {
    return this._subRootElement;
  }

  /**
   * Combines the parts of this JsonPatchOperationPathCombiner in to a JSON-PATCH path member
   *
   * e.g.   new JsonPatchOperationPathCombiner('sections', 'basic').getPath(['dc.title', '0'])
   * returns: {rootElement: 'sections', subRootElement: 'basic', path: '/sections/basic/dc.title/0'}
   *
   * @return {JsonPatchOperationPathObject}
   *      The combined path object
   */
  public getPath(fragment?: string|string[]): JsonPatchOperationPathObject {
    if (isNotUndefined(fragment) && Array.isArray(fragment)) {
      fragment = fragment.join('/');
    }

    let path = '/' + this.toString();
    if (isNotUndefined(fragment)) {
      path += '/' + fragment;
    }

    return {rootElement: this._rootElement, subRootElement: this._subRootElement, path: path};
  }
}

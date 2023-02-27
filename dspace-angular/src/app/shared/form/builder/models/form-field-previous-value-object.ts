import isEqual from 'lodash/isEqual';

export class FormFieldPreviousValueObject {

  private _path;
  private _value;

  constructor(path: any[] = null, value: any = null) {
    this._path = path;
    this._value = value;
  }

  get path() {
    return this._path;
  }

  set path(path: string | string[]) {
    this._path = path;
  }

  get value() {
    return this._value;
  }

  set value(value: any) {
    this._value = value;
  }

  public delete() {
    this._value = null;
    this._path = null;
  }

  public isPathEqual(path) {
    return this._path && isEqual(this._path, path);
  }
}

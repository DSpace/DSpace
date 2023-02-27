import { Injectable } from '@angular/core';
import { CookieAttributes, getJSON, remove, set } from 'js-cookie';
import { CookieService, ICookieService } from './cookie.service';

@Injectable()
export class ClientCookieService extends CookieService implements ICookieService {

  public set(name: string, value: any, options?: CookieAttributes): void {
    set(name, value, options);
    this.updateSource();
  }

  public remove(name: string, options?: CookieAttributes): void {
    remove(name, options);
    this.updateSource();
  }

  public get(name: string): any {
    return getJSON(name);
  }

  public getAll(): any {
    return getJSON();
  }
}

import { Inject, Injectable } from '@angular/core';

import { REQUEST } from '@nguniversal/express-engine/tokens';

import { Subject ,  Observable } from 'rxjs';
import { CookieAttributes } from 'js-cookie';

export interface ICookieService {
  readonly cookies$: Observable<{ readonly [key: string]: any }>;

  getAll(): any;

  get(name: string): any;

  set(name: string, value: any, options?: CookieAttributes): void;

  remove(name: string, options?: CookieAttributes): void;
}

@Injectable()
export abstract class CookieService implements ICookieService {
  protected readonly cookieSource = new Subject<{ readonly [key: string]: any }>();
  public readonly cookies$ = this.cookieSource.asObservable();

  constructor(@Inject(REQUEST) protected req: any) {
  }

  public abstract set(name: string, value: any, options?: CookieAttributes): void;

  public abstract remove(name: string, options?: CookieAttributes): void;

  public abstract get(name: string): any;

  public abstract getAll(): any;

  protected updateSource() {
    this.cookieSource.next(this.getAll());
  }
}

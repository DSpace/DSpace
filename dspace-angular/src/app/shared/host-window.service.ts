import { combineLatest as observableCombineLatest, Observable } from 'rxjs';

import { filter, distinctUntilChanged, map } from 'rxjs/operators';
import { HostWindowState } from './search/host-window.reducer';
import { Injectable } from '@angular/core';
import { createSelector, select, Store } from '@ngrx/store';

import { hasValue } from './empty.util';
import { AppState } from '../app.reducer';
import { CSSVariableService } from './sass-helper/css-variable.service';

export enum WidthCategory {
  XS,
  SM,
  MD,
  LG,
  XL
}

const hostWindowStateSelector = (state: AppState) => state.hostWindow;
const widthSelector = createSelector(hostWindowStateSelector, (hostWindow: HostWindowState) => hostWindow.width);

@Injectable()
export class HostWindowService {
  private breakPoints: { XS_MIN, SM_MIN, MD_MIN, LG_MIN, XL_MIN } = {} as any;

  constructor(
    private store: Store<AppState>,
    private variableService: CSSVariableService
  ) {
    /* See _exposed_variables.scss */
    variableService.getAllVariables()
      .subscribe((variables) => {
      this.breakPoints.XL_MIN = parseInt(variables['--bs-xl-min'], 10);
      this.breakPoints.LG_MIN = parseInt(variables['--bs-lg-min'], 10);
      this.breakPoints.MD_MIN = parseInt(variables['--bs-md-min'], 10);
      this.breakPoints.SM_MIN = parseInt(variables['--bs-sm-min'], 10);
    });
  }

  private getWidthObs(): Observable<number> {
    return this.store.pipe(
      select(widthSelector),
      filter((width) => hasValue(width))
    );
  }

  get widthCategory(): Observable<WidthCategory> {
    return this.getWidthObs().pipe(
      map((width: number) => {
        if (width < this.breakPoints.SM_MIN) {
          return WidthCategory.XS;
        } else if (width >= this.breakPoints.SM_MIN && width < this.breakPoints.MD_MIN) {
          return WidthCategory.SM;
        } else if (width >= this.breakPoints.MD_MIN && width < this.breakPoints.LG_MIN) {
          return WidthCategory.MD;
        } else if (width >= this.breakPoints.LG_MIN && width < this.breakPoints.XL_MIN) {
          return WidthCategory.LG;
        } else {
          return WidthCategory.XL;
        }
      }),
      distinctUntilChanged()
    );
  }

  isXs(): Observable<boolean> {
    return this.widthCategory.pipe(
      map((widthCat: WidthCategory) => widthCat === WidthCategory.XS),
      distinctUntilChanged()
    );
  }

  isSm(): Observable<boolean> {
    return this.widthCategory.pipe(
      map((widthCat: WidthCategory) => widthCat === WidthCategory.SM),
      distinctUntilChanged()
    );
  }

  isMd(): Observable<boolean> {
    return this.widthCategory.pipe(
      map((widthCat: WidthCategory) => widthCat === WidthCategory.MD),
      distinctUntilChanged()
    );
  }

  isLg(): Observable<boolean> {
    return this.widthCategory.pipe(
      map((widthCat: WidthCategory) => widthCat === WidthCategory.LG),
      distinctUntilChanged()
    );
  }

  isXl(): Observable<boolean> {
    return this.widthCategory.pipe(
      map((widthCat: WidthCategory) => widthCat === WidthCategory.XL),
      distinctUntilChanged()
    );
  }

  isXsOrSm(): Observable<boolean> {
    return observableCombineLatest(
      this.isXs(),
      this.isSm()
    ).pipe(
      map(([isXs, isSm]) => isXs || isSm),
      distinctUntilChanged()
    );
  }
}

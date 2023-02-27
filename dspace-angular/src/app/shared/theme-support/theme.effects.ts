import { Injectable } from '@angular/core';
import { createEffect, Actions, ofType, ROOT_EFFECTS_INIT } from '@ngrx/effects';
import { map } from 'rxjs/operators';
import { SetThemeAction } from './theme.actions';
import { hasValue } from '../empty.util';
import { BASE_THEME_NAME } from './theme.constants';
import { getDefaultThemeConfig } from '../../../config/config.util';

@Injectable()
export class ThemeEffects {
  /**
   * Initialize with a theme that doesn't depend on the route.
   */
  initTheme$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ROOT_EFFECTS_INIT),
      map(() => {
        const defaultThemeConfig = getDefaultThemeConfig();
        if (hasValue(defaultThemeConfig)) {
          return new SetThemeAction(defaultThemeConfig.name);
        } else {
          return new SetThemeAction(BASE_THEME_NAME);
        }
      })
    )
  );

  constructor(
    private actions$: Actions,
  ) {
  }
}

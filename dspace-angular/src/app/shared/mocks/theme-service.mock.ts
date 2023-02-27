import { ThemeService } from '../theme-support/theme.service';
import { of as observableOf } from 'rxjs';
import { ThemeConfig } from '../../../config/theme.model';
import { isNotEmpty } from '../empty.util';

export function getMockThemeService(themeName = 'base', themes?: ThemeConfig[]): ThemeService {
  const spy = jasmine.createSpyObj('themeService', {
    getThemeName: themeName,
    getThemeName$: observableOf(themeName),
    getThemeConfigFor: undefined,
    listenForRouteChanges: undefined,
  });

  if (isNotEmpty(themes)) {
    spy.getThemeConfigFor.and.callFake((name: string) => themes.find(theme => theme.name === name));
  }

  return spy;
}

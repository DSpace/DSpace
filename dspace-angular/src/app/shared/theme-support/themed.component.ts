import {
  Component,
  ViewChild,
  ViewContainerRef,
  ComponentRef,
  SimpleChanges,
  OnInit,
  OnDestroy,
  ComponentFactoryResolver,
  ChangeDetectorRef,
  OnChanges,
  HostBinding,
  ElementRef,
} from '@angular/core';
import { hasValue, isNotEmpty } from '../empty.util';
import { from as fromPromise, Observable, of as observableOf, Subscription, BehaviorSubject } from 'rxjs';
import { ThemeService } from './theme.service';
import { catchError, switchMap, map, tap } from 'rxjs/operators';
import { GenericConstructor } from '../../core/shared/generic-constructor';
import { BASE_THEME_NAME } from './theme.constants';

@Component({
  selector: 'ds-themed',
  styleUrls: ['./themed.component.scss'],
  templateUrl: './themed.component.html',
})
export abstract class ThemedComponent<T> implements OnInit, OnDestroy, OnChanges {
  @ViewChild('vcr', { read: ViewContainerRef }) vcr: ViewContainerRef;
  @ViewChild('content') themedElementContent: ElementRef;
  protected compRef: ComponentRef<T>;

  /**
   * A reference to the themed component. Will start as undefined and emit every time the themed
   * component is rendered
   */
  public compRef$: BehaviorSubject<ComponentRef<T>> = new BehaviorSubject(undefined);

  protected lazyLoadSub: Subscription;
  protected themeSub: Subscription;

  protected inAndOutputNames: (keyof T & keyof this)[] = [];

  /**
   * A data attribute on the ThemedComponent to indicate which theme the rendered component came from.
   */
  @HostBinding('attr.data-used-theme') usedTheme: string;

  constructor(
    protected resolver: ComponentFactoryResolver,
    protected cdr: ChangeDetectorRef,
    protected themeService: ThemeService,
  ) {
  }

  protected abstract getComponentName(): string;

  protected abstract importThemedComponent(themeName: string): Promise<any>;
  protected abstract importUnthemedComponent(): Promise<any>;

  ngOnChanges(changes: SimpleChanges): void {
    // if an input or output has changed
    if (this.inAndOutputNames.some((name: any) => hasValue(changes[name]))) {
      this.connectInputsAndOutputs();
      if (this.compRef?.instance && 'ngOnChanges' in this.compRef.instance) {
        (this.compRef.instance as any).ngOnChanges(changes);
      }
    }
  }

  ngOnInit(): void {
    this.destroyComponentInstance();
    this.themeSub = this.themeService.getThemeName$().subscribe(() => {
      this.renderComponentInstance();
    });
  }

  ngOnDestroy(): void {
    [this.themeSub, this.lazyLoadSub].filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
    this.destroyComponentInstance();
  }

  protected renderComponentInstance(): void {
    this.destroyComponentInstance();

    if (hasValue(this.lazyLoadSub)) {
      this.lazyLoadSub.unsubscribe();
    }

    this.lazyLoadSub = this.resolveThemedComponent(this.themeService.getThemeName()).pipe(
      switchMap((themedFile: any) => {
        if (hasValue(themedFile) && hasValue(themedFile[this.getComponentName()])) {
          // if the file is not null, and exports a component with the specified name,
          // return that component
          return [themedFile[this.getComponentName()]];
        } else {
          // otherwise import and return the default component
          return fromPromise(this.importUnthemedComponent()).pipe(
            tap(() => this.usedTheme = BASE_THEME_NAME),
            map((unthemedFile: any) => {
              return unthemedFile[this.getComponentName()];
            })
          );
        }
      }),
    ).subscribe((constructor: GenericConstructor<T>) => {
      const factory = this.resolver.resolveComponentFactory(constructor);
      this.compRef = this.vcr.createComponent(factory, undefined, undefined, [this.themedElementContent.nativeElement.childNodes]);
      this.connectInputsAndOutputs();
      this.compRef$.next(this.compRef);
      this.cdr.markForCheck();
      this.themedElementContent.nativeElement.remove();
    });
  }

  protected destroyComponentInstance(): void {
    if (hasValue(this.compRef)) {
      this.compRef.destroy();
      this.compRef = null;
    }
    if (hasValue(this.vcr)) {
      this.vcr.clear();
    }
  }

  protected connectInputsAndOutputs(): void {
    if (isNotEmpty(this.inAndOutputNames) && hasValue(this.compRef) && hasValue(this.compRef.instance)) {
      this.inAndOutputNames.filter((name: any) => this[name] !== undefined).forEach((name: any) => {
        this.compRef.instance[name] = this[name];
      });
    }
  }

  /**
   * Attempt to import this component from the current theme or a theme it {@link NamedThemeConfig.extends}.
   * Recurse until we succeed or when until we run out of themes to fall back to.
   *
   * @param themeName The name of the theme to check
   * @param checkedThemeNames The list of theme names that are already checked
   * @private
   */
  private resolveThemedComponent(themeName?: string, checkedThemeNames: string[] = []): Observable<any> {
    if (isNotEmpty(themeName)) {
      return fromPromise(this.importThemedComponent(themeName)).pipe(
        tap(() => this.usedTheme = themeName),
        catchError(() => {
          // Try the next ancestor theme instead
          const nextTheme = this.themeService.getThemeConfigFor(themeName)?.extends;
          const nextCheckedThemeNames = [...checkedThemeNames, themeName];
          if (checkedThemeNames.includes(nextTheme)) {
            throw new Error('Theme extension cycle detected: ' + [...nextCheckedThemeNames, nextTheme].join(' -> '));
          } else {
            return this.resolveThemedComponent(nextTheme, nextCheckedThemeNames);
          }
        }),
      );
    } else {
      // If we got here, we've failed to import this component from any ancestor theme → fall back to unthemed
      return observableOf(null);
    }
  }
}

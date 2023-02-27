/* eslint-disable max-classes-per-file */
import { ThemedComponent } from './themed.component';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../utils/var.directive';
import { ThemeService } from './theme.service';
import { getMockThemeService } from '../mocks/theme-service.mock';
import { TestComponent } from './test/test.component.spec';
import { ThemeConfig } from '../../../config/theme.model';

@Component({
  selector: 'ds-test-themed-component',
  templateUrl: './themed.component.html'
})
class TestThemedComponent extends ThemedComponent<TestComponent> {
  protected inAndOutputNames: (keyof TestComponent & keyof this)[] = ['testInput'];

  testInput = 'unset';

  protected getComponentName(): string {
    return 'TestComponent';
  }
  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`./test/${themeName}/themed-test.component.spec`);
  }
  protected importUnthemedComponent(): Promise<any> {
    return import('./test/test.component.spec');
  }
}

describe('ThemedComponent', () => {
  let component: TestThemedComponent;
  let fixture: ComponentFixture<TestThemedComponent>;
  let themeService: ThemeService;

  function setupTestingModuleForTheme(theme: string, themes?: ThemeConfig[]) {
    themeService = getMockThemeService(theme, themes);
    TestBed.configureTestingModule({
      imports: [],
      declarations: [TestThemedComponent, VarDirective],
      providers: [
        { provide: ThemeService, useValue: themeService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }

  function initComponent() {
    fixture = TestBed.createComponent(TestThemedComponent);
    component = fixture.componentInstance;
    spyOn(component as any, 'importThemedComponent').and.callThrough();
    component.testInput = 'changed';
    fixture.detectChanges();
  }

  describe('when the current theme matches a themed component', () => {
    beforeEach(waitForAsync(() => {
      setupTestingModuleForTheme('custom');
    }));

    beforeEach(initComponent);

    it('should set compRef to the themed component', waitForAsync(() => {
      fixture.whenStable().then(() => {
        expect((component as any).compRef.instance.type).toEqual('themed');
      });
    }));

    it('should sync up this component\'s input with the themed component', waitForAsync(() => {
      fixture.whenStable().then(() => {
        expect((component as any).compRef.instance.testInput).toEqual('changed');
      });
    }));

    it(`should set usedTheme to the name of the matched theme`, waitForAsync(() => {
      fixture.whenStable().then(() => {
        expect(component.usedTheme).toEqual('custom');
      });
    }));
  });

  describe('when the current theme doesn\'t match a themed component', () => {
    describe('and it doesn\'t extend another theme', () => {
      beforeEach(waitForAsync(() => {
        setupTestingModuleForTheme('non-existing-theme');
      }));

      beforeEach(initComponent);

      it('should set compRef to the default component', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect((component as any).compRef.instance.type).toEqual('default');
        });
      }));

      it('should sync up this component\'s input with the default component', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect((component as any).compRef.instance.testInput).toEqual('changed');
        });
      }));

      it(`should set usedTheme to the name of the base theme`, waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.usedTheme).toEqual('base');
        });
      }));
    });

    describe('and it extends another theme', () => {
      describe('that doesn\'t match it either', () => {
        beforeEach(waitForAsync(() => {
          setupTestingModuleForTheme('current-theme', [
            { name: 'current-theme', extends: 'non-existing-theme' },
          ]);
        }));

        beforeEach(initComponent);

        it('should set compRef to the default component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('current-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('non-existing-theme');
            expect((component as any).compRef.instance.type).toEqual('default');
          });
        }));

        it('should sync up this component\'s input with the default component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).compRef.instance.testInput).toEqual('changed');
          });
        }));

        it(`should set usedTheme to the name of the base theme`, waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect(component.usedTheme).toEqual('base');
          });
        }));
      });

      describe('that does match it', () => {
        beforeEach(waitForAsync(() => {
          setupTestingModuleForTheme('current-theme', [
            { name: 'current-theme', extends: 'custom' },
          ]);
        }));

        beforeEach(initComponent);

        it('should set compRef to the themed component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('current-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('custom');
            expect((component as any).compRef.instance.type).toEqual('themed');
          });
        }));

        it('should sync up this component\'s input with the themed component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).compRef.instance.testInput).toEqual('changed');
          });
        }));

        it(`should set usedTheme to the name of the matched theme`, waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect(component.usedTheme).toEqual('custom');
          });
        }));
      });

      describe('that extends another theme that doesn\'t match it either', () => {
        beforeEach(waitForAsync(() => {
          setupTestingModuleForTheme('current-theme', [
            { name: 'current-theme', extends: 'parent-theme' },
            { name: 'parent-theme', extends: 'non-existing-theme' },
          ]);
        }));

        beforeEach(initComponent);

        it('should set compRef to the default component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('current-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('parent-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('non-existing-theme');
            expect((component as any).compRef.instance.type).toEqual('default');
          });
        }));

        it('should sync up this component\'s input with the default component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).compRef.instance.testInput).toEqual('changed');
          });
        }));

        it(`should set usedTheme to the name of the base theme`, waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect(component.usedTheme).toEqual('base');
          });
        }));
      });

      describe('that extends another theme that does match it', () => {
        beforeEach(waitForAsync(() => {
          setupTestingModuleForTheme('current-theme', [
            { name: 'current-theme', extends: 'parent-theme' },
            { name: 'parent-theme', extends: 'custom' },
          ]);
        }));

        beforeEach(initComponent);

        it('should set compRef to the themed component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('current-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('parent-theme');
            expect((component as any).importThemedComponent).toHaveBeenCalledWith('custom');
            expect((component as any).compRef.instance.type).toEqual('themed');
          });
        }));

        it('should sync up this component\'s input with the themed component', waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect((component as any).compRef.instance.testInput).toEqual('changed');
          });
        }));

        it(`should set usedTheme to the name of the matched theme`, waitForAsync(() => {
          fixture.whenStable().then(() => {
            expect(component.usedTheme).toEqual('custom');
          });
        }));
      });
    });
  });
});

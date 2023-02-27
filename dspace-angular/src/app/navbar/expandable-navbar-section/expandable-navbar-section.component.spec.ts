import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ExpandableNavbarSectionComponent } from './expandable-navbar-section.component';
import { By } from '@angular/platform-browser';
import { MenuServiceStub } from '../../shared/testing/menu-service.stub';
import { Component } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { HostWindowService } from '../../shared/host-window.service';
import { MenuService } from '../../shared/menu/menu.service';
import { HostWindowServiceStub } from '../../shared/testing/host-window-service.stub';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { VarDirective } from '../../shared/utils/var.directive';

describe('ExpandableNavbarSectionComponent', () => {
  let component: ExpandableNavbarSectionComponent;
  let fixture: ComponentFixture<ExpandableNavbarSectionComponent>;
  const menuService = new MenuServiceStub();

  describe('on larger screens', () => {
    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule],
        declarations: [ExpandableNavbarSectionComponent, TestComponent, VarDirective],
        providers: [
          { provide: 'sectionDataProvider', useValue: {} },
          { provide: MenuService, useValue: menuService },
          { provide: HostWindowService, useValue: new HostWindowServiceStub(800) }
        ]
      }).overrideComponent(ExpandableNavbarSectionComponent, {
        set: {
          entryComponents: [TestComponent]
        }
      })
        .compileComponents();
    }));

    beforeEach(() => {
      spyOn(menuService, 'getSubSectionsByParentID').and.returnValue(observableOf([]));

      fixture = TestBed.createComponent(ExpandableNavbarSectionComponent);
      component = fixture.componentInstance;
      spyOn(component as any, 'getMenuItemComponent').and.returnValue(TestComponent);
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    describe('when the mouse enters the section header', () => {
      beforeEach(() => {
        spyOn(menuService, 'activateSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        sidebarToggler.triggerEventHandler('mouseenter', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should call activateSection on the menuService', () => {
        expect(menuService.activateSection).toHaveBeenCalled();
      });
    });

    describe('when the mouse leaves the section header', () => {
      beforeEach(() => {
        spyOn(menuService, 'deactivateSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        sidebarToggler.triggerEventHandler('mouseleave', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should call deactivateSection on the menuService', () => {
        expect(menuService.deactivateSection).toHaveBeenCalled();
      });
    });

    describe('when Enter key is pressed on section header (while inactive)', () => {
      beforeEach(() => {
        spyOn(menuService, 'activateSection');
        // Make sure section is 'inactive'. Requires calling ngOnInit() to update component 'active' property.
        spyOn(menuService, 'isSectionActive').and.returnValue(observableOf(false));
        component.ngOnInit();
        fixture.detectChanges();

        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        // dispatch the (keyup.enter) action used in our component HTML
        sidebarToggler.nativeElement.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter' }));
      });

      it('should call activateSection on the menuService', () => {
        expect(menuService.activateSection).toHaveBeenCalled();
      });
    });

    describe('when Enter key is pressed on section header (while active)', () => {
      beforeEach(() => {
        spyOn(menuService, 'deactivateSection');
        // Make sure section is 'active'. Requires calling ngOnInit() to update component 'active' property.
        spyOn(menuService, 'isSectionActive').and.returnValue(observableOf(true));
        component.ngOnInit();
        fixture.detectChanges();

        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        // dispatch the (keyup.enter) action used in our component HTML
        sidebarToggler.nativeElement.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter' }));
      });

      it('should call deactivateSection on the menuService', () => {
        expect(menuService.deactivateSection).toHaveBeenCalled();
      });
    });

    describe('when spacebar is pressed on section header (while inactive)', () => {
      beforeEach(() => {
        spyOn(menuService, 'activateSection');
        // Make sure section is 'inactive'. Requires calling ngOnInit() to update component 'active' property.
        spyOn(menuService, 'isSectionActive').and.returnValue(observableOf(false));
        component.ngOnInit();
        fixture.detectChanges();

        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        // dispatch the (keyup.space) action used in our component HTML
        sidebarToggler.nativeElement.dispatchEvent(new KeyboardEvent('keyup', { key: ' ' }));
      });

      it('should call activateSection on the menuService', () => {
        expect(menuService.activateSection).toHaveBeenCalled();
      });
    });

    describe('when spacebar is pressed on section header (while active)', () => {
      beforeEach(() => {
        spyOn(menuService, 'deactivateSection');
        // Make sure section is 'active'. Requires calling ngOnInit() to update component 'active' property.
        spyOn(menuService, 'isSectionActive').and.returnValue(observableOf(true));
        component.ngOnInit();
        fixture.detectChanges();

        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown'));
        // dispatch the (keyup.space) action used in our component HTML
        sidebarToggler.nativeElement.dispatchEvent(new KeyboardEvent('keyup', { key: ' ' }));
      });

      it('should call deactivateSection on the menuService', () => {
        expect(menuService.deactivateSection).toHaveBeenCalled();
      });
    });

    describe('when a click occurs on the section header', () => {
      beforeEach(() => {
        spyOn(menuService, 'toggleActiveSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown > a'));
        sidebarToggler.triggerEventHandler('click', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should not call toggleActiveSection on the menuService', () => {
        expect(menuService.toggleActiveSection).not.toHaveBeenCalled();
      });
    });
  });

  describe('on smaller, mobile screens', () => {
    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule],
        declarations: [ExpandableNavbarSectionComponent, TestComponent, VarDirective],
        providers: [
          { provide: 'sectionDataProvider', useValue: {} },
          { provide: MenuService, useValue: menuService },
          { provide: HostWindowService, useValue: new HostWindowServiceStub(300) }
        ]
      }).overrideComponent(ExpandableNavbarSectionComponent, {
        set: {
          entryComponents: [TestComponent]
        }
      })
        .compileComponents();
    }));

    beforeEach(() => {
      spyOn(menuService, 'getSubSectionsByParentID').and.returnValue(observableOf([]));

      fixture = TestBed.createComponent(ExpandableNavbarSectionComponent);
      component = fixture.componentInstance;
      spyOn(component as any, 'getMenuItemComponent').and.returnValue(TestComponent);
      fixture.detectChanges();
    });

    describe('when the mouse enters the section header', () => {
      beforeEach(() => {
        spyOn(menuService, 'activateSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown > a'));
        sidebarToggler.triggerEventHandler('mouseenter', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should not call activateSection on the menuService', () => {
        expect(menuService.activateSection).not.toHaveBeenCalled();
      });
    });

    describe('when the mouse leaves the section header', () => {
      beforeEach(() => {
        spyOn(menuService, 'deactivateSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown > a'));
        sidebarToggler.triggerEventHandler('mouseleave', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should not call deactivateSection on the menuService', () => {
        expect(menuService.deactivateSection).not.toHaveBeenCalled();
      });
    });

    describe('when a click occurs on the section header link', () => {
      beforeEach(() => {
        spyOn(menuService, 'toggleActiveSection');
        const sidebarToggler = fixture.debugElement.query(By.css('div.nav-item.dropdown > a'));
        sidebarToggler.triggerEventHandler('click', {
          preventDefault: () => {/**/
          }
        });
      });

      it('should call toggleActiveSection on the menuService', () => {
        expect(menuService.toggleActiveSection).toHaveBeenCalled();
      });
    });
  });

});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {
}

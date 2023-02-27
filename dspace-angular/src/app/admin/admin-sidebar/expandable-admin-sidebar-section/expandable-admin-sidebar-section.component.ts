import { Component, Inject, Injector, OnInit } from '@angular/core';
import { rotate } from '../../../shared/animations/rotate';
import { AdminSidebarSectionComponent } from '../admin-sidebar-section/admin-sidebar-section.component';
import { slide } from '../../../shared/animations/slide';
import { CSSVariableService } from '../../../shared/sass-helper/css-variable.service';
import { bgColor } from '../../../shared/animations/bgColor';
import { MenuService } from '../../../shared/menu/menu.service';
import { combineLatest as combineLatestObservable, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { rendersSectionForMenu } from '../../../shared/menu/menu-section.decorator';
import { MenuID } from '../../../shared/menu/menu-id.model';
import { Router } from '@angular/router';

/**
 * Represents a expandable section in the sidebar
 */
@Component({
  /* eslint-disable @angular-eslint/component-selector */
  selector: 'li[ds-expandable-admin-sidebar-section]',
  templateUrl: './expandable-admin-sidebar-section.component.html',
  styleUrls: ['./expandable-admin-sidebar-section.component.scss'],
  animations: [rotate, slide, bgColor]
})

@rendersSectionForMenu(MenuID.ADMIN, true)
export class ExpandableAdminSidebarSectionComponent extends AdminSidebarSectionComponent implements OnInit {
  /**
   * This section resides in the Admin Sidebar
   */
  menuID = MenuID.ADMIN;

  /**
   * The background color of the section when it's active
   */
  sidebarActiveBg;

  /**
   * Emits true when the sidebar is currently collapsed, true when it's expanded
   */
  sidebarCollapsed: Observable<boolean>;

  /**
   * Emits true when the sidebar's preview is currently collapsed, true when it's expanded
   */
  sidebarPreviewCollapsed: Observable<boolean>;

  /**
   * Emits true when the menu section is expanded, else emits false
   * This is true when the section is active AND either the sidebar or it's preview is open
   */
  expanded: Observable<boolean>;

  constructor(
    @Inject('sectionDataProvider') menuSection,
    protected menuService: MenuService,
    private variableService: CSSVariableService,
    protected injector: Injector,
    protected router: Router,
  ) {
    super(menuSection, menuService, injector, router);
  }

  /**
   * Set initial values for instance variables
   */
  ngOnInit(): void {
    super.ngOnInit();
    this.sidebarActiveBg = this.variableService.getVariable('--ds-admin-sidebar-active-bg');
    this.sidebarCollapsed = this.menuService.isMenuCollapsed(this.menuID);
    this.sidebarPreviewCollapsed = this.menuService.isMenuPreviewCollapsed(this.menuID);
    this.expanded = combineLatestObservable(this.active, this.sidebarCollapsed, this.sidebarPreviewCollapsed)
      .pipe(
        map(([active, sidebarCollapsed, sidebarPreviewCollapsed]) => (active && (!sidebarCollapsed || !sidebarPreviewCollapsed)))
      );
  }
}

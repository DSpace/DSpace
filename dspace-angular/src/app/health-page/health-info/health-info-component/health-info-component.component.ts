import { Component, Input } from '@angular/core';

import { HealthInfoComponent } from '../../models/health-component.model';
import { HealthComponentComponent } from '../../health-panel/health-component/health-component.component';

/**
 * Shows a health info object
 */
@Component({
  selector: 'ds-health-info-component',
  templateUrl: './health-info-component.component.html',
  styleUrls: ['./health-info-component.component.scss']
})
export class HealthInfoComponentComponent extends HealthComponentComponent {

  /**
   * The HealthInfoComponent object to display
   */
  @Input() healthInfoComponent: HealthInfoComponent|string;

  /**
   * The HealthInfoComponent object name
   */
  @Input() healthInfoComponentName: string;

  /**
   * A boolean representing if div should start collapsed
   */
  @Input() isNested = false;

  /**
   * A boolean representing if div should start collapsed
   */
  public isCollapsed = false;

  /**
   * Check if the HealthInfoComponent is has only string property or contains object
   *
   * @param entry The HealthInfoComponent to check
   * @return boolean
   */
  isPlainProperty(entry: HealthInfoComponent | string): boolean {
    return typeof entry === 'string';
  }

}

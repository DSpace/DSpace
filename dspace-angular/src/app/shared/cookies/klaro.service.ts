import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';

/**
 * Abstract class representing a service for handling Klaro consent preferences and UI
 */
@Injectable()
export abstract class KlaroService {
  /**
   * Initializes the service
   */
  abstract initialize();

  /**
   * Shows a dialog with the current consent preferences
   */
  abstract showSettings();

  /**
   * Return saved preferences stored in the klaro cookie
   */
  abstract getSavedPreferences(): Observable<any>;
}

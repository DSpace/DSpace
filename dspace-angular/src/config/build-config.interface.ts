import { AppConfig } from './app-config.interface';
import { UniversalConfig } from './universal-config.interface';

export interface BuildConfig extends AppConfig {
  universal: UniversalConfig;
}

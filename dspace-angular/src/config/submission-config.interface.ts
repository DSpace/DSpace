import { Config } from './config.interface';

interface AutosaveConfig extends Config {
  metadata: string[];
  timer: number;
}

interface TypeBindConfig extends Config {
  field: string;
}

interface IconsConfig extends Config {
  metadata: MetadataIconConfig[];
  authority: {
    confidence: ConfidenceIconConfig[];
  };
}

export interface MetadataIconConfig extends Config {
  name: string;
  style: string;
}

export interface ConfidenceIconConfig extends Config {
  value: any;
  style: string;
}

export interface SubmissionConfig extends Config {
  autosave: AutosaveConfig;
  typeBind: TypeBindConfig;
  icons: IconsConfig;
}

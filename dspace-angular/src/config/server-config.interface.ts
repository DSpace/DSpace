import { Config } from './config.interface';

export class ServerConfig implements Config {
  public ssl: boolean;
  public host: string;
  public port: number;
  public nameSpace: string;
  public baseUrl?: string;
}

import jwtDecode, { JwtPayload } from 'jwt-decode';

export const TOKENITEM = 'dsAuthInfo';

export class AuthTokenInfo {
  public accessToken: string;
  public expires: number;

  constructor(token: string) {
    this.accessToken = token.replace('Bearer ', '');
    try {
      const tokenClaims = jwtDecode<JwtPayload>(this.accessToken);
      // exp claim is in seconds, convert it se to milliseconds
      this.expires = tokenClaims.exp * 1000;
    } catch (err) {
      this.expires = 0;
    }
  }
}

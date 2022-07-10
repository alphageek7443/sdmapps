import Cookies from "universal-cookie";
import fetch from "node-fetch";
const cookieName = "SDMCookie";
const CALLBACK_URI = "/myCallbackURI";

class OAuth{
    constructor(clientId,clientSecret,authUrl){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authUrl = authUrl;
    }

    getToken(req){
        const cookies = new Cookies(req.headers.cookie);
        return cookies.get(cookieName);
    }

    async fetchToken(code,redirectUri) {
        const params = new URLSearchParams();
        params.set('client_id', this.clientId);
        params.set('client_secret', this.clientSecret);
        params.set('code', code);
        params.set('redirect_uri', redirectUri);
        params.set('grant_type', 'authorization_code');

        const response = await fetch(`${this.authUrl}/oauth/token`,{
            method: 'post',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Accept': 'application/json'
            },
            body: params
        });

        const json = await response.json();
        return json.access_token;
    }

    getMiddleware() {
        return async (req, res, next) => {
            const redirectUri = `${req.protocol}://${
                req.get("host")}${CALLBACK_URI}`;
            if (req.url.startsWith(CALLBACK_URI)) {
                const code = req.query.code;
                if (code) {
                    const token = await this.fetchToken(code, redirectUri);
                    res.cookie(cookieName, token, { 
                        maxAge: 1000 * 60 * 120, httpOnly: true,  path: "/", });
                }
                res.redirect("/");
            } else if (!this.getToken(req)) {
                res.redirect(`${this.authUrl}/oauth/authorize?client_id=${
                    encodeURIComponent(this.clientId)}&redirect_uri=${
                        encodeURIComponent(redirectUri)}&response_type=code`);
            } else {
                next();
            }
        };
    }
}

export default OAuth
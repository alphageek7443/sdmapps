import fetch from 'node-fetch';

class OAuth{
    constructor(clientId, clientSecret,authUrl){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authUrl = authUrl;
    }

    async getToken(){
        return await this.fetchToken().then(res => res);
    }

     async fetchToken(){
        return await fetch(this.authUrl+
            '/oauth/token?grant_type=client_credentials',{
                headers: { 'Authorization': 'Basic ' + Buffer.from(
                    `${this.clientId}:${this.clientSecret}`,
                     'binary').toString('base64') }})
            .then(res=> res.json())
            .then(res => res.access_token)
            .catch(error => console.error(error))
    }
}

export default OAuth
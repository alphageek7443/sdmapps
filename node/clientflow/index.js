import express from 'express'
import cmis from 'cmis';

import keys from './src/keys/default-key.json' assert { type: 'json' };
const {uaa:{clientid},uaa:{clientsecret},uaa:{url},uri,...other} = keys 

import OAuth from './src/libs/auth.js';

const app = express();
const oauth = new OAuth(clientid,clientsecret,url);
const port = process.env.PORT || 3004;

app.use(express.static("web"));

app.get('/token', async(req,res) => {
    var token = await oauth.getToken()
    res.send(token)
})

app.get('/documents', async(req,res) =>{
    var session = new cmis.CmisSession(uri+'browser');
    var token = await oauth.getToken()
    session.setToken(token).loadRepositories()
    .then(() => session.query("select * from cmis:document"))
    .then(data => res.send(data))
})

app.listen(port, () => { console.log(`Explore http://localhost:${port}`) });
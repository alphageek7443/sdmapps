using { sap.capire.media as db } from '../db/schema';
// Define Document Service
service DocumentService{
    entity Pictures as projection on db.Pictures;
}
namespace sap.capire.media;

entity Pictures {
  key ID        : UUID;
      name: String;
      repositoryId: String;
      content   : LargeBinary @Core.MediaType   : mediaType;
      mediaType : String      @Core.IsMediaType : true;
}
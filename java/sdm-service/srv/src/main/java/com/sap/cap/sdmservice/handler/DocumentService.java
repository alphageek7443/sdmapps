package com.sap.cap.sdmservice.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.util.ContentStreamUtils;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import com.sap.cap.sdmservice.config.ConnectSDMDataSource;
import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.ResolvedRefItem;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.documentservice.DocumentService_;
import cds.gen.documentservice.Pictures;
import cds.gen.documentservice.Pictures_;


@Component
@ServiceName(DocumentService_.CDS_NAME)
public class DocumentService implements EventHandler {

    @Autowired
    private ConnectSDMDataSource connectSDMDataSource;

    @Autowired
    private PersistenceService db;

    private final CqnAnalyzer analyzer;

    @Autowired
    DocumentService(CdsModel model) {
        this.analyzer = CqnAnalyzer.create(model);
    }

    private Session getCMISSession(
        EventContext context, String repositoryId) {
        return connectSDMDataSource.getSession(context, repositoryId);
    }

    @On(event =  CdsService.EVENT_READ, entity = Pictures_.CDS_NAME)
    public void read(CdsReadEventContext context){
        AnalysisResult analysisResult = analyzer
        .analyze(context.getCqn().ref());
        Object id = analysisResult.rootKeys().get("ID");
        ArrayList<ResolvedRefItem> resolvedRefItems = 
        (ArrayList<ResolvedRefItem>) analyzer
                .resolveRefItems(context.getCqn());
                
        if (id != null && resolvedRefItems.size() == 2
                && resolvedRefItems.get(0)
                .displayName().equals("content")) {

            Result resultQuery = db.run(Select.from(DocumentService_.PICTURES)
            .where(picture -> picture.ID().eq(id.toString())));
            Pictures picture = resultQuery.listOf(Pictures.class).get(0);

            Session session = getCMISSession(context, picture.getRepositoryId());
            String filename = picture.getId().toString();
            ItemIterable <QueryResult> cmisResults =session.query(
                "SELECT cmis:objectId FROM cmis:document where cmis:name LIKE "+
                "'"+filename+"%'", 
                false);
            
            String objectId = null;
            for(QueryResult hit: cmisResults) {
                for(PropertyData<?> property: hit.getProperties()) {

                    if(property.getQueryName().equals("cmis:objectId")){
                        objectId = property.getFirstValue().toString();
                        break;
                    }
                }
            }

            Document document = session.getLatestDocumentVersion(objectId);
            picture.setContent(document.getContentStream().getStream());
            picture.setMediaType(document.getContentStreamMimeType());
            context.setResult(Arrays.asList(picture));
        }            
    }

    @On(event = CdsService.EVENT_UPDATE, entity = Pictures_.CDS_NAME)
    public void update(CdsUpdateEventContext context, Pictures pictures) {

        Result result = db.run(Select.from(Pictures_.CDS_NAME)
        .byId(pictures.getId()));
        Pictures picture = result.listOf(Pictures.class).get(0);
    
        String repositoryId = result.first().get()
        .get(Pictures.REPOSITORY_ID).toString();
        String fileExtension= MimeTypeUtils.parseMimeType
        (pictures.getMediaType().toString()).getSubtype();
        String fileName = result.first().get().get(Pictures.ID)
        .toString()+"."+fileExtension;
        Session session = this.getCMISSession(context, repositoryId);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, fileName);

        try {
            ContentStream contentStream = ContentStreamUtils
            .createByteArrayContentStream(
                fileName, IOUtils.toByteArray( pictures.getContent()),
            pictures.getMediaType());
            Folder folder = session.getRootFolder();
            Document document = folder.createDocument(properties, 
            contentStream, VersioningState.MAJOR);
            System.out.println(document.getContentUrl());

        } catch (IOException e) {
            e.printStackTrace();
        }

        picture.setMediaType(pictures.getMediaType().toString());
        context.setResult(Arrays.asList(picture));
        context.setCompleted();
    }
}

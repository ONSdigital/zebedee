package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.Collection;

public class StaticFilesServiceImpl implements StaticFilesService {


    @Override
    public void publishCollection(Collection collection) {
        String collectionId = collection.getDescription().getId();

        if ( collectionId == null || collectionId.isEmpty() ) {
            throw new IllegalArgumentException("A collectionId must be set in the collection being published.");
        }

        try {
            staticFilesAPI.publish(collectionId);
        } catch (NoFilesFoundException noFilesFoundException)  {
            info().log(noFilesFoundException, "");
            throw nffe;
        } catch (FilesInWrongStateException filesInWrongStateException) {
            info().log(filesInWrongStateException, "");
            throw filesInWrongStateException;

        } catch (NotAuthorisedToPublishException notAuthorisedToPublishException) {
            info().log(notAuthorisedToPublishException, "");
            throw notAuthorisedToPublishException;
        } catch (Exception e) {

        }
    }
}

package com.github.onsdigital.zebedee.verification;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.DateConverter;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static java.util.Arrays.asList;

/**
 * Created by bren on 16/11/15.
 * Verification agent works after each publish to connect to website ( or another proxy ) to verify the content by checking content's hash
 */
public class VerificationAgent {

    private PooledHttpClient verificationProxyClient;
    private ExecutorService pool;
    private Zebedee zebedee;

    public VerificationAgent(Zebedee zebedee) {
        this.zebedee = zebedee;
        String defaultVerificationUrl = Configuration.getDefaultVerificationUrl();
        logInfo("Initializing verification agent").addParameter("url", defaultVerificationUrl).log();
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxTotalConnection(100);
        clientConfiguration.setDisableRedirectHandling(true);
        verificationProxyClient = new PooledHttpClient(defaultVerificationUrl, clientConfiguration);
        pool = Executors.newFixedThreadPool(100);
    }

    public void submitForVerification(PublishedCollection publishedCollection, Path jsonPath, CollectionReader reader) {
        logInfo("Submitting collection for external verification").collectionName(publishedCollection.getName()).log();
        List<Result> publishResults = publishedCollection.publishResults;
        for (Result publishResult : publishResults) {
            Set<UriInfo> uriInfos = publishResult.transaction.uriInfos;
            for (UriInfo uriInfo : uriInfos) {
                setHash(uriInfo, reader);
                uriInfo.verificationStatus = UriInfo.VERIFYING;
                publishedCollection.incrementVerifyInProgressCount();
                submit(publishedCollection, jsonPath, uriInfo);
            }
        }
        save(publishedCollection, jsonPath);
    }

    private void submit(PublishedCollection publishedCollection, Path jsoPath, UriInfo uriInfo) {
        pool.submit(new VerifyTask(publishedCollection, jsoPath, uriInfo));
    }

    //Resubmits uri to be verified with configured delay if failed
    private void reSubmit(final PublishedCollection publishedCollection, final Path jsonPath, final UriInfo uriInfo) {
        ((Runnable) () -> {
            try {
                Thread.sleep(Configuration.getVerifyRetrtyDelay());
            } catch (InterruptedException e) {
                logError(e, "Retry delay failed, continuing with verification retry").log();
            }
            uriInfo.verificationStatus = UriInfo.VERIFY_RETRYING;
            submit(publishedCollection, jsonPath, uriInfo);
        }).run();
    }

    private class VerifyTask implements Callable<Object> {
        private final UriInfo uriInfo;
        private final PublishedCollection publishedCollection;
        private Path jsonPath;

        VerifyTask(PublishedCollection publishedCollection, Path jsonPath, UriInfo uriInfo) {
            this.uriInfo = uriInfo;
            this.publishedCollection = publishedCollection;
            this.jsonPath = jsonPath;
        }

        private void verify() {
            try {
                uriInfo.verificationRetryCount++;
                if (isPublished(uriInfo)) {
                    onVerified();
                } else {
                    onVerifyFailed("Not yet available");
                }
            } catch (HttpResponseException e) {
                onVerifyFailed("Verification agent error code:" + e.getStatusCode());
            } catch (IOException e) {
                String errorMessage = "Failed verifying " + e.getMessage();
                logError(e, "Failed verifying").addParameter("uri", uriInfo.uri).log();
                onVerifyFailed(errorMessage);
            }

        }

        private void onVerified() {
            logInfo("Succesfully verified").addParameter("uri", uriInfo.uri).log();
            publishedCollection.incrementVerified();
            publishedCollection.decrementVerifyInProgressCount();
            uriInfo.verificationStatus = UriInfo.VERIFIED;
            uriInfo.verificationEnd = DateConverter.toString(new Date());
            saveIfDone();
        }

        private void onVerifyFailed(String errorMessage) {
            if (Configuration.getVerifyRetrtyCount() == uriInfo.verificationRetryCount) {
                uriInfo.verificationStatus = UriInfo.VERIFY_FAILED;
                publishedCollection.incrementVerifyFailed();
                publishedCollection.decrementVerifyInProgressCount();
                uriInfo.verifyMessage = errorMessage;
            } else {
                reSubmit(publishedCollection, jsonPath, uriInfo);
            }
            saveIfDone();
        }

        private void saveIfDone() {
            if (publishedCollection.verifyInprogressCount == 0) {
                save(publishedCollection, jsonPath);
            }
        }

        @Override
        public Object call() throws Exception {
            verify();
            return null;
        }
    }

    //Verifies content is published by comparing hash value obtained through external website proxy with published hash value
    private boolean isPublished(UriInfo uriInfo) throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("If-None-Match", uriInfo.sha);
        try (CloseableHttpResponse response = verificationProxyClient.sendGet("/hash", headers,
                asList((NameValuePair) new BasicNameValuePair("uri", uriInfo.uri)))) {
            String websiteHash = EntityUtils.toString(response.getEntity());
            return uriInfo.sha.equals(websiteHash);
        }
    }

    private void setHash(UriInfo uriInfo, CollectionReader reader) {
        String uri = uriInfo.uri;
        try {
            try (Resource resource = reader.getResource(uri)) {
                uriInfo.sha = ContentUtil.hash(resource.getData());
            }
        } catch (Exception e) {
            logError(e, "Failed resolving hash for content").addParameter("uri", uri).log();
            e.printStackTrace();
        }
    }

    private void save(PublishedCollection publishedCollection, Path jsonPath) {
        try {
            zebedee.getPublishedCollections().save(publishedCollection, jsonPath);
        } catch (IOException e) {
            logError(e, "Saving published collection failed")
                    .collectionName(publishedCollection.getName()).log();
        }
    }
}

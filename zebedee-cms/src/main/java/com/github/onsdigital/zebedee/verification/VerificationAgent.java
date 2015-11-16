package com.github.onsdigital.zebedee.verification;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;

/**
 * Created by bren on 16/11/15.
 */
public class VerificationAgent {

    private PooledHttpClient verificationProxyClient;
    private ExecutorService pool;
    private Zebedee zebedee;

    public VerificationAgent(Zebedee zebedee) {
        this.zebedee = zebedee;
        String defaultVerificationUrl = Configuration.getDefaultVerificationUrl();
        System.out.println("Initializing verification agent with url " + defaultVerificationUrl);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxTotalConnection(100);
        clientConfiguration.setDisableRedirectHandling(true);
        verificationProxyClient = new PooledHttpClient(defaultVerificationUrl, clientConfiguration);
        pool = Executors.newFixedThreadPool(50);
    }

    public void submitForVerification(PublishedCollection publishedCollection, Path jsonPath) {
        System.out.println("Submitting collection " + publishedCollection.name + " for external verification");
        List<Result> publishResults = publishedCollection.publishResults;
        for (Result publishResult : publishResults) {
            Set<UriInfo> uriInfos = publishResult.transaction.uriInfos;
            for (UriInfo uriInfo : uriInfos) {
                uriInfo.verificationStatus = UriInfo.VERIFYING;
                submit(publishedCollection, jsonPath, uriInfo);
            }
        }
    }

    private void submit(PublishedCollection publishedCollection, Path jsoPath,   UriInfo uriInfo) {
        pool.submit(new VerifyTask(publishedCollection, jsoPath, uriInfo));
    }

    //Resubmits uri to be verified with configured delay if failed
    private void reSubmit(final  PublishedCollection publishedCollection, final Path jsonPath, final UriInfo uriInfo) {
        new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(Configuration.getVerifyRetrtyDelay());
                } catch (InterruptedException e) {
                    System.err.println("Warning! Retry delay failed, continues with verification retry ");
                }
                uriInfo.verificationStatus = UriInfo.VERIFY_RETRYING;
                submit(publishedCollection, jsonPath, uriInfo);
            }
        }.run();
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
            //TODO: Use proper etag headers with a head call when caching solution is implemented
            try {
                uriInfo.verificationRetryCount++;
                System.out.println("Verifying " + uriInfo.uri + ", trial number " + uriInfo.verificationRetryCount);
                if (isPublished(uriInfo)) {
                    onVerified();
                } else {
                    onVerifyFailed("Not yet available");
                }
            } catch (HttpResponseException e) {
                onVerifyFailed("Verification agent error code:" + e.getStatusCode());
            } catch (IOException e) {
                String errorMessage = "Failed verifying " + e.getMessage();
                System.err.println(errorMessage + " " + uriInfo.uri + "\n" + e.getMessage());
                onVerifyFailed(errorMessage);
            }

        }

        private void onVerified() {
            System.out.println("Succesfully verified " + uriInfo.uri);
            publishedCollection.incrementVerified();
            uriInfo.verificationStatus = UriInfo.VERIFIED;
            uriInfo.verificationEnd = DateConverter.toString(new Date());
        }

        private void onVerifyFailed(String errorMessage) {
            System.out.println("Failed verifying " + uriInfo.uri + " " + errorMessage);
            if (Configuration.getVerifyRetrtyCount() == uriInfo.verificationRetryCount) {
                uriInfo.verificationStatus = UriInfo.VERIFY_FAILED;
                publishedCollection.incrementVerifyFailed();
                uriInfo.verifyMessage = errorMessage;
            } else {
                reSubmit(publishedCollection, jsonPath, uriInfo);
            }
            save();
        }

        private void save()  {
            try {
                zebedee.publishedCollections.save(publishedCollection, jsonPath);
            } catch (IOException e) {
                System.err.println("!!!!!!Saving published collection verification status failed for " + uriInfo.uri);
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
        CloseableHttpResponse response = verificationProxyClient.sendGet("/hash", null, asList((NameValuePair) new BasicNameValuePair("uri", uriInfo.uri)));
        String hash = EntityUtils.toString(response.getEntity());
        return false;

    }


}

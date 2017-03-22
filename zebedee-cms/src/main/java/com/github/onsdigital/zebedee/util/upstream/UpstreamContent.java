package com.github.onsdigital.zebedee.util.upstream;


import com.github.onsdigital.zebedee.model.Collection;

public class UpstreamContent {
    public static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET", "upstream-content");
    public static final String ACCESS_KEY =  System.getenv().getOrDefault("S3_ACCESS_KEY", "123");
    public static final String SECRET_ACCESS_KEY =  System.getenv().getOrDefault("S3_SECRET_ACCESS_KEY", "321");
    public static final String S3_HOST = System.getenv().getOrDefault("S3_URL", "http://localhost:4000");


    public static String buildS3Address(Collection collection, String uri) {
        final String collectionId = collection.description.id;
        return "s3://" + S3_BUCKET + "/" + collectionId  + uri;
    }

    public static String buildS3Location(Collection collection, String uri) {
        final String collectionId = collection.description.id;
        return  "/" + collectionId + "/" + uri;
    }
}

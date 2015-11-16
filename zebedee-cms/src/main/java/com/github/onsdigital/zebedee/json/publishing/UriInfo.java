package com.github.onsdigital.zebedee.json.publishing;

/**
 * Information about the transfer of a single file.
 */
public class UriInfo {

    public static final String STARTED = "started";
    public static final String UPLOADED = "uploaded";
    public static final String UPLOAD_FAILED = "upload failed";
    public static final String COMMIT_FAILED = "commit failed";
    public static final String COMMITTED = "committed";
    public static final String ROLLED_BACK = "rolled back";
    public static final String UNKNOWN = "This URI was not recorded in Transaction info";

    public static final String CREATE = "created";
    public static final String UPDATE = "updated";

    public static final String VERIFYING = "verifying";
    public static final String VERIFY_RETRYING = "retrying";
    public static final String VERIFY_FAILED = "failed";
    public static final String VERIFIED = "verified";

    /**
     * This is a String rather than an enum to make deserialisation lenient.
     * <p/>
     * This should be one of the following constant values defined in this class:
     * <ul>
     * <li>{@value #STARTED}</li>
     * <li>{@value #UPLOADED}</li>
     * <li>{@value #UPLOAD_FAILED}</li>
     * <li>{@value #COMMIT_FAILED}</li>
     * <li>{@value #COMMITTED}</li>
     * <li>{@value #ROLLED_BACK}</li>
     * <li>{@value #UNKNOWN}</li>
     * </ul>
     */
    public String status;

    /**
     * This is a String rather than an enum to make deserialisation lenient.
     * <p/>
     * This should be one of the following constant values defined in this class:
     * <ul>
     * <li>{@value #CREATE}</li>
     * <li>{@value #UPDATE}</li>
     * </ul>
     */
    public String action;
    public String uri;
    public String start;
    public String end;
    public long duration;
    public String verificationStatus;
    public String verificationEnd;
    public int verificationRetryCount;
    public String verifyMessage;
    public String sha;
    public long size;
    public String error;
}

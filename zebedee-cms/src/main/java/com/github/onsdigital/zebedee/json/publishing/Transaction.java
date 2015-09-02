package com.github.onsdigital.zebedee.json.publishing;

import org.apache.commons.lang3.StringUtils;

import java.util.*;


/**
 * Details of a single transaction, including any files transferred and any errors encountered.
 * <p/>
 * NB a {@link Transaction} is the unit of synchronization, so methods that manipulate the collections in this class synchronize on <code>this</code>.
 */
public class Transaction {

    // Whilst an ID collision is technically possible it's a
    // theoretical rather than a practical consideration.
    public String id;
    public String startDate;
    public String endDate;
    public String wrappedKey;
    public String salt;

    public Set<UriInfo> uriInfos = new HashSet<>();
    public List<String> errors = new ArrayList<>();

    /**
     * The actual files on disk in this transaction.
     * This might differ slightly from {@link #uriInfos}
     * if there is an issue, so useful to have a direct view of these.
     */
    public Map<String, List<String>> files;


    /**
     * Checks for errors in this transaction.
     *
     * @return If {@link #errors} contains anything, or if any {@link UriInfo#error error} field in {@link #uriInfos} is not blank, true.
     */
    public boolean hasErrors() {
        boolean result = errors !=null && errors.size() > 0;
        if (errors !=null) {
            for (UriInfo uriInfo : uriInfos) {
                result |= StringUtils.isNotBlank(uriInfo.error);
            }
        }
        return result;
    }
}

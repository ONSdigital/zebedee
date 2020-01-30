package com.github.onsdigital.zebedee.logging;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.event.BaseEvent;
import com.github.onsdigital.logging.v2.event.Severity;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.ClickEvent;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;

import static com.github.onsdigital.logging.v2.DPLogger.logConfig;

public class CMSLogEvent extends BaseEvent<CMSLogEvent> {

    public static CMSLogEvent warn() {
        return new CMSLogEvent(logConfig().getNamespace(), Severity.WARN);
    }

    public static CMSLogEvent info() {
        return new CMSLogEvent(logConfig().getNamespace(), Severity.INFO);
    }

    public static CMSLogEvent error() {
        return new CMSLogEvent(logConfig().getNamespace(), Severity.ERROR);
    }

    private CMSLogEvent(String namespace, Severity severity) {
        super(namespace, severity, DPLogger.logConfig().getLogStore());
    }

    public CMSLogEvent user(Session session) {
        if (session != null) {
            user(session.getEmail());
        }
        return this;
    }

    public CMSLogEvent user(String email) {
        if (StringUtils.isNotEmpty(email)) {
            data("user", email);
        }
        return this;
    }

    public CMSLogEvent collectionID(CollectionDescription desc) {
        if (desc != null) {
            collectionID(desc.getId());
        }
        return this;
    }

    public CMSLogEvent collectionID(Collection collection) {
        if (collection != null && collection.getDescription() != null) {
            collectionID(collection.getDescription().getId());
        }
        return this;
    }

    public CMSLogEvent collectionID(String collectionID) {
        if (StringUtils.isNotEmpty(collectionID)) {
            data("collection_id", collectionID);
        }
        return this;
    }

    public CMSLogEvent datasetID(String datasetID) {
        if (StringUtils.isNotEmpty(datasetID)) {
            data("dataset_id", datasetID);
        }
        return this;
    }

    public CMSLogEvent email(Session s) {
        if (s != null) {
            data("email", s.getEmail());
        }
        return this;
    }

    public CMSLogEvent serviceAccountID(ServiceAccount account) {
        if (account != null && StringUtils.isNotEmpty(account.getID())) {
            data("service_account_id", account.getID());
        }
        return this;
    }

    public CMSLogEvent datasetPermissions(CRUD CRUD) {
        if (CRUD != null) {
            data("dataset_permissions", CRUD);
        }
        return this;
    }

    public CMSLogEvent uri(Path uri) {
        if (uri != null) {
            uri(uri.toString());
        }
        return this;
    }

    public CMSLogEvent uri(String uri) {
        if (StringUtils.isNotEmpty(uri)) {
            data("uri", uri);
        }
        return this;
    }

    public CMSLogEvent florenceClickEvent(ClickEvent e) {
        if (null != e && e.getCollection() != null) {
            collectionID(e.getCollection().getId());
            data("trigger", e.getTrigger());
            user(e.getUser());
        }
        return this;
    }

    public CMSLogEvent host(String host) {
        if (StringUtils.isNotEmpty(host)) {
            data("host", host);
        }
        return this;
    }

    public CMSLogEvent transactionId(String transactionId) {
        if (StringUtils.isNotEmpty(transactionId)) {
            data("transaction_id", transactionId);
        }
        return this;
    }
}

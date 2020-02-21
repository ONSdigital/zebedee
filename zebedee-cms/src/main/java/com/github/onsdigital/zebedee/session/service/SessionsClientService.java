package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.session.service.client.SessionClient;
import com.session.service.client.SessionClientImpl;
import com.session.service.entities.SessionCreated;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

public class SessionsClientService implements Sessions {

    private SessionClient client;

    public SessionsClientService() {
        this.client = new SessionClientImpl("http://localhost:6666", "");
    }

    @Override
    public Session create(User user) throws IOException {
        SessionCreated sessionCreated = client.createNewSession(user.getEmail());
        return fromAPIModel(client.getSessionByID(sessionCreated.getId()));
    }

    @Override
    public Session get(HttpServletRequest request) throws IOException {
        String sessionID = RequestUtils.getSessionId(request);
        Session session = null;
        if (StringUtils.isNotEmpty(sessionID)) {
            session = fromAPIModel(client.getSessionByID(sessionID));
        }
        return session;
    }

    @Override
    public Session get(String id) throws IOException {
        Session session = null;
        if (StringUtils.isNotEmpty(id)) {
            session = fromAPIModel(client.getSessionByID(id));
        }
        return session;
    }

    @Override
    public Session find(String email) throws IOException {
        return fromAPIModel(client.getSessionByEmail(email));
    }

    @Override
    public boolean exists(String id) throws IOException {
        return (client.sessionExists(id));
    }

    @Override
    public boolean expired(Session session) {
        if (client.getSessionByID(session.getId()) != null) {
            return false;
        }
        return true;
    }

    @Override
    public Date getExpiryDate(Session session) {
        try {
            Date lastAccessed = get(session.getId()).getLastAccess();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(lastAccessed);
            calendar.add(Calendar.MINUTE, 30);
            return calendar.getTime();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean flushAllSessions() {
        info().log("flushing all sessiosn");
        return client.clear();
    }

    Session fromAPIModel(com.session.service.Session sess) {
        Session session = null;
        if (sess != null) {
            session = new Session(sess.getId(), sess.getEmail(), sess.getStart(), sess.getLastAccess());
        }
        return session;
    }
}

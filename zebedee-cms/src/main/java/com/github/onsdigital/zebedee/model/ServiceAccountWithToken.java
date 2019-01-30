package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.JSONable;

public class ServiceAccountWithToken  extends ServiceAccount implements JSONable {

        private final String token;

        public ServiceAccountWithToken(String id, String token) {
            super(id);
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        @Override
        public String toJSON() {
            return ContentUtil.serialise(this);
        }
    }
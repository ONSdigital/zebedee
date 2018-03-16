package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.service.ServiceStoreImpl;
import org.apache.commons.lang3.BooleanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.function.Supplier;

@Api
public class Service {

    private Supplier<String> randomIdGenerator = () -> Random.id();

    @POST
    public void createService(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition) throws IOException {
        if (BooleanUtils.isTrue(permissionDefinition.admin)) {
            final ServiceStore serviceStoreImpl = Root.zebedee.getServiceStore();
            final String token = randomIdGenerator.get();

            serviceStoreImpl.store(token, request.getInputStream());

        }
    }


    private class NewService {
        private String token;
        private String id;

        public NewService(String token, String id) {
            this.token = token;
            this.id = id;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}

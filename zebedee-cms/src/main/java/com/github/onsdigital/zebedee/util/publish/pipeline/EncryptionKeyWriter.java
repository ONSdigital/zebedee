package com.github.onsdigital.zebedee.util.publish.pipeline;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder;
import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EncryptionKeyWriter {

    private final VaultConfig config;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public EncryptionKeyWriter() {
        final String vaultToken = System.getenv().getOrDefault("VAULT_TOKEN", "");
        final String vaultEndpoint = System.getenv().getOrDefault("VAULT_ADDR", "http://127.0.0.1:8200");
        final int renewTime = Integer.valueOf(System.getenv().getOrDefault("VAULT_RENEW_TIME", "5"));
        final boolean ssl = vaultEndpoint.contains("https://");
        try {
            config = new VaultConfig().address(vaultEndpoint)
                    .token(vaultToken)
                    .sslVerify(ssl)
                    .build();
        } catch (VaultException e) {
            throw new RuntimeException(e);
        }

        scheduler.scheduleAtFixedRate(new RenewToken(), renewTime, renewTime, TimeUnit.MINUTES);
    }

    public void writeKey(Collection collection, Zebedee zebedee) {
        if (collection.description.isEncrypted) {
            final SecretKey key = zebedee.getKeyringCache().schedulerCache.get(collection.description.id);
            final String encrytionKey = Base64.getEncoder().encodeToString(key.getEncoded());
            final Map<String, String> values = new HashMap<>();
            final String path = "secret/zebedee-cms/" + collection.description.id;
            values.put("encryption_key", encrytionKey);
            final Vault vault = new Vault(config);
            try {
                List<String> keyExists = vault.logical().list(path);
                if (keyExists.size() == 0) {
                    vault.logical()
                            .write(path, values);
                }
            } catch (VaultException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private class RenewToken implements Runnable  {
        @Override
        public void run() {
            Vault vault = new Vault(config);
            try {
                vault.auth().renewSelf();
                ZebedeeLogBuilder.logInfo("Renewed vault token").log();
            } catch (VaultException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

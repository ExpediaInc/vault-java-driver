package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;
import com.bettercloud.vault.util.TimeUtil;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>The implementing class for Vault's system operations.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Sys {

    private final VaultConfig config;

    public Sys(final VaultConfig config) {
        this.config = config;
    }

    /**
     * <p>Renews a lease given a lease ID.</p>
     *
     * @param leaseId The Vault lease ID to renew, e.g. <code>mysql/creds/readonly/cf08b224-deda-f3e4-c721-d80b1b1a5ba8</code>
     * @return A {@link LogicalResponse} object.
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse renewLease(final String leaseId) throws VaultException {
        return renewLease(leaseId, null, null);
    }

    /**
     * <p>Renews a lease given a lease ID and increments it by a given time period.</p>
     *
     * @param leaseId The Vault lease ID to renew, e.g. <code>mysql/creds/readonly/cf08b224-deda-f3e4-c721-d80b1b1a5ba8</code>
     * @param time The time value for the incrementation.
     * @param timeUnit The time unit for the incrementation.
     * @return A {@link LogicalResponse} object.
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse renewLease(final String leaseId, final Long time,
                                      final TimeUnit timeUnit) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // Make an HTTP request to Vault
                final Rest rest = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/sys/renew/" + leaseId)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null);

                if (time != null) {
                    JsonObject object = Json.object().add("increment", TimeUtil.timeString(time, timeUnit));

                    rest.body(object.toString().getBytes());
                }

                RestResponse restResponse = rest.put();

                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                String jsonString;
                try {
                    jsonString = new String(restResponse.getBody(), "UTF-8");//NOPMD
                } catch (UnsupportedEncodingException e) {
                    throw new VaultException(e);
                }

                // Parse JSON
                final Map<String, String> data = new HashMap<String, String>();//NOPMD
                JsonValue responseData = Json.parse(jsonString).asObject().get("data");

                if(!responseData.isNull()) {
                    for (final JsonObject.Member member : responseData.asObject()) {
                        final JsonValue jsonValue = member.getValue();
                        if (jsonValue == null || jsonValue.isNull()) {
                            continue;
                        } else if (jsonValue.isString()) {
                            data.put(member.getName(), jsonValue.asString());
                        } else {
                            data.put(member.getName(), jsonValue.toString());
                        }
                    }
                }
                return new LogicalResponse(restResponse, retryCount, data);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    // ... otherwise, give up.
                    throw new VaultException(e);
                }
            }
        }
    }
}

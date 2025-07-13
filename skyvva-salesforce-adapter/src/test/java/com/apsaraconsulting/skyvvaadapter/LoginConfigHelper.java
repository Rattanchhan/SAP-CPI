package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.util.ObjectHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.apache.camel.util.ObjectHelper.isNotEmpty;

public final class LoginConfigHelper {

    private static final LoginConfigHelper INSTANCE = new LoginConfigHelper();

    private final Map<String, String> configuration;

    private LoginConfigHelper() {
        configuration = new HashMap<>();
        try {
            final ResourceBundle properties = ResourceBundle.getBundle("test-salesforce-login");
            properties.keySet().forEach(k -> configuration.put(k, properties.getString(k)));
        } catch (final MissingResourceException ignored) {
            // ignoring if missing
        }

        System.getenv().keySet().stream()//
                .filter(k -> k.startsWith("SALESFORCE_") && isNotEmpty(System.getenv(k)))
                .forEach(k -> configuration.put(fromEnvName(k), System.getenv(k)));
        System.getProperties().keySet().stream().map(String.class::cast)
                .filter(k -> k.startsWith("salesforce.") && isNotEmpty(System.getProperty(k)))
                .forEach(k -> configuration.put(k, System.getProperty(k)));
    }

    private String fromEnvName(final String envVariable) {
        return envVariable.replace('_', '.').toLowerCase();
    }

    SalesforceLoginConfig createLoginConfig() {
        final SalesforceLoginConfig loginConfig = new SalesforceLoginConfig();

        final String explicitType = configuration.get("salesforce.auth.type");
        if (ObjectHelper.isNotEmpty(explicitType)) {
            loginConfig.setType(AuthenticationType.valueOf(explicitType));
        }
        final String loginUrl = configuration.get("salesforce.login.url");
        if (ObjectHelper.isNotEmpty(loginUrl)) {
            loginConfig.setLoginUrl(loginUrl);
        }
        loginConfig.setClientId(configuration.get("salesforce.client.id"));
        loginConfig.setClientSecret(configuration.get("salesforce.client.secret"));
        loginConfig.setUserName(configuration.get("salesforce.username"));
        loginConfig.setPassword(configuration.get("salesforce.password"));

        final KeyStoreParameters keystore = new KeyStoreParameters();
        keystore.setResource(configuration.get("salesforce.keystore.resource"));
        keystore.setPassword(configuration.get("salesforce.keystore.password"));
        keystore.setType(configuration.get("salesforce.keystore.type"));
        keystore.setProvider(configuration.get("salesforce.keystore.provider"));
        loginConfig.setKeystore(keystore);

        validate(loginConfig);

        return loginConfig;
    }

    void validate(final SalesforceLoginConfig loginConfig) {
        loginConfig.validate();
    }

    public static SalesforceLoginConfig getLoginConfig() {
        return INSTANCE.createLoginConfig();
    }

}

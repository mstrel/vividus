package org.vividus.bdd.model;

import java.util.HashMap;
import java.util.Map;

public class MailServerConfiguration
{
    private final String username;
    private final String password;
    private final Map<String, String> properties = new HashMap<>();

    public MailServerConfiguration(String username, String password, Map<String, String> properties)
    {
        this.username = username;
        this.password = password;
        this.properties.putAll(properties);
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }
}

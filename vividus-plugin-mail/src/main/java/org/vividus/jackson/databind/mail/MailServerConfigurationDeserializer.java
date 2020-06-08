package org.vividus.jackson.databind.mail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.vividus.bdd.model.MailServerConfiguration;

@Named
public class MailServerConfigurationDeserializer extends JsonDeserializer<MailServerConfiguration>
{
    @Override
    public MailServerConfiguration deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        JsonNode node = p.getCodec().readTree(p);

        String username = getSafely(node, "username");
        String password = getSafely(node, "password");

        Map<String, String> properties = new HashMap<>();
        JsonNode propertiesNode = node.findPath("properties");
        if (!propertiesNode.isMissingNode())
        {
            gatherProperties(StringUtils.EMPTY, propertiesNode, properties);
        }

        return new MailServerConfiguration(username, password, properties);
    }

    private void gatherProperties(String baseKey, JsonNode root, Map<String, String> container)
    {
        String separator = baseKey.isEmpty() ? StringUtils.EMPTY : ".";
        root.fields().forEachRemaining(e ->
        {
            JsonNode node = e.getValue();
            if (node.isValueNode())
            {
                container.put(baseKey + separator + e.getKey(), node.asText());
            }
            if (node.isObject())
            {
                gatherProperties(e.getKey(), node, container);
            }
        });
    }

    private String getSafely(JsonNode root, String property)
    {
        JsonNode node = root.findPath(property);
        Validate.isTrue(!node.isMissingNode(), "Required property '%s' is not set", property);
        return node.asText();
    }
}

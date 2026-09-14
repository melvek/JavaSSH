package com.mestrap.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.mestrap.entity.HostVars;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HostMapDeserializer extends JsonDeserializer<Map<String, HostVars>> {

    @Override
    public Map<String, HostVars> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        Map<String, HostVars> result = new HashMap<>();
        JsonNode rootNode = p.getCodec().readTree(p);

        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String hostName = field.getKey();
            JsonNode hostNode = field.getValue();

            HostVars hostVars = new HostVars();
            Map<String, Object> extraFields = new HashMap<>();

            // Determine whether it is the shorthand form or the full form
            if (hostNode.isTextual()) {
                // Shorthand form: prod_trans_1: 50.82.81.2
                hostVars.setHost(hostNode.asText());
            } else if (hostNode.isObject()) {
                // Full form: prod_trans_2: {host: 50.82.81.3, remote_path: /xxx}
                Iterator<Map.Entry<String, JsonNode>> hostFields = hostNode.fields();
                while (hostFields.hasNext()) {
                    Map.Entry<String, JsonNode> hostField = hostFields.next();
                    String key = hostField.getKey();
                    JsonNode value = hostField.getValue();

                    // Handle known fields
                    switch (key) {
                        case "host":
                            hostVars.setHost(value.asText());
                            break;
                        case "port":
                            hostVars.setPort(value.asInt());
                            break;
                        case "username":
                            hostVars.setUserName(value.asText());
                            break;
                        case "password":
                            hostVars.setPassword(value.asText());
                            break;
                        default:
                            // All undefined fields go into extraFields
                            extraFields.put(key, convertValue(value));
                            break;
                    }
                }
            }

            // Set extraFields
            if (!extraFields.isEmpty()) {
                hostVars.setExtraFields(extraFields);
            }

            result.put(hostName, hostVars);
        }

        return result;
    }

    /**
     * Convert JsonNode to the corresponding Java object
     */
    private Object convertValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isLong()) {
            return node.asLong();
        }
        if (node.isDouble() || node.isFloat()) {
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            // Simple handling of arrays
            return node.toString();
        }
        if (node.isObject()) {
            // Simple handling of objects
            return node.toString();
        }
        return node.toString();
    }
}
package com.mestrap.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class HostVars {
    private String host;
    private Integer port;

    @JsonProperty("username")
    private String userName;
    private String password;

    // Store all undefined fields
    private Map<String, Object> extraFields = new HashMap<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Include extra fields during serialization as well
    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields;
    }

    // Capture all undefined fields
    @JsonAnySetter
    public void setExtraField(String key, Object value) {
        extraFields.put(key, value);
    }


    // Convenience method: get the value of an extra field
    public Object getExtra(String key) {
        return extraFields.get(key);
    }

    /**
     * ✅ Merge two extraFields; if the target key already exists, skip it
     *
     * @param sourceExtraFields Source extraFields (data to be merged in)
     * @return The number of key-value pairs actually added
     */
    public int mergeExtraFields(Map<String, Object> sourceExtraFields) {
        if (sourceExtraFields == null || sourceExtraFields.isEmpty()) {
            return 0;
        }

        int addedCount = 0;
        for (Map.Entry<String, Object> entry : sourceExtraFields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // If the target does not contain the key, add it
            if (!extraFields.containsKey(key)) {
                extraFields.put(key, value);
                addedCount++;
            }
        }

        return addedCount;
    }

    /**
     * ✅ Merge extraFields from another HostVars object
     *
     * @param source HostVars source object
     * @return The number of key-value pairs actually added
     */
    public int mergeExtraFields(HostVars source) {
        if (source == null) {
            return 0;
        }
        return mergeExtraFields(source.getExtraFields());
    }

    // ==================== Full HostVars merge methods ====================

    /**
     * Merge the entire HostVars object (including all fields and extraFields)
     * Rule: if the target field already has a value, skip it (do not overwrite)
     *
     * @param source Source HostVars object
     */
    public void merge(HostVars source) {
        merge(source, false);
    }

    /**
     * Merge the entire HostVars object (including all fields and extraFields)
     *
     * @param source Source HostVars object
     * @param overwrite Whether to overwrite existing fields (true=overwrite, false=skip)
     */
    public void merge(HostVars source, boolean overwrite) {
        if (source == null) {
            return;
        }

        // 2. Merge port
        if (source.getPort() != null) {
            if (this.port == null || overwrite) {
                this.port = source.getPort();
            }
        }

        // 3. Merge userName
        if (source.getUserName() != null && !source.getUserName().isEmpty()) {
            if (this.userName == null || this.userName.isEmpty() || overwrite) {
                this.userName = source.getUserName();
            }
        }

        // 4. Merge password
        if (source.getPassword() != null && !source.getPassword().isEmpty()) {
            if (this.password == null || this.password.isEmpty() || overwrite) {
                this.password = source.getPassword();
            }
        }

        // 5. Merge extraFields
        Map<String, Object> sourceExtra = source.getExtraFields();
        if (sourceExtra != null && !sourceExtra.isEmpty()) {
            for (Map.Entry<String, Object> entry : sourceExtra.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (!this.extraFields.containsKey(key) || overwrite) {
                    this.extraFields.put(key, value);
                }
            }
        }
    }

    /**
     * Merge the entire HostVars object (skip existing fields)
     * Alias method, equivalent to merge(source, false)
     *
     * @param source Source HostVars object
     */
    public void mergeIfAbsent(HostVars source) {
        merge(source, false);
    }

    /**
     * Merge the entire HostVars object (overwrite existing fields)
     * Alias method, equivalent to merge(source, true)
     *
     * @param source Source HostVars object
     */
    public void mergeWithOverwrite(HostVars source) {
        merge(source, true);
    }

}
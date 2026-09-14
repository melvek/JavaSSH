package com.mestrap.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Inventory {
    private Map<String, ServerGroup> servers;

    @JsonProperty("global_vars")
    private HostVars globalVars = new HostVars();

    public Map<String, ServerGroup> getServers() {
        return servers;
    }

    public void setServers(Map<String, ServerGroup> servers) {
        this.servers = servers;
    }

    public HostVars getGlobalVars() {
        return globalVars;
    }

    public void setGlobalVars(HostVars globalVars) {
        this.globalVars = globalVars;
    }
}

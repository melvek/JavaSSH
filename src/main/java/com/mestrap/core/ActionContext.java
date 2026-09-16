package com.mestrap.core;

import com.mestrap.entity.HostVars;
import java.util.HashMap;
import java.util.Map;

public class ActionContext {

    private final HostVars hostVars;
    private final Map<String, Object> with;
    private final Map<String, Object> vars;
    private final Map<String, Object> outputs = new HashMap<>();

    public ActionContext(HostVars hostVars, Map<String, Object> with, Map<String, Object> vars) {
        this.hostVars = hostVars;
        this.with = with == null ? new HashMap<>() : with;
        this.vars = vars == null ? new HashMap<>() : vars;
    }

    public HostVars getHostVars() { return hostVars; }
    public Map<String, Object> getWith() { return with; }
    public Map<String, Object> getVars() { return vars; }
    public Map<String, Object> getOutputs() { return outputs; }
}
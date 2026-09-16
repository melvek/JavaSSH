package com.mestrap.entity;

import java.util.Map;

public class Step {
    private String name;
    private String action;
    private Map<String, Object> with;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getWith() { return with; }
    public void setWith(Map<String, Object> with) { this.with = with; }
}
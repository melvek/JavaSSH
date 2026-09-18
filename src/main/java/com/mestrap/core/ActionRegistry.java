package com.mestrap.core;

import com.mestrap.action.CommandAction;
import com.mestrap.action.PushAction;
import com.mestrap.action.TaskAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author melvek
 */
public class ActionRegistry {

    private final Map<String, TaskAction> actions = new HashMap<>();

    public ActionRegistry() {
        register(new CommandAction());
        register(new PushAction());
        // 后续扩展：register(new SleepAction()); ...
    }

    public void register(TaskAction action) {
        actions.put(action.name(), action);
    }

    public TaskAction get(String name) {
        return actions.get(name);
    }

    /** 返回所有已注册的 action，用于帮助信息展示 */
    public Collection<TaskAction> all() {
        return actions.values();
    }
}
package com.mestrap.core;

import com.mestrap.entity.Task;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskRegistry {

    private final Map<String, Task> merged = new LinkedHashMap<>();

    public TaskRegistry() {
       merged.putAll(BuiltinTasks.all());
    }

    /** 加载用户流程，覆盖同名内置流程 */
    public void loadTasks(Map<String, Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        tasks.forEach((name, task) -> {
            task.setName(name);
            merged.put(name, task);
        });
    }

    public Task resolve(String name) {
        return merged.get(name);
    }

}
package com.mestrap.core;

import com.mestrap.entity.Task;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务注册表：内置任务 + 用户任务，用户任务同名时覆盖内置任务。
 *
 * @author melvek
 */
public final class TaskRegistry {

    private final Map<String, Task> merged = new LinkedHashMap<>(16);

    public TaskRegistry() {
        merged.putAll(BuiltinTasks.all());
    }

    /**
     * 加载用户任务，同名时覆盖内置任务。
     *
     * @param tasks 用户任务映射，可为 null
     */
    public void loadTasks(Map<String, Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        tasks.forEach((name, task) -> {
            task.setName(name);
            merged.put(name, task);
        });
    }

    /**
     * 按名查找任务。
     *
     * @param name 任务名
     * @return 任务对象，未找到返回 null
     */
    public Task resolve(String name) {
        return merged.get(name);
    }

    /**
     * 返回所有已注册的任务（只读）。
     *
     * @return 任务映射
     */
    public Map<String, Task> all() {
        return Collections.unmodifiableMap(merged);
    }
}
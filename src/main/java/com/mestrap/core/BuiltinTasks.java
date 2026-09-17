package com.mestrap.core;

import com.mestrap.entity.Task;
import com.mestrap.entity.Step;

import java.util.*;

public class BuiltinTasks {

    public static Map<String, Task> all() {
        Map<String, Task> map = new LinkedHashMap<>();
        map.put("command", command());
        map.put("push", push());
        return map;
    }

    private static Task command() {
        Task c = new Task();
        c.setName("command");
        c.setDescription("Execute a remote command");

        Step s = new Step();
        s.setName("exec");
        s.setAction("command");

        Map<String, Object> withMap = new HashMap<>();
        withMap.put("command", "${command}");
        s.setWith(withMap);

        c.setSteps(Collections.singletonList(s));
        return c;
    }

    private static Task push() {
        Task c = new Task();
        c.setName("push");
        c.setDescription("Upload a file to remote server");

        Step s = new Step();
        s.setName("upload");
        s.setAction("push");

        Map<String, Object> withMap = new HashMap<>();
        withMap.put("file", "${file}");
        withMap.put("dest", "${dest}");
        withMap.put("zip", "${zip}");
        withMap.put("force", "${force}");
        s.setWith(withMap);

        c.setSteps(Collections.singletonList(s));
        return c;
    }

}
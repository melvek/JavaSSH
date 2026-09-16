package com.mestrap.core;

import com.mestrap.entity.Task;
import com.mestrap.entity.Step;

import java.util.*;

public class BuiltinTasks {

    public static Map<String, Task> all() {
        Map<String, Task> map = new LinkedHashMap<>();
        map.put("command", command());
        map.put("push", push());
        map.put("deploy", deploy());
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
        withMap.put("dest", "${service_path}");
        s.setWith(withMap);

        c.setSteps(Collections.singletonList(s));
        return c;
    }

    private static Task deploy() {
        Task c = new Task();
        c.setName("deploy");
        c.setDescription("Upload a file and restart service");

        Step s1 = new Step();
        s1.setName("upload");
        s1.setAction("push");

        Map<String, Object> withMap1 = new HashMap<>();
        withMap1.put("file", "${file}");
        withMap1.put("dest", "${service_path}");
        s1.setWith(withMap1);

        Step s2 = new Step();
        s2.setName("restart");
        s2.setAction("command");

        Map<String, Object> withMap2 = new HashMap<>();
        withMap2.put("command", "${command}");
        s2.setWith(withMap2);

        List<Step> steps = new ArrayList<>();
        steps.add(s1);
        steps.add(s2);
        c.setSteps(steps);
        return c;
    }
}
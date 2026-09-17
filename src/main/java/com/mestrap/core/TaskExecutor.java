package com.mestrap.core;

import com.mestrap.entity.Task;
import com.mestrap.entity.HostVars;
import com.mestrap.entity.Step;
import com.mestrap.exception.JsshException;
import com.mestrap.utils.LogPrinter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskExecutor {

    private final ActionRegistry actionRegistry;

    public TaskExecutor(ActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    /**
     * 对一批主机执行一条流程
     */
    public int executeAll(Task task,
                          Map<String, HostVars> hosts,
                          Map<String, Object> globalVars,
                          Map<String, Object> cliVars) {

        int success = 0, failed = 0;

        LogPrinter.section("Start");
        LogPrinter.info("Task: " + task.getName() + " (" + task.getSteps().size() + " steps)");
        LogPrinter.info("Targets: " + hosts.size());

        int idx = 0;
        for (Map.Entry<String, HostVars> e : hosts.entrySet()) {
            idx++;
            String hostName = e.getKey();
            HostVars hostVars = e.getValue();

            LogPrinter.progress(idx, hosts.size(), "Processing: " + hostName);

            try {
                executeOnHost(task, hostVars, globalVars, cliVars);
                success++;
                LogPrinter.success(hostName + " OK");
            } catch (Exception ex) {
                failed++;
                LogPrinter.error(hostName + " FAIL: " + ex.getMessage());
            }
        }

        LogPrinter.summary(success + failed, success, failed);
        return failed == 0 ? 0 : 1;
    }

    /**
     * 对单台主机执行整条流程
     */
    public void executeOnHost(Task task,
                              HostVars hostVars,
                              Map<String, Object> globalVars,
                              Map<String, Object> cliVars) {

        Map<String, Object> baseVars = mergeBaseVars(globalVars, hostVars, cliVars);

        List<Step> steps = task.getSteps();
        int total = steps.size();

        for (int i = 0; i < total; i++) {
            Step step = steps.get(i);

            LogPrinter.info("[STEP " + (i + 1) + "/" + total + "] " + step.getName());

            TaskAction action = actionRegistry.get(step.getAction());
            if (action == null) {
                throw new RuntimeException("Unknown action: " + step.getAction());
            }

            // 步骤级变量池：baseVars + step.with，CLI 最后叠加（最高优先级）
            // FIXED 修复 cli 参数会被 step 参数覆盖的错误
            Map<String, Object> stepVars = new HashMap<>(baseVars);
            if (step.getWith() != null) {
                stepVars.putAll(step.getWith());
            }
            if (cliVars != null) {
                // CLI 中的参数优先级最高
                stepVars.putAll(cliVars);
            }

            try {
                action.execute(new ActionContext(hostVars, step.getWith(), stepVars));
            } catch (Exception e) {
                throw new RuntimeException("Step failed: " + step.getName() + " - " + e.getMessage(), e);
            }

            // 执行成功后等待
            if (step.getDelay() > 0) {
                LogPrinter.emptyLine();
                LogPrinter.info("Waiting " + step.getDelay() + "s before next step...");
                try {
                    Thread.sleep(step.getDelay() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JsshException(hostVars.getHost(), step.getName(), "Interrupted while waiting after step", e);
                }
            }
            LogPrinter.emptyLine();
        }
    }

    private Map<String, Object> mergeBaseVars(Map<String, Object> globalVars,
                                              HostVars hostVars,
                                              Map<String, Object> cliVars) {
        Map<String, Object> vars = new HashMap<>();
        if (globalVars != null) {
            vars.putAll(globalVars);
        }
        if (hostVars.getExtraFields() != null) {
            vars.putAll(hostVars.getExtraFields());
        }
        if (cliVars != null) {
            vars.putAll(cliVars);
        }
        return vars;
    }
}
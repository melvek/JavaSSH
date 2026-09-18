package com.mestrap.cli;

import com.mestrap.action.TaskAction;
import com.mestrap.core.*;
import com.mestrap.entity.Task;
import com.mestrap.entity.HostVars;
import com.mestrap.entity.Inventory;
import com.mestrap.entity.Step;
import com.mestrap.utils.ConfirmUtil;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author melvek
 */
public class CommandDispatcher {

    private final TaskRegistry taskRegistry = new TaskRegistry();
    private final ActionRegistry actionRegistry = new ActionRegistry();

    public int dispatch(String[] args) {

        // 无参数：打印帮助
        if (args == null || args.length == 0) {
            ShowHelp.printGlobal(actionRegistry);
            return 1;
        }

        // 第一个参数 = 流程名
        String taskName = args[0];
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);

        // 加载清单（先预扫描 -i）
        String invFile = preScanInventory(args);
        Inventory inventory = InventoryLoader.load(invFile);

        // 注册用户流程（同名覆盖内置流程）
        taskRegistry.loadTasks(inventory.getTasks());

        // 解析流程
        Task task = taskRegistry.resolve(taskName);
        if (task == null) {
            // 流程不存在时，如果用户想看帮助，仍然输出帮助
            if (containsHelp(args)) {
                ShowHelp.printGlobal(actionRegistry);
                return 0;
            }
            ShowHelp.printGlobal(actionRegistry);
            return 1;
        }

        // 构建 Options（全局 + 该流程用到的 action 选项）
        Options options = buildOptions(task);

        // 解析参数
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, remaining);
        } catch (ParseException e) {
            LogPrinter.error("Invalid arguments: " + e.getMessage());
            LogPrinter.hint("Use -h to see help");
            return 1;
        }

        // 元信息
        if (cl.hasOption(GlobalOptions.VERSION)) {
            ShowHelp.printVersion();
            return 0;
        }
        if (cl.hasOption(GlobalOptions.HELP)) {
            ShowHelp.printGlobal(actionRegistry);
            return 0;
        }

        // 解析目标主机
        Map<String, HostVars> hosts = HostResolver.resolve(
                cl.getArgList(), inventory, cl);

        if (hosts.isEmpty()) {
            LogPrinter.error("No target hosts specified");
            LogPrinter.hint("Usage: jssh " + taskName + " <hosts...> [options]");
            return 1;
        }

        // 展示主机列表
        LogPrinter.section("Target hosts");
        hosts.forEach((name, vars) ->
                LogPrinter.listItem(name, vars.getHost()));

        // 仅列出主机
        if (cl.hasOption(GlobalOptions.LIST)) {
            return 0;
        }

        // 确认
        if (!cl.hasOption(GlobalOptions.YES)) {
            // noinspection AlibabaUndefineMagicConstant
            if (!ConfirmUtil.confirm("Confirm to proceed")) {
                return 0;
            }
        }

        // 提取 CLI 变量注入
        Map<String, Object> cliVars = extractCliVars(cl, task);

        // 执行
        TaskExecutor executor = new TaskExecutor(actionRegistry);
        return executor.executeAll(
                task,
                hosts,
                inventory.getGlobalVars() != null
                        ? inventory.getGlobalVars().getExtraFields()
                        : Collections.<String, Object>emptyMap(),
                cliVars
        );
    }

    /**
     * 构建 Options：全局选项 + 该流程用到的所有 action 的 cliOptions
     */
    private Options buildOptions(Task task) {
        Options options = new Options();

        // 全局选项
        for (Option o : GlobalOptions.all()) {
            options.addOption(o);
        }

        // 流程里用到的 action 选项（去重）
        Set<String> seen = new HashSet<>();
        for (Step step : task.getSteps()) {
            String actionName = step.getAction();
            if (actionName == null || seen.contains(actionName)) {
                continue;
            }
            seen.add(actionName);

            TaskAction action = actionRegistry.get(actionName);
            if (action == null) {
                continue;
            }

            for (Option o : action.cliOptions()) {
                options.addOption(o);
            }
        }

        return options;
    }

    /**
     * 提取 CLI 变量注入：根据 action 声明的 cliVarMapping 把 -e / -f / -d 等
     * 映射到 ${command} / ${file} / ${dest} 等变量
     */
    private Map<String, Object> extractCliVars(CommandLine cl, Task task) {
        Map<String, Object> vars = new HashMap<>(8);
        Set<String> seen = new HashSet<>();

        for (Step step : task.getSteps()) {
            String actionName = step.getAction();
            if (actionName == null || seen.contains(actionName)) {
                continue;
            }
            seen.add(actionName);

            TaskAction action = actionRegistry.get(actionName);
            if (action == null) {
                continue;
            }

            for (Map.Entry<String, String> e : action.cliVarMapping().entrySet()) {
                String cliOpt = e.getKey();
                String varKey = e.getValue();
                if (cl.hasOption(cliOpt)) {
                    vars.put(varKey, cl.getOptionValue(cliOpt));
                }
            }
        }

        return vars;
    }

    /**
     * 预扫描 -i / --inventory，用于在完整解析前确定清单文件路径
     */
    private String preScanInventory(String[] args) {
        String defaultFile = "inventory.yaml";
        for (int i = 0; i < args.length; i++) {
            boolean hasInv = ("-i".equals(args[i]) || "--inventory".equals(args[i]));

            if (hasInv && i + 1 < args.length) {
                return args[i + 1];
            }
            if (args[i].startsWith("--inventory=")) {
                return args[i].substring("--inventory=".length());
            }
        }
        return defaultFile;
    }

    /**
     * 判断参数里是否带 -h / --help
     */
    private boolean containsHelp(String[] args) {
        for (String a : args) {
            if ("-h".equals(a) || "--help".equals(a)) {
                return true;
            }
        }
        return false;
    }
}
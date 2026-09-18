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
        // 获取流程名称之后，从原参数列表中去掉
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);

        // ---- 1. 一次性注册所有选项 ----
        Options options = buildAllOptions();

        // ---- 2. 直接解析 ----
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

        // ---- 4. 加载清单 ----
        String invFile = cl.hasOption("i")
                ? cl.getOptionValue("i")
                : "inventory.yaml";
        Inventory inventory = InventoryLoader.load(invFile);

        // 注册用户流程（同名覆盖内置流程）
        taskRegistry.loadTasks(inventory.getTasks());

        // 解析流程
        Task task = taskRegistry.resolve(taskName);
        if (task == null) {
            LogPrinter.error("Unknown task: <" + taskName + ">");
            // 流程不存在时，输出帮助
            ShowHelp.printGlobal(actionRegistry);
            return 1;
        }

        // 解析目标主机
        Map<String, HostVars> hosts = HostResolver.resolve(cl.getArgList(), inventory, cl);

        if (hosts.isEmpty()) {
            LogPrinter.error("No target hosts specified");
            LogPrinter.hint("Usage: jssh " + taskName + " <hosts...> [options]");
            return 1;
        }

        // 展示主机列表
        LogPrinter.section("Target hosts");
        hosts.forEach((name, vars) -> LogPrinter.listItem(name, vars.getHost()));

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
        Map<String, Object> cliVars = extractCliVars(cl);

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
     * 构建选项
     * @return
     */
    private Options buildAllOptions() {
        Options options = new Options();

        // 全局选项
        for (Option o : GlobalOptions.all()) {
            options.addOption(o);
        }

        // 所有 action 的选项，一次性注册
        for (TaskAction action : actionRegistry.all()) {
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
    private Map<String, Object> extractCliVars(CommandLine cl) {
        Map<String, Object> vars = new HashMap<>(8);

        for (TaskAction action : actionRegistry.all()) {
            for (Map.Entry<String, String> e : action.cliVarMapping().entrySet()) {
                // 长选项名
                String cliOpt = e.getKey();
                String varKey = e.getValue();
                Option opt = findOption(action, cliOpt);
                if (opt == null) {
                    continue;
                }

                if (opt.hasArg()) {
                    if (cl.hasOption(cliOpt)) {
                        vars.put(varKey, cl.getOptionValue(cliOpt));
                        LogPrinter.info("Add var:" + varKey);
                    }
                } else {
                    // 开关型
                    if (cl.hasOption(cliOpt)) {
                        vars.put(varKey, "true");
                        LogPrinter.info("Add var:" + varKey);
                    }
                }
            }
        }
        return vars;
    }

    private Option findOption(TaskAction action, String longOpt) {
        for (Option o : action.cliOptions()) {
            if (longOpt.equals(o.getLongOpt())) {
                return o;
            }
        }
        return null;
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
package com.mestrap.action;

import com.mestrap.core.ActionContext;
import com.mestrap.core.JSchCommandExecutor;
import com.mestrap.core.TaskAction;
import com.mestrap.utils.LogPrinter;
import com.mestrap.utils.VariableReplacer;
import org.apache.commons.cli.Option;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandAction implements TaskAction {

    @Override
    public String name() { return "command"; }

    @Override
    public List<Option> cliOptions() {
        return Collections.singletonList(
                Option.builder("e").longOpt("execute").hasArg().argName("string")
                        .desc("Command string (injected as ${command})")
                        .build()
        );
    }

    @Override
    public Map<String, String> cliVarMapping() {
        return Collections.singletonMap("execute", "command");
    }

    @Override
    public void execute(ActionContext ctx) throws Exception {
        Object raw = ctx.getWith().get("command");
        if (raw == null) {
            throw new IllegalArgumentException("command action requires 'command' parameter");
        }

        String cmd = VariableReplacer.replace(String.valueOf(raw), ctx.getVars());

        LogPrinter.info("Execute command: " + cmd);

        int exitCode = JSchCommandExecutor.executeCommand(
                ctx.getHostVars().getHost(),
                ctx.getHostVars().getPort(),
                ctx.getHostVars().getUserName(),
                ctx.getHostVars().getPassword(),
                cmd
        );

        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }
}
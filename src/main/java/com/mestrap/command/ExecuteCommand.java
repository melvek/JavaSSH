package com.mestrap.command;

import com.mestrap.core.JSchCommandExecutor;
import com.mestrap.entity.HostVars;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;

import java.util.LinkedList;

/**
 * Execute remote command: jssh command -e "execute string" *
 */
public class ExecuteCommand extends JavaSSHCommand {

    @Override
    public void initializeOptions() {
        // options.addOption(Option.builder("n").longOpt("no-safe-check").desc("Ignore dangerous operation commands").build());
        addOption(Option.builder("e").longOpt("execute").hasArg().argName("string").desc("Command to execute").build());
        addOption(Option.builder("h").longOpt("help").desc("Show command help information").build());
    }

    @Override
    public String getCommandName() {
        return "command";
    }

    @Override
    public void process(CommandLine commandLine, HostVars vars) throws MissingArgumentException {
        String executeString;

        // Check required parameters
        if (commandLine.hasOption("e")) {
            executeString = commandLine.getOptionValue("e");
        } else {
            executeString = (String) vars.getExtra("command");
        }

        if (executeString == null) {
            throw new MissingArgumentException("No execution command specified. Please use the -e option, or add a command parameter in the inventory file");
        }

        executeString = replace(executeString, vars.getExtraFields());
        LogPrinter.info("Command to execute resolved: " + executeString);

        String result = JSchCommandExecutor.executeCommand(vars.getHost(), vars.getPort(), vars.getUserName(), vars.getPassword(), executeString);
        System.out.println(result);
    }
}
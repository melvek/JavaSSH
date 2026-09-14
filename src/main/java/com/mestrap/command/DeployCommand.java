package com.mestrap.command;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.mestrap.core.JSchCommandExecutor;
import com.mestrap.core.JSchFileUploader;
import com.mestrap.entity.HostVars;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;

import java.io.File;
import java.util.LinkedList;

public class DeployCommand extends JavaSSHCommand{

    @Override
    public void initializeOptions() {
        addOption(Option.builder("f").longOpt("files").hasArg().argName("file or folder").desc("File to push to remote").build());
        addOption(Option.builder("d").longOpt("dest-path").hasArg().argName("path").desc("File to push to remote").build());
        addOption(Option.builder("F").longOpt("force").desc("Allow overwriting existing remote files").build());
        addOption(Option.builder("z").longOpt("zip").desc("Allow file compression; requires remote unzip command support").build());
        addOption(Option.builder("h").longOpt("help").desc("Show push help information").build());
        addOption(Option.builder("e").longOpt("execute").hasArg().argName("string").desc("Command to execute").build());
        addOption(Option.builder("y").longOpt("yes").desc("Skip confirmation prompt").build());
        addOption(Option.builder("h").longOpt("help").desc("Show command help information").build());
    }

    @Override
    public String getCommandName() {
        return "deploy";
    }

    @Override
    public void process(CommandLine commandLine, HostVars vars) throws MissingArgumentException {

        String localFile;
        String remoteFile;
        String executeString;

        // Check required parameters
        if (commandLine.hasOption("f")) {
            localFile = commandLine.getOptionValue("f");
        } else {
            localFile = (String) vars.getExtra("package_path");
        }

        if (localFile == null) {
            throw new MissingArgumentException("No update path or package specified. Please use the -f option, or add a package_path parameter in the inventory file");
        }

        localFile = replace(localFile, vars.getExtraFields());

        File file = new File(localFile);
        if (file.exists()) {
            localFile = file.getAbsolutePath();
        } else {
            throw new MissingArgumentException("Specified file does not exist: " + localFile);
        }

        if (commandLine.hasOption("d")) {
            remoteFile = commandLine.getOptionValue("d");
        } else {
            remoteFile = (String) vars.getExtra("service_path");
        }

        if (remoteFile == null) {
            throw new MissingArgumentException("No remote path specified. Please use the -d option, or add a service_path parameter in the inventory file");
        }

        remoteFile = replace(remoteFile, vars.getExtraFields());

        // Check required parameters
        if (commandLine.hasOption("e")) {
            executeString = commandLine.getOptionValue("e");
        } else {
            executeString = (String) vars.getExtra("command");
        }

        executeString = replace(executeString, vars.getExtraFields());

        try {
            LogPrinter.info("File to deploy: " + localFile);
            LogPrinter.info("Server deployment path: " + remoteFile);
            JSchFileUploader.uploadFile(vars.getHost(), vars.getPort(), vars.getUserName(), vars.getPassword(), localFile, remoteFile);
            LogPrinter.info("Application update completed");
            LogPrinter.info("Executing remote command: " + executeString);
            String result = JSchCommandExecutor.executeCommand(vars.getHost(), vars.getPort(), vars.getUserName(), vars.getPassword(), executeString);
            LogPrinter.info(result);
        } catch (SftpException e) {
            throw new RuntimeException(e);
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }
}
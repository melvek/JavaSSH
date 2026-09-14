package com.mestrap.command;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.mestrap.core.JSchFileUploader;
import com.mestrap.entity.HostVars;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;

import java.util.LinkedList;

public class PushCommand extends JavaSSHCommand {

    @Override
    public void initializeOptions() {
        addOption(Option.builder("f").longOpt("files").hasArg().argName("file or folder").desc("File to push to remote").build());
        addOption(Option.builder("d").longOpt("dest-path").hasArg().argName("path").desc("File to push to remote").build());
        addOption(Option.builder("F").longOpt("force").desc("Allow overwriting existing remote files").build());
        addOption(Option.builder("z").longOpt("zip").desc("Allow file compression; requires remote unzip command support").build());
        addOption(Option.builder("h").longOpt("help").desc("Show push help information").build());
    }

    @Override
    public String getCommandName() {
        return "push";
    }

    @Override
    public void process(CommandLine commandLine, HostVars vars) throws MissingArgumentException {
        try {
            String localFile;
            String remoteFile;

            // Check required parameters
            if (commandLine.hasOption("f")) {
                localFile = commandLine.getOptionValue("f");
            } else {
                throw new MissingArgumentException("No upload file specified. Please use the -f option");
            }

            if (commandLine.hasOption("d")) {
                remoteFile = commandLine.getOptionValue("d");
            } else {
                remoteFile = (String) vars.getExtra("service_path");
            }

            if (remoteFile == null) {
                throw new MissingArgumentException("No remote path specified. Please use the -d option, or add a service_path parameter in the inventory file");
            }

            LogPrinter.info("File to deploy: " + localFile);
            LogPrinter.info("Server deployment path: " + remoteFile);

            JSchFileUploader.uploadFile(vars.getHost(), vars.getPort(), vars.getUserName(), vars.getPassword(), localFile, remoteFile);
        } catch (JSchException e) {
            throw new RuntimeException(e);
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }
}
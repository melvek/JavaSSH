package com.mestrap.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mestrap.entity.HostVars;
import com.mestrap.entity.Inventory;
import com.mestrap.utils.ConfirmUtil;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JavaSSHCommand {

    public JavaSSHCommand() {
        initializeGlobalOptions();
    }

    /**
     * Global option list
     */
    private LinkedList<Option> globalOptionList;

    /**
     * Command-specific option list
     */
    private LinkedList<Option> commandOptionList = new LinkedList<>();

    public void addOption(Option option) {
        commandOptionList.add(option);
    }

    /**
     * Inventory file entity
     */
    private Inventory inventory;

    private int successCount = 0;       // Number of successful executions
    private int failureCount = 0;       // Number of failed executions
    private List<String> failedHosts = new ArrayList<>();       // Hosts that failed execution

    /**
     * Load inventory file
     * @param inventoryFile
     */
    public void load(String inventoryFile) {

        LogPrinter.info("Loading inventory file: " + inventoryFile);
        File file = new File(inventoryFile);

        if (!file.exists()) {
            LogPrinter.error("Inventory file does not exist: " + file.getAbsolutePath());
            LogPrinter.hint("Please ensure the file exists, or use the -i option to specify the correct file path");
            throw new RuntimeException("Inventory file not found: " + file.getAbsolutePath());
        }

        try (InputStream input = new FileInputStream(file)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            inventory = mapper.readValue(input, Inventory.class);

            // ========== Added output ==========
            int serverCount = inventory.getServers() != null ? inventory.getServers().size() : 0;
            LogPrinter.success("Successfully loaded inventory file, containing " + serverCount + " server groups");

            // Add date to inventory extra info
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            inventory.getGlobalVars().setExtraField("date", sdf.format(new Date()));

        } catch (Exception e) {
            // ========== Added output ==========
            LogPrinter.error("Failed to parse inventory file: " + e.getMessage());
            LogPrinter.hint("Please check if the fina_uat.yaml format is correct");
            throw new RuntimeException("Failed to parse fina_uat.yaml", e);
        }
    }

    private void initializeGlobalOptions() {
        globalOptionList = new LinkedList<>();
        globalOptionList.add(Option.builder("i").longOpt("inventory").hasArg().argName("file").desc("Specify server inventory file").build());
        globalOptionList.add(Option.builder("l").longOpt("list").desc("Show server list").build());

        globalOptionList.add(Option.builder("P").longOpt("port").hasArg().argName("int").desc("Server connection port").build());
        globalOptionList.add(Option.builder("u").longOpt("user").hasArg().argName("string").desc("Server username").build());
        globalOptionList.add(Option.builder("p").longOpt("password").hasArg().argName("string").desc("Server password").build());

        globalOptionList.add(Option.builder("delay").desc("Wait time").build());
        globalOptionList.add(Option.builder("v").longOpt("version").desc("Show version information").build());
        globalOptionList.add(Option.builder("h").longOpt("help").desc("Show this help message").build());
    }

    public void execute(String[] args) {

        // 初始化参数信息
        initializeOptions();

        Options options = new Options();

        for (Option option : commandOptionList) {
            options.addOption(option);
        }

        for (Option option : globalOptionList) {
            options.addOption(option);
        }

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine commandLine = parser.parse(options, args);

            // ========== Added output: handle help ==========
            if (commandLine.hasOption("h")) {
                ShowHelp(true);
                return;
            }

            // ========== Added output: handle version ==========
            if (commandLine.hasOption("v")) {
                printVersion();
                return;
            }

            String inventoryFile = "fina_uat.yaml";
            if (commandLine.hasOption("i")) {
                inventoryFile = commandLine.getOptionValue("i");
            }

            load(inventoryFile);

            // ========== Added output: override parameter hints ==========
            if (commandLine.hasOption("P")) {
                int port = Integer.parseInt(commandLine.getOptionValue("P"));
                inventory.getGlobalVars().setPort(port);
            }
            if (commandLine.hasOption("u")) {
                String user = commandLine.getOptionValue("u");
                inventory.getGlobalVars().setUserName(user);
            }
            if (commandLine.hasOption("p")) {
                String password = commandLine.getOptionValue("p");
                inventory.getGlobalVars().setPassword(password);
                LogPrinter.warning("Passing passwords via command line is a security risk; use environment variables instead");
            }

            Map<String, HostVars> targetHost = new LinkedHashMap<>();

            HostVars globalVars = inventory.getGlobalVars();

            List<String> hostNames = commandLine.getArgList();

            // ========== Added output: check if servers are specified ==========
            if (hostNames.isEmpty()) {
                LogPrinter.error("No target server or server group specified");
                LogPrinter.hint("Usage example: jssh " + getCommandName() + " prod-trans -f app.jar");
                return;
            }

            for (String hostName : hostNames) {
                boolean flag = false;
                if (inventory.getServers().containsKey(hostName)) {

                    HostVars serverGroupVars = inventory.getServers().get(hostName).getVars();
                    serverGroupVars.merge(globalVars);

                    inventory.getServers().get(hostName).getHosts().forEach((name, var) -> {
                        var.merge(serverGroupVars);
                        targetHost.put(name, var);
                    });
                    flag = true;

                    // ========== Added output: resolved to server group ==========
                    LogPrinter.info("Server group: " + hostName + " (" + inventory.getServers().get(hostName).getHosts().size() + ")");
                }

                if (!flag) {
                    HostVars vars = new HostVars();
                    vars.setHost(hostName);
                    vars.setPort(globalVars.getPort());
                    vars.setUserName(globalVars.getUserName());
                    vars.setPassword(globalVars.getPassword());
                    targetHost.put(hostName, vars);

                    // ========== Added output: added standalone server ==========
                    LogPrinter.info("Server: " + hostName);
                }
            }

            if (commandLine.hasOption("l") || getCommandName().equals("deploy")) {
                // ========== Added output: show server list ==========
                LogPrinter.section("Target server list");
                targetHost.forEach((name, vars) -> {
                    LogPrinter.listItem(name, vars.getHost());
                });

                if (commandLine.hasOption("l")) {
                    return;
                }

                if (!ConfirmUtil.confirm("Confirm whether the server list to update is correct")) {
                    return;
                }
            }

            // ========== Added output: start task execution ==========
            LogPrinter.section("Starting task execution");
            LogPrinter.info("Number of target servers: " + targetHost.size());

            final int[] current = {0};
            targetHost.forEach((name, vars) -> {
                // ========== Added output: progress hint ==========
                current[0]++;
                LogPrinter.progress(current[0], targetHost.size(), "Processing: " + name + " [" + vars.getHost() + "]");

                try {
                    process(commandLine, vars);

                    // ========== Added output: execution succeeded ==========
                    successCount++;
                    LogPrinter.success(name + " executed successfully");
                } catch (MissingArgumentException e) {
                    // ========== Added output: execution failed ==========
                    failureCount++;
                    failedHosts.add(name);
                    LogPrinter.error(name + " execution failed: " + e.getMessage());
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    // ========== Added output: execution failed ==========
                    failureCount++;
                    failedHosts.add(name);
                    LogPrinter.error(name + " B execution failed: " + e);
                }

                LogPrinter.emptyLine();
            });

            // ========== Added output: execution summary ==========
            LogPrinter.summary(
                    successCount + failureCount,
                    successCount,
                    failureCount,
                    failedHosts.toArray(new String[0])
            );
        } catch (ParseException e) {
            // ========== Added output ==========
            LogPrinter.error("Parameter parsing failed: " + e.getMessage());
            LogPrinter.hint("Use -h or --help to view help information");
            throw new RuntimeException(e);
        }
    }

    public abstract void initializeOptions();

    public abstract String getCommandName();

    public abstract void process(CommandLine commandLine, HostVars vars) throws MissingArgumentException;

    public String replace(String template, Map<String, Object> data) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Pattern placeHolder = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = placeHolder.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String replacement = data.containsKey(key) ?
                    String.valueOf(data.get(key)) : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // ========== Added: print version information ==========
    private void printVersion() {
        LogPrinter.section("JavaSSH v1.0.0");
        LogPrinter.info("Lightweight operations tool based on JSch");
        String buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LogPrinter.keyValue("Build time", buildTime);
    }

    // ========== Modified: enhanced help information (keeps original logic, only optimizes display) ==========
    public void ShowHelp(boolean withCommand) {
        Options globalOptions = new Options();
        for (Option option : globalOptionList) {
            globalOptions.addOption(option);
        }

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setOptionComparator(null);


        // ========== Added output: more user-friendly main help ==========
        System.out.println("\nJavaSSH - Lightweight operations tool based on JSch");
        System.out.println("Version: 1.0.0\n");

        if (withCommand) {
            // ========== Added output: command help title ==========
            System.out.println("Command help: " + getCommandName());
            System.out.println();

            Options commandOptions = new Options();
            for (Option option : commandOptionList) {
                commandOptions.addOption(option);
            }

            helpFormatter.printHelp("jssh " + getCommandName() + " [hosts...] [options]\n\nCommand options:\n", commandOptions);
            System.out.println();

        } else {

            System.out.println("Usage:");
            System.out.println("  jssh <command> [hosts...] [options]\n");

            System.out.println("Available commands:");
            System.out.println("  command  Execute commands on remote servers");
            System.out.println("  push     Upload files to remote servers");
            System.out.println("  deploy   Deploy applications to remote servers");
            // System.out.println("  pull     Download files from remote servers (to be implemented)\n");

            System.out.println("  jssh <command> -h  View detailed command help\n");

            System.out.println("Examples:");
            System.out.println("  # Execute remote command");
            System.out.println("  jssh command web-server-01 -e 'ls -la /opt'");
            System.out.println();
            System.out.println("  # Upload file");
            System.out.println("  jssh push app-server -f app.jar -d /opt/app/");
            System.out.println();
            System.out.println("  # Deploy using inventory file");
            System.out.println("  jssh deploy -i fina_uat.yaml prod-trans -f app.jar\n");
        }

        // System.out.println("Global options:");
        helpFormatter.setSyntaxPrefix("");
        helpFormatter.printHelp("Global options:", globalOptions);
    }
}
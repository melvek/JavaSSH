package com.mestrap.core;

import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ShowHelp {

    public static void printVersion() {
        LogPrinter.section("JavaSSH v1.0.0");
        LogPrinter.info("Lightweight SSH operations tool based on JSch");
        String t = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LogPrinter.keyValue("Build time", t);
    }

    public static void printGlobal(ActionRegistry actionRegistry) {

        System.out.println("\nJavaSSH - Lightweight SSH operations tool");
        System.out.println("Version: 1.0.0\n");

        System.out.println("Usage:");
        System.out.println("  jssh <task> [hosts...] [options]\n");

        System.out.println("Available actions for task definitions:\n");

        HelpFormatter hf = new HelpFormatter();
        hf.setOptionComparator(null);
        hf.setSyntaxPrefix("Action: ");
        hf.setLeftPadding(2);
        hf.setLongOptPrefix(" --");

        for (TaskAction action : actionRegistry.all()) {
            Options opts = new Options();
            for (Option o : action.cliOptions()) {
                opts.addOption(o);
            }

            if (opts.getOptions().isEmpty()) {
                System.out.println("      (no parameters)\n");
            } else {
                hf.printHelp(action.name(), opts);
            }
            System.out.println();
        }

        System.out.println("Global options:");
        System.out.println("  -i, --inventory <file>   Inventory file (default: inventory.yaml)");
        System.out.println("  -l, --list               List target hosts only");
        System.out.println("  -P, --port     <int>     Override port");
        System.out.println("  -u, --user     <string>  Override username");
        System.out.println("  -p, --password <string>  Override password (not recommended)");
        System.out.println("  -y, --yes                Skip confirmation");
        System.out.println("  -h, --help               Show help");
        System.out.println("  -v, --version            Show version");
        System.out.println();
    }
}
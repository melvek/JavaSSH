package com.mestrap.cli;

import org.apache.commons.cli.Option;

import java.util.ArrayList;
import java.util.List;

public class GlobalOptions {

    public static List<Option> all() {
        List<Option> list = new ArrayList<>();
        list.add(Option.builder("i").longOpt("inventory").hasArg().argName("file")
                .desc("Inventory file (default: inventory.yaml)").build());
        list.add(Option.builder("l").longOpt("list")
                .desc("List target hosts only").build());
        list.add(Option.builder("P").longOpt("port").hasArg().argName("int")
                .desc("Override port").build());
        list.add(Option.builder("u").longOpt("user").hasArg().argName("string")
                .desc("Override username").build());
        list.add(Option.builder("p").longOpt("password").hasArg().argName("string")
                .desc("Override password (not recommended)").build());
        list.add(Option.builder("y").longOpt("yes")
                .desc("Skip confirmation").build());
        list.add(Option.builder("v").longOpt("version")
                .desc("Show version").build());
        list.add(Option.builder("h").longOpt("help")
                .desc("Show help").build());
        return list;
    }
}
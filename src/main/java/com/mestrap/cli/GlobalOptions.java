package com.mestrap.cli;

import org.apache.commons.cli.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局 CLI 选项定义。
 *
 * <p>选项名以常量形式暴露，供 dispatcher 和帮助信息统一引用，
 * 避免字面量散落各处。
 *
 * @author melvek
 */
public final class GlobalOptions {

    /** 清单文件 */
    public static final String INVENTORY = "inventory";
    /** 列出主机 */
    public static final String LIST = "list";
    /** 端口 */
    public static final String PORT = "port";
    /** 密码 */
    public static final String PASSWORD = "password";
    /** 用户名 */
    public static final String USERNAME = "user";
    /** 跳过确认 */
    public static final String YES = "yes";
    /** 版本 */
    public static final String VERSION = "version";
    /** 帮助 */
    public static final String HELP = "help";

    private GlobalOptions() {}

    /**
     * 构建全部全局选项。
     *
     * @return 选项列表
     */
    public static List<Option> all() {
        List<Option> list = new ArrayList<>();

        list.add(Option.builder("i").longOpt(INVENTORY).hasArg().argName("file")
                .desc("Inventory file (default: inventory.yaml)").build());
        list.add(Option.builder("l").longOpt(LIST)
                .desc("List target hosts only").build());
        list.add(Option.builder("P").longOpt(PORT).hasArg().argName("int")
                .desc("Override port").build());
        list.add(Option.builder("u").longOpt(USERNAME).hasArg().argName("string")
                .desc("Override username").build());
        list.add(Option.builder("p").longOpt(PASSWORD).hasArg().argName("string")
                .desc("Override password (not recommended)").build());
        list.add(Option.builder("y").longOpt(YES)
                .desc("Skip confirmation").build());
        list.add(Option.builder("v").longOpt(VERSION)
                .desc("Show version").build());
        list.add(Option.builder("h").longOpt(HELP)
                .desc("Show help").build());

        return list;
    }
}
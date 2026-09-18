package com.mestrap.core;

import com.mestrap.cli.GlobalOptions;
import com.mestrap.entity.HostVars;
import com.mestrap.entity.Inventory;
import com.mestrap.entity.ServerGroup;
import com.mestrap.utils.LogPrinter;
import org.apache.commons.cli.CommandLine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HostResolver {

    public static Map<String, HostVars> resolve(List<String> hostNames, Inventory inventory, CommandLine cl) {

        Map<String, HostVars> target = new LinkedHashMap<>();
        HostVars globalVars = inventory.getGlobalVars();

        if (hostNames == null || hostNames.isEmpty()) {
            return target;
        }

        for (String hostName : hostNames) {

            boolean resolved = false;

            if (inventory.getServers() != null
                    && inventory.getServers().containsKey(hostName)) {

                ServerGroup group = inventory.getServers().get(hostName);
                HostVars groupVars = group.getVars();
                if (groupVars != null) {
                    groupVars.merge(globalVars);
                }

                group.getHosts().forEach((name, vars) -> {
                    if (groupVars != null) {
                        vars.merge(groupVars);
                    }
                    target.put(name, vars);
                });
                resolved = true;
                LogPrinter.info("Server group: " + hostName
                        + " (" + group.getHosts().size() + ")");
            }

            if (!resolved) {
                HostVars vars = new HostVars();
                vars.setHost(hostName);
                if (globalVars != null) {
                    vars.setPort(globalVars.getPort());
                    vars.setUserName(globalVars.getUserName());
                    vars.setPassword(globalVars.getPassword());
                }
                target.put(hostName, vars);
                LogPrinter.info("Host: " + hostName);
            }
        }

        // CLI 覆盖
        if (cl.hasOption(GlobalOptions.PORT)) {
            int p = Integer.parseInt(cl.getOptionValue("P"));
            target.values().forEach(v -> v.setPort(p));
        }
        if (cl.hasOption(GlobalOptions.USERNAME)) {
            String u = cl.getOptionValue("u");
            target.values().forEach(v -> v.setUserName(u));
        }
        if (cl.hasOption(GlobalOptions.PASSWORD)) {
            String p = cl.getOptionValue("p");
            target.values().forEach(v -> v.setPassword(p));
            LogPrinter.warning("Password passed via CLI is a security risk");
        }

        return target;
    }
}
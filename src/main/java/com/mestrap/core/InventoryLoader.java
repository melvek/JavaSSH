package com.mestrap.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mestrap.entity.Inventory;
import com.mestrap.utils.LogPrinter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InventoryLoader {

    public static Inventory load(String path) {
        File file = new File(path);
        if (!file.exists()) {
            LogPrinter.error("Inventory not found: " + file.getAbsolutePath());
            throw new RuntimeException("Inventory file not found: " + file.getAbsolutePath());
        }

        try (InputStream in = new FileInputStream(file)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Inventory inv = mapper.readValue(in, Inventory.class);

            int groups = inv.getServers() != null ? inv.getServers().size() : 0;
            int chains = inv.getChains() != null ? inv.getChains().size() : 0;
            LogPrinter.success("Loaded inventory: " + groups + " groups, "
                    + chains + " chains");

            if (inv.getGlobalVars() == null) {
                inv.setGlobalVars(new com.mestrap.entity.HostVars());
            }

            // 注入日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            inv.getGlobalVars().setExtraField("date", sdf.format(new Date()));

            return inv;
        } catch (Exception e) {
            LogPrinter.error("Failed to parse inventory: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
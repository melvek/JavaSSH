package com.mestrap.core;

import com.mestrap.entity.Chain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChainRegistry {

    private final Map<String, Chain> merged = new LinkedHashMap<>();

    public ChainRegistry() {
       merged.putAll(BuiltinChains.all());
    }

    /** 加载用户链，覆盖同名内置链 */
    public void loadUserChains(Map<String, Chain> userChains) {
        if (userChains == null || userChains.isEmpty()) return;
        userChains.forEach((name, chain) -> {
            chain.setName(name);
            merged.put(name, chain);
        });
    }

    public Chain resolve(String name) {
        return merged.get(name);
    }

}
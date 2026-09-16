package com.mestrap.core;

import org.apache.commons.cli.Option;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TaskAction {

    String name();

    default List<Option> cliOptions() {
        return Collections.emptyList();
    }

    default Map<String, String> cliVarMapping() {
        return Collections.emptyMap();
    }

    void execute(ActionContext ctx) throws Exception;
}
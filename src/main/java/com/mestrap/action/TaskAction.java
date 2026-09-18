package com.mestrap.action;

import com.mestrap.core.ActionContext;
import org.apache.commons.cli.Option;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author melvek
 */
public interface TaskAction {

    /**
     * Name of the action
     * @return
     */
    String name();

    /**
     * Add options of this action
     * @return options
     */
    default List<Option> cliOptions() {
        return Collections.emptyList();
    }

    /**
     * Mapped key if options that used in task commands
     * @return
     */
    default Map<String, String> cliVarMapping() {
        return Collections.emptyMap();
    }

    /**
     * Execute action with action context
     * @param ctx
     * @throws Exception
     */
    void execute(ActionContext ctx) throws Exception;
}
package com.mestrap.action;

import com.mestrap.core.ActionContext;
import org.apache.commons.cli.Option;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 任务中的原子动作。
 *
 * <p>实现类需声明动作名、可选参数、以及动作的执行逻辑。
 *
 * @author melvek
 */
public interface TaskAction {

    /**
     * 动作名，用于 task 的 step.action 字段。
     *
     * @return 动作名，如 "command"、"push"
     */
    String name();

    /**
     * 该动作声明的 CLI 选项。
     *
     * @return 选项列表，无选项时返回空列表
     */
    default List<Option> cliOptions() {
        return Collections.emptyList();
    }

    /**
     * CLI 选项名到变量名的映射。
     *
     * <p>key 必须是长选项名（{@link Option#getLongOpt()}），
     * value 是注入到动作参数中的键名。
     *
     * <p>例如 command 动作：{@code {"execute" -> "command"}}，
     * 表示 CLI 的 {@code --execute} 值会作为参数 {@code command} 传给动作。
     *
     * @return 映射表，无映射时返回空表
     */
    default Map<String, String> cliVarMapping() {
        return Collections.emptyMap();
    }

    /**
     * 执行动作。
     *
     * @param ctx 动作上下文，包含主机信息、步骤参数、变量池
     * @throws Exception 执行异常
     */
    void execute(ActionContext ctx) throws Exception;
}
package com.mestrap.core;

import com.mestrap.entity.HostVars;

import java.util.Collections;
import java.util.Map;

/**
 * 动作上下文。
 *
 * <p>持有执行一个步骤所需的三类信息：
 * <ul>
 *   <li>{@code hostVars}：目标主机信息</li>
 *   <li>{@code with}：步骤参数（step.with），只读</li>
 *   <li>{@code vars}：变量池，供 ${...} 解析，只读</li>
 * </ul>
 *
 * @author melvek
 */
public class ActionContext {

    private final HostVars hostVars;

    /** 步骤参数，参数容器 */
    private final Map<String, Object> with;

    /** 变量池，供 ${...} 解析 */
    private final Map<String, Object> vars;

    /**
     * 构造动作上下文。
     *
     * @param hostVars 目标主机信息
     * @param with     步骤参数，可为 null
     * @param vars     变量池，可为 null
     */
    public ActionContext(HostVars hostVars,
                         Map<String, Object> with,
                         Map<String, Object> vars) {
        this.hostVars = hostVars;
        this.with = with == null ? Collections.<String, Object>emptyMap() : with;
        this.vars = vars == null ? Collections.<String, Object>emptyMap() : vars;
    }

    public HostVars getHostVars() {
        return hostVars;
    }

    public Map<String, Object> getWith() {
        return with;
    }

    public Map<String, Object> getVars() {
        return vars;
    }
}
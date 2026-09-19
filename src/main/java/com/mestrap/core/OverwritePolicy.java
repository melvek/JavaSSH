package com.mestrap.core;

/**
 * 远程文件覆盖策略。
 *
 * <p>由 push action 的 force / backup 参数组合决定：
 * <ul>
 *   <li>{@code !force} → FAIL</li>
 *   <li>{@code force && !backup} → OVERWRITE</li>
 *   <li>{@code force && backup} → BACKUP</li>
 * </ul>
 *
 * @author melvek
 */
public enum OverwritePolicy {

    /** 目标已存在则报错（默认） */
    FAIL,

    /** 直接覆盖，不备份 */
    OVERWRITE,

    /** 备份原文件（追加时间戳）后覆盖 */
    BACKUP;

    /**
     * 根据 force / backup 推导覆盖策略。
     *
     * @param force  是否允许覆盖
     * @param backup 覆盖前是否备份
     * @return 覆盖策略
     */
    public static OverwritePolicy of(boolean force, boolean backup) {
        if (!force) {
            return FAIL;
        }
        return backup ? BACKUP : OVERWRITE;
    }
}
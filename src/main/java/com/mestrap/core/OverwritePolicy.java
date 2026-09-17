package com.mestrap.core;

/**
 * @author melvek
 * @date 2026/9/17 17:43
 * @description 文件处理方式
 */
public enum OverwritePolicy {

    /** 目标已存在则报错（默认） */
    FAIL,

    /** 直接覆盖，不备份 */
    OVERWRITE,

    /** 备份原文件（追加时间戳）后覆盖 */
    BACKUP;

    public static OverwritePolicy of(boolean force, boolean backup) {
        if (!force) {
            return FAIL;
        }
        return backup ? BACKUP : OVERWRITE;
    }
}

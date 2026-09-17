package com.mestrap.exception;

/**
 * @author melvek
 * @date 2026/9/17 17:26
 * @description JsshException 统一异常处理
 */
public class JsshException extends RuntimeException {

    /** 失败主机，可为空 */
    private final String host;

    /** 失败步骤，可为空 */
    private final String stepName;

    /**
     * 仅带消息的构造方法。
     *
     * @param message 错误消息
     */
    public JsshException(String message) {
        super(message);
        this.host = null;
        this.stepName = null;
    }

    /**
     * 带消息和原因的构造方法。
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public JsshException(String message, Throwable cause) {
        super(message, cause);
        this.host = null;
        this.stepName = null;
    }

    /**
     * 带主机、步骤、消息和原因的构造方法。
     *
     * @param host     失败主机
     * @param stepName 失败步骤
     * @param message  错误消息
     * @param cause    原始异常
     */
    public JsshException(String host, String stepName, String message, Throwable cause) {
        super(message, cause);
        this.host = host;
        this.stepName = stepName;
    }

    public String getHost() {
        return host;
    }

    public String getStepName() {
        return stepName;
    }
}
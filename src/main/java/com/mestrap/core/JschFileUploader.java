package com.mestrap.core;

import com.jcraft.jsch.*;
import com.mestrap.exception.JsshException;
import com.mestrap.utils.Constant;
import com.mestrap.utils.EncryptTool;
import com.mestrap.utils.LogPrinter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author melvek
 */
public class JschFileUploader {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    /**
     * 上传文件
     *
     * @param policy 覆盖策略：FAIL / OVERWRITE / BACKUP
     * @return 0 表示成功
     * @throws JsshException 上传失败或目标已存在且策略为 FAIL
     */
    public static int uploadFile(String host, int port, String username,
                                 String password, String localFile,
                                 String remoteTarget, OverwritePolicy policy)
            throws JsshException {

        Session session = null;
        ChannelSftp sftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(EncryptTool.decrypt(password));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            // 1. 解析最终目标路径
            String finalPath = resolveTargetPath(sftp, localFile, remoteTarget);
            String parentDir = getParentDirectory(finalPath);

            // 2. 父目录必须存在
            if (!directoryExists(sftp, parentDir)) {
                throw new JsshException("Parent directory does not exist: " + parentDir);
            }

            // 3. 目标不能是目录
            if (directoryExists(sftp, finalPath)) {
                throw new JsshException(
                        "Target exists and is a directory: " + finalPath);
            }

            // 4. 根据策略处理已存在的文件
            if (exists(sftp, finalPath)) {
                switch (policy) {
                    case FAIL:
                        throw new JsshException(
                                "Remote file already exists: " + finalPath
                                        + " (use -F/--force to overwrite, add -B/--backup to keep a backup)");
                    case OVERWRITE:
                        LogPrinter.info("Overwriting existing file: " + finalPath);
                        break;
                    case BACKUP:
                        String backup = backupExisting(sftp, finalPath);
                        LogPrinter.info("Backed up existing file to: " + backup);
                        break;
                    default:
                }
            }

            // 5. 上传
            try (FileInputStream fis = new FileInputStream(localFile)) {
                sftp.put(fis, finalPath, ChannelSftp.OVERWRITE);
            }

            LogPrinter.success("Uploaded: " + localFile + " -> " + finalPath);
            return 0;

        } catch (JSchException | SftpException | IOException e) {
            throw new JsshException("Upload failed: " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 备份已存在的文件：重命名为 base.<timestamp>.ext
     *
     * @return 备份后的完整路径
     */
    private static String backupExisting(ChannelSftp sftp, String targetPath)
            throws SftpException {

        String fileName = new File(targetPath).getName();
        String parentDir = getParentDirectory(targetPath);

        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext  = dot > 0 ? fileName.substring(dot) : "";

        String timestamp = DATE_FORMAT.get().format(new Date());
        String backupName = base + "." + timestamp + ext;
        String backupPath = parentDir + "/" + backupName;

        sftp.rename(targetPath, backupPath);
        return backupPath;
    }

    /**
     * 解析目标路径，模拟 cp 行为
     */
    private static String resolveTargetPath(ChannelSftp sftp, String localFile,
                                            String remoteTarget) throws SftpException {
        String fileName = new File(localFile).getName();

        // 以 / 结尾：目标是目录
        if (remoteTarget.endsWith(Constant.SEPARATOR)) {
            return remoteTarget + fileName;
        }

        // 目标已存在且是目录：放到目录里
        if (exists(sftp, remoteTarget) && isDirectory(sftp, remoteTarget)) {
            return remoteTarget + Constant.SEPARATOR + fileName;
        }

        // 其他：视为文件路径
        return remoteTarget;
    }

    private static boolean exists(ChannelSftp sftp, String path) {
        try { sftp.stat(path); return true; }
        catch (SftpException e) { return false; }
    }

    private static boolean isDirectory(ChannelSftp sftp, String path) {
        try { return sftp.stat(path).isDir(); }
        catch (SftpException e) { return false; }
    }

    private static boolean directoryExists(ChannelSftp sftp, String path) {
        return isDirectory(sftp, path);
    }

    private static String getParentDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash <= 0 ? "/" : normalized.substring(0, lastSlash);
    }
}
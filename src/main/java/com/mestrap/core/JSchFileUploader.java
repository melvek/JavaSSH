package com.mestrap.core;

import com.jcraft.jsch.*;
import com.mestrap.utils.EncryptTool;
import com.mestrap.utils.LogPrinter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JSchFileUploader {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    /**
     * Upload a file, simulating the behavior of the cp command
     *
     * @param host         Server address
     * @param port         Port
     * @param username     Username
     * @param password     Password
     * @param localFile    Local file path
     * @param remoteTarget Remote target path (simulating the target argument of cp)
     * @throws JSchException SSH exception
     * @throws SftpException SFTP exception
     */
    public static void uploadFile(String host, int port, String username,
                                  String password, String localFile,
                                  String remoteTarget) throws JSchException, SftpException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            session = jsch.getSession(username, host, port);

            // Decrypt password
            String pwd = EncryptTool.decrypt(password);
            session.setPassword(pwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String finalRemotePath = resolveTargetPath(channelSftp, localFile, remoteTarget);
            String parentDir = getParentDirectory(finalRemotePath);

            if (!directoryExists(channelSftp, parentDir)) {
                throw new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE,
                        "Parent directory does not exist: " + parentDir + " (please create the directory first)");
            }

            if (directoryExists(channelSftp, finalRemotePath)) {
                throw new SftpException(ChannelSftp.SSH_FX_FAILURE,
                        "Target already exists and is a directory; cannot upload as a file: " + finalRemotePath);
            }

            backupExistingRemoteFile(channelSftp, finalRemotePath);

            try (FileInputStream fis = new FileInputStream(localFile)) {
                channelSftp.put(fis, finalRemotePath, ChannelSftp.OVERWRITE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LogPrinter.success("Upload successful: " + localFile + " -> " + finalRemotePath);

        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * If the remote target file already exists, rename it to a timestamped backup file.
     * The new upload will then use the original target path.
     *
     * @param sftp       SFTP channel
     * @param targetPath Original target file path
     * @throws SftpException SFTP exception
     */
    private static void backupExistingRemoteFile(ChannelSftp sftp, String targetPath) throws SftpException {
        // If the file does not exist, nothing to back up
        if (!exists(sftp, targetPath)) {
            return;
        }

        // If it is a directory, do not rename (caller already checked, defensive here)
        if (isDirectory(sftp, targetPath)) {
            return;
        }

        String fileName = new File(targetPath).getName();
        String parentDir = getParentDirectory(targetPath);

        // Separate file name and extension
        String baseName;
        String extension;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            baseName = fileName;
            extension = "";
        }

        // Generate a new backup file name with a timestamp
        String timestamp = DATE_FORMAT.get().format(new Date());
        String backupFileName = baseName + "." + timestamp + extension;
        String backupPath = parentDir + "/" + backupFileName;

        // Rename the existing remote file to the backup name
        sftp.rename(targetPath, backupPath);
        System.out.println("Remote file already exists, renamed to backup: " + backupFileName);
    }

    /**
     * Resolve the target path, simulating the behavior of the cp command
     *
     * @param sftp         SFTP channel
     * @param localFile    Local file path
     * @param remoteTarget User-specified target
     * @return The final target file path
     */
    private static String resolveTargetPath(ChannelSftp sftp, String localFile,
                                            String remoteTarget) throws SftpException {
        // Get the local file name
        String fileName = new File(localFile).getName();

        // Case 1: ends with /, target is a directory
        if (remoteTarget.endsWith("/")) {
            return remoteTarget + fileName;
        }

        // Case 2: target already exists
        if (exists(sftp, remoteTarget)) {
            if (isDirectory(sftp, remoteTarget)) {
                // If the target is a directory, copy into the directory
                String normalizedPath = remoteTarget.endsWith("/") ? remoteTarget : remoteTarget + "/";
                return normalizedPath + fileName;
            } else {
                // If the target is a file, upload to this path (existing file will be backed up later)
                return remoteTarget;
            }
        }

        // Case 3: target does not exist → treat as a file path
        return remoteTarget;
    }

    /**
     * Check whether a path exists (file or directory)
     */
    private static boolean exists(ChannelSftp sftp, String path) {
        try {
            sftp.stat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    /**
     * Check whether it is a directory
     */
    private static boolean isDirectory(ChannelSftp sftp, String path) throws SftpException {
        try {
            SftpATTRS attrs = sftp.stat(path);
            return attrs.isDir();
        } catch (SftpException e) {
            return false;
        }
    }

    /**
     * Check whether a directory exists
     */
    private static boolean directoryExists(ChannelSftp sftp, String path) {
        try {
            SftpATTRS attrs = sftp.stat(path);
            return attrs.isDir();
        } catch (SftpException e) {
            return false;
        }
    }

    /**
     * Get the parent directory
     */
    private static String getParentDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        String normalized = path.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');

        if (lastSlash <= 0) {
            return "/";
        }

        return normalized.substring(0, lastSlash);
    }
}
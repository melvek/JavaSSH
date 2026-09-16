package com.mestrap.core;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mestrap.utils.EncryptTool;
import com.mestrap.utils.LogPrinter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JSchCommandExecutor {

    public static int executeCommand(String host, int port, String username, String password, String command) {
        StringBuilder output = new StringBuilder();
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();
            // 1. Create SSH session
            session = jsch.getSession(username, host, port);

            // Decrypt password
            String pwd = EncryptTool.decrypt(password);
            session.setPassword(pwd);

            // Configuration: skip host key check (for testing only; production should handle key verification)
            session.setConfig("StrictHostKeyChecking", "no");

            // 2. Connect session (set timeout to 10 seconds)
            session.connect(10000);

            // 3. Open channel for executing command
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // 4. Get input stream for command execution result
            // If error information is needed, use channel.getErrStream()
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));

            // 5. Connect channel and read output
            channel.connect();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            LogPrinter.info(output.toString());

            return channel.getExitStatus();
        } catch (JSchException | java.io.IOException e) {
            System.err.println("Execution error: " + e.getMessage());
            return 1;
        } finally {
            // 6. Close connection, release resources
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

}
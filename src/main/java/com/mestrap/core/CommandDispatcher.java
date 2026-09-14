package com.mestrap.core;

import com.mestrap.command.*;

public class CommandDispatcher {

    // This method is the testable core logic
    public int dispatch(String[] args) {
        // Handle empty arguments
        if (args == null || args.length == 0) {
            new ExecuteCommand().ShowHelp(false);
            return 1;
        }

        String commandString = args[0];

        // Remove the first command argument; the remaining arguments are the actual arguments to process
        String[] remaining = new String[args.length - 1];
        System.arraycopy(args, 1, remaining, 0, remaining.length);

        // Dispatch command
        JavaSSHCommand command;
        switch (commandString) {
            case "command":
                command = new ExecuteCommand();
                break;
            case "push":
                command = new PushCommand();
                break;
            case "deploy":
                command = new DeployCommand();
                break;
            default:
                command = new ExecuteCommand();
                command.ShowHelp(false);
                return 1;
        }

        command.execute(remaining);
        return 0; // Assume execution succeeded
    }
}
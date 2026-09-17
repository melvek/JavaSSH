package com.mestrap;

import com.mestrap.core.CommandDispatcher;

/**
 * JavaSSH is a lightweight operations tool developed based on JSch, used for batch uploading files to remote servers and executing remote commands.
 * Built-in parameters of fina_uat.yaml:
 * ${service_path}: Service deployment path, used for the -d option parameter of the push command
 * ${command}: Command content, used for the -e option parameter of the command
 * Parameters can be used with ${command}
 * @author melvek
 */
public class JsshApplication {
    public static void main( String[] args ) {
        CommandDispatcher dispatcher = new CommandDispatcher();
        int exitCode = dispatcher.dispatch(args);
        System.exit(exitCode);
    }
}
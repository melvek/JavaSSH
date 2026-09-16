package com.mestrap.action;

import com.mestrap.core.ActionContext;
import com.mestrap.core.JSchFileUploader;
import com.mestrap.core.TaskAction;
import com.mestrap.utils.LogPrinter;
import com.mestrap.utils.VariableReplacer;
import org.apache.commons.cli.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PushAction implements TaskAction {

    @Override
    public String name() { return "push"; }

    @Override
    public List<Option> cliOptions() {
        List<Option> list = new ArrayList<>();
        list.add(Option.builder("f").longOpt("files").hasArg().argName("file")
                .desc("Local file (injected as ${file})").build());
        list.add(Option.builder("d").longOpt("dest-path").hasArg().argName("path")
                .desc("Remote destination (injected as ${dest})").build());
        list.add(Option.builder("F").longOpt("force")
                .desc("Allow overwriting existing remote files").build());
        list.add(Option.builder("z").longOpt("zip")
                .desc("Compress before upload (requires remote unzip)").build());
        return list;
    }

    @Override
    public void execute(ActionContext ctx) throws Exception {
        Object fileRaw = ctx.getWith().get("file");
        Object destRaw = ctx.getWith().get("dest");

        if (fileRaw == null) throw new IllegalArgumentException("push action requires 'file' parameter");
        if (destRaw == null) throw new IllegalArgumentException("push action requires 'dest' parameter");

        String file = VariableReplacer.replace(String.valueOf(fileRaw), ctx.getVars());
        String dest = VariableReplacer.replace(String.valueOf(destRaw), ctx.getVars());

        File sourceFile = new File(file);
        if (!sourceFile.exists()) {
            throw new RuntimeException("Source file dose not exists! Command failed with exit code -1");
        }

        LogPrinter.info("Push file " + sourceFile.getAbsolutePath() + " to " + dest);

        int exitCode = JSchFileUploader.uploadFile(
                ctx.getHostVars().getHost(),
                ctx.getHostVars().getPort(),
                ctx.getHostVars().getUserName(),
                ctx.getHostVars().getPassword(),
                sourceFile.getAbsolutePath(), dest
        );

        ctx.getOutputs().put("file", sourceFile.getAbsolutePath());
        ctx.getOutputs().put("dest", dest);

        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }
}
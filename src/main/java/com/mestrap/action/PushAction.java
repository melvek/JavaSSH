package com.mestrap.action;

import com.mestrap.core.ActionContext;
import com.mestrap.core.JschFileUploader;
import com.mestrap.core.OverwritePolicy;
import com.mestrap.utils.LogPrinter;
import com.mestrap.utils.VariableReplacer;
import org.apache.commons.cli.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送本地文件至远程服务器
 * -f, --file 指定要上传的本地文件或者文件夹
 *            若目标是文件夹，则使用指定的压缩方式（zip 或者 tar.gz), 若未指定，则默认使用 zip 格式
 * -d, --dest 远程文件路径
 *            若路径以 '/' 结尾，则认为其为目标文件夹，否则，判断目标是否存在，若存在且为文件夹，则上传至该文件夹下
 *            若为文件，则上传目标为指定的文件名
 * -F, --force 若远程文件已存在，依据该值决定是否覆盖
 * -B, --backup 若远程文件已存在，依据该值决定是否对原文件进行备份
 * -z, --zip 若上传目标是压缩文件，指定该参数后，将在上传完成后自动解压
 *
 * @author melvek
 */
public class PushAction implements TaskAction {

    @Override
    public String name() { return "push"; }

    @Override
    public List<Option> cliOptions() {
        List<Option> list = new ArrayList<>();
        list.add(Option.builder("f").longOpt("file").hasArg().argName("file")
                .desc("Local file (injected as ${file})").build());
        list.add(Option.builder("d").longOpt("dest").hasArg().argName("path")
                .desc("Remote destination (injected as ${dest})").build());
        list.add(Option.builder("F").longOpt("force")
                .desc("Allow overwriting existing remote files").build());
        list.add(Option.builder("B").longOpt("backup")
                .desc("Back up existing remote file before overwrite (injected as ${backup})").build());
        list.add(Option.builder("z").longOpt("zip")
                .desc("Compress before upload (requires remote unzip)").build());
        return list;
    }

    @Override
    public Map<String, String> cliVarMapping() {
        Map<String, String> cliVar = new HashMap<>(8);
        cliVar.put("file", "file");
        cliVar.put("dest", "dest");
        cliVar.put("force", "force");
        cliVar.put("backup", "backup");
        cliVar.put("zip", "zip");
        return cliVar;
    }

    @Override
    public void execute(ActionContext ctx) throws Exception {

        boolean force  = isTruthy(ctx.getWith().get("force"));
        boolean backup = isTruthy(ctx.getWith().get("backup"));

        Object fileRaw = ctx.getWith().get("file");
        Object destRaw = ctx.getWith().get("dest");

        if (fileRaw == null) {
            throw new IllegalArgumentException("push action requires 'file' parameter");
        }
        if (destRaw == null) {
            throw new IllegalArgumentException("push action requires 'dest' parameter");
        }

        String file = VariableReplacer.replace(String.valueOf(fileRaw), ctx.getVars());
        String dest = VariableReplacer.replace(String.valueOf(destRaw), ctx.getVars());

        OverwritePolicy policy = OverwritePolicy.of(force, backup);

        File sourceFile = new File(file);
        if (!sourceFile.exists()) {
            throw new RuntimeException("Source file dose not exists! Command failed with exit code -1");
        }

        LogPrinter.info("Push file " + sourceFile.getAbsolutePath() + " to " + dest);

        int exitCode = JschFileUploader.uploadFile(
                ctx.getHostVars().getHost(),
                ctx.getHostVars().getPort(),
                ctx.getHostVars().getUserName(),
                ctx.getHostVars().getPassword(),
                sourceFile.getAbsolutePath(), dest, policy
        );

        ctx.getOutputs().put("file", sourceFile.getAbsolutePath());
        ctx.getOutputs().put("dest", dest);

        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }

    private boolean isTruthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }

        String s = String.valueOf(v).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }
}
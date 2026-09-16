package com.mestrap;

import com.mestrap.core.CommandDispatcher;
import com.mestrap.utils.LogPrinter;
import junit.framework.TestCase;

public class AppTest extends TestCase
{
    private String[] parseArgs(String argStr) {
        return argStr.split(" ");
    }

    public void testDeployCommand() {
        LogPrinter.setColorEnabled(true);

        CommandDispatcher dispatcher = new CommandDispatcher();
        // 模拟用户输入 deploy 命令
        String[] args = parseArgs("deploy -i inventory.yaml web_master -e \"${service_path}deploy.sh\" -f app.jar -y");
        // String[] args = parseArgs("");

        int result = dispatcher.dispatch(args);

        assertEquals(0, result); // 验证返回码
        // 更深入的验证：可以通过Mock DeployCommand来验证execute是否被调用（见下方进阶）
    }

    public void testEmptyArgsShowsHelp() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        int result = dispatcher.dispatch(new String[]{});
        assertEquals(0, result); // 返回非0表示出错
    }

    public void testUnknownCommandShowsHelp() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        String[] args = {"command", "-h"};

        int result = dispatcher.dispatch(args);
        assertEquals(0, result);
    }

}

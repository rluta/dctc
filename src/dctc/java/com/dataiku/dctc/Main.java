package com.dataiku.dctc;

import static com.dataiku.dip.utils.PrettyString.nlcat;
import static com.dataiku.dip.utils.PrettyString.scat;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.dataiku.dctc.DCTCLog.Mode;
import com.dataiku.dctc.command.AddAccount;
import com.dataiku.dctc.command.Cat;
import com.dataiku.dctc.command.Command;
import com.dataiku.dctc.command.Cp;
import com.dataiku.dctc.command.Dispatch;
import com.dataiku.dctc.command.Edit;
import com.dataiku.dctc.command.Find;
import com.dataiku.dctc.command.Grep;
import com.dataiku.dctc.command.Head;
import com.dataiku.dctc.command.ListColumns;
import com.dataiku.dctc.command.Ls;
import com.dataiku.dctc.command.Mkdir;
import com.dataiku.dctc.command.Mv;
import com.dataiku.dctc.command.Rm;
import com.dataiku.dctc.command.Rmdir;
import com.dataiku.dctc.command.Sync;
import com.dataiku.dctc.command.Tail;
import com.dataiku.dctc.command.Version;
import com.dataiku.dctc.configuration.GlobalConf;
import com.dataiku.dctc.configuration.StructuredConf;
import com.dataiku.dctc.exception.UserException;
import com.dataiku.dip.utils.IndentedWriter;
import com.dataiku.dip.utils.StdOut;

public class Main {
    private static void indent(String str, int size) {
        for (int i = 0; i < size; ++i) {
            System.out.print(str);
        }
    }
    private static void globalUsage(int exitCode) {
        if (exitCode != 0) {
            System.setOut(System.err);
        }
        System.out.println("usage: dctc command [OPTIONS...] [ARGUMENTS...]");
        System.out.println();
        System.out.println("Available commands are:");
        int len = 0;
        for (Command cmd: cmds.values()) {
            len = Math.max(len, cmd.cmdname().length());
        }
        len += 2;
        for (Command cmd: cmds.values()) {
            System.out.print(scat("  -", cmd.cmdname()));
            indent(" ", len - cmd.cmdname().length());
            System.out.println(cmd.tagline());
        }
        System.out.println(nlcat("For more informations see the project homepage:",
                              "http://dctc.io"));
        System.exit(exitCode);
    }
    private static void commandHelp(int exitCode, String command) {
        if (exitCode != 0) {
            System.setOut(System.err);
        }
        IndentedWriter printer = new IndentedWriter();
        printer.setFirstLineIndentsize(2);
        printer.setIndentSize(2);
        printer.setTermSize(Math.min(GlobalConf.getColNumber(), 80));
        for (Command cmd: cmds.values()) {
            if (cmd.cmdname().equals(command)) {
                commandHelp(cmd, printer);
                System.exit(exitCode);
            }
        }
        System.out.println("Command not found: " + command);
        globalUsage(1);
    }
    public static void commandHelp(Command cmd, IndentedWriter printer) {
        System.out.println("dctc " + cmd.cmdname() + " -- " + cmd.tagline());
        System.out.println();
        cmd.longDescription(printer);
        System.out.println();
        cmd.usage();
    }

    static Logger logger = Logger.getLogger(Main.class);
    public static void setLogger() {
        Logger.getRootLogger().removeAllAppenders();
        ConsoleAppender ca = new ConsoleAppender(new PatternLayout("[%r] [%t] [%-5p] [%c] %x - %m%n"));
        ca.setName("console");
        ca.setTarget(ConsoleAppender.SYSTEM_ERR);
        ca.activateOptions();
        Logger.getRootLogger().addAppender(ca);
        Logger.getRootLogger().setLevel(Level.INFO);
    }
    private static void addCmd(Command cmd) {
        cmds.put(cmd.cmdname(), cmd);
    }
    private static void fillCommand() {
        addCmd(new AddAccount());
        addCmd(new Cat());
        addCmd(new Cp());
        addCmd(new Dispatch());
        addCmd(new Edit());
        addCmd(new Find());
        addCmd(new Grep());
        addCmd(new Head());
        addCmd(new ListColumns());
        addCmd(new Ls());
        addCmd(new Mkdir());
        addCmd(new Mv());
        addCmd(new Rmdir());
        addCmd(new Rm());
        addCmd(new Sync());
        addCmd(new Tail());
        addCmd(new Version());
    }
    public static void atExit() {
        Runtime.getRuntime().addShutdownHook
            (
             new Thread()
             {
                 public void run() {
                     System.out.flush();
                     System.err.flush();
                 }
             } );
    }
    public static void atBegin() {
       System.setOut(new StdOut(System.out));
       System.setErr(new StdOut(System.err));
    }

    public static void main(String[] args) {
        atExit();
        atBegin();
        try {
            setLogger();
            fillCommand();
            DCTCLog.setMode(Mode.STDERR);

            StructuredConf conf;
            try {
                conf = new StructuredConf();
                conf.parse(GlobalConf.confPath());
                conf.parseSsh(GlobalConf.sshConfigFile());
                if (conf.getFileBuilder().check()) {
                    System.err.println("One or more errors are present in the configuration file.");
                }
            } catch (IOException e) {
                System.err.println("dctc fail: " + e.getMessage());
                return;
            }

            if (args.length >= 1) {
                args = conf.getAlias().resolve(args);
                String usercmd = args[0];
                String[] cmdargs = new String[args.length - 1];
                System.arraycopy(args, 1, cmdargs, 0, args.length - 1);
                if (usercmd.equals("help") || usercmd.equals("-help")
                    || usercmd.equals("--help") || usercmd.equals("-h")
                    || usercmd.equals("-?")) {
                    if (cmdargs.length > 0) {
                        commandHelp(0, cmdargs[0]);
                    } else {
                        globalUsage(0);
                    }
                }
                Command cmd = cmds.get(usercmd);
                if (cmd != null) {
                    if (cmd.cmdname().equals("add-account")) {
                        assert cmd instanceof AddAccount;
                        ((AddAccount) cmd).setConfiguration(conf.getConfAppenders());
                    }
                    //cmd.setConfiguration(conf);
                    cmd.setFileBuilder(conf.getFileBuilder());
                    try {
                        cmd.perform(cmdargs);
                    } catch (Command.EndOfCommand e) {}
                    System.exit(cmd.getExitCode());
                }
                System.err.println("Unknown command: " + usercmd);
            }
            globalUsage(1);
        } catch (UserException e) {
            System.err.println("dctc: ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, Command> cmds = new TreeMap<String, Command>();
}

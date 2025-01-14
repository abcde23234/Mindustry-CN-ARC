package mindustry.net;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.mod.Mods.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class CrashSender{

    public static String createReport(String error){
        String report = "喜报！你的学术端崩溃了！\n";
        report += "在确定不是你自己的问题后在这里报告: " + Vars.reportIssueURL + "\n\n";
        return report
        + "版本: " + Version.combined() + (Vars.headless ? " (服务器)" : "") + "\n"
        + "系统: " + OS.osName + " x" + (OS.osArchBits) + " (" + OS.osArch + ")\n"
        + ((OS.isAndroid || OS.isIos) && app != null ? "Android API level: " + Core.app.getVersion() + "\n" : "")
        + "Java版本: " + OS.javaVersion + "\n"
        + "可用内存: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "mb\n"
        + "核心数量: " + Runtime.getRuntime().availableProcessors() + "\n"
        + (mods == null ? "<没有mod>" : "Mods: " + (!mods.list().contains(LoadedMod::shouldBeEnabled) ? "没有 (原版)" : mods.list().select(LoadedMod::shouldBeEnabled).toString(", ", mod -> mod.name + ":" + mod.meta.version)))
        + "\n\n" + error;
    }

    public static void log(Throwable exception){
        try{
            Core.settings.getDataDirectory().child("crashes").child("crash_" + System.currentTimeMillis() + ".txt")
            .writeString(createReport(Strings.neatError(exception)));
        }catch(Throwable ignored){
        }
    }

    public static void send(Throwable exception, Cons<File> writeListener){
        try{
            try{
                //log to file
                Log.err(exception);
            }catch(Throwable no){
                exception.printStackTrace();
            }

            //try saving game data
            try{
                settings.manualSave();
            }catch(Throwable ignored){}

            //don't create crash logs for custom builds, as it's expected
            if(OS.username.equals("anuke") && !"steam".equals(Version.modifier)){
                ret();
            }

            //attempt to load version regardless
            if(Version.number == 0){
                try{
                    ObjectMap<String, String> map = new ObjectMap<>();
                    PropertiesUtils.load(map, new InputStreamReader(CrashSender.class.getResourceAsStream("/version.properties")));

                    Version.type = map.get("type");
                    Version.number = Integer.parseInt(map.get("number"));
                    Version.modifier = map.get("modifier");
                    if(map.get("build").contains(".")){
                        String[] split = map.get("build").split("\\.");
                        Version.build = Integer.parseInt(split[0]);
                        Version.revision = Integer.parseInt(split[1]);
                    }else{
                        Version.build = Strings.canParseInt(map.get("build")) ? Integer.parseInt(map.get("build")) : -1;
                    }
                }catch(Throwable e){
                    e.printStackTrace();
                    Log.err("Failed to parse version.");
                }
            }

            try{
                File file = new File(OS.getAppDataDirectoryString(Vars.appName), "crashes/crash-report-" + new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss").format(new Date()) + ".txt");
                new Fi(OS.getAppDataDirectoryString(Vars.appName)).child("crashes").mkdirs();
                new Fi(file).writeString(createReport(writeException(exception)));
                writeListener.get(file);
            }catch(Throwable e){
                Log.err("Failed to save local crash report.", e);
            }

            //attempt to close connections, if applicable
            try{
                net.dispose();
            }catch(Throwable ignored){
            }

        }catch(Throwable death){
            death.printStackTrace();
        }

        ret();
    }

    private static void ret(){
        System.exit(1);
    }

    private static String writeException(Throwable e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}

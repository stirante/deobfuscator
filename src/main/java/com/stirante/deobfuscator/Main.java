package com.stirante.deobfuscator;

import jadx.api.IJadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.core.codegen.ClassGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.CodegenException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by stirante
 */
public class Main {
    static volatile HashMap<String, String> oldToNew = new HashMap<>();
    static volatile ArrayList<String> checkLater = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Provide file path!");
            return;
        }
        Field field = JavaClass.class.getDeclaredField("cls");
        field.setAccessible(true);
        Logger.getGlobal().setLevel(Level.SEVERE);
        File file = new File(args[0]);
//        File file = new File("C:\\Users\\Artia\\Downloads\\com.snapchat.android_9.39.5.0-933_minAPI16(armeabi-v7a)(nodpi)_apkmirror.com.apk");
        String name = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".jobf";
        final PrintStream out = new PrintStream(new FileOutputStream(new File(file.getParentFile(), name)));
        out.println("p =defaultPackage");
        final Pattern timberPattern = Pattern.compile("Timber\\..+\\(\"([A-Za-z_/]+)\".+");
//        final Pattern verbosePattern = Pattern.compile("aiI\\..+\\(\"([A-Za-z_/]+)\".+");
//        final Pattern messengerPattern = Pattern.compile("a\\..+\\(\"([A-Za-z_/]+)\".+");
        final Pattern logPattern = Pattern.compile("Log\\..+\\(\"([A-Za-z_/]+)\".+");
        final Pattern tagPattern = Pattern.compile("static final String TAG = \"([A-za-z0-9_/]+)\";");
        final Pattern[] patterns = new Pattern[]{tagPattern, timberPattern, logPattern};
        final Pattern cleaner = Pattern.compile("C[0-9]+");
        final IJadxArgs jArgs = new IJadxArgs() {
            @Override
            public File getOutDir() {
                return null;
            }

            @Override
            public int getThreadsCount() {
                return 0;
            }

            @Override
            public boolean isCFGOutput() {
                return false;
            }

            @Override
            public boolean isRawCFGOutput() {
                return false;
            }

            @Override
            public boolean isFallbackMode() {
                return false;
            }

            @Override
            public boolean isShowInconsistentCode() {
                return true;
            }

            @Override
            public boolean isVerbose() {
                return false;
            }

            @Override
            public boolean isSkipResources() {
                return false;
            }

            @Override
            public boolean isSkipSources() {
                return false;
            }

            @Override
            public boolean isDeobfuscationOn() {
                return false;
            }

            @Override
            public int getDeobfuscationMinLength() {
                return 0;
            }

            @Override
            public int getDeobfuscationMaxLength() {
                return 0;
            }

            @Override
            public boolean isDeobfuscationForceSave() {
                return false;
            }

            @Override
            public boolean useSourceNameAsClassAlias() {
                return true;
            }

            @Override
            public boolean escapeUnicode() {
                return false;
            }

            @Override
            public boolean isReplaceConsts() {
                return true;
            }

            @Override
            public boolean isExportAsGradleProject() {
                return false;
            }
        };
        final JadxDecompiler jadxDecompiler = new JadxDecompiler();
        jadxDecompiler.loadFile(file);
        final Method m = JavaClass.class.getDeclaredMethod("getClassNode");
        m.setAccessible(true);
        for (final JavaClass cls : jadxDecompiler.getClasses()) {
            if (cls.getFullName().startsWith("com.") || cls.getFullName().startsWith("org.") || cls.getFullName().startsWith("android.") || cls.getFullName().startsWith("net."))
                continue;
            if (cls.getName().length() > 3)
                continue;
            String clsName = null;
            CodeWriter writer = new CodeWriter();
            try {
                cls.decompile();
            } catch (Throwable t) {
                System.out.println("Couldn't decompile " + cls.getFullName());
            }
            ClassNode node = (ClassNode) field.get(cls);
            SourceFileAttr attr = node.get(AType.SOURCE_FILE);
            if (attr != null) {
                out.println("c " + cls.getFullName() + "=" + attr.getFileName() + "_" + cls.getName());
                oldToNew.put(cls.getFullName(), attr.getFileName());
                continue;
            }
            String fullName = cls.getFullName();
            fullName = cleaner.matcher(fullName).replaceAll("");
            fullName = fullName.replace("defpackage.", "");
            String origClassName = cls.getName();
            origClassName = cleaner.matcher(origClassName).replaceAll("");
            ClassGen classGen = null;
            try {
                classGen = new ClassGen((ClassNode) m.invoke(cls), jArgs);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            try {
                if (classGen != null) {
                    classGen.addClassBody(writer);
                }
            } catch (CodegenException e) {
                e.printStackTrace();
            }
            String[] lines = writer.toString().split("\n");
            for (String line : lines) {
                if (clsName != null) break;
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        System.out.println(matcher.group(0));
                        clsName = matcher.group(1);
                        clsName = clsName.replaceAll("\\/", "And");
                        clsName = clsName.substring(0, 1).toUpperCase() + clsName.substring(1);
                        if (origClassName.contains("$"))
                            clsName = clsName + origClassName.substring(origClassName.lastIndexOf('$'));
                        break;
                    }
                }
                if (clsName != null) {
                    out.println("c " + fullName + "=" + clsName + "_" + origClassName);
                    oldToNew.put(fullName, clsName);
                    if (origClassName.contains("$")) {
                        origClassName = origClassName.substring(0, origClassName.lastIndexOf('$'));
                        clsName = clsName.substring(0, clsName.lastIndexOf('$'));
                        fullName = fullName.substring(0, fullName.lastIndexOf('$'));
                        if (!oldToNew.containsKey(origClassName)) {
                            out.println("c " + fullName + "=" + clsName + "_" + origClassName);
                            oldToNew.put(fullName, clsName);
                        }
                    }
                }
            }
            if (clsName == null && origClassName.contains("$")) {
                checkLater.add(fullName);
            }
        }
        for (String s : checkLater) {
            String suffix = s.substring(s.lastIndexOf('$'));
            String fullName = s.substring(0, s.lastIndexOf('$'));
            String origClassName;
            if (fullName.contains("."))
                origClassName = fullName.substring(fullName.lastIndexOf('.'));
            else
                origClassName = fullName;
            if (oldToNew.containsKey(origClassName)) {
                String clsName = oldToNew.get(origClassName) + suffix;
                fullName = fullName + suffix;
                origClassName = origClassName + suffix;
                out.println("c " + fullName + "=" + clsName + "_" + origClassName);
                oldToNew.put(fullName, clsName);
            }
        }
        out.flush();
        out.close();
        for (String s : oldToNew.keySet()) {
            System.out.println(s + " -> " + oldToNew.get(s));
        }
    }
}

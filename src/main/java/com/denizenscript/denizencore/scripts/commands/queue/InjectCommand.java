package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.TaskScriptContainer;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;
import java.util.function.Consumer;

public class InjectCommand extends AbstractCommand {

    public InjectCommand() {
        setName("inject");
        setSyntax("inject (locally) [<script>] (path:<name>) (instantly)");
        setRequiredArguments(1, 4);
        isProcedural = true;
    }

    // <--[command]
    // @Name Inject
    // @Syntax inject (locally) [<script>] (path:<name>) (instantly)
    // @Required 1
    // @Maximum 4
    // @Short Runs a script in the current queue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Description
    // Injects a script into the current queue.
    // This means this task will run with all of the original queue's definitions and tags.
    // It will also now be part of the queue, so any delays or definitions used in the injected script will be
    // accessible in the original queue.
    //
    // @Tags
    // None
    //
    // @Usage
    // Injects the InjectedTask task into the current queue
    // - inject InjectedTask
    // -->

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        for (ScriptContainer script : ScriptRegistry.scriptContainers.values()) {
            if (script instanceof TaskScriptContainer) {
                addOne.accept(script.getName());
            }
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (arg.matches("instant", "instantly")) {
                scriptEntry.addObject("instant", new ElementTag(true));
            }
            else if (arg.matches("local", "locally")) {
                scriptEntry.addObject("local", new ElementTag(true));
            }
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && arg.limitToOnlyPrefix("script")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("path")
                    && arg.matchesPrefix("path", "p")) {
                String path = arg.asElement().asString();
                if (!scriptEntry.hasObject("script")) {
                    int dotIndex = path.indexOf('.');
                    if (dotIndex > 0) {
                        ScriptTag script = new ScriptTag(path.substring(0, dotIndex));
                        if (script.isValid()) {
                            scriptEntry.addObject("script", script);
                            path = path.substring(dotIndex + 1);
                        }
                    }
                }
                scriptEntry.addObject("path", new ElementTag(path));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script") && !scriptEntry.hasObject("local")) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be injected.");
        }
        if (scriptEntry.hasObject("local") && !scriptEntry.hasObject("path") && !scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must specify a PATH.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ScriptTag script = scriptEntry.getObjectTag("script");
        if (script == null) {
            script = scriptEntry.getScript();
        }
        ElementTag instant = scriptEntry.getElement("instant");
        ElementTag path = scriptEntry.getElement("path");
        ElementTag local = scriptEntry.getElement("local");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script, instant, path, local);
        }
        List<ScriptEntry> entries;
        if (local != null && local.asBoolean()) {
            String pathName = path != null ? path.asString() : script.getName();
            entries = scriptEntry.getScript().getContainer().getEntries(scriptEntry.entryData.clone(), pathName);
        }
        else if (path != null) {
            entries = script.getContainer().getEntries(scriptEntry.entryData.clone(), path.asString());
        }
        else {
            entries = script.getContainer().getBaseEntries(scriptEntry.entryData.clone());
        }

        if (entries == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script inject failed (invalid script path '" + path + "')!");
            return;
        }
        if (instant != null && instant.asBoolean()) {
            scriptEntry.getResidingQueue().runNow(entries);
        }
        else {
            scriptEntry.getResidingQueue().injectEntries(entries, 0);
        }
    }
}

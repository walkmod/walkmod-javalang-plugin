package org.walkmod.javalang.writers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import org.walkmod.javalang.actions.Action;
import org.walkmod.javalang.actions.ActionsApplier;
import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.patchs.Patches;
import org.walkmod.javalang.walkers.DefaultJavaWalker;
import org.walkmod.patches.Patch;
import org.walkmod.patches.PatchFormat;
import org.walkmod.walkers.VisitorContext;
import org.walkmod.writers.AbstractPatchWriter;

public class PatchWriter extends AbstractPatchWriter {

    private String getLocation(File original) {
        String location = original.getPath();
        try {

            String rootDir = getOutputDirectory().getCanonicalPath();
            if (location.startsWith(rootDir)) {
                location = location.substring(rootDir.length() + 1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return location;
    }

    protected Patch generatePatch(StringBuffer buffer, List<Action> actions, File original, boolean multiple) {

        ActionsApplier actionsApplier = new ActionsApplier();
        actionsApplier.setActionList(actions);
        actionsApplier.setText(original);
        actionsApplier.execute();
        String modification = actionsApplier.getModifiedText();
        String originalContents;
        String location = getLocation(original);
        try {
            originalContents = FileUtils.readFileToString(original);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Action firstAction = actions.get(0);
        Action lastAction = actions.get(actions.size() - 1);
        String diff = Patches.generatePatch(originalContents, modification, location);

        Patch patch = new Patch(diff, firstAction.getBeginLine(), firstAction.getBeginColumn(), lastAction.getEndLine(),
                lastAction.getEndColumn(), getCause(), location, multiple);
        return patch;
    }

    protected void updateCause(Object n) {
        if (n instanceof Node) {
            Object oData = ((Node) n).getData();
            if (oData != null && (oData instanceof Map)) {
                Map<String, Object> data = (Map<String, Object>) oData;
                if (data.containsKey("cause")) {
                    setCause(data.get("cause").toString());
                }
            }
        }
    }

    @Override
    public String getContent(Object n, VisitorContext vc) {
        File original = (File) vc.get(DefaultJavaWalker.ORIGINAL_FILE_KEY);
        if (vc != null && vc.containsKey(DefaultJavaWalker.ACTIONS_TO_APPY_KEY) && original != null) {
            updateCause(n);

            StringBuffer buffer = new StringBuffer();

            List<Action> actions = (List<Action>) vc.get(DefaultJavaWalker.ACTIONS_TO_APPY_KEY);
            List<Patch> patches = new LinkedList<>();

            if (isPatchPerFile()) {
                patches.add(generatePatch(buffer, actions, original, true));
            }
            if (isPatchPerChange()) {
                for (Action action : actions) {
                    LinkedList<Action> singleAction = new LinkedList<>();
                    singleAction.add(action);
                    patches.add(generatePatch(buffer, singleAction, original, false));
                }
            }

            PatchFormat patchFormat = Enum.valueOf(PatchFormat.class, getPatchFormat().toUpperCase());
            buffer.append(patchFormat.getFormatter().format(getLocation(original), patches));

            return buffer.toString();
        }
        return "";
    }
}

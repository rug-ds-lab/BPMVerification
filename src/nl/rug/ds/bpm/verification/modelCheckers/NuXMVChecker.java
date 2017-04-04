package nl.rug.ds.bpm.verification.modelCheckers;

import nl.rug.ds.bpm.editor.models.ModelChecker;

/**
 * Created by Mark Kloosterhuis.
 */
public class NuXMVChecker extends NuSMVChecker {


    public NuXMVChecker(ModelChecker checkerSettings) {
        super("NuXMV");
        this.checkerSettings = checkerSettings;
        checkerPath = checkerSettings.getLocation();
        inputChecker = new StringBuilder();
    }
}

package ru.taximaxim.codekeeper.ui.prefs;

import java.io.File;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import ru.taximaxim.codekeeper.ui.UIConsts;

/**
 * Extends {@link ScopedPreferenceStore} with app's default values.
 * 
 * @author Alexander Levsha
 */
public class UIScopedPreferenceStore extends ScopedPreferenceStore {
    
    public UIScopedPreferenceStore() {
        super(InstanceScope.INSTANCE, UIConsts.PLUGIN_ID);
        setDefaultValues();
    }
    
    private void setDefaultValues() {
        setDefault(UIConsts.PREF_PGDUMP_EXE_PATH, "pg_dump");
        setDefault(UIConsts.PREF_DB_STORE, "default\t\t\t\t\t0");
        setDefault(UIConsts.PREF_GIT_KEY_PRIVATE_FILE, (new File(new File(
                System.getProperty("user.home"), ".ssh"), "id_rsa")).toString());
        setDefault(UIConsts.PREF_RECENT_PROJECTS, "");
        setDefault(UIConsts.PREF_PGDUMP_CUSTOM_PARAMS, "");
    }
}
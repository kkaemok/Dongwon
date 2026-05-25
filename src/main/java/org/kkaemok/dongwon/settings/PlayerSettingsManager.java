package org.kkaemok.dongwon.settings;

import org.kkaemok.dongwon.progression.ProfileManager;

import java.util.UUID;

public final class PlayerSettingsManager {
    private final ProfileManager profileManager;
    private boolean defaultTpaGuiEnabled;

    public PlayerSettingsManager(ProfileManager profileManager, boolean defaultTpaGuiEnabled) {
        this.profileManager = profileManager;
        this.defaultTpaGuiEnabled = defaultTpaGuiEnabled;
    }

    public boolean isTpaGuiEnabled(UUID playerId) {
        return profileManager.isTpaGuiEnabled(playerId, defaultTpaGuiEnabled);
    }

    public boolean toggleTpaGui(UUID playerId) {
        return profileManager.toggleTpaGui(playerId, defaultTpaGuiEnabled);
    }

    public void setDefaultTpaGuiEnabled(boolean defaultTpaGuiEnabled) {
        this.defaultTpaGuiEnabled = defaultTpaGuiEnabled;
    }

    public void save() {
        profileManager.save();
    }
}

package com.thecoderscorner.menu.editorui.project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PersistedProject {
    private String version = "1.00";
    private String projectName;
    private String author;
    private Instant lastEdited;
    private ArrayList<PersistedMenu> items;

    public PersistedProject() {
    }

    public PersistedProject(String projectName, String author, Instant lastEdited, List<PersistedMenu> items) {
        this.projectName = projectName;
        this.author = author;
        this.lastEdited = lastEdited;
        this.items = new ArrayList<>(items);
    }

    public String getVersion() {
        return version;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAuthor() {
        return author;
    }

    public Instant getLastEdited() {
        return lastEdited;
    }

    public List<PersistedMenu> getItems() {
        return items;
    }


}

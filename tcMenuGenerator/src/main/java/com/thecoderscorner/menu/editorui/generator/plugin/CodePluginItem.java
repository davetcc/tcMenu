/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.plugin;

import com.thecoderscorner.menu.editorui.generator.core.CreatorProperty;
import com.thecoderscorner.menu.editorui.generator.core.SubSystem;
import com.thecoderscorner.menu.editorui.generator.core.HeaderDefinition;

import java.util.List;
import java.util.Objects;

public class CodePluginItem {
    private String id;
    private String description;
    private String extendedDescription;
    private List<EmbeddedPlatform> supportedPlatforms;
    private List<String> requiredLibraries;
    private SubSystem subsystem;
    private String imageFileName;
    private String docsLink;
    private boolean themeNeeded;
    private CodePluginConfig config;
    private List<CreatorProperty> properties;
    private List<CodeVariable> variables;
    private List<HeaderDefinition> includeFiles;
    private List<RequiredSourceFile> requiredSourceFiles;
    private List<FunctionDefinition> functions;
    private CodePluginManager manager;

    public CodePluginItem() {
    }

    public CodePluginManager getManager() {
        return manager;
    }

    public void setManager(CodePluginManager manager) {
        this.manager = manager;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtendedDescription() {
        return extendedDescription;
    }

    public void setExtendedDescription(String extendedDescription) {
        this.extendedDescription = extendedDescription;
    }

    public List<EmbeddedPlatform> getSupportedPlatforms() {
        return supportedPlatforms;
    }

    public void setSupportedPlatforms(List<EmbeddedPlatform> supportedPlatforms) {
        this.supportedPlatforms = supportedPlatforms;
    }

    public boolean isThemeNeeded() {
        return themeNeeded;
    }

    public void setThemeNeeded(boolean themeNeeded) {
        this.themeNeeded = themeNeeded;
    }

    public SubSystem getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(SubSystem subsystem) {
        this.subsystem = subsystem;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getDocsLink() {
        return docsLink;
    }

    public void setDocsLink(String docsLink) {
        this.docsLink = docsLink;
    }

    public CodePluginConfig getConfig() {
        return config;
    }

    public void setConfig(CodePluginConfig config) {
        this.config = config;
    }

    public List<CreatorProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<CreatorProperty> properties) {
        this.properties = properties;
    }

    public List<CodeVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<CodeVariable> variables) {
        this.variables = variables;
    }

    public List<HeaderDefinition> getIncludeFiles() {
        return includeFiles;
    }

    public void setIncludeFiles(List<HeaderDefinition> includeFiles) {
        this.includeFiles = includeFiles;
    }

    public List<RequiredSourceFile> getRequiredSourceFiles() {
        return requiredSourceFiles;
    }

    public void setRequiredSourceFiles(List<RequiredSourceFile> requiredSourceFiles) {
        this.requiredSourceFiles = requiredSourceFiles;
    }

    public List<FunctionDefinition> getFunctions() {
        return functions;
    }

    public List<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public void setRequiredLibraries(List<String> requiredLibraries) {
        this.requiredLibraries = requiredLibraries;
    }

    public void setFunctions(List<FunctionDefinition> functions) {
        this.functions = functions;
    }

    @Override
    public String toString() {
        return "CodePluginItem{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", extendedDescription='" + extendedDescription + '\'' +
                ", platforms=" + supportedPlatforms +
                ", subsystem=" + subsystem +
                ", imageFileName='" + imageFileName + '\'' +
                '}';
    }

    public CodePluginItem deepCopy() {
        var pluginCopy = new CodePluginItem();
        pluginCopy.setId(id);
        pluginCopy.setDescription(description);
        pluginCopy.setConfig(config);
        pluginCopy.setFunctions(functions);
        pluginCopy.setVariables(variables);
        pluginCopy.setDocsLink(docsLink);
        pluginCopy.setExtendedDescription(extendedDescription);
        pluginCopy.setImageFileName(imageFileName);
        pluginCopy.setIncludeFiles(includeFiles);
        pluginCopy.setRequiredLibraries(requiredLibraries);
        pluginCopy.setRequiredSourceFiles(requiredSourceFiles);
        pluginCopy.setSubsystem(subsystem);
        pluginCopy.setSupportedPlatforms(supportedPlatforms);
        pluginCopy.setThemeNeeded(themeNeeded);
        pluginCopy.setManager(manager);
        pluginCopy.setProperties(properties.stream().map(prop -> new CreatorProperty(
                prop.getName(), prop.getDescription(), prop.getExtendedDescription(), prop.getLatestValue(), prop.getSubsystem(),
                prop.getPropType(), prop.getValidationRules(), prop.getApplicability())).toList());
        return pluginCopy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodePluginItem that = (CodePluginItem) o;
        return themeNeeded == that.themeNeeded && Objects.equals(id, that.id) && Objects.equals(description, that.description) && Objects.equals(extendedDescription, that.extendedDescription) && Objects.equals(supportedPlatforms, that.supportedPlatforms) && Objects.equals(requiredLibraries, that.requiredLibraries) && subsystem == that.subsystem && Objects.equals(imageFileName, that.imageFileName) && Objects.equals(docsLink, that.docsLink) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, extendedDescription, supportedPlatforms, requiredLibraries, subsystem, imageFileName, docsLink, themeNeeded, config);
    }
}

/*
 * Copyright (c)  2016-2020 https://www.thecoderscorner.com (Dave Cherry).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator;

import com.thecoderscorner.menu.persist.ReleaseType;
import com.thecoderscorner.menu.persist.VersionInfo;

import java.util.Map;

public interface LibraryVersionDetector {
    void changeReleaseType(ReleaseType releaseType);
    ReleaseType getReleaseType();
    Map<String, VersionInfo> acquireVersions();
    boolean availableVersionsAreValid(boolean refresh);
}

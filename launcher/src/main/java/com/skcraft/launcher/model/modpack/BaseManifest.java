/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.modpack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import net.teamfruit.skcraft.launcher.model.modpack.ConnectServerInfo;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseManifest {

    private String title;
    private String thumb;
    private String name;
    private String version;
    private ConnectServerInfo server;

}

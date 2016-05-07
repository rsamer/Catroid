/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2016 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.common;

import com.google.android.gms.common.images.WebImage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScratchProjectData implements Serializable {

    public static class ScratchRemixProjectData implements Serializable {
        private static final long serialVersionUID = 1L;

        public ScratchRemixProjectData(String title, String owner, WebImage projectImage) {

        }
    }

    private static final long serialVersionUID = 1L;

    private String title;
    private String owner;
    private String description;
    private String projectUrl;
    private WebImage projectImage;
    private int views;
    private int favorites;
    private int loves;
    private List<ScratchRemixProjectData> remixes;

    public ScratchProjectData(String title, String owner, String description, String projectUrl,
                              int views, int favorites, int loves) {
        this.title = title;
        this.owner = owner;
        this.description = description;
        this.projectUrl = projectUrl;
        this.projectImage = null;
        this.remixes = new ArrayList<>();
        this.views = views;
        this.favorites = favorites;
        this.loves = loves;
    }

    public String getProjectUrl() { return projectUrl; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public WebImage getProjectImage() { return projectImage; }

    public void setProjectImage(WebImage projectImage) { this.projectImage = projectImage; }

    public void addRemixProject(ScratchRemixProjectData remixProjectData) {
        remixes.add(remixProjectData);
    }

    public List<ScratchRemixProjectData> getRemixes() { return remixes; }

    public int getViews() { return views; }

    public int getFavorites() { return favorites; }

    public int getLoves() { return loves; }

}

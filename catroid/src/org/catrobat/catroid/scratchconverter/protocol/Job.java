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

package org.catrobat.catroid.scratchconverter.protocol;

import com.google.android.gms.common.images.WebImage;

public class Job {
	private long jobID;
	private String programTitle;
	private WebImage programImage;

	public Job(final long jobID, final String programTitle, final WebImage programImage) {
		this.jobID = jobID;
		this.programTitle = programTitle;
		this.programImage = programImage;
	}

	public String getProgramTitle() {
		return programTitle;
	}

	public long getJobID() {
		return jobID;
	}

	public WebImage getProgramImage() {
		return programImage;
	}

}

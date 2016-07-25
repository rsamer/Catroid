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

import java.util.ArrayList;
import java.util.List;

public class JobConsole {
	private static final String TAG = JobConsole.class.getSimpleName();

	private List<String> lines;
	private double progress;

	public JobConsole() {
		lines = new ArrayList<>();
		progress = 0.0;
	}

	public void setProgress(final double progress) {
		this.progress = progress;
	}

	public void addLines(final String[] lines) {
		for (final String line : lines) {
			this.lines.add(line);
		}
	}

}

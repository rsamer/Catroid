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

public enum JSONKeys {

	// main JSON-keys for messages received by server
	CATEGORY("category"),
	DATA("data"),
	TYPE("type");

	private final String rawValue;

	JSONKeys(final String rawValue) {
		this.rawValue = rawValue;
	}

	@Override
	public String toString() {
		return rawValue;
	}

	// JSON-keys of arguments contained in data
	public enum JSONDataKeys {
		MSG("msg"),
		JOB_ID("jobID"),
		JOB_TITLE("jobTitle"),
		JOB_IMAGE_URL("jobImageURL"),
		LINES("lines"),
		PROGRESS("progress"),
		URL("url"),
		CACHED_UTC_DATE("cachedUTCDate"),
		JOBS_INFO("jobsInfo"),
		CATROBAT_LANGUAGE_VERSION("catLangVers"),
		CLIENT_ID("clientID");

		private final String rawValue;

		JSONDataKeys(final String rawValue) {
			this.rawValue = rawValue;
		}

		@Override
		public String toString() {
			return rawValue;
		}
	}

	// JSON-keys of job-arguments in InfoMessage
	public enum JSONJobDataKeys {
		STATE("state"),
		JOB_ID("jobID"),
		TITLE("title"),
		IMAGE_URL("imageURL"),
		PROGRESS("progress"),
		ALREADY_DOWNLOADED("alreadyDownloaded"),
		DOWNLOAD_URL("downloadURL");
		private final String rawValue;

		JSONJobDataKeys(final String rawValue) {
			this.rawValue = rawValue;
		}

		@Override
		public String toString() {
			return rawValue;
		}
	}

}

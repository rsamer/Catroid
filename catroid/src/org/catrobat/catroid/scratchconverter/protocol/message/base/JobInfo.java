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

package org.catrobat.catroid.scratchconverter.protocol.message.base;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JobInfo {
	public enum ArgumentType {
		STATUS("status"),
		JOB_ID("jobID"),
		TITLE("title"),
		URL("url"),
		PROGRESS("progress");
		private final String rawValue;

		ArgumentType(final String rawValue) {
			this.rawValue = rawValue;
		}

		@Override
		public String toString() {
			return rawValue;
		}
	}

	public enum Status {
		READY(0),
		RUNNING(1),
		FINISHED(2),
		FAILED(3);

		private int status;

		private static Map<Integer, Status> map = new HashMap<>();

		static {
			for (Status legEnum : Status.values()) {
				map.put(legEnum.status, legEnum);
			}
		}

		Status(final int status) {
			this.status = status;
		}

		public static Status valueOf(int status) {
			return map.get(status);
		}
	}

	private Status status;
	private long jobID;
	private String title;
	private String url;
	private double progress;

	public JobInfo(final Status status, final long jobID, final String title, final String url, final double
			progress) {
		this.status = status;
		this.jobID = jobID;
		this.title = title;
		this.url = url;
		this.progress = progress;
	}

	public static JobInfo fromJson(JSONObject data) throws JSONException {
		final Status status = Status.valueOf(data.getInt(ArgumentType.STATUS.toString()));
		final long jobID = data.getLong(ArgumentType.JOB_ID.toString());
		final String title = data.getString(ArgumentType.TITLE.toString());
		final String url = data.getString(ArgumentType.URL.toString());
		final double progress = data.getDouble(ArgumentType.PROGRESS.toString());
		return new JobInfo(status, jobID, title, url, progress);
	}

	public Status getStatus() {
		return status;
	}

	public long getJobID() {
		return jobID;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public double getProgress() {
		return progress;
	}

}

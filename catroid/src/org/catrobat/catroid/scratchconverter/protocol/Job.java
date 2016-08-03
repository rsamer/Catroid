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

import android.net.Uri;

import com.google.android.gms.common.images.WebImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Job {
	public enum ArgumentType {
		STATUS("status"),
		JOB_ID("jobID"),
		TITLE("title"),
		IMAGE_URL("imageURL"),
		IMAGE_WIDTH("imageWidth"),
		IMAGE_HEIGHT("imageHeight"),
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

	public enum State {
		INITIALIZED(-1),
		READY(0),
		RUNNING(1),
		FINISHED(2),
		FAILED(3);

		private int state;

		private static Map<Integer, State> map = new HashMap<>();

		static {
			for (State legEnum : State.values()) {
				map.put(legEnum.state, legEnum);
			}
		}

		State(final int state) {
			this.state = state;
		}

		public static State valueOf(int state) {
			return map.get(state);
		}
	}

	private State state;
	private long jobID;
	private String title;
	private WebImage image;
	private double progress;

	public Job(long jobID, String title, WebImage image) {
		this.state = State.INITIALIZED;
		this.jobID = jobID;
		this.title = title;
		this.image = image;
		this.progress = 0.0;
	}

	public static Job fromJson(JSONObject data) throws JSONException {
		final State state = State.valueOf(data.getInt(ArgumentType.STATUS.toString()));
		final long jobID = data.getLong(ArgumentType.JOB_ID.toString());
		final String title = data.getString(ArgumentType.TITLE.toString());
		final String imageURL = data.getString(ArgumentType.IMAGE_URL.toString());
		final int imageWidth = data.getInt(ArgumentType.IMAGE_WIDTH.toString());
		final int imageHeight = data.getInt(ArgumentType.IMAGE_HEIGHT.toString());
		final double progress = data.getDouble(ArgumentType.PROGRESS.toString());
		final Job job = new Job(jobID, title, new WebImage(Uri.parse(imageURL), imageWidth, imageHeight));
		job.state = state;
		job.progress = progress;
		return job;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public long getJobID() {
		return jobID;
	}

	public String getTitle() {
		return title;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public WebImage getImage() {
		return image;
	}

	public void setImage(WebImage image) {
		this.image = image;
	}

}

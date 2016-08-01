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

package org.catrobat.catroid.scratchconverter.protocol.message.job;

import android.support.annotation.Nullable;
import android.util.Log;

import org.catrobat.catroid.scratchconverter.protocol.message.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

abstract public class JobMessage extends Message {

	private static final String TAG = JobMessage.class.getSimpleName();

	public enum Type {
		// NOTE: do not change values! -> starts with 1!
		JOB_FAILED(1),
		JOB_RUNNING(2),
		JOB_ALREADY_RUNNING(3),
		JOB_READY(4),
		JOB_OUTPUT(5),
		JOB_PROGRESS(6),
		JOB_FINISHED(7),
		JOB_DOWNLOAD(8);

		private int typeID;

		private static Map<Integer, Type> map = new HashMap<>();

		static {
			for (Type legEnum : Type.values()) {
				map.put(legEnum.typeID, legEnum);
			}
		}

		Type(final int typeID) {
			this.typeID = typeID;
		}

		public static Type valueOf(int typeID) {
			return map.get(typeID);
		}
	}

	private Type type;

	public JobMessage(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Nullable
	public static <T extends JobMessage> T fromJson(JSONObject jsonMessage) throws JSONException {
		final JSONObject jsonData = jsonMessage.getJSONObject("data");
		switch (Type.valueOf(jsonMessage.getInt("type"))) {
			case JOB_FAILED:
				return (T)new JobFailedMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()));
			case JOB_RUNNING:
				return (T)new JobRunningMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()));
			case JOB_ALREADY_RUNNING:
				return (T)new JobAlreadyRunningMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()));
			case JOB_READY:
				return (T)new JobReadyMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()));
			case JOB_OUTPUT:
				final JSONArray jsonLines = jsonData.getJSONArray(ArgumentType.LINES.toString());
				final List<String> lineList = new ArrayList<>();
				for (int i = 0; i < jsonLines.length(); ++i) {
					lineList.add(jsonLines.getString(i));
				}
				final String[] lines = lineList.toArray(new String[lineList.size()]);
				return (T)new JobOutputMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()), lines);
			case JOB_PROGRESS:
				return (T)new JobProgressMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()),
						jsonData.getDouble(ArgumentType.PROGRESS.toString()));
			case JOB_FINISHED:
				return (T)new JobFinishedMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()));
			case JOB_DOWNLOAD:
				final String dateUTC = jsonData.getString(ArgumentType.CACHED_UTC_DATE.toString());
				final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date cachedDate = null;
				try {
					cachedDate = dateFormat.parse(dateUTC);
				} catch (ParseException e) {
					Log.e(TAG, e.getLocalizedMessage());
				}
				return (T)new JobDownloadMessage(jsonData.getLong(ArgumentType.JOB_ID.toString()),
						jsonData.getString(ArgumentType.URL.toString()), cachedDate);
			default:
				return null;
		}
	}
}

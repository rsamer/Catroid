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

import android.support.annotation.Nullable;

import org.catrobat.catroid.scratchconverter.protocol.message.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

abstract public class BaseMessage extends Message {

	public enum Type {
		// NOTE: do not change values!
		ERROR(0),
		JOBS_INFO(9),
		CLIENT_ID(10);

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

	public BaseMessage(Type type) {
		this.type = type;
	}

	@Nullable
	public static <T extends BaseMessage> T fromJson(final JSONObject jsonMessage) throws JSONException {
		final JSONObject jsonData = jsonMessage.getJSONObject("data");
		switch (Type.valueOf(jsonMessage.getInt("type"))) {
			case ERROR:
				return (T)new ErrorMessage(jsonData.getString("msg"));
			case CLIENT_ID:
				return (T)new ClientIDMessage(jsonData.getLong("clientID"));
			case JOBS_INFO:
				JSONArray jobsInfo = jsonData.getJSONArray("jobsInfo");
				for (int i = 0; i < jobsInfo.length(); ++i) {
					JSONObject jobInfo = jobsInfo.getJSONObject(i);
					jobInfo.getInt("status");
					jobInfo.getInt("jid");
					jobInfo.getString("title");
					jobInfo.getString("url");
					jobInfo.getDouble("progress");
				}
				// TODO: implement...
				//message = new JobsInfoMessage(jsonMessage.getLong("jobID"), jobsInfo);
				return null;
		}
		return null;
	}

}

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

package org.catrobat.catroid.scratchconverter.protocol.message;

import java.util.HashMap;
import java.util.Map;

abstract public class Message {
	public enum ArgumentType {
		MSG("msg"),
		JOB_ID("jobID"),
		LINES("lines"),
		PROGRESS("progress"),
		URL("url"),
		CACHED_UTC_DATE("cachedUTCDate"),
		JOBS_INFO("jobsInfo"),
		CLIENT_ID("clientID");
		private final String rawValue;

		ArgumentType(final String rawValue) {
			this.rawValue = rawValue;
		}

		@Override
		public String toString() {
			return rawValue;
		}
	}

	private final Map<ArgumentType, Object> payload;

	public Message() {
		this.payload = new HashMap<>();
	}

	final protected void addArgument(ArgumentType type, Object value) {
		payload.put(type, value);
	}

	final public Object getArgument(ArgumentType type) {
		return payload.get(type);
	}

}

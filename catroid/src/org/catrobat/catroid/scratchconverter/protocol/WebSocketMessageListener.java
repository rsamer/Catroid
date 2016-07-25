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

import android.support.annotation.NonNull;
import android.util.Log;

import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.scratchconverter.protocol.message.base.BaseMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobMessage;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final public class WebSocketMessageListener implements MessageListener, WebSocket.StringCallback {

	private static final String TAG = WebSocketMessageListener.class.getSimpleName();

	private BaseMessageHandler baseMessageHandler;
	private Map<Long, JobHandler> jobHandlers;
	private Map<Long, Set<JobConsoleViewListener>> jobConsoleViewListeners;
	private Set<JobConsoleViewListener> globalJobConsoleViewListeners;
	private Set<BaseInfoViewListener> baseInfoViewListeners;

	public WebSocketMessageListener() {
		this.baseMessageHandler = null;
		this.jobHandlers = new HashMap<>();
		this.jobConsoleViewListeners = new HashMap<>();
		this.globalJobConsoleViewListeners = new HashSet<>();
		this.baseInfoViewListeners = new HashSet<>();
	}

	public JobHandler getJobHandler(final long jobID) {
		return jobHandlers.get(jobID);
	}

	public void setBaseMessageHandler(final BaseMessageHandler baseMessageHandler) {
		this.baseMessageHandler = baseMessageHandler;
	}

	public void addJobHandler(JobHandler handler) {
		jobHandlers.put(handler.getJobID(), handler);
	}

	@Override
	public void addBaseInfoViewListener(BaseInfoViewListener baseInfoViewListener) {
		baseInfoViewListeners.add(baseInfoViewListener);
	}

	@Override
	public void addGlobalJobConsoleViewListener(JobConsoleViewListener jobConsoleViewListener) {
		globalJobConsoleViewListeners.add(jobConsoleViewListener);
	}

	@Override
	public void addJobConsoleViewListener(long jobID, JobConsoleViewListener jobConsoleViewListener) {
		Set<JobConsoleViewListener> listeners = jobConsoleViewListeners.get(jobID);
		if (listeners == null) {
			listeners = new HashSet<>();
		}
		listeners.add(jobConsoleViewListener);
		jobConsoleViewListeners.put(jobID, listeners);
	}

	@NonNull
	public BaseInfoViewListener[] getBaseInfoViewListeners() {
		return baseInfoViewListeners.toArray(new BaseInfoViewListener[baseInfoViewListeners.size()]);
	}

	@NonNull
	public JobConsoleViewListener[] getJobConsoleViewListeners(long jobID) {
		final Set<JobConsoleViewListener> mergedListenersList = new HashSet<>();
		final Set<JobConsoleViewListener> listenersList = jobConsoleViewListeners.get(jobID);
		if (listenersList != null) {
			mergedListenersList.addAll(listenersList);
		}
		mergedListenersList.addAll(globalJobConsoleViewListeners);
		return mergedListenersList.toArray(new JobConsoleViewListener[mergedListenersList.size()]);
	}

	@Override
	public void onStringAvailable(String s) {
		try {
			if (s == null) {
				return;
			}
			Log.d(TAG, "Receiving: " + s);
			JSONObject jsonMessage = new JSONObject(s);
			if (jsonMessage.length() == 0) {
				return;
			}
			BaseMessage.Type baseMessageType = BaseMessage.Type.valueOf(jsonMessage.getInt("type"));

			if (baseMessageType != null) {
				// case: base message
				baseMessageHandler.onBaseMessage(BaseMessage.fromJson(jsonMessage), getBaseInfoViewListeners());

			} else {
				// case: job message
				final JSONObject jsonData = jsonMessage.getJSONObject("data");
				final long jobID = jsonData.getLong(JobMessage.ArgumentType.JOB_ID.toString());
				JobHandler jobHandler = jobHandlers.get(jobID);
				if (jobHandler != null) {
					jobHandler.onJobMessage(JobMessage.fromJson(jsonMessage), getJobConsoleViewListeners(jobID));
				} else {
					Log.w(TAG, "No JobHandler registered for job with ID: " + jobID);
				}

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}

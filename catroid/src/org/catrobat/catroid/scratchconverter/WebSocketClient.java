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

package org.catrobat.catroid.scratchconverter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.protocol.BaseMessageHandler;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.JobHandler;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.scratchconverter.protocol.command.Command;
import org.catrobat.catroid.scratchconverter.protocol.command.RetrieveJobsInfoCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.ScheduleJobCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.SetClientIDCommand;
import org.catrobat.catroid.scratchconverter.protocol.message.Message;
import org.catrobat.catroid.scratchconverter.protocol.message.base.BaseMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ClientIDMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ErrorMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.JobsInfoMessage;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;

final public class WebSocketClient implements Client, BaseMessageHandler {

	private interface ConnectCallback {
		void onSuccess();
		void onFailure(ClientException ex);
	}

	private static final String TAG = WebSocketClient.class.getSimpleName();

	private final Context context;
	private final Handler handler;
	private Client.State state;
	private long clientID;
	private WebSocketMessageListener messageListener;
	private AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
	private WebSocket webSocket;
	private ConnectAuthCallback connectAuthCallback;
	private ConvertCallback convertCallback;

	public WebSocketClient(final Context context, final long clientID, final WebSocketMessageListener messageListener,
			final ConvertCallback convertCallback)
	{
		this.context = context;
		this.handler = new Handler(context.getMainLooper());
		this.clientID = clientID;
		this.state = State.NOT_CONNECTED;
		messageListener.setBaseMessageHandler(this);
		this.messageListener = messageListener;
		this.webSocket = null;
		this.connectAuthCallback = null;
		this.convertCallback = convertCallback;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	@Override
	public boolean isClosed() {
		return state == State.NOT_CONNECTED;
	}

	@Override
	public boolean isAuthenticated() {
		return state == State.CONNECTED_AUTHENTICATED;
	}

	public void setAsyncHttpClient(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	private void connect(final ConnectCallback connectCallback) {
		if (state == State.CONNECTED) {
			connectCallback.onSuccess();
			return;
		}
		Preconditions.checkState(webSocket == null);
		Preconditions.checkState(asyncHttpClient != null);

		asyncHttpClient.websocket(Constants.SCRATCH_CONVERTER_WEB_SOCKET, null, new
				AsyncHttpClient.WebSocketConnectCallback() {
					@Override
					public void onCompleted(Exception ex, final WebSocket newWebSocket) {
						Preconditions.checkState(state != State.CONNECTED && webSocket == null);
						if (ex != null) {
							connectCallback.onFailure(new ClientException(ex));
							return;
						}

						state = State.CONNECTED;
						webSocket = newWebSocket;

						// onMessage callback
						webSocket.setStringCallback(messageListener);

						// onClose callback
						webSocket.setClosedCallback(new CompletedCallback() {
							@Override
							public void onCompleted(Exception ex) {
								state = State.NOT_CONNECTED;
								connectAuthCallback.onConnectionClosed(new ClientException(ex));
							}
						});
						connectCallback.onSuccess();
					}
				});
	}

	public void close() {
		Preconditions.checkState(state != State.NOT_CONNECTED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(connectAuthCallback != null);
		webSocket.close();
	}

	private void authenticate() {
		Preconditions.checkState(state == State.CONNECTED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new SetClientIDCommand(clientID));
	}

	public void connectAndAuthenticate(final ConnectAuthCallback connectAuthCallback) {
		this.connectAuthCallback = connectAuthCallback;

		switch (state) {
			case NOT_CONNECTED:
				connect(new ConnectCallback() {
					@Override
					public void onSuccess() {
						Log.i(TAG, "Successfully connected to WebSocket server");
						authenticate();
					}

					@Override
					public void onFailure(ClientException ex) {
						connectAuthCallback.onConnectionFailure(ex);
					}
				});
				break;

			case CONNECTED:
				Log.i(TAG, "Already connected to WebSocket server!");
				authenticate();
				break;

			case CONNECTED_AUTHENTICATED:
				Log.i(TAG, "Already authenticated!");
				connectAuthCallback.onSuccess(clientID);
				break;

		}
	}

	@Override
	public void retrieveJobsInfo() {
		Preconditions.checkState(state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new RetrieveJobsInfoCommand(clientID));
	}

	@Override
	public void convertJob(final Job job, final boolean verbose, final boolean force)
	{
		Preconditions.checkState(state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(clientID != INVALID_CLIENT_ID);

		final long jobID = job.getJobID();
		Log.i(TAG, "Scheduling new job with ID: " + jobID);

		JobHandler jobHandler = messageListener.getJobHandler(jobID);
		if (jobHandler != null) {
			Log.d(TAG, "JobHandler for jobID " + jobID + " already exists!");
			if (force == false && jobHandler.getCurrentState().isInProgress()) {
				convertCallback.onConversionFailure(job, new ClientException("Job in progress!"));
				return;
			}
			jobHandler.setCallback(convertCallback);
		} else {
			Log.d(TAG, "Creating new JobHandler for jobID " + jobID);
			jobHandler = new JobHandler(context, job, convertCallback);
			messageListener.addJobHandler(jobHandler);
		}

		jobHandler.onJobScheduled(messageListener.getJobConsoleViewListeners(jobID));
		sendCommand(new ScheduleJobCommand(jobID, clientID, force, verbose));
	}

	@Override
	public void onBaseMessage(BaseMessage baseMessage, final BaseInfoViewListener[] viewListeners) {
		// case: JobsInfoMessage
		if (baseMessage instanceof JobsInfoMessage) {
			final Job[] jobs = (Job[])baseMessage.getArgument(Message.ArgumentType.JOBS_INFO);
			for (Job job : jobs) {
				if (messageListener.getJobHandler(job.getJobID()) == null) {
					JobHandler jobHandler = new JobHandler(context, job, convertCallback);
					// TODO: combine JobHandler and Job states !!!
					JobHandler.State state = JobHandler.State.UNSCHEDULED;
					switch (job.getState()) {
						case READY:
							state = JobHandler.State.READY;
							break;
						case RUNNING:
							state = JobHandler.State.RUNNING;
							break;
						case FAILED:
							state = JobHandler.State.FAILED;
							break;
						case FINISHED:
							state = JobHandler.State.UNSCHEDULED;
							break;
					}
					jobHandler.setState(state);
					messageListener.addJobHandler(jobHandler);
				}
			}

			if (viewListeners != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (BaseInfoViewListener viewListener : viewListeners) {
							viewListener.onJobsInfo(jobs);
						}
					}
				});
			}
			return;
		}

		// case: ErrorMessage
		if (baseMessage instanceof ErrorMessage) {
			final String errorMessage = (String)baseMessage.getArgument(Message.ArgumentType.MSG);
			Log.e(TAG, errorMessage);

			if (state == State.CONNECTED) {
				Preconditions.checkState(connectAuthCallback != null);
				connectAuthCallback.onAuthenticationFailure(new ClientException(errorMessage));
			} else if (state == State.CONNECTED_AUTHENTICATED) {
				// TODO: determine how to get job??? -> introduce custom JobErrorMessage!
				convertCallback.onConversionFailure(null, new ClientException(errorMessage));
			}

			if (viewListeners != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (BaseInfoViewListener viewListener : viewListeners) {
							viewListener.onError(errorMessage);
						}
					}
				});
			}
			return;
		}

		// case: ClientIDMessage & RenewClientIDMessage
		if (baseMessage instanceof ClientIDMessage) {
			Preconditions.checkState(state == State.CONNECTED);
			long oldClientID = clientID;
			clientID = (long)baseMessage.getArgument(Message.ArgumentType.CLIENT_ID);

			if (clientID != oldClientID) {
				Log.d(TAG, "New Client ID: " + clientID);
			}
			state = State.CONNECTED_AUTHENTICATED;
			connectAuthCallback.onSuccess(clientID);
			return;
		}

		// case: Unhandled base message!
		Log.e(TAG, "No handler implemented for base message: " + baseMessage);
	}

	private void sendCommand(final Command command) {
		Preconditions.checkArgument(command != null);
		Preconditions.checkState(state == State.CONNECTED || state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);

		final String dataToSend = command.toJson().toString();
		Log.d(TAG, "Sending: " + dataToSend);
		webSocket.send(dataToSend);
	}

	private void runOnUiThread(Runnable r) {
		handler.post(r);
	}

}

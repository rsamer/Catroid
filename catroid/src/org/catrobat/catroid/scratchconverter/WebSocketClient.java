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
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.protocol.BaseMessageHandler;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.JobHandler;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.scratchconverter.protocol.command.Command;
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
	}
	private interface AuthCallback {
		void onSuccess();
	}

	private static final String TAG = WebSocketClient.class.getSimpleName();

	private final Context context;
	private final Handler handler;
	private Client.State state;
	private long clientID;
	private WebSocketMessageListener messageListener;
	private AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
	private WebSocket webSocket;
	private AuthCallback authCallback;
	private ClientCallback clientCallback;

	public WebSocketClient(final Context context, final long clientID, final WebSocketMessageListener messageListener) {
		this.context = context;
		this.handler = new Handler(context.getMainLooper());
		this.clientID = clientID;
		this.state = State.NOT_CONNECTED;
		messageListener.setBaseMessageHandler(this);
		this.messageListener = messageListener;
		this.webSocket = null;
		this.authCallback = null;
		this.clientCallback = null;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	public void setAsyncHttpClient(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	public boolean isJobInProgress(final long jobID) {
		JobHandler jobHandler = messageListener.getJobHandler(jobID);
		return jobHandler != null && jobHandler.getCurrentState().isInProgress();
	}

	private void connect(final ConnectCallback connectCallback, final ClientCallback clientCallback) {
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
							state = State.FAILED;
							clientCallback.onAuthenticationFailure(new ClientCallback.ClientException(ex));
							return;
						}

						state = State.CONNECTED;
						webSocket = newWebSocket;

						webSocket.setStringCallback(messageListener);
						connectCallback.onSuccess();
					}
				});
	}

	private void authenticate(final AuthCallback authCallback) {
		if (state == State.AUTHENTICATED) {
			authCallback.onSuccess();
			return;
		}
		Preconditions.checkState(state == State.CONNECTED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new SetClientIDCommand(clientID));
	}

	@Override
	public void convertJob(final Job job, final boolean verbose, final boolean force,
			final ClientCallback clientCallback) {
		this.clientCallback = clientCallback;
		this.authCallback = new AuthCallback() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "Authentication successful!");
				convert(job, verbose, force, clientCallback);
			}
		};

		if (state == State.NOT_CONNECTED) {
			connect(new ConnectCallback() {
				@Override
				public void onSuccess() {
					Log.i(TAG, "Successfully connected to WebSocket server");
					authenticate(authCallback);
				}
			}, clientCallback);
		} else if (state == State.CONNECTED) {
			Log.i(TAG, "Already connected to WebSocket server!");
			authenticate(authCallback);
		} else if (state == State.AUTHENTICATED) {
			Log.i(TAG, "Already authenticated!");
			convert(job, verbose, force, clientCallback);
		} else {
			Log.e(TAG, "Unhandled state: " + state);
		}
	}

	private void convert(final Job job, final boolean verbose, boolean force, final ClientCallback clientCallback) {
		Preconditions.checkState(state == State.AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(clientID != INVALID_CLIENT_ID);

		final long jobID = job.getJobID();
		Log.i(TAG, "Scheduling new job with ID: " + jobID);
		JobHandler jobHandler = messageListener.getJobHandler(jobID);
		if (jobHandler != null) {
			Log.d(TAG, "JobHandler for jobID " + jobID + " already exists!");
			if (force == false && jobHandler.getCurrentState().isInProgress()) {
				clientCallback.onConversionFailure(new ClientCallback.ClientException("Job in progress!"));
				return;
			}
			jobHandler.setCallback(clientCallback);
		} else {
			Log.d(TAG, "Creating new JobHandler for jobID " + jobID);
			jobHandler = new JobHandler(context, job, clientCallback);
			messageListener.addJobHandler(jobHandler);
		}
		jobHandler.onJobScheduled(messageListener.getJobConsoleViewListeners(jobID));
		sendCommand(new ScheduleJobCommand(jobID, clientID, force, verbose));
	}

	private void sendCommand(final Command command) {
		Preconditions.checkArgument(command != null);
		Preconditions.checkState(state == State.CONNECTED || state == State.AUTHENTICATED);
		Preconditions.checkState(webSocket != null);

		final String dataToSend = command.toJson().toString();
		Log.d(TAG, "Sending: " + dataToSend);
		webSocket.send(dataToSend);
	}

	@Override
	public void onBaseMessage(BaseMessage baseMessage, final BaseInfoViewListener[] viewListeners) {
		// case: JobsInfoMessage
		if (baseMessage instanceof JobsInfoMessage) {
			final Job[] jobs = (Job[])baseMessage.getArgument(Message.ArgumentType.JOBS_INFO);
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
				clientCallback.onAuthenticationFailure(new ClientCallback.ClientException(errorMessage));
			} else if (state == State.AUTHENTICATED) {
				clientCallback.onConversionFailure(new ClientCallback.ClientException(errorMessage));
			}
			state = State.FAILED;
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
				clientCallback.onClientIDChanged(clientID);
			}
			state = State.AUTHENTICATED;
			authCallback.onSuccess();
			return;
		}

		// case: Unhandled base message!
		Log.e(TAG, "No handler implemented for base message: " + baseMessage);
	}

	private void runOnUiThread(Runnable r) {
		handler.post(r);
	}

}

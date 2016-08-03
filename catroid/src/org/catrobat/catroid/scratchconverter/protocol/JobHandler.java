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

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ClientException;
import org.catrobat.catroid.scratchconverter.protocol.message.Message;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobAlreadyRunningMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobDownloadMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobFailedMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobFinishedMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobOutputMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobProgressMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobReadyMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobRunningMessage;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;

import java.util.Date;

public class JobHandler implements Client.DownloadFinishedListener {

	private static final String TAG = JobHandler.class.getSimpleName();

	public enum State {
		UNSCHEDULED, SCHEDULED, READY, RUNNING, CONVERSION_FINISHED, DOWNLOAD_READY, FAILED;

		public boolean isInProgress() {
			return this != UNSCHEDULED && this != FAILED;
		}
	}

	private final Handler handler;
	private Job job;
	private State currentState;
	private Client.ConvertCallback callback;

	public JobHandler(final Context context, final Job job, Client.ConvertCallback callback) {
		Preconditions.checkArgument(job != null);
		this.handler = new Handler(context.getMainLooper());
		this.job = job;
		this.currentState = State.UNSCHEDULED;
		this.callback = callback;
	}

	public void setState(State state) {
		this.currentState = state;
	}

	public void onJobScheduled(@NonNull final JobConsoleViewListener[] viewListeners) {
		Log.d(TAG, "Setting job as scheduled (jobID: " + job.getJobID() + ")");
		this.currentState = State.SCHEDULED;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobScheduled(job);
				}
			}
		});
	}

	@Override
	public void onDownloadFinished(String programName, String url) {
		Log.d(TAG, "Download finished - Resetting job with ID: " + job.getJobID());
		this.currentState = State.UNSCHEDULED;
	}

	@Override
	public void onUserCanceledDownload(String url) {
		Log.d(TAG, "User canceled download - Resetting job with ID: " + job.getJobID());
		this.currentState = State.UNSCHEDULED;
	}

	public void onUserCanceledConversion() {
		Log.d(TAG, "User canceled conversion - Resetting job with ID: " + job.getJobID());
		this.currentState = State.UNSCHEDULED;
	}

	public Job getJob() {
		return job;
	}

	public long getJobID() {
		return job.getJobID();
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCallback(Client.ConvertCallback callback) {
		this.callback = callback;
	}

	public void onJobMessage(final JobMessage jobMessage, @NonNull final JobConsoleViewListener[] viewListeners) {
		final long jobID = (long)jobMessage.getArgument(Message.ArgumentType.JOB_ID);
		Preconditions.checkArgument(jobID == job.getJobID());
		Preconditions.checkState(currentState != State.UNSCHEDULED);

		switch (currentState) {

			case SCHEDULED:
				if (jobMessage instanceof JobReadyMessage) {
					handleJobReadyMessage(viewListeners);
					return;
				} else if (jobMessage instanceof JobAlreadyRunningMessage) {
					handleJobAlreadyRunningMessage(viewListeners);
					return;
				} else if (jobMessage instanceof JobDownloadMessage) {
					final String downloadURL = (String)jobMessage.getArgument(Message.ArgumentType.URL);
					final Date cachedUTCDate = (Date)jobMessage.getArgument(Message.ArgumentType.CACHED_UTC_DATE);
					handleJobDownloadMessage(downloadURL, viewListeners, cachedUTCDate);
					return;
				}
				break;

			case READY:
				if (jobMessage instanceof JobRunningMessage) {
					handleJobRunningMessage(viewListeners);
					return;
				}
				break;

			case RUNNING:
				if (jobMessage instanceof JobProgressMessage) {
					final double progress = (double)jobMessage.getArgument(Message.ArgumentType.PROGRESS);
					handleJobProgressMessage(progress, viewListeners);
					return;
				} else if (jobMessage instanceof JobOutputMessage) {
					final String[] lines = (String[])jobMessage.getArgument(Message.ArgumentType.LINES);
					handleJobOutputMessage(lines, viewListeners);
					return;
				} else if (jobMessage instanceof JobFinishedMessage) {
					handleJobFinishedMessage(viewListeners);
					return;
				} else if (jobMessage instanceof JobFailedMessage) {
					handleJobFailedMessage(viewListeners);
					return;
				}
				break;

			case CONVERSION_FINISHED:
				if (jobMessage instanceof JobDownloadMessage) {
					final String downloadURL = (String)jobMessage.getArgument(Message.ArgumentType.URL);
					final Date cachedUTCDate = (Date)jobMessage.getArgument(Message.ArgumentType.CACHED_UTC_DATE);
					handleJobDownloadMessage(downloadURL, viewListeners, cachedUTCDate);
					return;
				}
				break;

			default:
				break;
		}

		Log.e(TAG, "Ignoring message of incompatible type '" + jobMessage.getType()
				+ "' for current state '" + currentState + "'");
	}

	private void handleJobReadyMessage(@NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.SCHEDULED);

		job.setState(Job.State.READY);
		currentState = State.READY;
		callback.onConversionReady(job);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobReady(job);
				}
			}
		});
	}

	private void handleJobAlreadyRunningMessage(@NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.SCHEDULED);
		job.setState(Job.State.READY);
		currentState = State.READY;
		handleJobRunningMessage(viewListeners);
	}

	private void handleJobRunningMessage(@NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.READY);

		job.setState(Job.State.RUNNING);
		currentState = State.RUNNING;
		callback.onConversionStart(job);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobStarted(job);
				}
			}
		});
	}

	private void handleJobProgressMessage(final double progress, @NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.RUNNING);

		job.setProgress(progress);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobProgress(job, progress);
				}
			}
		});
	}

	private void handleJobOutputMessage(@NonNull final String[] lines, @NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.RUNNING);

		for (String line : lines) {
			Log.d(TAG, line);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobOutput(job, lines);
				}
			}
		});
	}

	private void handleJobFinishedMessage(@NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.RUNNING);

		job.setState(Job.State.FINISHED);
		currentState = State.CONVERSION_FINISHED;
		callback.onConversionFinished(job);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobFinished(job);
				}
			}
		});
	}

	private void handleJobFailedMessage(@NonNull final JobConsoleViewListener[] viewListeners) {
		Preconditions.checkState(currentState == State.RUNNING);

		job.setState(Job.State.FAILED);
		currentState = State.FAILED;
		callback.onConversionFailure(job, new ClientException("Job failed"));

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobFailed(job);
				}
			}
		});
	}

	private void handleJobDownloadMessage(@NonNull final String downloadURL,
			@NonNull final JobConsoleViewListener[] viewListeners, @NonNull final Date cachedUTCDate) {
		Preconditions.checkState(currentState == State.SCHEDULED || currentState == State.CONVERSION_FINISHED);

		currentState = State.DOWNLOAD_READY;
		callback.onDownloadReady(job, this, downloadURL, cachedUTCDate);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (JobConsoleViewListener viewListener : viewListeners) {
					viewListener.onJobDownloadReady(job);
				}
			}
		});
	}

	private void runOnUiThread(Runnable r) {
		handler.post(r);
	}

}

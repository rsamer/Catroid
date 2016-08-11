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

import com.google.common.base.Preconditions;

import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ClientException;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobAlreadyRunningMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobDownloadMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobFailedMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobFinishedMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobOutputMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobProgressMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobReadyMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobRunningMessage;

public class JobHandler implements Client.DownloadFinishedCallback {

	private static final String TAG = JobHandler.class.getSimpleName();

	// TODO: combine JobHandler and Job states !!!
	public enum State {
		UNSCHEDULED, SCHEDULED, READY, RUNNING, CONVERSION_FINISHED, DOWNLOAD_READY, FAILED;

		public boolean isInProgress() {
			return this != UNSCHEDULED && this != FAILED;
		}
	}

	private Job job;
	private State currentState;
	private Client.ConvertCallback callback;

	public JobHandler(final Job job, Client.ConvertCallback callback) {
		Preconditions.checkArgument(job != null);
		this.job = job;
		this.currentState = State.UNSCHEDULED;
		this.callback = callback;
	}

	public void setState(State state) {
		Log.d(TAG, "Setting state of job to " + state + " (jobID: " + job.getJobID() + ")");
		this.currentState = state;
	}

	public void onJobScheduled() {
		Log.d(TAG, "Setting job as scheduled (jobID: " + job.getJobID() + ")");
		this.currentState = State.SCHEDULED;
		callback.onJobScheduled(job);
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

	public void onJobMessage(final JobMessage jobMessage) {
		Preconditions.checkArgument(job.getJobID() == jobMessage.getJobID());
		Preconditions.checkState(currentState != State.UNSCHEDULED);

		switch (currentState) {
			case SCHEDULED:
				if (jobMessage instanceof JobReadyMessage) {
					handleJobReadyMessage((JobReadyMessage) jobMessage);
					return;
				} else if (jobMessage instanceof JobAlreadyRunningMessage) {
					handleJobAlreadyRunningMessage((JobAlreadyRunningMessage) jobMessage);
					return;
				} else if (jobMessage instanceof JobDownloadMessage) {
					handleJobDownloadMessage((JobDownloadMessage)jobMessage);
					return;
				} else if (jobMessage instanceof JobFailedMessage) {
					handleJobFailedMessage((JobFailedMessage) jobMessage);
					return;
				}
				break;

			case READY:
				if (jobMessage instanceof JobRunningMessage) {
					handleJobRunningMessage((JobRunningMessage) jobMessage);
					return;
				}
				break;

			case RUNNING:
				if (jobMessage instanceof JobProgressMessage) {
					handleJobProgressMessage((JobProgressMessage) jobMessage);
					return;
				} else if (jobMessage instanceof JobOutputMessage) {
					handleJobOutputMessage((JobOutputMessage) jobMessage);
					return;
				} else if (jobMessage instanceof JobFinishedMessage) {
					handleJobFinishedMessage((JobFinishedMessage) jobMessage);
					return;
				} else if (jobMessage instanceof JobFailedMessage) {
					handleJobFailedMessage((JobFailedMessage) jobMessage);
					return;
				}
				break;

			case CONVERSION_FINISHED:
				if (jobMessage instanceof JobDownloadMessage) {
					handleJobDownloadMessage((JobDownloadMessage) jobMessage);
					return;
				}
				break;

		}

		Log.w(TAG, "Unable to handle message of type in current state " + currentState);
	}

	private void handleJobReadyMessage(@NonNull final JobReadyMessage jobReadyMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobReadyMessage.getJobID());
		Preconditions.checkState(currentState == State.SCHEDULED);

		job.setState(Job.State.READY);
		currentState = State.READY;
		callback.onConversionReady(job);
	}

	private void handleJobAlreadyRunningMessage(@NonNull final JobAlreadyRunningMessage jobAlreadyRunningMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobAlreadyRunningMessage.getJobID());
		Preconditions.checkState(currentState == State.SCHEDULED);

		job.setState(Job.State.READY);
		currentState = State.READY;
		handleJobRunningMessage(new JobRunningMessage(jobAlreadyRunningMessage.getJobID()));
	}

	private void handleJobRunningMessage(@NonNull final JobRunningMessage jobRunningMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobRunningMessage.getJobID());
		Preconditions.checkState(currentState == State.READY);

		job.setState(Job.State.RUNNING);
		currentState = State.RUNNING;
		callback.onConversionStart(job);
	}

	private void handleJobProgressMessage(@NonNull final JobProgressMessage jobProgressMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobProgressMessage.getJobID());
		Preconditions.checkState(currentState == State.RUNNING);

		job.setProgress(jobProgressMessage.getProgress());
		callback.onJobProgress(job, jobProgressMessage.getProgress());
	}

	private void handleJobOutputMessage(@NonNull final JobOutputMessage jobOutputMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobOutputMessage.getJobID());
		Preconditions.checkState(currentState == State.RUNNING);

		final String[] lines = jobOutputMessage.getLines();
		for (String line : lines) {
			Log.d(TAG, line);
		}
		callback.onJobOutput(job, lines);
	}

	private void handleJobFinishedMessage(@NonNull final JobFinishedMessage jobFinishedMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobFinishedMessage.getJobID());
		Preconditions.checkState(currentState == State.RUNNING);

		job.setState(Job.State.FINISHED);
		currentState = State.CONVERSION_FINISHED;
		callback.onConversionFinished(job);
	}

	private void handleJobFailedMessage(@NonNull final JobFailedMessage jobFailedMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobFailedMessage.getJobID());
		Preconditions.checkState(currentState == State.SCHEDULED || currentState == State.RUNNING);

		job.setState(Job.State.FAILED);
		currentState = State.FAILED;
		callback.onConversionFailure(job, new ClientException("Job failed - Reason: " + jobFailedMessage.getMessage()));
	}

	private void handleJobDownloadMessage(@NonNull final JobDownloadMessage jobDownloadMessage) {
		Preconditions.checkArgument(getJob().getJobID() == jobDownloadMessage.getJobID());
		Preconditions.checkState(currentState == State.SCHEDULED || currentState == State.CONVERSION_FINISHED);

		currentState = State.DOWNLOAD_READY;
		callback.onDownloadReady(job, this, jobDownloadMessage.getDownloadURL(), jobDownloadMessage.getCachedUTCDate());
	}

}

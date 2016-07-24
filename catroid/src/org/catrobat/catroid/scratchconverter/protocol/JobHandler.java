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

import com.google.common.base.Preconditions;

import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.job.JobReadyMessage;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;

public class JobHandler {
	public enum State {
		INITIALIZED, READY, RUNNING, CONVERSION_FINISHED, DOWNLOAD_FINISHED, FAILED;

		public boolean isInProgress() {
			return this != FAILED && this != DOWNLOAD_FINISHED;
		}
	}

	private Job job;
	private State state;
	private JobConsole jobConsole;
	private Client.CompletionCallback callback;

	public JobHandler(final Job job, Client.CompletionCallback callback) {
		Preconditions.checkArgument(job != null);
		this.job = job;
		this.state = State.INITIALIZED;
		this.jobConsole = new JobConsole();
		this.callback = callback;
	}

	public long getJobID() {
		return job.getJobID();
	}

	public State getState() {
		return state;
	}

	public void setCallback(Client.CompletionCallback callback) {
		this.callback = callback;
	}

	public void onJobMessage(JobMessage jobMessage, JobConsoleViewListener[] viewListeners) {
		// TODO: code for state machine goes here!!!
		if (jobMessage instanceof JobReadyMessage) {
			callback.onConversionReady();
			if (viewListeners != null) {
				for (JobConsoleViewListener viewListener : viewListeners) {
					// TODO: notify view...
				}
			}
		}
	}
}

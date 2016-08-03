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

package org.catrobat.catroid.ui.scratchconverter;

import android.app.Activity;
import android.content.ContextWrapper;

import com.google.android.gms.common.images.WebImage;

import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.protocol.Job;

public class ScratchConverterContextWrapper extends ContextWrapper {

	private static final String TAG = ScratchConverterContextWrapper.class.getSimpleName();
	//private static final Lock lock = new ReentrantLock();

	private final Activity activity;
	private final Client converterClient;

	public ScratchConverterContextWrapper(Activity activity, Client converterClient) {
		super(activity);
		this.activity = activity;
		this.converterClient = converterClient;
	}

	public void convertProgram(final long jobID, final String programTitle, final WebImage programImage,
			final boolean verbose, final boolean force) {

		// TODO: make sure NOT running on UI-thread!!
		final Job job = new Job(jobID, programTitle, programImage);

		//lock.lock();
		converterClient.convertJob(job, verbose, force);
	}

}

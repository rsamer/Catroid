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

package org.catrobat.catroid.transfers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.ScratchProgramData;
import org.catrobat.catroid.common.ScratchSearchResult;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.web.ServerCalls;
import org.catrobat.catroid.web.WebScratchProgramException;
import org.catrobat.catroid.web.WebconnectionException;

import java.io.InterruptedIOException;

public class FetchScratchProgramDetailsTask extends AsyncTask<Long, Void, ScratchProgramData> {

	public interface ScratchProgramListTaskDelegate {
		void onPreExecute();
		void onPostExecute(ScratchProgramData projectData);
	}

	private static final String TAG = FetchScratchProgramDetailsTask.class.getSimpleName();
	private static final int MAX_NUM_OF_RETRIES = 2;
	private static final int MIN_TIMEOUT = 1_000; // in ms

	private Context context;
	private Handler handler;
	private ScratchProgramListTaskDelegate delegate = null;
	private ScratchDataFetcher fetcher = null;

	public interface ScratchDataFetcher {
		ScratchProgramData fetchScratchProgramDetails(final long programID)
				throws WebconnectionException, WebScratchProgramException, InterruptedIOException;
		ScratchSearchResult fetchDefaultScratchPrograms() throws WebconnectionException, InterruptedIOException;
		ScratchSearchResult scratchSearch(final String query, final ServerCalls.ScratchSearchSortType sortType,
				final int numberOfItems, final int page) throws WebconnectionException, InterruptedIOException;
	}

	public FetchScratchProgramDetailsTask setContext(final Context context) {
		this.context = context;
		this.handler = new Handler(context.getMainLooper());
		return this;
	}

	public FetchScratchProgramDetailsTask setDelegate(ScratchProgramListTaskDelegate delegate) {
		this.delegate = delegate;
		return this;
	}

	public FetchScratchProgramDetailsTask setFetcher(ScratchDataFetcher fetcher) {
		this.fetcher = fetcher;
		return this;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (delegate != null) {
			delegate.onPreExecute();
		}
	}

	@Override
	protected ScratchProgramData doInBackground(Long... params) {
		Preconditions.checkArgument(params.length == 1, "No project ID given!");
		final long projectID = params[0];
		Preconditions.checkArgument(projectID > 0, "Invalid project ID given!");
		try {
			return fetchProjectData(projectID);
		} catch (InterruptedIOException exception) {
			Log.i(TAG, "Task has been cancelled in the meanwhile!");
			return null;
		}
	}

	public ScratchProgramData fetchProjectData(final long projectID) throws InterruptedIOException {
		// exponential backoff
		int delay;
		for (int attempt = 0; attempt <= MAX_NUM_OF_RETRIES; attempt++) {
			if (isCancelled()) {
				Log.i(TAG, "Task has been cancelled in the meanwhile!");
				return null;
			}

			try {
				return fetcher.fetchScratchProgramDetails(projectID);
			} catch (WebScratchProgramException e) {
				String userErrorMessage = context.getString(R.string.error_scratch_program_not_accessible_any_more);
				if (e.getStatusCode() == WebScratchProgramException.ERROR_PROGRAM_NOT_ACCESSIBLE) {
					userErrorMessage = context.getString(R.string.error_scratch_program_not_accessible_any_more);
				}

				final String finalUserErrorMessage = userErrorMessage;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ToastUtil.showError(context, finalUserErrorMessage);
					}
				});

				return null;
			} catch (WebconnectionException e) {
				Log.e(TAG, e.getMessage() + "\n" + e.getStackTrace());
				delay = MIN_TIMEOUT + (int) (MIN_TIMEOUT * Math.random() * (attempt + 1));
				Log.i(TAG, "Retry #" + (attempt + 1) + " to fetch scratch project list scheduled in "
						+ delay + " ms due to " + e.getLocalizedMessage());
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e1) {}
			}
		}
		Log.w(TAG, "Maximum number of " + (MAX_NUM_OF_RETRIES + 1) + " attempts exceeded! Server not reachable?!");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ToastUtil.showError(context, context.getString(R.string.error_request_timeout));
			}
		});
		return null;
	}

	@Override
	protected void onPostExecute(ScratchProgramData projectData) {
		super.onPostExecute(projectData);
		if (delegate != null && !isCancelled()) {
			delegate.onPostExecute(projectData);
		}
	}

	private void runOnUiThread(Runnable r) {
		handler.post(r);
	}

}

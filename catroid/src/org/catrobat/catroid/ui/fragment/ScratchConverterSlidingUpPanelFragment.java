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

package org.catrobat.catroid.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.images.WebImage;
import com.google.common.base.Splitter;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.ClientCallback.DownloadFinishedListener;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.ui.adapter.ScratchJobAdapter;
import org.catrobat.catroid.ui.adapter.ScratchJobAdapter.ScratchJobEditListener;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;
import org.catrobat.catroid.utils.ExpiringDiskCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryImageCache;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.utils.WebImageLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScratchConverterSlidingUpPanelFragment extends Fragment
		implements BaseInfoViewListener, JobConsoleViewListener, DownloadFinishedListener, ScratchJobEditListener {

	private static final String TAG = ScratchConverterSlidingUpPanelFragment.class.getSimpleName();

	private ImageView convertIconImageView;
	private TextView convertPanelHeadlineView;
	private TextView convertPanelStatusView;
	private ImageView upDownArrowImageView;
	private WebImageLoader webImageLoader;
	private ListView conversionListView;
	private ListView convertedProgramsListView;
	private Map<Long, Job> downloadJobsMap = Collections.synchronizedMap(new HashMap<Long, Job>());
	private RelativeLayout convertedProgramsList;
	private RelativeLayout conversionList;
	private ScratchJobAdapter conversionAdapter;
	private ScratchJobAdapter convertedProgramsAdapter;
	private Activity activity;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final ExecutorService executorService = Executors.newFixedThreadPool(Constants.WEBIMAGE_DOWNLOADER_POOL_SIZE);
		webImageLoader = new WebImageLoader(
				ExpiringLruMemoryImageCache.getInstance(),
				ExpiringDiskCache.getInstance(getActivity()),
				executorService
		);

		final View rootView = inflater.inflate(R.layout.fragment_scratch_converter_sliding_up_panel, container, false);
		convertIconImageView = (ImageView) rootView.findViewById(R.id.scratch_convert_icon);
		convertPanelHeadlineView = (TextView) rootView.findViewById(R.id.scratch_convert_headline);
		convertPanelStatusView = (TextView) rootView.findViewById(R.id.scratch_convert_status_text);
		upDownArrowImageView = (ImageView) rootView.findViewById(R.id.scratch_up_down_image_button);
		conversionList = (RelativeLayout) rootView.findViewById(R.id.scratch_conversion_list);
		conversionListView = (ListView) rootView.findViewById(R.id.scratch_conversion_list_view);
		convertedProgramsList = (RelativeLayout) rootView.findViewById(R.id.scratch_converted_programs_list);
		convertedProgramsListView = (ListView) rootView.findViewById(R.id.scratch_converted_programs_list_view);
		conversionList.setVisibility(View.GONE);
		convertedProgramsList.setVisibility(View.GONE);

		return rootView;
	}

	@Override
	public void onAttach(Context context) {
		// TODO: Workaround: since getActivity() returns null in some cases!
		// see: http://stackoverflow.com/questions/6215239/getactivity-returns-null-in-fragment-function
		super.onAttach(context);
		if (context instanceof Activity) {
			activity = (Activity) context;
		}
	}

	private void initConversionAdapter(List<Job> jobList) {
		if (jobList == null) {
			jobList = new ArrayList<>();
		}
		if (activity == null) {
			// TODO: check why this fails...
			Log.e(TAG, "Activity is NULL!!!");
			return;
		}
		conversionAdapter = new ScratchJobAdapter(activity,
				R.layout.fragment_scratch_job_list_item,
				R.id.scratch_job_list_item_title,
				jobList);
		conversionListView.setAdapter(conversionAdapter);
		conversionAdapter.setScratchJobEditListener(this);
		Utils.setListViewHeightBasedOnItems(conversionListView);
		conversionList.setVisibility(View.VISIBLE);
	}

	public void rotateImageButton(float degrees) {
		upDownArrowImageView.setAlpha(Math.max(1.0f - (float)Math.sin(degrees/360.0f * 2.0f * Math.PI), 0.3f));
		upDownArrowImageView.setRotation(degrees);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BaseInfoViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobsInfo(final Job[] jobs) {
		if (! Looper.getMainLooper().equals(Looper.myLooper())) {
			throw new AssertionError("You should not change the UI from any thread except UI thread!");
		}
		if (jobs == null || jobs.length == 0) {
			return;
		}
		initConversionAdapter(Arrays.asList(jobs));
	}

	@Override
	public void onError(final String errorMessage) {
		// TODO: implement
	}

	//------------------------------------------------------------------------------------------------------------------
	// JobConsoleViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobScheduled(final Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText("Scheduled");

		final WebImage httpImageMetadata = job.getImage();
		convertIconImageView.setImageBitmap(null);
		if (httpImageMetadata != null) {
			int width = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_thumbnail_width);
			int height = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_thumbnail_height);
			webImageLoader.fetchAndShowImage(httpImageMetadata.getUrl().toString(),
					convertIconImageView, width, height);
		}
	}

	@Override
	public void onJobReady(final Job job) {
		convertPanelStatusView.setText("Prepare...");
	}

	@Override
	public void onJobStarted(final Job job) {
		convertPanelStatusView.setText("Started...");
	}

	@Override
	public void onJobProgress(final Job job, final double progress) {
		convertPanelStatusView.setText(Utils.round(progress, 1) + "%");
	}

	@Override
	public void onJobOutput(final Job job, @NonNull final String[] lines) {
		/*
		for (final String line : lines) {
			convertPanelConsoleView.append(line);
		}
		convertPanelConsoleView.setMovementMethod(new ScrollingMovementMethod());
		*/
	}

	@Override
	public void onJobFinished(final Job job) {
		convertPanelStatusView.setText("Conversion finished...");
	}

	@Override
	public void onJobFailed(final Job job) {
		convertPanelStatusView.setText("Job failed!");
	}

	@Override
	public void onJobCanceled(Job job) {
		convertPanelStatusView.setText("Job canceled!");
	}

	@Override
	public void onJobDownloadReady(final Job job) {
		downloadJobsMap.put(job.getJobID(), job);
		convertPanelStatusView.setText("Downloading...");
	}

	@Override
	public void onDownloadFinished(final String programName, final String url) {
		if (! url.startsWith(Constants.SCRATCH_CONVERTER_BASE_URL)) {
			Log.w(TAG, "Received download-finished call for program: '" + programName + "' with invalid url: " + url);
			return;
		}

		// extract ID from url
		final String query = url.split("\\?")[1];
		final Map<String, String> queryParamsMap = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);
		final long jobID = Long.parseLong(queryParamsMap.get("id"));
		if (jobID <= 0) {
			Log.w(TAG, "Received download-finished call for program: '" + programName + "' with invalid url: " + url);
			return;
		}

		final Job job = downloadJobsMap.get(jobID);
		if (job == null) {
			Log.e(TAG, "No job with ID " + jobID + " found in downloadJobsMap!");
			return;
		}
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Download finished!");
			}
		});
	}

	@Override
	public void onProjectEdit(int position) {
		// TODO: implement! -> open program...
	}

}

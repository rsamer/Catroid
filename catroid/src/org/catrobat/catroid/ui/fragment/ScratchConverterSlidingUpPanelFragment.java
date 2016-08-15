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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.images.WebImage;
import com.google.common.base.Preconditions;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.scratchconverter.Client.DownloadFinishedCallback;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.ui.ProjectActivity;
import org.catrobat.catroid.ui.ScratchConverterActivity;
import org.catrobat.catroid.ui.adapter.ScratchJobAdapter;
import org.catrobat.catroid.ui.adapter.ScratchJobAdapter.ScratchJobEditListener;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;
import org.catrobat.catroid.ui.scratchconverter.JobViewListener;
import org.catrobat.catroid.utils.DownloadUtil;
import org.catrobat.catroid.utils.ExpiringDiskCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryImageCache;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.utils.WebImageLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ScratchConverterSlidingUpPanelFragment extends Fragment
		implements BaseInfoViewListener, JobViewListener, DownloadFinishedCallback, ScratchJobEditListener {

	private static final String TAG = ScratchConverterSlidingUpPanelFragment.class.getSimpleName();

	private ExecutorService executorService;
	private ImageView convertIconImageView;
	private TextView convertPanelHeadlineView;
	private TextView convertPanelStatusView;
	private RelativeLayout convertProgressLayout;
	private ProgressBar convertProgressBar;
	private TextView convertStatusProgressTextView;
	private ImageView upDownArrowImageView;
	private WebImageLoader webImageLoader;
	private ListView conversionListView;
	private ListView convertedProgramsListView;
	private Map<Long, Job> downloadJobsMap = Collections.synchronizedMap(new HashMap<Long, Job>());
	private Map<Long, String> downloadedProgramsMap = Collections.synchronizedMap(new HashMap<Long, String>());
	private RelativeLayout convertedProgramsList;
	private RelativeLayout conversionList;
	private ScratchJobAdapter conversionAdapter;
	private ScratchJobAdapter convertedProgramsAdapter;
	private List<Job> allJobs;

	public void setExecutorService(final ExecutorService service) {
		executorService = service;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		allJobs = new ArrayList<>();
		final View rootView = inflater.inflate(R.layout.fragment_scratch_converter_sliding_up_panel, container, false);
		convertIconImageView = (ImageView) rootView.findViewById(R.id.scratch_convert_icon);
		convertPanelHeadlineView = (TextView) rootView.findViewById(R.id.scratch_convert_headline);
		convertPanelStatusView = (TextView) rootView.findViewById(R.id.scratch_convert_status_text);
		convertProgressLayout = (RelativeLayout) rootView.findViewById(R.id.scratch_convert_progress_layout);
		convertProgressBar = (ProgressBar) rootView.findViewById(R.id.scratch_convert_progress_bar);
		convertStatusProgressTextView = (TextView) rootView.findViewById(R.id.scratch_convert_status_progress_text);
		upDownArrowImageView = (ImageView) rootView.findViewById(R.id.scratch_up_down_image_button);
		conversionList = (RelativeLayout) rootView.findViewById(R.id.scratch_conversion_list);
		conversionListView = (ListView) rootView.findViewById(R.id.scratch_conversion_list_view);
		convertedProgramsList = (RelativeLayout) rootView.findViewById(R.id.scratch_converted_programs_list);
		convertedProgramsListView = (ListView) rootView.findViewById(R.id.scratch_converted_programs_list_view);

		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		conversionList.setVisibility(View.GONE);
		convertedProgramsList.setVisibility(View.GONE);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		webImageLoader = new WebImageLoader(
				ExpiringLruMemoryImageCache.getInstance(),
				ExpiringDiskCache.getInstance(getActivity()),
				executorService
		);
		initAdapters();
	}

	private void initAdapters() {
		Preconditions.checkState(getActivity() != null);
		conversionAdapter = new ScratchJobAdapter(getActivity(),
				R.layout.fragment_scratch_job_list_item,
				R.id.scratch_job_list_item_title,
				new ArrayList<Job>(),
				executorService);
		conversionListView.setAdapter(conversionAdapter);
		conversionList.setVisibility(View.GONE);

		convertedProgramsAdapter = new ScratchJobAdapter(getActivity(),
				R.layout.fragment_scratch_job_list_item,
				R.id.scratch_job_list_item_title,
				new ArrayList<Job>(),
				executorService);
		convertedProgramsAdapter.setScratchJobEditListener(this);
		convertedProgramsListView.setAdapter(convertedProgramsAdapter);
		convertedProgramsList.setVisibility(View.GONE);
	}

	public void rotateImageButton(float degrees) {
		upDownArrowImageView.setAlpha(Math.max(1.0f - (float)Math.sin(degrees/360.0f * 2.0f * Math.PI), 0.3f));
		upDownArrowImageView.setRotation(degrees);
	}

	public boolean hasJobs() {
		return allJobs != null && allJobs.size() > 0;
	}

	private void setIconImageView(final WebImage webImage) {
		if (webImage != null && webImage.getUrl() != null) {
			final int width = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_tiny_thumbnail_width);
			final int height = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_tiny_thumbnail_height);
			final String originalImageURL = webImage.getUrl().toString();

			// load image but only thumnail!
			// in order to download only thumbnail version of the original image
			// we have to reduce the image size in the URL
			final String thumbnailImageURL = Utils.changeSizeOfScratchImageURL(originalImageURL, height);
			webImageLoader.fetchAndShowImage(thumbnailImageURL, convertIconImageView, width, height);
		} else {
			convertIconImageView.setImageBitmap(null);
		}
	}

	private void updateAdapterJobs(final List<Job> jobList) {
		allJobs = jobList;
		final List<Job> inProgressJobs = new ArrayList<>();
		final List<Job> finishedJobs = new ArrayList<>();
		for (Job job : jobList) {
			if (job.getState() == Job.State.FINISHED || job.getState() == Job.State.FAILED) {
				finishedJobs.add(job);
			} else {
				inProgressJobs.add(job);
			}
		}

		conversionAdapter.clear();
		if (inProgressJobs.size() > 0) {
			conversionAdapter.addAll(inProgressJobs);
			Utils.setListViewHeightBasedOnItems(conversionListView);
			conversionList.setVisibility(View.VISIBLE);
			conversionAdapter.notifyDataSetChanged();
		} else {
			conversionList.setVisibility(View.GONE);
		}

		convertedProgramsAdapter.clear();
		if (finishedJobs.size() > 0) {
			convertedProgramsAdapter.addAll(finishedJobs);
			Utils.setListViewHeightBasedOnItems(convertedProgramsListView);
			convertedProgramsList.setVisibility(View.VISIBLE);
			convertedProgramsAdapter.notifyDataSetChanged();
		} else {
			convertedProgramsList.setVisibility(View.GONE);
		}
	}

	private void updateAdapterSingleJob(final Job job) {
		final List<Job> jobList = new ArrayList<>();
		if (allJobs != null) {
			for (Job existingJob : allJobs) {
				if (job.getJobID() != existingJob.getJobID()) {
					jobList.add(existingJob);
				}
			}
		}

		jobList.add(0, job);
		updateAdapterJobs(jobList);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BaseInfoViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobsInfo(final Job[] jobs) {
		if (jobs == null || jobs.length == 0) {
			((ScratchConverterActivity) getActivity()).hideSlideUpPanelBar();
			return;
		}
		((ScratchConverterActivity) getActivity()).showSlideUpPanelBar(0);
		updateAdapterJobs(new ArrayList<>(Arrays.asList(jobs)));

		int finishedJobs = 0;
		WebImage webImage = null;
		for (Job job : jobs) {
			if (job.getState() == Job.State.FINISHED) {
				if (webImage == null && job.getImage() != null && job.getImage().getUrl() != null) {
					webImage = job.getImage();
				}
				finishedJobs++;
			}
		}
		setIconImageView(webImage);
		convertPanelHeadlineView.setText(getResources().getString(R.string.status_in_progress_x_jobs, (jobs.length - finishedJobs)));
		convertPanelStatusView.setText(getResources().getString(R.string.status_completed_x_jobs, finishedJobs));
	}

	@Override
	public void onError(final String errorMessage) {
		if (! Looper.getMainLooper().equals(Looper.myLooper())) {
			throw new AssertionError("You should not change the UI from any thread except UI thread!");
		}

		Log.e(TAG, "An error occurred: " + errorMessage);
		ToastUtil.showError(getActivity(), errorMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// JobViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobScheduled(final Job job) {
		((ScratchConverterActivity)getActivity()).showSlideUpPanelBar(0);
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_scheduled);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
	}

	@Override
	public void onJobReady(final Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_waiting_for_worker);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
		job.setProgress(0.0);
		updateAdapterSingleJob(job);
	}

	@Override
	public void onJobStarted(final Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_started);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
		job.setProgress(0.0);
		updateAdapterSingleJob(job);
	}

	@Override
	public void onJobProgress(final Job job, final double progress) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setVisibility(View.GONE);
		convertProgressLayout.setVisibility(View.VISIBLE);
		setIconImageView(job.getImage());
		final int progressInteger = Double.valueOf(progress).intValue();
		convertProgressBar.setProgress(progressInteger);
		//convertStatusProgressTextView.setText(String.format("%.1f", progress) + "%");
		convertStatusProgressTextView.setText(progressInteger + "%");
		updateAdapterSingleJob(job);
	}

	@Override
	public void onJobOutput(final Job job, @NonNull final String[] lines) {
		// reserved for later use (i.e. next ScratchConverter release)!
	}

	@Override
	public void onJobFinished(final Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_conversion_finished);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
		updateAdapterSingleJob(job);
		downloadJobsMap.put(job.getJobID(), job);
	}

	@Override
	public void onJobFailed(final Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_job_failed);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
		updateAdapterSingleJob(job);
	}

	@Override
	public void onUserCanceledJob(Job job) {
		convertPanelHeadlineView.setText(job.getTitle());
		convertPanelStatusView.setText(R.string.status_job_canceled);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
		setIconImageView(job.getImage());
		updateAdapterSingleJob(job);
	}

	@Override
	public void onDownloadStarted(String url) {
		convertPanelStatusView.setText(R.string.status_downloading);
		convertPanelStatusView.setVisibility(View.VISIBLE);
		convertProgressLayout.setVisibility(View.GONE);
	}

	@Override
	public void onDownloadFinished(final String catrobatProgramName, final String url) {
		Log.i(TAG, "Download of program '" + catrobatProgramName + "' finished (URL was " + url + ")");
		long jobID = Utils.extractScratchJobIDFromURL(url);
		if (jobID == Constants.INVALID_SCRATCH_PROGRAM_ID) {
			Log.w(TAG, "Received download-finished call for program: '" + catrobatProgramName + "' with invalid url: " + url);
			return;
		}

		final Job job = downloadJobsMap.get(jobID);
		downloadedProgramsMap.put(jobID, catrobatProgramName);
		if (job == null) {
			Log.e(TAG, "No job with ID " + jobID + " found in downloadJobsMap!");
			return;
		}

		try {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
			r.play();
		} catch (Exception e) {
			e.printStackTrace();
		}
		convertPanelStatusView.setText(R.string.status_download_finished);
		updateAdapterSingleJob(job);
	}

	@Override
	public void onUserCanceledDownload(final String url) {
		Log.i(TAG, "User canceled download with URL: " + url);
		long jobID = Utils.extractScratchJobIDFromURL(url);
		if (jobID == Constants.INVALID_SCRATCH_PROGRAM_ID) {
			Log.w(TAG, "Received download-canceled call for program with invalid url: " + url);
			return;
		}

		final Job job = downloadJobsMap.get(jobID);
		if (job == null) {
			Log.e(TAG, "No job with ID " + jobID + " found in downloadJobsMap!");
			return;
		}

		convertPanelStatusView.setText(R.string.status_download_canceled);
		updateAdapterSingleJob(job);
	}

	@Override
	public void onProjectEdit(int position) {
		if (!Looper.getMainLooper().equals(Looper.myLooper())) {
			throw new AssertionError("You should not change the UI from any thread except UI thread!");
		}

		Log.i(TAG, "User clicked on position: " + position);

		final Job job = convertedProgramsAdapter.getItem(position);
		if (job == null) {
			Log.e(TAG, "Job not found in conversionAdapter!");
			return;
		}

		if (job.getState() == Job.State.FAILED) {
			ToastUtil.showError(getActivity(), R.string.error_cannot_open_failed_scratch_program);
			return;
		}

		String catrobatProgramName = downloadedProgramsMap.get(job.getJobID());
		catrobatProgramName = catrobatProgramName == null ? job.getTitle() : catrobatProgramName;

		if (!StorageHandler.getInstance().projectExists(catrobatProgramName)) {
			AlertDialog.Builder builder = new CustomAlertDialogBuilder(getActivity());
			builder.setTitle(R.string.warning);
			builder.setMessage(R.string.error_cannot_open_not_existing_scratch_program);
			builder.setNeutralButton(R.string.close, null);
			Dialog errorDialog = builder.create();
			errorDialog.show();
			return;
		}

		Intent intent = new Intent(getActivity(), ProjectActivity.class);
		intent.putExtra(Constants.PROJECTNAME_TO_LOAD, catrobatProgramName);
		intent.putExtra(Constants.PROJECT_OPENED_FROM_PROJECTS_LIST, false);
		getActivity().startActivity(intent);
	}

}

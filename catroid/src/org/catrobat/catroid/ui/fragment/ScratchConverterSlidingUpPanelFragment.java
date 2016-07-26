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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.WebImage;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.message.base.JobInfo;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;
import org.catrobat.catroid.utils.ExpiringDiskCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryImageCache;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.utils.WebImageLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScratchConverterSlidingUpPanelFragment extends Fragment implements BaseInfoViewListener, JobConsoleViewListener {

	private static final String TAG = ScratchConverterSlidingUpPanelFragment.class.getSimpleName();

	private ImageView convertIconImageView;
	private TextView convertPanelHeadlineView;
	private TextView convertPanelStatusView;
	private TextView convertPanelConsoleView;
	private ImageView upDownArrowImageView;
	private WebImageLoader webImageLoader;

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
		convertPanelConsoleView = (TextView) rootView.findViewById(R.id.scratch_convert_panel_console);
		upDownArrowImageView = (ImageView) rootView.findViewById(R.id.scratch_up_down_image_button);
		return rootView;
	}

	public void rotateImageButton(float degrees) {
		upDownArrowImageView.setAlpha(Math.max(1.0f - (float)Math.sin(degrees/360.0f * 2.0f * Math.PI), 0.3f));
		upDownArrowImageView.setRotation(degrees);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BaseInfoViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobsInfo(final JobInfo[] jobsInfo) {

	}

	@Override
	public void onError(final String errorMessage) {

	}

	//------------------------------------------------------------------------------------------------------------------
	// JobConsoleViewListener callbacks
	//------------------------------------------------------------------------------------------------------------------
	@Override
	public void onJobScheduled(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelHeadlineView.setText(job.getProgramTitle());
				convertPanelStatusView.setText("Scheduled!");

				final WebImage httpImageMetadata = job.getProgramImage();
				convertIconImageView.setImageBitmap(null);
				if (httpImageMetadata != null) {
					int width = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_thumbnail_width);
					int height = getActivity().getResources().getDimensionPixelSize(R.dimen.scratch_project_thumbnail_height);
					webImageLoader.fetchAndShowImage(httpImageMetadata.getUrl().toString(),
							convertIconImageView, width, height);
				}
			}
		});
	}

	@Override
	public void onJobReady(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Prepare");
			}
		});
	}

	@Override
	public void onJobStarted(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Started...");
			}
		});
	}

	@Override
	public void onJobProgress(final Job job, final double progress) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText(Utils.round(progress, 1) + "%");
			}
		});
	}

	@Override
	public void onJobOutput(final Job job, @NonNull final String[] lines) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (final String line : lines) {
					convertPanelConsoleView.append(line);
				}
				convertPanelConsoleView.setMovementMethod(new ScrollingMovementMethod());
			}
		});
	}

	@Override
	public void onJobFinished(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Conversion finished...");
			}
		});
	}

	@Override
	public void onJobFailed(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Job failed!");
			}
		});
	}

	@Override
	public void onJobDownloadReady(final Job job) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertPanelStatusView.setText("Downloading...");
			}
		});
	}
}

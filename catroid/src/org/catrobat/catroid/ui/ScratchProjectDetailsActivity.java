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

package org.catrobat.catroid.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProjectData;
import org.catrobat.catroid.common.ScratchProjectData.ScratchRemixProjectData;
import org.catrobat.catroid.common.ScratchProjectData.VisibilityState;
import org.catrobat.catroid.common.ScratchProjectPreviewData;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.transfers.FetchScratchProjectDetailsTask;
import org.catrobat.catroid.transfers.FetchScratchProjectDetailsTask.ScratchProjectListTaskDelegate;
import org.catrobat.catroid.transfers.FetchScratchProjectDetailsTask.ScratchProjectDataFetcher;
import org.catrobat.catroid.ui.adapter.ScratchRemixedProjectAdapter;
import org.catrobat.catroid.ui.adapter.ScratchRemixedProjectAdapter.ScratchRemixedProjectEditListener;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;
import org.catrobat.catroid.utils.ExpiringDiskCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryImageCache;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.utils.WebImageLoader;
import org.catrobat.catroid.web.ServerCalls;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import uk.co.deanwild.flowtextview.FlowTextView;

import static android.view.View.*;

public class ScratchProjectDetailsActivity extends BaseActivity implements
		ScratchProjectListTaskDelegate, ScratchRemixedProjectEditListener, JobConsoleViewListener {

	private static final String TAG = ScratchProjectDetailsActivity.class.getSimpleName();
	private static ScratchProjectDataFetcher dataFetcher = ServerCalls.getInstance();
	private static MessageListener messageListener = null;
	private static ExecutorService executorService = null;

	private int imageWidth;
	private int imageHeight;

	private ScratchProjectPreviewData projectData;
	private TextView titleTextView;
	private TextView ownerTextView;
	private ImageView imageView;
	private TextView visibilityWarningTextView;
	private FlowTextView instructionsFlowTextView;
	private TextView notesAndCreditsLabelView;
	private TextView notesAndCreditsTextView;
	private TextView favoritesTextView;
	private TextView lovesTextView;
	private TextView viewsTextView;
	private TextView tagsTextView;
	private TextView sharedTextView;
	private TextView modifiedTextView;
	private Button convertButton;
	private WebImageLoader webImageLoader;
	private ListView remixedProjectsListView;
	private ProgressDialog progressDialog;
	private ScratchRemixedProjectAdapter scratchRemixedProjectAdapter;
	private ScrollView mainScrollView;
	private RelativeLayout detailsLayout;
	private TextView remixesLabelView;
	private FetchScratchProjectDetailsTask fetchDetailsTask = new FetchScratchProjectDetailsTask();

	// dependency-injection for testing with mock object
	public static void setDataFetcher(final ScratchProjectDataFetcher fetcher) {
		dataFetcher = fetcher;
	}

	public static void setMessageListener(final MessageListener listener) {
		messageListener = listener;
	}

	public static void setExecutorService(final ExecutorService service) {
		executorService = service;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scratch_project_details);

		projectData = getIntent().getParcelableExtra(Constants.INTENT_SCRATCH_PROJECT_DATA);
		assert(projectData != null);

		webImageLoader = new WebImageLoader(ExpiringLruMemoryImageCache.getInstance(),
				ExpiringDiskCache.getInstance(this), executorService);

		imageWidth = getResources().getDimensionPixelSize(R.dimen.scratch_project_image_width);
		imageHeight = getResources().getDimensionPixelSize(R.dimen.scratch_project_image_height);
		titleTextView = (TextView) findViewById(R.id.scratch_project_title);
		ownerTextView = (TextView) findViewById(R.id.scratch_project_owner);
		mainScrollView = (ScrollView) findViewById(R.id.scratch_project_scroll_view);
		imageView = (ImageView) findViewById(R.id.scratch_project_image_view);
		imageView.getLayoutParams().width = imageWidth;
		imageView.getLayoutParams().height = imageHeight;
		visibilityWarningTextView = (TextView) findViewById(R.id.scratch_project_visibility_warning);
		instructionsFlowTextView = (FlowTextView) findViewById(R.id.scratch_project_instructions_flow_text);
		notesAndCreditsLabelView = (TextView) findViewById(R.id.scratch_project_notes_and_credits_label);
		notesAndCreditsTextView = (TextView) findViewById(R.id.scratch_project_notes_and_credits_text);
		favoritesTextView = (TextView) findViewById(R.id.scratch_project_favorites_text);
		lovesTextView = (TextView) findViewById(R.id.scratch_project_loves_text);
		viewsTextView = (TextView) findViewById(R.id.scratch_project_views_text);
		tagsTextView = (TextView) findViewById(R.id.scratch_project_tags_text);
		sharedTextView = (TextView) findViewById(R.id.scratch_project_shared_text);
		modifiedTextView = (TextView) findViewById(R.id.scratch_project_modified_text);
		remixedProjectsListView = (ListView) findViewById(R.id.scratch_project_remixes_list_view);
		convertButton = (Button) findViewById(R.id.scratch_project_convert_button);
		detailsLayout = (RelativeLayout) findViewById(R.id.scratch_project_details_layout);
		remixesLabelView = (TextView) findViewById(R.id.scratch_project_remixes_label);

		messageListener.addJobConsoleViewListener(projectData.getId(), this);

		convertButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				disableConvertButton();

				Intent intent = new Intent();
				intent.putExtra(Constants.INTENT_SCRATCH_PROJECT_DATA, (Parcelable) projectData);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		loadData(projectData);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.INTENT_REQUEST_CODE_CONVERT && resultCode == RESULT_OK) {
			setResult(RESULT_OK, intent);
			finish();
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		messageListener.removeJobConsoleViewListener(projectData.getId(), this);
		fetchDetailsTask.cancel(true);
		progressDialog.dismiss();
	}

	private void loadData(ScratchProjectPreviewData scratchProjectData) {
		Log.i(TAG, scratchProjectData.getTitle());
		instructionsFlowTextView.setText("-");
		notesAndCreditsLabelView.setVisibility(GONE);
		notesAndCreditsTextView.setVisibility(GONE);
		tagsTextView.setVisibility(GONE);
		remixesLabelView.setVisibility(GONE);
		remixedProjectsListView.setVisibility(GONE);
		detailsLayout.setVisibility(GONE);
		visibilityWarningTextView.setVisibility(GONE);

		// TODO: use LRU cache!

		if (scratchRemixedProjectAdapter != null) {
			scratchRemixedProjectAdapter.clear();
		}

		if (scratchProjectData != null) {
			titleTextView.setText(scratchProjectData.getTitle());

			if (scratchProjectData.getProjectImage() != null && scratchProjectData.getProjectImage().getUrl() != null) {
				webImageLoader.fetchAndShowImage(
						scratchProjectData.getProjectImage().getUrl().toString(),
						imageView, imageWidth, imageHeight
				);
			}
		}
		fetchDetailsTask.setContext(this).setDelegate(this).setFetcher(dataFetcher);
		fetchDetailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, scratchProjectData.getId());
	}

	private void initRemixAdapter(List<ScratchRemixProjectData> scratchRemixedProjectsData) {
		if (scratchRemixedProjectsData == null) {
			scratchRemixedProjectsData = new ArrayList<>();
		}
		scratchRemixedProjectAdapter = new ScratchRemixedProjectAdapter(this,
				R.layout.fragment_scratch_project_list_item,
				R.id.scratch_projects_list_item_title,
				scratchRemixedProjectsData,
				executorService);
		remixedProjectsListView.setAdapter(scratchRemixedProjectAdapter);
		scratchRemixedProjectAdapter.setScratchRemixedProjectEditListener(this);
		Utils.setListViewHeightBasedOnItems(remixedProjectsListView);
	}

	public void onProjectEdit(int position) {
		Log.d(TAG, "Clicked on remix at position: " + position);
		ScratchRemixProjectData remixData = scratchRemixedProjectAdapter.getItem(position);
		Log.d(TAG, "Project ID of clicked item is: " + remixData.getId());

		ScratchProjectPreviewData prevData = new ScratchProjectPreviewData(remixData.getId(), remixData.getTitle(), null);
		prevData.setProjectImage(remixData.getProjectImage());
		Intent intent = new Intent(this, ScratchProjectDetailsActivity.class);
		intent.putExtra(Constants.INTENT_SCRATCH_PROJECT_DATA, (Parcelable) prevData);
		startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_CONVERT);
	}

	private void enableConvertButton() {
		convertButton.setText(R.string.convert);
		convertButton.setEnabled(true);
	}

	private void disableConvertButton() {
		convertButton.setEnabled(false);
		convertButton.setText(R.string.converting);
	}


	//----------------------------------------------------------------------------------------------
	// Scratch Project Details Task Delegate Methods
	//----------------------------------------------------------------------------------------------
	@Override
	public void onPreExecute() {
		Log.i(TAG, "onPreExecute for FetchScratchProjectsTask called");
		final ScratchProjectDetailsActivity activity = this;
		progressDialog = new ProgressDialog(activity);
		progressDialog.setCancelable(false);
		progressDialog.getWindow().setGravity(Gravity.CENTER);
		progressDialog.setMessage(activity.getResources().getString(R.string.loading));
		progressDialog.show();
	}

	@Override
	public void onPostExecute(final ScratchProjectData projectData) {
		Log.i(TAG, "onPostExecute for FetchScratchProjectsTask called");
		if (! Looper.getMainLooper().equals(Looper.myLooper())) {
			throw new AssertionError("You should not change the UI from any thread except UI thread!");
		}

		Preconditions.checkNotNull(progressDialog, "No progress dialog set/initialized!");
		progressDialog.dismiss();
		if (projectData == null) {
			return;
		}

		titleTextView.setText(projectData.getTitle());
		ownerTextView.setText(getString(R.string.by) + " " + projectData.getOwner());
		String instructionsText = projectData.getInstructions().replace("\n\n", "\n");
		instructionsText = (instructionsText.length() > 0) ? instructionsText : "--";
		Log.d(TAG, "Instructions: " + instructionsText);
		final String notesAndCredits = projectData.getNotesAndCredits().replace("\n\n", "\n");

		if (notesAndCredits.length() > 0) {
			notesAndCreditsTextView.setText(notesAndCredits);
			notesAndCreditsLabelView.setVisibility(VISIBLE);
			notesAndCreditsTextView.setVisibility(VISIBLE);
		}
		instructionsFlowTextView.setText(instructionsText);

		float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics());
		instructionsFlowTextView.setTextSize(textSize);
		instructionsFlowTextView.setTextColor(Color.LTGRAY);

		favoritesTextView.setText(Utils.humanFriendlyFormattedShortNumber(projectData.getFavorites()));
		lovesTextView.setText(Utils.humanFriendlyFormattedShortNumber(projectData.getLoves()));
		viewsTextView.setText(Utils.humanFriendlyFormattedShortNumber(projectData.getViews()));

		StringBuilder tagList = new StringBuilder();
		int index = 0;
		for (String tag : projectData.getTags()) {
			tagList.append((index++ > 0 ? ", " : "") + tag);
		}
		if (tagList.length() > 0) {
			tagsTextView.setText(tagList);
			tagsTextView.setVisibility(VISIBLE);
		}
		final String sharedDateString = Utils.formatDate(projectData.getSharedDate(), Locale.getDefault());
		final String modifiedDateString = Utils.formatDate(projectData.getModifiedDate(), Locale.getDefault());
		Log.d(TAG, "Shared: " + sharedDateString);
		Log.d(TAG, "Modified: " + modifiedDateString);
		sharedTextView.setText(getString(R.string.shared) + ": " + sharedDateString);
		modifiedTextView.setText(getString(R.string.modified) + ": " + modifiedDateString);
		detailsLayout.setVisibility(VISIBLE);
		if (projectData.getVisibilityState() != VisibilityState.PUBLIC) {
			visibilityWarningTextView.setVisibility(VISIBLE);
			convertButton.setEnabled(false);
		} else {
			visibilityWarningTextView.setVisibility(GONE);
			enableConvertButton();
		}

		if (projectData.getRemixes() != null && projectData.getRemixes().size() > 0) {
			remixesLabelView.setVisibility(VISIBLE);
			remixedProjectsListView.setVisibility(VISIBLE);
			initRemixAdapter(projectData.getRemixes());
		}

		// workaround to avoid scrolling down to list view after all list items have been initialized
		mainScrollView.postDelayed(new Runnable() {
			public void run() {
				mainScrollView.fullScroll(ScrollView.FOCUS_UP);
			}
		}, 300);
	}

	//----------------------------------------------------------------------------------------------
	// JobConsoleViewListener Events
	//----------------------------------------------------------------------------------------------
	@Override
	public void onJobScheduled(Job job) {
		if (job.getJobID() == projectData.getId()) {
			disableConvertButton();
		}
	}

	@Override
	public void onJobReady(Job job) {}

	@Override
	public void onJobStarted(Job job) {}

	@Override
	public void onJobProgress(Job job, double progress) {}

	@Override
	public void onJobOutput(Job job, @NonNull String[] lines) {}

	@Override
	public void onJobFinished(Job job) {}

	@Override
	public void onJobFailed(Job job) {
		if (job.getJobID() == projectData.getId()) {
			enableConvertButton();
		}
	}

	@Override
	public void onJobCanceled(Job job) {
		if (job.getJobID() == projectData.getId()) {
			enableConvertButton();
		}
	}

	@Override
	public void onJobDownloadReady(Job job) {
		if (job.getJobID() == projectData.getId()) {
			enableConvertButton();
		}
	}
}

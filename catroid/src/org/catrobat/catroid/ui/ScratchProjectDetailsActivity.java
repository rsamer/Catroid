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

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProjectData;
import org.catrobat.catroid.common.ScratchProjectPreviewData;
import org.catrobat.catroid.common.ScratchSearchResult;
import org.catrobat.catroid.transfers.FetchScratchProjectDetailsTask;
import org.catrobat.catroid.transfers.FetchScratchProjectsTask;
import org.catrobat.catroid.utils.ExpiringDiskCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryImageCache;
import org.catrobat.catroid.utils.ExpiringLruMemoryObjectCache;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.WebImageLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScratchProjectDetailsActivity extends BaseActivity
        implements FetchScratchProjectDetailsTask.ScratchProjectListTaskDelegate {

    private static final String TAG = ScratchProjectDetailsActivity.class.getSimpleName();

    private TextView projectTitleTextView;
    private ImageView projectImageView;
    private TextView projectInstructionsTextView;
    private TextView projectNotesAndCreditsTextView;
    private WebImageLoader webImageLoader;
    private ProgressDialog progressDialog;
    private FetchScratchProjectDetailsTask currentTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scratch_project_details);

        ScratchProjectPreviewData scratchProjectData = getIntent().getParcelableExtra(Constants.SCRATCH_PROJECT_DATA);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        webImageLoader = new WebImageLoader(
                ExpiringLruMemoryImageCache.getInstance(),
                ExpiringDiskCache.getInstance(this),
                executorService
        );

        Log.i(TAG, scratchProjectData.getTitle());
        projectTitleTextView = (TextView) findViewById(R.id.scratch_project_title);
        projectImageView = (ImageView) findViewById(R.id.scratch_project_image_view);
        projectInstructionsTextView = (TextView) findViewById(R.id.scratch_project_instructions_text);
        projectNotesAndCreditsTextView = (TextView) findViewById(R.id.scratch_project_notes_and_credits_text);

        int width = getResources().getDimensionPixelSize(R.dimen.scratch_project_image_width);
        int height = getResources().getDimensionPixelSize(R.dimen.scratch_project_image_height);

        if (scratchProjectData != null) {
            projectTitleTextView.setText(scratchProjectData.getTitle());

            if (scratchProjectData.getProjectImage() != null) {
                webImageLoader.fetchAndShowImage(
                        scratchProjectData.getProjectImage().getUrl().toString(),
                        projectImageView, width, height
                );
            }
        }

        // TODO: consider pagination for cache!
        // cache miss
        Log.d(TAG, "Cache miss!");
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = new FetchScratchProjectDetailsTask();
        currentTask.setDelegate(this).execute(scratchProjectData.getId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    //----------------------------------------------------------------------------------------------
    // Scratch Project Details Task Delegate Methods
    //----------------------------------------------------------------------------------------------
    @Override
    public void onPreExecute() {
        Log.i(TAG, "onPreExecute for FetchScratchProjectsTask called");
        final ScratchProjectDetailsActivity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setCancelable(false);
                progressDialog.getWindow().setGravity(Gravity.CENTER);
                progressDialog.setMessage(activity.getResources().getString(R.string.loading));
                progressDialog.show();
            }
        });
    }

    @Override
    public void onPostExecute(final ScratchProjectData projectData) {
        Log.i(TAG, "onPostExecute for FetchScratchProjectsTask called");
        final ScratchProjectDetailsActivity activity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (! Looper.getMainLooper().equals(Looper.myLooper())) {
                    throw new AssertionError("You should not change the UI from any thread "
                            + "except UI thread!");
                }

                Preconditions.checkNotNull(progressDialog, "No progress dialog set/initialized!");
                progressDialog.dismiss();
                if (projectData == null) {
                    ToastUtil.showError(activity, "Unable to connect to server, please try later");
                    return;
                }

                activity.projectTitleTextView.setText(projectData.getTitle());
                activity.projectInstructionsTextView.setText(projectData.getDescription());
                activity.projectNotesAndCreditsTextView.setText(
                        "Views: " + projectData.getViews() + "\n" +
                                "Favorites: " + projectData.getFavorites() + "\n" +
                                "Loves: " + projectData.getLoves() + "\n"
                );
            }
        });
    }

}

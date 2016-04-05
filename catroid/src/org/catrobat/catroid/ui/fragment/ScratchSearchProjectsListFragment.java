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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProjectData;
import org.catrobat.catroid.transfers.ScratchSearchTask;
import org.catrobat.catroid.ui.ScratchConverterActivity;
import org.catrobat.catroid.ui.adapter.ScratchProjectAdapter;
import org.catrobat.catroid.web.ServerCalls;

import java.util.ArrayList;

public class ScratchSearchProjectsListFragment extends Fragment implements ScratchSearchTask.ScratchSearchTaskDelegate {

    private SearchView searchView;
    private ImageButton audioButton;
    private ListView searchResultsListView;
    private ProgressDialog progressDialog;
    private ScratchProjectAdapter scratchProjectAdapter;
    private LruCache<String, ServerCalls.ScratchSearchResult> scratchSearchResultCache = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setRetainInstance(true);
        super.onCreate(savedInstanceState);
        scratchSearchResultCache = new LruCache<>(Constants.SCRATCH_SEARCH_RESULT_CACHE_SIZE);
    }

    @Override
    public void onResume() {
        super.onResume();
//        setShowDetails(true); //settings.getBoolean(SHARED_PREFERENCE_NAME, false)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final ScratchSearchProjectsListFragment fragment = this;
        final ScratchConverterActivity activity = (ScratchConverterActivity)getActivity();
        Context context = activity.getApplicationContext();
        final View rootView = inflater.inflate(R.layout.fragment_scratch_search_projects_list, container, false);
        searchView = (SearchView)rootView.findViewById(R.id.search_view_scratch);
        searchResultsListView = (ListView) rootView.findViewById(R.id.list_view_search_scratch);
        searchResultsListView.setVisibility(View.INVISIBLE);
        audioButton = (ImageButton) rootView.findViewById(R.id.mic_button_image_scratch);
        progressDialog = new ProgressDialog(activity);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setGravity(Gravity.CENTER);
        progressDialog.hide();
        scratchProjectAdapter = new ScratchProjectAdapter(context,
                R.layout.fragment_scratch_project_list_item,
                R.id.scratch_projects_list_item_title,
                new ArrayList<ScratchProjectData>());
        searchResultsListView.setAdapter(scratchProjectAdapter);

        int id = searchView.getContext()
                .getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) searchView.findViewById(id);
        //textView.setTextColor(Color.BLACK);
        //textView.setHintTextColor(Color.GRAY);
        textView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //Toast.makeText(activity, String.valueOf(hasFocus),Toast.LENGTH_SHORT).show();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i("S2CC", "SUBMIT");
                Log.i("S2CC", query);
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i("S2CC", newText);
                if (newText.length() <= 2) {
                    searchResultsListView.setVisibility(View.INVISIBLE);
                    return false;
                }

                searchResultsListView.setVisibility(View.VISIBLE);

                // TODO: consider pagination for cache!
                ServerCalls.ScratchSearchResult cachedResult = scratchSearchResultCache.get(newText);
                if (cachedResult != null) {
                    Log.d("S2CC", "Cache hit!");
                    onPostExecute(cachedResult);
                    return false;
                }

                // cache miss
                Log.d("S2CC", "Cache miss!");
                new ScratchSearchTask().setDelegate(fragment).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newText);
                return false;
            }
        });

        // TODO: >> powered by "https://www.google.com/uds/css/small-logo.png" Custom Search << (grey)
        return rootView;
    }

    @Override
    public void onPreExecute() {
        Preconditions.checkNotNull(progressDialog, "No progress dialog set/initialized!");
        progressDialog.setMessage(getActivity().getResources().getString(R.string.search_progress));
        //progressDialog.show();
    }

    @Override
    public void onPostExecute(ServerCalls.ScratchSearchResult result) {
        Preconditions.checkNotNull(progressDialog, "No progress dialog set/initialized!");
        Preconditions.checkNotNull(scratchProjectAdapter, "Scratch project adapter cannot be null!");
        progressDialog.hide();
        scratchSearchResultCache.put(result.getQuery(), result);
        if (result == null || result.getProjectList() == null) {
            Toast.makeText(getActivity(), "Unable to connect to server, please try later", Toast.LENGTH_LONG).show();
            return;
        }
        scratchProjectAdapter.clear();
        for (ScratchProjectData projectData : result.getProjectList()) {
            scratchProjectAdapter.add(projectData);
            Log.i("S2CC", projectData.getTitle());
        }
        scratchProjectAdapter.notifyDataSetChanged();
    }

    /*
    public boolean getShowDetails() {
        return scratchProjectAdapter.getShowDetails();
    }
    */

    public void setShowDetails(boolean showDetails) {
        /*
        scratchProjectAdapter.setShowDetails(showDetails);
        scratchProjectAdapter.notifyDataSetChanged();
        */
    }
}

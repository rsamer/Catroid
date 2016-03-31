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

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.transfers.ScratchSearchTask;

public class ScratchConverterActivity extends Activity {

    SearchView search;
    ImageButton buttonAudio;
    ListView searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scratch_converter);
        Log.i("S2CC", "Hello world!");

        // define a typeface for formatting text fields and list view
        search = (SearchView) findViewById(R.id.search_view_scratch);
        //search.setQueryHint("Start typing to search...");

        searchResults = (ListView) findViewById(R.id.list_view_search_scratch);
        buttonAudio = (ImageButton) findViewById(R.id.mic_button_image_scratch);

        // this part of the code is to handle the situation
        // when user enters any search criteria, how should the application behave?
        search.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //Toast.makeText(activity, String.valueOf(hasFocus),Toast.LENGTH_SHORT).show();
            }
        });

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i("S2CC", "SUBMIT");
                Log.i("S2CC", query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i("S2CC", newText);
                if (newText.length() > 3) {
                    // searchResults.setVisibility(myFragmentView.VISIBLE);
                    new ScratchSearchTask().execute(newText);
                } else {
                    // searchResults.setVisibility(myFragmentView.INVISIBLE);
                }
                return false;
            }
        });

        // TODO: >> powered by "https://www.google.com/uds/css/small-logo.png" Custom Search << (grey)

    }

    //this filters products from productResults and copies to filteredProductResults based on search text
    /*
    public void filterProductArray(String newText) {
        String pName;
        filteredProductResults.clear();
        for (int i = 0; i < productResults.size(); i++) {
            pName = productResults.get(i).getProductName().toLowerCase();
            if (pName.contains(newText.toLowerCase()) ||
                    productResults.get(i).getProductBarcode().contains(newText)) {
                filteredProductResults.add(productResults.get(i));
            }
        }
    }
    */
}

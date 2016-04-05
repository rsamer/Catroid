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

package org.catrobat.catroid.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.catrobat.catroid.common.ScratchProjectData;
import org.catrobat.catroid.R;

import java.util.List;

public class ScratchProjectAdapter extends ArrayAdapter<ScratchProjectData> {

    private static class ViewHolder {
        private RelativeLayout background;
        private TextView projectName;
        private ImageView image;
        private TextView size;
        private TextView dateChanged;
    }

    private static LayoutInflater inflater;

    public ScratchProjectAdapter(Context context, int resource, int textViewResourceId, List<ScratchProjectData> objects) {
        super(context, resource, textViewResourceId, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View projectView = convertView;
        final ViewHolder holder;
        if (projectView == null) {
            projectView = inflater.inflate(R.layout.fragment_scratch_project_list_item, parent, false);
            holder = new ViewHolder();
            holder.background = (RelativeLayout) projectView.findViewById(R.id.scratch_projects_list_item_background);
            holder.projectName = (TextView) projectView.findViewById(R.id.scratch_projects_list_item_title);
            holder.image = (ImageView) projectView.findViewById(R.id.scratch_projects_list_item_image);
            holder.size = (TextView) projectView.findViewById(R.id.scratch_projects_list_item_size_2);
            holder.dateChanged = (TextView) projectView.findViewById(R.id.scratch_projects_list_item_changed_2);
            projectView.setTag(holder);
        } else {
            holder = (ViewHolder) projectView.getTag();
        }

        // ------------------------------------------------------------
        ScratchProjectData projectData = getItem(position);
        String projectName = projectData.getTitle();

        //set name of project:
        holder.projectName.setText(projectName);

        // set size of project:
        holder.size.setText("test");

        //set last changed:
        holder.dateChanged.setText("test");
        holder.projectName.setSingleLine(true);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewHolder clickedViewHolder = (ViewHolder) v.getTag();
                v.clearFocus();
                v.requestFocus();
                if (clickedViewHolder != null) {
                    Log.d("S2CC", "Clicked on list view item: " + clickedViewHolder.projectName);
                } else {
                    Log.d("S2CC", "Clicked on list view item");
                }

                // TODO: implement!
            }
        });
        return projectView;
    }
}

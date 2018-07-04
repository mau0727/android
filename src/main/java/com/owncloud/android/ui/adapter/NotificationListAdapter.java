/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.lib.resources.notifications.models.RichObject;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.svg.SvgDecoder;
import com.owncloud.android.utils.svg.SvgDrawableTranscoder;
import com.owncloud.android.utils.svg.SvgSoftwareLayerSetter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This Adapter populates a RecyclerView with all notifications for an account within the app.
 */
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder> {
    private List<Notification> mValues;
    private Context context;

    public NotificationListAdapter(Context context) {
        this.mValues = new ArrayList<>();
        this.context = context;
    }

    public void setNotificationItems(List<Notification> notificationItems) {
        mValues.clear();
        mValues.addAll(notificationItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = mValues.get(position);
        holder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context, notification.getDatetime().getTime()));

        String subject = notification.getSubject();
        if (!TextUtils.isEmpty(notification.getLink())) {
            subject = subject + " ↗";
            holder.subject.setTypeface(holder.subject.getTypeface(), Typeface.BOLD);
            holder.subject.setOnClickListener(v -> openLink(notification.getLink()));
            holder.subject.setText(subject);
        } else {
            if (!TextUtils.isEmpty(notification.subjectRich)) {
                holder.subject.setText(makeSpecialPartsBold(notification));
            } else {
                holder.subject.setText(subject);
            }
        }

        holder.message.setText(notification.getMessage());
        holder.message.setAlpha(0.57f); // TODO set in xml, once there is an own notification_list_item.xml

        // Todo set proper action icon (to be clarified how to pick)
        if (!TextUtils.isEmpty(notification.getIcon())) {
            downloadIcon(notification.getIcon(), holder.activityIcon);
        }
    }

    private SpannableStringBuilder makeSpecialPartsBold(Notification notification) {
        String text = notification.getSubjectRich();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        int openingBrace = text.indexOf("{");
        int closingBrace;
        while (openingBrace != -1) {
            closingBrace = text.indexOf("}", openingBrace) + 1;
            String replaceablePart = text.substring(openingBrace + 1, closingBrace - 1);

            RichObject richObject = notification.subjectRichParameters.get(replaceablePart);
            if (richObject != null) {
                String name = richObject.getName();
                ssb.replace(openingBrace, closingBrace, name);
                text = ssb.toString();
                closingBrace = openingBrace + name.length();

                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), openingBrace, closingBrace, 0);
                ssb.setSpan(new ForegroundColorSpan(Color.BLACK), openingBrace, closingBrace,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            openingBrace = text.indexOf("{", closingBrace);
        }

        return ssb;
    }

    private void downloadIcon(String icon, ImageView itemViewType) {
        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(context)
                .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.ic_notification)
                .error(R.drawable.ic_notification)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<>());


        Uri uri = Uri.parse(icon);
        requestBuilder
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .load(uri)
                .into(itemViewType);
    }

    private void openLink(String link) {
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView activityIcon;
        private final TextView subject;
        private final TextView message;
        private final TextView dateTime;

        private NotificationViewHolder(View itemView) {
            super(itemView);
            activityIcon = itemView.findViewById(R.id.activity_icon);
            subject = itemView.findViewById(R.id.activity_subject);
            message = itemView.findViewById(R.id.activity_message);
            dateTime = itemView.findViewById(R.id.activity_datetime);
        }
    }
}

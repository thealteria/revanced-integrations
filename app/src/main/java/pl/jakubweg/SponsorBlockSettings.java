package pl.jakubweg;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static pl.jakubweg.Helper.getStringByName;

public class SponsorBlockSettings {

    public static final String CACHE_DIRECTORY_NAME = "sponsor-block-segments-1";
    public static final String PREFERENCES_NAME = "sponsor-block";
    public static final String PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP = "show-toast";
    public static final String PREFERENCES_KEY_COUNT_SKIPS = "count-skips";
    public static final String PREFERENCES_KEY_UUID = "uuid";
    public static final String PREFERENCES_KEY_CACHE_SEGMENTS = "cache-enabled";
    public static final String PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP = "new-segment-step-accuracy";
    public static final String PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED = "sb-enabled";
    public static final String PREFERENCES_KEY_NEW_SEGMENT_ENABLED = "sb-new-segment-enabled";
    public static final String sponsorBlockSkipSegmentsUrl = "https://sponsor.ajay.app/api/skipSegments";
    public static final String sponsorBlockViewedUrl = "https://sponsor.ajay.app/api/viewedVideoSponsorTime";
    public static final SegmentBehaviour DefaultBehaviour = SegmentBehaviour.SkipAutomatically;
    public static boolean isSponsorBlockEnabled = false;
    public static boolean isAddNewSegmentEnabled = false;
    public static boolean showToastWhenSkippedAutomatically = true;
    public static boolean countSkips = true;
    public static boolean cacheEnabled = true;
    public static int adjustNewSegmentMillis = 150;
    public static String uuid = "<invalid>";
    public static File cacheDirectory;
    private static String sponsorBlockUrlCategories = "[]";

    static Context context;

    public SponsorBlockSettings(Context context) {
        SponsorBlockSettings.context = context;
    }

    public static String getSponsorBlockUrlWithCategories(String videoId) {
        return sponsorBlockSkipSegmentsUrl + "?videoID=" + videoId + "&categories=" + sponsorBlockUrlCategories;
    }

    public static String getSponsorBlockViewedUrl(String UUID) {
        return sponsorBlockViewedUrl + "?UUID=" + UUID;
    }

    public static void update(Context context) {
        if (context == null) return;
        File directory = cacheDirectory = new File(context.getCacheDir(), CACHE_DIRECTORY_NAME);
        if (!directory.mkdirs() && !directory.exists()) {
            Log.e("jakubweg.Settings", "Unable to create cache directory");
            cacheDirectory = null;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        isSponsorBlockEnabled = preferences.getBoolean(PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED, isSponsorBlockEnabled);
        if (!isSponsorBlockEnabled) {
            SkipSegmentView.hide();
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideButton();
            PlayerController.sponsorSegmentsOfCurrentVideo = null;
        } else if (isAddNewSegmentEnabled) {
            SponsorBlockUtils.showButton();
        }

        isAddNewSegmentEnabled = preferences.getBoolean(PREFERENCES_KEY_NEW_SEGMENT_ENABLED, isAddNewSegmentEnabled);
        if (!isAddNewSegmentEnabled) {
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideButton();
        } else {
            SponsorBlockUtils.showButton();
        }

        SegmentBehaviour[] possibleBehaviours = SegmentBehaviour.values();
        final ArrayList<String> enabledCategories = new ArrayList<>(possibleBehaviours.length);
        for (SegmentInfo segment : SegmentInfo.valuesWithoutPreview()) {
            SegmentBehaviour behaviour = null;
            String value = preferences.getString(segment.key, null);
            if (value == null)
                behaviour = DefaultBehaviour;
            else {
                for (SegmentBehaviour possibleBehaviour : possibleBehaviours) {
                    if (possibleBehaviour.key.equals(value)) {
                        behaviour = possibleBehaviour;
                        break;
                    }
                }
            }
            if (behaviour == null)
                behaviour = DefaultBehaviour;

            segment.behaviour = behaviour;
            if (behaviour.showOnTimeBar)
                enabledCategories.add(segment.key);
        }

        //"[%22sponsor%22,%22outro%22,%22music_offtopic%22,%22intro%22,%22selfpromo%22,%22interaction%22]";
        if (enabledCategories.size() == 0)
            sponsorBlockUrlCategories = "[]";
        else
            sponsorBlockUrlCategories = "[%22" + TextUtils.join("%22,%22", enabledCategories) + "%22]";


        showToastWhenSkippedAutomatically = preferences.getBoolean(PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP, showToastWhenSkippedAutomatically);
        cacheEnabled = preferences.getBoolean(PREFERENCES_KEY_CACHE_SEGMENTS, true);
        adjustNewSegmentMillis = Integer.parseInt(preferences
                .getString(PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP,
                        String.valueOf(adjustNewSegmentMillis)));


        uuid = preferences.getString(PREFERENCES_KEY_UUID, null);
        if (uuid == null) {
            uuid = (UUID.randomUUID().toString() +
                    UUID.randomUUID().toString() +
                    UUID.randomUUID().toString())
                    .replace("-", "");
            preferences.edit().putString(PREFERENCES_KEY_UUID, uuid).apply();
        }
    }

    public enum SegmentBehaviour {
        SkipAutomatically("skip", getStringByName(context, "skip_automatically"), true, true),
        ManualSkip("manual-skip", getStringByName(context, "skip_showbutton"), false, true),
        Ignore("ignore", getStringByName(context, "skip_ignore"), false, false);

        public final String key;
        public final String name;
        public final boolean skip;
        public final boolean showOnTimeBar;

        SegmentBehaviour(String key,
                         String name,
                         boolean skip,
                         boolean showOnTimeBar) {
            this.key = key;
            this.name = name;
            this.skip = skip;
            this.showOnTimeBar = showOnTimeBar;
        }
    }

    public enum SegmentInfo {
        Sponsor("sponsor", getStringByName(context, "segments_sponsor"), getStringByName(context, "skipped_sponsor"), getStringByName(context, "segments_sponsor_sum"), null, 0xFF00d400),
        Intro("intro", getStringByName(context, "segments_intermission"), getStringByName(context, "skipped_intermission"), getStringByName(context, "segments_intermission_sum"), null, 0xFF00ffff),
        Outro("outro", getStringByName(context, "segments_endcard"), getStringByName(context, "skipped_endcard"), getStringByName(context, "segments_endcards_sum"), null, 0xFF0202ed),
        Interaction("interaction", getStringByName(context, "segments_subscribe"), getStringByName(context, "skipped_subscribe"), getStringByName(context, "segments_subscribe_sum"), null, 0xFFcc00ff),
        SelfPromo("selfpromo", getStringByName(context, "segments_selfpromo"), getStringByName(context, "skipped_selfpromo"), getStringByName(context, "segments_selfpromo_sum"), null, 0xFFffff00),
        MusicOfftopic("music_offtopic", getStringByName(context, "segments_music"), getStringByName(context, "skipped_music"), getStringByName(context, "segments_music_sum"), null, 0xFFff9900),
        Preview("preview", "", getStringByName(context, "skipped_preview"), "", SegmentBehaviour.SkipAutomatically, 0xFF000000),
        ;

        private static SegmentInfo[] mValuesWithoutPreview = new SegmentInfo[]{
                Sponsor,
                Intro,
                Outro,
                Interaction,
                SelfPromo,
                MusicOfftopic
        };
        private static Map<String, SegmentInfo> mValuesMap = new HashMap<>(7);

        static {
            for (SegmentInfo value : valuesWithoutPreview())
                mValuesMap.put(value.key, value);
        }

        public final String key;
        public final String title;
        public final String skipMessage;
        public final String description;
        public final int color;
        public final Paint paint;
        public SegmentBehaviour behaviour;
        private CharSequence lazyTitleWithDot;

        SegmentInfo(String key,
                    String title,
                    String skipMessage,
                    String description,
                    SegmentBehaviour behaviour,
                    int color) {

            this.key = key;
            this.title = title;
            this.skipMessage = skipMessage;
            this.description = description;
            this.behaviour = behaviour;
            this.color = color & 0xFFFFFF;
            paint = new Paint();
            paint.setColor(color);
        }

        public static SegmentInfo[] valuesWithoutPreview() {
            return mValuesWithoutPreview;
        }

        public static SegmentInfo byCategoryKey(String key) {
            return mValuesMap.get(key);
        }

        public CharSequence getTitleWithDot() {
            return (lazyTitleWithDot == null) ?
                    lazyTitleWithDot = Html.fromHtml(String.format("<font color=\"#%06X\">⬤</font> %s", color, title))
                    : lazyTitleWithDot;
        }
    }
}
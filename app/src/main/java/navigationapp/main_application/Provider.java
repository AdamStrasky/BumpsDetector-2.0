package navigationapp.main_application;

import android.provider.BaseColumns;

public interface Provider {
    public interface bumps_detect extends BaseColumns {
        public static final String TABLE_NAME_BUMPS = "my_bumps";
        public static final String B_ID_BUMPS= "b_id_bumps";
        public static final String RATING = "rating";
        public static final String COUNT = "count";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String LATITUDE = "latitude";
        public static final String LONGTITUDE = "longitude";
        public static final String MANUAL = "manual";
        public static final String TYPE = "type";
        public static final String FIX = "fix";
        public static final String INFO = "info";
    }

    public interface new_bumps extends BaseColumns {
        public static final String TABLE_NAME_NEW_BUMPS = "new_bumps";
        public static final String LATITUDE = "latitude";
        public static final String LONGTITUDE = "longitude";
        public static final String INTENSITY = "intensity";
        public static final String MANUAL = "manual";
        public static final String CREATED_AT = "created_at";
        public static final String TYPE = "type";
        public static final String TEXT = "text";
    }
}

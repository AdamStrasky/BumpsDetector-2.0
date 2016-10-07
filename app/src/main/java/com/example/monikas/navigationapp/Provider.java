package com.example.monikas.navigationapp;

import android.provider.BaseColumns;

/**
 * Created by Adam on 7.10.2016.
 */

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
    }

    public interface bumps_collision extends BaseColumns {
        public static final String TABLE_NAME_COLLISIONS = "collisions";
        public static final String C_ID = "c_id";
        public static final String B_ID_COLLISIONS = "b_id_collisions";
        public static final String INTENSITY = "intensity";
        public static final String CRETED_AT = "created_at";
    }
}

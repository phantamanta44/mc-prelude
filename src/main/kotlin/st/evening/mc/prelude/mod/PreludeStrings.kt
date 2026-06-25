package st.evening.mc.prelude.mod

import st.evening.mc.prelude.PreludeConsts

object PreludeStrings {
    const val PRELUDE_KEY: String = PreludeConsts.MOD_ID

    const val GUI_KEY: String = "$PRELUDE_KEY.gui"
    const val GUI_CONFIG_ENTRIES: String = "$GUI_KEY.config_entries"
    const val GUI_CONFIG_SYNC_FAIL: String = "$GUI_KEY.config_sync_fail"

    const val ENUM_KEY: String = "$PRELUDE_KEY.enum"
    const val ENUM_BLOCK_SIDE_KEY: String = "$ENUM_KEY.block_side"
    const val ENUM_BLOCK_SIDE_NORTH: String = "$ENUM_BLOCK_SIDE_KEY.north"
    const val ENUM_BLOCK_SIDE_EAST: String = "$ENUM_BLOCK_SIDE_KEY.east"
    const val ENUM_BLOCK_SIDE_SOUTH: String = "$ENUM_BLOCK_SIDE_KEY.south"
    const val ENUM_BLOCK_SIDE_WEST: String = "$ENUM_BLOCK_SIDE_KEY.west"

    const val ENUM_RELATIVE_FACE_KEY: String = "$ENUM_KEY.relative_face"
    const val ENUM_RELATIVE_FACE_FRONT: String = "$ENUM_RELATIVE_FACE_KEY.front"
    const val ENUM_RELATIVE_FACE_LEFT: String = "$ENUM_RELATIVE_FACE_KEY.left"
    const val ENUM_RELATIVE_FACE_BACK: String = "$ENUM_RELATIVE_FACE_KEY.back"
    const val ENUM_RELATIVE_FACE_RIGHT: String = "$ENUM_RELATIVE_FACE_KEY.right"
    const val ENUM_RELATIVE_FACE_UP: String = "$ENUM_RELATIVE_FACE_KEY.up"
    const val ENUM_RELATIVE_FACE_DOWN: String = "$ENUM_RELATIVE_FACE_KEY.down"

    const val ENUM_REDSTONE_BEHAVIOUR_KEY: String = "$ENUM_KEY.redstone_behaviour"
    const val ENUM_REDSTONE_BEHAVIOUR_NONE: String = "$ENUM_REDSTONE_BEHAVIOUR_KEY.none"
    const val ENUM_REDSTONE_BEHAVIOUR_ACTIVE_LOW: String = "$ENUM_REDSTONE_BEHAVIOUR_KEY.active_low"
    const val ENUM_REDSTONE_BEHAVIOUR_ACTIVE_HIGH: String = "$ENUM_REDSTONE_BEHAVIOUR_KEY.active_high"
}

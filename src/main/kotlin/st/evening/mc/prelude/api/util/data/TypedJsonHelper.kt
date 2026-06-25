package st.evening.mc.prelude.api.util.data

import st.evening.mc.prelude.api.data.tjson.TJson

object TypedJsonHelper {
    fun mergeInPlace(mergeInto: TJson.Object, mergeFrom: TJson.Object) {
        mergeFrom.entries.forEach { (key, value) ->
            if (value is TJson.Object) {
                val existing = mergeInto[key]
                if (existing is TJson.Object) {
                    mergeInPlace(existing, value)
                    return@forEach
                }
            }
            mergeInto[key] = value.copy()
        }
    }

    fun merge(mergeInto: TJson.Object, mergeFrom: TJson.Object): TJson.Object {
        val result = mergeInto.copy()
        mergeInPlace(result, mergeFrom)
        return result
    }
}

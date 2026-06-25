package st.evening.mc.prelude.api.util.game

import net.minecraft.util.ResourceLocation

inline fun ResourceLocation.mapPath(f: (String) -> String): ResourceLocation = ResourceLocation(namespace, f(path))

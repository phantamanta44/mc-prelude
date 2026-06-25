package st.evening.mc.prelude.api.config.json

import net.minecraft.crash.CrashReport
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.ReportedException
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.apache.logging.log4j.Level
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.PreludeConfig
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.mapToTJsonArray
import st.evening.mc.prelude.api.data.tjson.toTJsonIntArray
import st.evening.mc.prelude.api.data.tjson.toTJsonStringArray
import st.evening.mc.prelude.api.event.ConfigSyncEvent
import st.evening.mc.prelude.api.event.InitializationCompleteEvent
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.type.Maybe
import st.evening.mc.prelude.api.type.orElse
import st.evening.mc.prelude.api.util.collection.RepeatList
import st.evening.mc.prelude.api.util.data.CapturePropertyName
import st.evening.mc.prelude.api.util.data.GenericDelegateProvider
import st.evening.mc.prelude.api.util.data.GenericPropertyProvider
import st.evening.mc.prelude.api.util.data.UnboundGenericProperty
import st.evening.mc.prelude.api.util.data.UnboundGenericWritableProperty
import st.evening.mc.prelude.api.util.data.ValueProperty
import st.evening.mc.prelude.api.util.data.get
import st.evening.mc.prelude.api.util.data.getEnumConstant
import st.evening.mc.prelude.api.util.data.mapEnumConstants
import st.evening.mc.prelude.api.util.data.mapToNbtList
import st.evening.mc.prelude.api.util.data.set
import st.evening.mc.prelude.api.util.data.toNbtIntArray
import st.evening.mc.prelude.api.util.data.toNbtStringList
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import java.io.File
import java.io.Writer

fun JsonConfig.defaultInit(reg: ModRegistrar, configId: String = "", changeCallback: (() -> Unit)? = null) {
    loadOrSaveToDisk()
    defaultInitConfigGui(reg, configId, changeCallback)
    defaultInitSync(reg, configId, changeCallback)
}

fun JsonConfig.defaultInitConfigGui(reg: ModRegistrar, configId: String = "", changeCallback: (() -> Unit)? = null) {
    val modId = reg.modId
    reg.on<ConfigChangedEvent.OnConfigChangedEvent> { event ->
        if (event.modID == modId && event.configID == configId) {
            saveToDisk()
            changeCallback?.invoke()
        }
    }
    onPhysicalClient {
        reg.on<InitializationCompleteEvent> { _ ->
            reg.setConfigGuiFactory(JsonConfigGuiScreen.Factory(modId, configId, "", this))
        }
    }
}

fun JsonConfig.defaultInitSync(reg: ModRegistrar, configId: String = "", changeCallback: (() -> Unit)? = null) {
    val modId = reg.modId
    reg.on<ConfigSyncEvent.Build> { event ->
        event.addConfigData(modId, configId, getSyncData())
    }
    reg.on<ConfigSyncEvent.Receive> { event ->
        event.getConfigData(modId, configId)?.let {
            if (readSyncData(it)) {
                changeCallback?.invoke()
            } else {
                event.result = Event.Result.DENY
            }
        }
    }
    onPhysicalClient {
        reg.on<FMLNetworkEvent.ClientDisconnectionFromServerEvent> { _ ->
            clearSyncData()
            changeCallback?.invoke()
        }
    }
}

class JsonConfig(private val configFile: File) {
    val root: ConfigObject = ConfigObject(null, true)

    constructor(configFileName: String) :
        this(Loader.instance().configDir.resolve("$configFileName.tjson"))

    constructor(configDir: String, configName: String) :
        this("$configDir${File.pathSeparator}$configName.tjson")

    fun saveToDisk() {
        Prelude.logger.info("Saving config to file: {}", configFile)
        configFile.bufferedWriter().use { root.writeToJson(it) }
    }

    fun loadFromDisk() {
        Prelude.logger.info("Loading config from file: {}", configFile)
        val ctx = ReadContext()
        try {
            root.readFromJson(TypedJsonParser.parseObject(configFile.readText()), JsonPath.Root, ctx)
        } catch (e: Exception) {
            throw IllegalStateException("Could not load config: $configFile", e)
        }
        if (ctx.missingKeys.isNotEmpty()) {
            when (Prelude.config.mods.missingConfigKeyLoadBehaviour) {
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.IGNORE -> {
                    if (Prelude.logger.isEnabled(Level.WARN)) {
                        Prelude.logger.warn("Ignoring missing config keys from file: {}", configFile)
                        ctx.missingKeys.forEach { (path, element) ->
                            Prelude.logger.warn("- {}: {}", path, element.getTypeName())
                        }
                    }
                }
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.OVERWRITE -> {
                    Prelude.logger.warn("Found missing config keys; overwriting config file: {}", configFile)
                    saveToDisk()
                }
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.FAIL -> {
                    val report = CrashReport.makeCrashReport(
                        IllegalStateException("Missing config keys!"),
                        "Reading config file: $configFile"
                    )
                    val category = report.makeCategory("Missing keys")
                    ctx.missingKeys.forEach { (path, element) ->
                        category.addCrashSection(path.toString(), element.getTypeName())
                    }
                    throw ReportedException(report)
                }
            }
        }
    }

    fun loadOrSaveToDisk() {
        if (configFile.exists()) {
            loadFromDisk()
        } else {
            Prelude.logger.info("No existing config on disk: {}", configFile)
            saveToDisk()
        }
    }

    fun getSyncData(): NBTTagCompound = root.getSyncValue()

    fun readSyncData(dto: NBTTagCompound): Boolean {
        Prelude.logger.info("Syncing config from server: {}", configFile)
        val ctx = ReadContext()
        try {
            root.readFromSync(dto, JsonPath.Root, ctx)
        } catch (e: Exception) {
            Prelude.logger.error("Sync failed for config: {}", configFile, e)
            return false
        }
        if (ctx.missingKeys.isNotEmpty()) {
            when (Prelude.config.mods.missingConfigKeySyncBehaviour) {
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.IGNORE -> {
                    if (Prelude.logger.isEnabled(Level.WARN)) {
                        Prelude.logger.warn("Ignoring missing config keys while syncing config: {}", configFile)
                        ctx.missingKeys.forEach { (path, element) ->
                            Prelude.logger.warn("- {}: {}", path, element.getTypeName())
                        }
                    }
                }
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.OVERWRITE -> {
                    Prelude.logger.warn("Using defaults for missing config keys while syncing config: {}", configFile)
                    ctx.missingKeys.values.forEach {
                        it.setSyncValueToDefault()
                    }
                }
                PreludeConfig.ModsConfig.MissingConfigKeyBehaviour.FAIL -> {
                    Prelude.logger.error("Found missing config keys while syncing config: {}", configFile)
                    ctx.missingKeys.forEach { (path, element) ->
                        Prelude.logger.error("- {}: {}", path, element.getTypeName())
                    }
                    return false
                }
            }
        }
        return true
    }

    fun clearSyncData() {
        Prelude.logger.info("Clearing synced config: {}", configFile)
        root.clearSyncedValue()
    }

    class ReadContext {
        val missingKeys: MutableMap<JsonPath, ConfigElement<*, *>> = mutableMapOf()
    }

    sealed class ConfigElement<J : TJson, N : NBTBase>(val description: String?, val sync: Boolean) {
        companion object { // TODO localizable config metadata
            protected fun Writer.writeDocComment(indent: String, lines: List<String>) {
                write(indent)
                write("/**\n")
                lines.forEach {
                    write(indent)
                    write(" * ")
                    write(it)
                    write("\n")
                }
                write(indent)
                write(" */\n")
            }
        }

        abstract val jsonType: Class<J>

        abstract val nbtType: Int

        abstract fun getTypeName(): String

        open fun getDocComment(lines: MutableList<String>) {
            description?.let {
                lines.addAll(it.split('\n'))
            }
        }

        abstract fun setToDefault()

        abstract fun getJsonValue(): J

        abstract fun writeToJson(out: Writer, indent: String = "")

        @Throws(JsonConfigException::class)
        abstract fun readFromJson(dto: J, path: JsonPath, ctx: ReadContext)

        @Throws(JsonConfigException::class)
        fun readFromJsonUnchecked(dto: TJson, path: JsonPath, ctx: ReadContext) {
            if (!jsonType.isInstance(dto)) {
                throw JsonConfigException(path, "Config value is not a ${getTypeName()}: $dto")
            }
            @Suppress("UNCHECKED_CAST")
            return readFromJson(dto as J, path, ctx)
        }

        abstract fun getSyncValue(): N

        @Throws(JsonConfigException::class)
        abstract fun readFromSync(dto: N, path: JsonPath, ctx: ReadContext)

        @Throws(JsonConfigException::class)
        open fun readFromSyncUnchecked(dto: NBTBase, path: JsonPath, ctx: ReadContext) {
            if (dto.id.toInt() != nbtType) {
                throw JsonConfigException(path, "Config value is not a ${getTypeName()}: $dto")
            }
            @Suppress("UNCHECKED_CAST")
            return readFromSync(dto as N, path, ctx)
        }

        abstract fun setSyncValueToDefault()

        abstract fun clearSyncedValue()
    }

    sealed class ConfigLeaf<T, J : TJson, N : NBTBase>(
        val defaultValue: T,
        description: String?,
        sync: Boolean,
        val restartRequirement: RestartRequirement
    ) : ConfigElement<J, N>(description, sync), UnboundGenericWritableProperty<T> {
        @set:Throws(JsonConfigException::class)
        var currentValue: T = defaultValue
            set(newValue) {
                validateValue(newValue)?.let { throw IllegalArgumentException(it) }
                field = newValue
            }
        var syncedValue: Maybe<T> = Maybe.Nothing()

        val effectiveValue: T
            get() = syncedValue.orElse { currentValue }

        open fun validateValue(value: T): String? = null

        override fun getPropertyValue(): T = effectiveValue

        override fun setPropertyValue(newValue: T) {
            currentValue = newValue // no validation! we assume the caller knows what they're doing
        }

        override fun getDocComment(lines: MutableList<String>) {
            super.getDocComment(lines)
            lines += "@type ${getTypeName()}"
            // no reason to write restart conditions in the comments; they're only useful in-game
        }

        override fun setToDefault() {
            currentValue = defaultValue
        }

        override fun writeToJson(out: Writer, indent: String) {
            getJsonValue().writePrettyString(out, indent)
        }

        @Throws(JsonConfigException::class)
        override fun readFromJson(dto: J, path: JsonPath, ctx: ReadContext) {
            readUnvalidated(dto, path, ctx)
            validateValue(currentValue)?.let { throw JsonConfigException(path, it) }
        }

        @Throws(JsonConfigException::class)
        abstract fun readUnvalidated(dto: J, path: JsonPath, ctx: ReadContext)

        @Throws(JsonConfigException::class)
        override fun readFromSyncUnchecked(dto: NBTBase, path: JsonPath, ctx: ReadContext) {
            super.readFromSyncUnchecked(dto, path, ctx)
            if (restartRequirement == RestartRequirement.REQUIRES_MC_RESTART && currentValue != effectiveValue) {
                throw JsonConfigException(path, "Mismatched config value received from server: $dto")
            }
        }

        override fun setSyncValueToDefault() {
            syncedValue = Maybe.Just(defaultValue)
        }

        override fun clearSyncedValue() {
            syncedValue = Maybe.Nothing()
        }
    }

    sealed class ConfigLeafArray<T : List<*>, J : TJson, N : NBTBase>(
        defaultValue: T,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val sizeLimit: ListSizeLimit?
    ) : ConfigLeaf<T, J, N>(defaultValue, description, sync, restartRequirement) {
        override fun validateValue(value: T): String? = sizeLimit?.validateValue(value)
    }

    class ConfigObject(
        description: String?,
        sync: Boolean = true
    ) : ConfigElement<TJson.Object, NBTTagCompound>(description, sync) {
        val entries: Map<String, ConfigElement<*, *>>
            field: MutableMap<String, ConfigElement<*, *>> = LinkedHashMap()

        override val jsonType: Class<TJson.Object>
            get() = TJson.Object::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_COMPOUND

        override fun getTypeName(): String = "config category"

        private fun <T : ConfigElement<*, *>> T.put(key: String): T {
            check(key !in entries) { "Duplicate config key: $key" }
            entries[key] = this
            return this
        }

        fun putObj(key: String, description: String? = null, sync: Boolean = true): ConfigObject =
            ConfigObject(description, sync).put(key)

        fun <T> obj(
            description: String? = null,
            sync: Boolean = true,
            factory: (ConfigObject) -> T
        ): GenericPropertyProvider<T> = CapturePropertyName { ValueProperty(factory(putObj(it, description, sync))) }

        fun <T> putObjArray(
            key: String,
            typeName: String,
            factory: (ConfigObject) -> T,
            defaultSize: Int,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null
        ): ConfigObjectArray<T> =
            ConfigObjectArray(typeName, factory, defaultSize, description, sync, restartRequirement, sizeLimit).put(key)

        fun <T> objArray(
            typeName: String,
            factory: (ConfigObject) -> T,
            defaultSize: Int,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null
        ): GenericDelegateProvider<ConfigObjectArray<T>> = CapturePropertyName {
            putObjArray(it, typeName, factory, defaultSize, description, sync, restartRequirement, sizeLimit)
        }

        fun <T> putObjArray(
            key: String,
            typeName: String,
            factory: (ConfigObject) -> T,
            defaultConfigurators: List<(T) -> Unit>,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null
        ): ConfigObjectArray<T> =
            ConfigObjectArray(typeName, factory, defaultConfigurators, description, sync, restartRequirement, sizeLimit)
                .put(key)

        fun <T> objArray(
            typeName: String,
            factory: (ConfigObject) -> T,
            defaultConfigurators: List<(T) -> Unit>,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null
        ): GenericDelegateProvider<ConfigObjectArray<T>> = CapturePropertyName {
            putObjArray(it, typeName, factory, defaultConfigurators, description, sync, restartRequirement, sizeLimit)
        }

        fun putStringArray(
            key: String,
            defaultValue: List<String> = emptyList(),
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null,
            validPattern: Regex? = null
        ): ConfigStringArray =
            ConfigStringArray(defaultValue, description, sync, restartRequirement, sizeLimit, validPattern).put(key)

        fun stringArray(
            defaultValue: List<String> = emptyList(),
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null,
            validPattern: Regex? = null
        ): GenericDelegateProvider<ConfigStringArray> = CapturePropertyName {
            putStringArray(it, defaultValue, description, sync, restartRequirement, sizeLimit, validPattern)
        }

        fun putIntArray(
            key: String,
            defaultValue: List<Int> = emptyList(),
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null,
            minValue: Int? = null,
            maxValue: Int? = null
        ): ConfigIntArray =
            ConfigIntArray(defaultValue, description, sync, restartRequirement, sizeLimit, minValue, maxValue).put(key)

        fun intArray(
            defaultValue: List<Int> = emptyList(),
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            sizeLimit: ListSizeLimit? = null,
            minValue: Int? = null,
            maxValue: Int? = null
        ): GenericDelegateProvider<ConfigIntArray> = CapturePropertyName {
            putIntArray(it, defaultValue, description, sync, restartRequirement, sizeLimit, minValue, maxValue)
        }

        fun putString(
            key: String,
            defaultValue: String,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            validPattern: Regex? = null
        ): ConfigString = ConfigString(defaultValue, description, sync, restartRequirement, validPattern).put(key)

        fun string(
            defaultValue: String,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            validPattern: Regex? = null
        ): GenericDelegateProvider<ConfigString> =
            CapturePropertyName { putString(it, defaultValue, description, sync, restartRequirement, validPattern) }

        fun <E : Enum<E>> putEnum(
            key: String,
            defaultValue: E,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE
        ): ConfigEnum<E> = ConfigEnum(defaultValue, description, sync, restartRequirement).put(key)

        fun <E : Enum<E>> enum(
            defaultValue: E,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE
        ): GenericDelegateProvider<ConfigEnum<E>> =
            CapturePropertyName { putEnum(it, defaultValue, description, sync, restartRequirement) }

        fun putBool(
            key: String,
            defaultValue: Boolean,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE
        ): ConfigBool = ConfigBool(defaultValue, description, sync, restartRequirement).put(key)

        fun bool(
            defaultValue: Boolean,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE
        ): GenericDelegateProvider<ConfigBool> =
            CapturePropertyName { putBool(it, defaultValue, description, sync, restartRequirement) }

        fun putByte(
            key: String,
            defaultValue: Byte,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Byte? = null,
            maxValue: Byte? = null
        ): ConfigByte = ConfigByte(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun byte(
            defaultValue: Byte,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Byte? = null,
            maxValue: Byte? = null
        ): GenericDelegateProvider<ConfigByte> =
            CapturePropertyName { putByte(it, defaultValue, description, sync, restartRequirement, minValue, maxValue) }

        fun putShort(
            key: String,
            defaultValue: Short,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Short? = null,
            maxValue: Short? = null
        ): ConfigShort = ConfigShort(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun short(
            defaultValue: Short,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Short? = null,
            maxValue: Short? = null
        ): GenericDelegateProvider<ConfigShort> = CapturePropertyName {
            putShort(it, defaultValue, description, sync, restartRequirement, minValue, maxValue)
        }

        fun putInt(
            key: String,
            defaultValue: Int,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Int? = null,
            maxValue: Int? = null
        ): ConfigInt = ConfigInt(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun int(
            defaultValue: Int,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Int? = null,
            maxValue: Int? = null
        ): GenericDelegateProvider<ConfigInt> =
            CapturePropertyName { putInt(it, defaultValue, description, sync, restartRequirement, minValue, maxValue) }

        fun putLong(
            key: String,
            defaultValue: Long,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Long? = null,
            maxValue: Long? = null
        ): ConfigLong = ConfigLong(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun long(
            defaultValue: Long,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Long? = null,
            maxValue: Long? = null
        ): GenericDelegateProvider<ConfigLong> =
            CapturePropertyName { putLong(it, defaultValue, description, sync, restartRequirement, minValue, maxValue) }

        fun putFloat(
            key: String,
            defaultValue: Float,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Float? = null,
            maxValue: Float? = null
        ): ConfigFloat = ConfigFloat(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun float(
            defaultValue: Float,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Float? = null,
            maxValue: Float? = null
        ): GenericDelegateProvider<ConfigFloat> = CapturePropertyName {
            putFloat(it, defaultValue, description, sync, restartRequirement, minValue, maxValue)
        }

        fun putDouble(
            key: String,
            defaultValue: Double,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Double? = null,
            maxValue: Double? = null
        ): ConfigDouble = ConfigDouble(defaultValue, description, sync, restartRequirement, minValue, maxValue).put(key)

        fun double(
            defaultValue: Double,
            description: String? = null,
            sync: Boolean = true,
            restartRequirement: RestartRequirement = RestartRequirement.NONE,
            minValue: Double? = null,
            maxValue: Double? = null
        ): GenericDelegateProvider<ConfigDouble> = CapturePropertyName {
            putDouble(it, defaultValue, description, sync, restartRequirement, minValue, maxValue)
        }

        override fun setToDefault() {
            entries.values.forEach { it.setToDefault() }
        }

        override fun getJsonValue(): TJson.Object = TJson.Object().also { obj ->
            entries.forEach { (key, element) -> obj[key] = element.getJsonValue() }
        }

        override fun writeToJson(out: Writer, indent: String) {
            if (entries.isEmpty()) {
                out.write("{}")
                return
            }
            out.write("{\n")
            val newIndent = "$indent  "
            val iter = entries.iterator()
            while (true) {
                val (key, elem) = iter.next()
                val docComment = mutableListOf<String>()
                elem.getDocComment(docComment)
                if (docComment.isNotEmpty()) {
                    out.writeDocComment(newIndent, docComment)
                }
                out.write(newIndent)
                out.write(TJson.escapeJsonString(key))
                out.write(": ")
                elem.writeToJson(out, newIndent)
                if (!iter.hasNext()) break
                out.write(",\n\n")
            }
            out.write("\n")
            out.write(indent)
            out.write("}")
        }

        @Throws(JsonConfigException::class)
        override fun readFromJson(dto: TJson.Object, path: JsonPath, ctx: ReadContext) {
            entries.forEach { (key, element) ->
                val elementDto = dto[key]
                if (elementDto == null) {
                    ctx.missingKeys[path / key] = element
                    return@forEach
                }
                element.readFromJsonUnchecked(elementDto, path / key, ctx)
            }
        }

        override fun getSyncValue(): NBTTagCompound = NBTTagCompound().also { dto ->
            entries.forEach { (key, element) ->
                if (element.sync) {
                    dto[key] = element.getSyncValue()
                }
            }
        }

        @Throws(JsonConfigException::class)
        override fun readFromSync(dto: NBTTagCompound, path: JsonPath, ctx: ReadContext) {
            entries.forEach { (key, element) ->
                if (element.sync) {
                    val elementDto = dto[key]
                    if (elementDto == null) {
                        ctx.missingKeys[path / key] = element
                        return@forEach
                    }
                    element.readFromSyncUnchecked(elementDto, path / key, ctx)
                }
            }
        }

        override fun setSyncValueToDefault() {
            entries.values.forEach { it.setSyncValueToDefault() }
        }

        override fun clearSyncedValue() {
            entries.values.forEach { it.clearSyncedValue() }
        }
    }

    class ConfigObjectArray<T>(
        private val _typeName: String,
        private val valueFactory: (ConfigObject) -> T,
        private val defaultConfigurators: List<(T) -> Unit>,
        description: String?,
        sync: Boolean,
        val restartRequirement: RestartRequirement,
        val sizeLimit: ListSizeLimit?
    ) : ConfigElement<TJson.Array, NBTTagList>(description, sync),
        UnboundGenericProperty<ConfigObjectArray.EntryList<T>> {

        var entries: List<Entry<T>> = createDefaultEntries()
        private var syncedEntries: List<Entry<T>>? = null

        constructor(
            typeName: String,
            valueFactory: (ConfigObject) -> T,
            defaultSize: Int,
            description: String?,
            sync: Boolean,
            restartRequirement: RestartRequirement,
            sizeLimit: ListSizeLimit?
        ) : this(typeName, valueFactory, RepeatList({}, defaultSize), description, sync, restartRequirement, sizeLimit)

        override val jsonType: Class<TJson.Array>
            get() = TJson.Array::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_LIST

        val values: EntryList<T> = EntryListImpl()

        override fun getTypeName(): String = _typeName

        override fun getPropertyValue(): EntryList<T> = values

        fun createDefaultEntries(): List<Entry<T>> = defaultConfigurators.map { configure ->
            val configObj = ConfigObject(null, true)
            val value = valueFactory(configObj)
            configure(value)
            return@map Entry(configObj, value)
        }

        fun createNewEntry(): Entry<T> {
            val configObj = ConfigObject(null, true)
            return Entry(configObj, valueFactory(configObj))
        }

        override fun setToDefault() {
            entries = createDefaultEntries()
        }

        override fun getJsonValue(): TJson.Array = entries.mapToTJsonArray { it.configObj.getJsonValue() }

        override fun writeToJson(out: Writer, indent: String) {
            if (entries.isEmpty()) {
                out.write("[]")
                return
            }
            out.write("[\n")
            val newIndent = "$indent  "
            val iter = entries.iterator()
            while (true) {
                val elem = iter.next().configObj
                out.write(newIndent)
                elem.writeToJson(out, newIndent)
                if (!iter.hasNext()) break
                out.write(",\n")
            }
            out.write("\n")
            out.write(indent)
            out.write("]")
        }

        override fun readFromJson(dto: TJson.Array, path: JsonPath, ctx: ReadContext) {
            entries = dto.children.mapIndexed { index, valueDto ->
                val configObj = ConfigObject(null, true)
                val value = valueFactory(configObj)
                configObj.readFromJsonUnchecked(valueDto, path / index, ctx)
                return@mapIndexed Entry(configObj, value)
            }
        }

        override fun getSyncValue(): NBTTagList = entries.mapToNbtList { it.configObj.getSyncValue() }

        override fun readFromSync(dto: NBTTagList, path: JsonPath, ctx: ReadContext) {
            if (entries.size == dto.tagCount()) {
                syncedEntries = entries
                dto.forEachIndexed { index, valueDto ->
                    entries[index].configObj.readFromSyncUnchecked(valueDto, path / index, ctx)
                }
            } else if (restartRequirement == RestartRequirement.REQUIRES_MC_RESTART) {
                throw JsonConfigException(
                    path, "Mismatched config array length received from server: ${dto.tagCount()}"
                )
            } else {
                syncedEntries = dto.mapIndexed { index, valueDto ->
                    val configObj = ConfigObject(null, true)
                    val value = valueFactory(configObj)
                    configObj.readFromSyncUnchecked(valueDto, path / index, ctx)
                    return@mapIndexed Entry(configObj, value)
                }
            }
        }

        override fun setSyncValueToDefault() {
            if (entries.size == defaultConfigurators.size) {
                syncedEntries = entries
                entries.forEach { it.configObj.setSyncValueToDefault() }
            } else {
                syncedEntries = createDefaultEntries()
            }
        }

        override fun clearSyncedValue() {
            entries.forEach { it.configObj.clearSyncedValue() }
            syncedEntries = null
        }

        class Entry<T>(val configObj: ConfigObject, val value: T)

        interface EntryList<T> : List<T> {
            fun configure(configurators: List<(T) -> Unit>)

            fun configure(size: Int, configurator: (T) -> Unit) {
                configure(RepeatList(configurator, size))
            }

            fun clear() {
                configure(emptyList())
            }
        }

        private inner class EntryListImpl : AbstractList<T>(), EntryList<T> {
            override val size: Int
                get() = entries.size

            override fun get(index: Int): T = entries[index].value

            override fun configure(configurators: List<(T) -> Unit>) {
                entries = configurators.map { configure ->
                    val configObj = ConfigObject(null, true)
                    val value = valueFactory(configObj)
                    configure(value)
                    return@map Entry(configObj, value)
                }
            }
        }
    }

    class ConfigStringArray(
        defaultValue: List<String>,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        sizeLimit: ListSizeLimit?,
        val validPattern: Regex?,
    ) : ConfigLeafArray<List<String>, TJson.Array, NBTTagList>(
        defaultValue, description, sync, restartRequirement, sizeLimit
    ) {
        override val jsonType: Class<TJson.Array>
            get() = TJson.Array::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_LIST

        override fun getTypeName(): String = validPattern?.let { "list of strings matching $it" } ?: "list of strings"

        override fun validateValue(value: List<String>): String? {
            super.validateValue(value)?.let { return it }
            validPattern?.let { pattern ->
                value.forEach {
                    if (!pattern.matches(it)) {
                        return "Entry does not match $pattern: $it"
                    }
                }
            }
            return null
        }

        override fun getJsonValue(): TJson.Array = currentValue.toTJsonStringArray()

        override fun readUnvalidated(dto: TJson.Array, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.children.map {
                (it as? TJson.String ?: throw JsonConfigException(path, "Value is not a string: $it")).value
            }
        }

        override fun getSyncValue(): NBTTagList = effectiveValue.toNbtStringList()

        override fun readFromSync(dto: NBTTagList, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(
                dto.map {
                    (it as? NBTTagString ?: throw JsonConfigException(path, "Value is not a string: $it")).string
                }
            )
        }
    }

    class ConfigIntArray(
        defaultValue: List<Int>,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        sizeLimit: ListSizeLimit?,
        val minValue: Int?,
        val maxValue: Int?
    ) : ConfigLeafArray<List<Int>, TJson.Array, NBTTagIntArray>(
        defaultValue, description, sync, restartRequirement, sizeLimit
    ) {
        override val jsonType: Class<TJson.Array>
            get() = TJson.Array::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_INT_ARRAY

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "list of integers between $minValue and $maxValue"
            } else {
                "list of integers with minimum $minValue"
            }
        } else if (maxValue != null) {
            "list of integers with maximum $maxValue"
        } else {
            "list of integers"
        }

        override fun validateValue(value: List<Int>): String? {
            super.validateValue(value)?.let { return it }
            if (minValue != null || maxValue != null) {
                value.forEach { num ->
                    minValue?.let {
                        if (num < it) return "Value below minimum of $it: $num"
                    }
                    maxValue?.let {
                        if (num > it) return "Value above maximum of $it: $num"
                    }
                }
            }
            return null
        }

        override fun getJsonValue(): TJson.Array = currentValue.toTJsonIntArray()

        override fun readUnvalidated(dto: TJson.Array, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.children.map {
                (it as? TJson.Int ?: throw JsonConfigException(path, "Value is not an integer: $it")).value
            }
        }

        override fun getSyncValue(): NBTTagIntArray = effectiveValue.toNbtIntArray()

        override fun readFromSync(dto: NBTTagIntArray, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.intArray.toList())
        }
    }

    class ConfigString(
        defaultValue: String,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val validPattern: Regex?
    ) : ConfigLeaf<String, TJson.String, NBTTagString>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.String>
            get() = TJson.String::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_STRING

        override fun getTypeName(): String = validPattern?.let { "string matching $it" } ?: "string"

        override fun validateValue(value: String): String? {
            validPattern?.let { pattern ->
                if (!pattern.matches(value)) return "Value does not match $pattern: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.String = TJson.String(currentValue)

        override fun readUnvalidated(dto: TJson.String, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagString = NBTTagString(effectiveValue)

        override fun readFromSync(dto: NBTTagString, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.string)
        }
    }

    class ConfigEnum<E : Enum<E>>(
        defaultValue: E,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement
    ) : ConfigLeaf<E, TJson.String, NBTTagByte>(defaultValue, description, sync, restartRequirement) {
        val enumType: Class<E> = defaultValue.declaringJavaClass
        val valueStrings: List<String> = enumType.mapEnumConstants { it.toString() }

        override val jsonType: Class<TJson.String>
            get() = TJson.String::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_BYTE

        override fun getTypeName(): String = valueStrings.joinToString(", ", "[", "]")

        override fun getJsonValue(): TJson.String = TJson.String(currentValue.toString())

        override fun readUnvalidated(dto: TJson.String, path: JsonPath, ctx: ReadContext) {
            try {
                currentValue = enumType.getEnumConstant(dto.value)
            } catch (_: IllegalArgumentException) {
                throw JsonConfigException(path, "No such ${enumType.simpleName} value: ${dto.value}")
            }
        }

        override fun getSyncValue(): NBTTagByte = NBTTagByte(effectiveValue.ordinal.toByte())

        override fun readFromSync(dto: NBTTagByte, path: JsonPath, ctx: ReadContext) {
            try {
                syncedValue = Maybe.Just(enumType.getEnumConstant(dto.byte.toInt()))
            } catch (_: IndexOutOfBoundsException) {
                throw JsonConfigException(path, "Out-of-bounds index for ${enumType.simpleName}: ${dto.byte}")
            }
        }
    }

    class ConfigBool(
        defaultValue: Boolean,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
    ) : ConfigLeaf<Boolean, TJson.Bool, NBTTagByte>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Bool>
            get() = TJson.Bool::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_BYTE

        override fun getTypeName(): String = "boolean"

        override fun getJsonValue(): TJson.Bool = TJson.Bool(currentValue)

        override fun readUnvalidated(dto: TJson.Bool, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagByte = NBTTagByte(if (effectiveValue) 1 else 0)

        override fun readFromSync(dto: NBTTagByte, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.byte != 0.toByte())
        }
    }

    class ConfigByte(
        defaultValue: Byte,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Byte?,
        val maxValue: Byte?
    ) : ConfigLeaf<Byte, TJson.Byte, NBTTagByte>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Byte>
            get() = TJson.Byte::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_BYTE

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "byte between $minValue and $maxValue"
            } else {
                "byte with minimum $minValue"
            }
        } else if (maxValue != null) {
            "byte with maximum $maxValue"
        } else {
            "byte"
        }

        override fun validateValue(value: Byte): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Byte = TJson.Byte(currentValue)

        override fun readUnvalidated(dto: TJson.Byte, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagByte = NBTTagByte(effectiveValue)

        override fun readFromSync(dto: NBTTagByte, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.byte)
        }
    }

    class ConfigShort(
        defaultValue: Short,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Short?,
        val maxValue: Short?
    ) : ConfigLeaf<Short, TJson.Short, NBTTagShort>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Short>
            get() = TJson.Short::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_SHORT

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "short between $minValue and $maxValue"
            } else {
                "short with minimum $minValue"
            }
        } else if (maxValue != null) {
            "short with maximum $maxValue"
        } else {
            "short"
        }

        override fun validateValue(value: Short): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Short = TJson.Short(currentValue)

        override fun readUnvalidated(dto: TJson.Short, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagShort = NBTTagShort(effectiveValue)

        override fun readFromSync(dto: NBTTagShort, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.short)
        }
    }

    class ConfigInt(
        defaultValue: Int,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Int?,
        val maxValue: Int?
    ) : ConfigLeaf<Int, TJson.Int, NBTTagInt>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Int>
            get() = TJson.Int::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_INT

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "integer between $minValue and $maxValue"
            } else {
                "integer with minimum $minValue"
            }
        } else if (maxValue != null) {
            "integer with maximum $maxValue"
        } else {
            "integer"
        }

        override fun validateValue(value: Int): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Int = TJson.Int(currentValue)

        override fun readUnvalidated(dto: TJson.Int, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagInt = NBTTagInt(effectiveValue)

        override fun readFromSync(dto: NBTTagInt, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.int)
        }
    }

    class ConfigLong(
        defaultValue: Long,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Long?,
        val maxValue: Long?
    ) : ConfigLeaf<Long, TJson.Long, NBTTagLong>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Long>
            get() = TJson.Long::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_LONG

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "long between $minValue and $maxValue"
            } else {
                "long with minimum $minValue"
            }
        } else if (maxValue != null) {
            "long with maximum $maxValue"
        } else {
            "long"
        }

        override fun validateValue(value: Long): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Long = TJson.Long(currentValue)

        override fun readUnvalidated(dto: TJson.Long, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagLong = NBTTagLong(effectiveValue)

        override fun readFromSync(dto: NBTTagLong, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.long)
        }
    }

    class ConfigFloat(
        defaultValue: Float,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Float?,
        val maxValue: Float?
    ) : ConfigLeaf<Float, TJson.Float, NBTTagFloat>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Float>
            get() = TJson.Float::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_FLOAT

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "float between $minValue and $maxValue"
            } else {
                "float with minimum $minValue"
            }
        } else if (maxValue != null) {
            "float with maximum $maxValue"
        } else {
            "float"
        }

        override fun validateValue(value: Float): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Float = TJson.Float(currentValue)

        override fun readUnvalidated(dto: TJson.Float, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagFloat = NBTTagFloat(effectiveValue)

        override fun readFromSync(dto: NBTTagFloat, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.float)
        }
    }

    class ConfigDouble(
        defaultValue: Double,
        description: String?,
        sync: Boolean,
        restartRequirement: RestartRequirement,
        val minValue: Double?,
        val maxValue: Double?
    ) : ConfigLeaf<Double, TJson.Double, NBTTagDouble>(defaultValue, description, sync, restartRequirement) {
        override val jsonType: Class<TJson.Double>
            get() = TJson.Double::class.java
        override val nbtType: Int
            get() = Constants.NBT.TAG_DOUBLE

        override fun getTypeName(): String = if (minValue != null) {
            if (maxValue != null) {
                "double between $minValue and $maxValue"
            } else {
                "double with minimum $minValue"
            }
        } else if (maxValue != null) {
            "double with maximum $maxValue"
        } else {
            "double"
        }

        override fun validateValue(value: Double): String? {
            minValue?.let {
                if (value < it) return "Value below minimum of $it: $value"
            }
            maxValue?.let {
                if (value > it) return "Value above maximum of $it: $value"
            }
            return null
        }

        override fun getJsonValue(): TJson.Double = TJson.Double(currentValue)

        override fun readUnvalidated(dto: TJson.Double, path: JsonPath, ctx: ReadContext) {
            currentValue = dto.value
        }

        override fun getSyncValue(): NBTTagDouble = NBTTagDouble(effectiveValue)

        override fun readFromSync(dto: NBTTagDouble, path: JsonPath, ctx: ReadContext) {
            syncedValue = Maybe.Just(dto.double)
        }
    }

    sealed interface ListSizeLimit {
        val limit: Int

        fun <T> validateValue(list: List<T>): String?

        class MaxSize(override val limit: Int) : ListSizeLimit {
            override fun <T> validateValue(list: List<T>): String? {
                if (list.size > limit) {
                    return "List can have at most $limit elements, but has ${list.size}!"
                }
                return null
            }
        }

        class FixedSize(override val limit: Int) : ListSizeLimit {
            override fun <T> validateValue(list: List<T>): String? {
                if (list.size != limit) {
                    return "List must have $limit elements, but has ${list.size}!"
                }
                return null
            }
        }
    }

    enum class RestartRequirement {
        NONE, REQUIRES_WORLD_RESTART, REQUIRES_MC_RESTART
    }
}

// this overlaps a bit with SerializationException
// this one is supposed to be checked, though... if only kotlin actually had that feature
class JsonConfigException(path: JsonPath, message: String) : Exception("$path: $message")

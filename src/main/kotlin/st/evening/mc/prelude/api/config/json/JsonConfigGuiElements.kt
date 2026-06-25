package st.evening.mc.prelude.api.config.json

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.fml.client.config.ConfigGuiType
import net.minecraftforge.fml.client.config.GuiButtonExt
import net.minecraftforge.fml.client.config.GuiConfig
import net.minecraftforge.fml.client.config.GuiConfigEntries
import net.minecraftforge.fml.client.config.GuiEditArray
import net.minecraftforge.fml.client.config.GuiEditArrayEntries
import net.minecraftforge.fml.client.config.IConfigElement
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.util.data.getEnumConstant
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.coremod.mixin.GuiEditArrayAccessor
import st.evening.mc.prelude.mod.PreludeStrings
import java.util.regex.Pattern

@ClientSide.Physical
abstract class AbstractJsonConfigElement<J : JsonConfig.ConfigElement<*, *>>(
    private val _name: String,
    private val _qualifiedName: String,
    protected val element: J
) : IConfigElement {
    override fun getConfigEntryClass(): Class<out GuiConfigEntries.IConfigEntry>? = null
    override fun getArrayEntryClass(): Class<out GuiEditArrayEntries.IArrayEntry>? = null
    override fun getName(): String = _name
    override fun getQualifiedName(): String = _qualifiedName
    override fun getLanguageKey(): String = ""
    override fun getComment(): String? = element.description
    override fun showInGui(): Boolean = true

    override fun isList(): Boolean = false
    override fun isListLengthFixed(): Boolean = false
    override fun getMaxListLength(): Int = -1
    override fun getMinValue(): Any? = getMinimum() ?: Integer.MIN_VALUE // forge GUI assumes this is non-null sometimes
    override fun getMaxValue(): Any? = getMaximum() ?: Integer.MAX_VALUE
    override fun getValidValues(): Array<out String>? = null
    override fun getValidationPattern(): Pattern? = null

    open fun getMinimum(): Any? = null
    open fun getMaximum(): Any? = null

    override fun setToDefault() {
        element.setToDefault()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <J : JsonConfig.ConfigElement<*, *>> show(
            name: String,
            qualifiedName: String,
            element: J
        ): AbstractJsonConfigElement<J> = when (element) {
            is JsonConfig.ConfigObject -> JsonConfigObjectElement(name, qualifiedName, element)
            is JsonConfig.ConfigObjectArray<*> -> JsonConfigObjectArrayElement(name, qualifiedName, element)
            is JsonConfig.ConfigStringArray -> JsonConfigStringArrayElement(name, qualifiedName, element)
            is JsonConfig.ConfigIntArray -> JsonConfigIntArrayElement(name, qualifiedName, element)
            is JsonConfig.ConfigString -> JsonConfigStringElement(name, qualifiedName, element)
            is JsonConfig.ConfigEnum<*> -> JsonConfigEnumElement(name, qualifiedName, element)
            is JsonConfig.ConfigBool -> JsonConfigBoolElement(name, qualifiedName, element)
            is JsonConfig.ConfigByte -> JsonConfigByteElement(name, qualifiedName, element)
            is JsonConfig.ConfigShort -> JsonConfigShortElement(name, qualifiedName, element)
            is JsonConfig.ConfigInt -> JsonConfigIntElement(name, qualifiedName, element)
            is JsonConfig.ConfigLong -> JsonConfigLongElement(name, qualifiedName, element)
            is JsonConfig.ConfigFloat -> JsonConfigFloatElement(name, qualifiedName, element)
            is JsonConfig.ConfigDouble -> JsonConfigDoubleElement(name, qualifiedName, element)
        } as AbstractJsonConfigElement<J> // sound by narrowing on J

        fun findConfigScreenModId(screen: GuiScreen?): String = when (screen) {
            is GuiConfig -> screen.modID
            is GuiEditArray -> findConfigScreenModId((screen as GuiEditArrayAccessor).`prelude$parentScreen`())
            // could also add a case for GuiSelectString, but we don't need it for now
            else -> throw NoSuchElementException("Could not find mod ID for config GUI screen!")
        }
    }
}

@ClientSide.Physical
abstract class AbstractJsonConfigLeafElement<T, J : JsonConfig.ConfigLeaf<T, *, *>>(
    name: String,
    qualifiedName: String,
    element: J
) : AbstractJsonConfigElement<J>(name, qualifiedName, element) {
    override fun isProperty(): Boolean = true
    override fun getChildElements(): List<IConfigElement>? = null

    override fun isDefault(): Boolean = element.currentValue == element.defaultValue
    override fun getDefault(): Any? = element.defaultValue
    override fun getDefaults(): Array<out Any?>? = null

    override fun get(): Any? = element.currentValue
    override fun getList(): Array<out Any?>? = null

    override fun set(aVal: Array<out Any>) {}

    override fun requiresWorldRestart(): Boolean =
        element.restartRequirement == JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART

    override fun requiresMcRestart(): Boolean =
        element.restartRequirement == JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
}

@ClientSide.Physical
abstract class AbstractJsonConfigLeafArrayElement<T : List<*>, J : JsonConfig.ConfigLeafArray<T, *, *>>(
    name: String,
    qualifiedName: String,
    element: J
) : AbstractJsonConfigLeafElement<T, J>(name, qualifiedName, element) {
    override fun getDefault(): Any? = element.defaultValue.joinToString(", ", "[", "]")
    override fun getDefaults(): Array<out Any?>? = element.defaultValue.toTypedArray()
    override fun isList(): Boolean = true
    override fun isListLengthFixed(): Boolean = element.sizeLimit is JsonConfig.ListSizeLimit.FixedSize
    override fun getMaxListLength(): Int = element.sizeLimit?.limit ?: -1

    override fun getList(): Array<out Any?>? = element.currentValue.toTypedArray()

    override fun set(value: Any) {}
    abstract override fun set(aVal: Array<out Any>)
}

@ClientSide.Physical
class JsonConfigObjectElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigObject
) : AbstractJsonConfigElement<JsonConfig.ConfigObject>(name, qualifiedName, element) {
    private val _childElements: List<IConfigElement> = element.entries.map { (key, elem) ->
        show(key, "$qualifiedName${Configuration.CATEGORY_SPLITTER}$key", elem)
    }

    override fun isProperty(): Boolean = false
    override fun getChildElements(): List<IConfigElement?> = _childElements
    override fun getType(): ConfigGuiType = ConfigGuiType.CONFIG_CATEGORY
    override fun requiresWorldRestart(): Boolean = false
    override fun requiresMcRestart(): Boolean = false

    override fun isDefault(): Boolean = false
    override fun getDefault(): Any? = null
    override fun getDefaults(): Array<out Any?>? = null
    override fun get(): Any? = null
    override fun getList(): Array<out Any?>? = null

    override fun set(value: Any) {}
    override fun set(aVal: Array<out Any>) {}
}

@ClientSide.Physical
class JsonConfigObjectArrayElement<T>(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigObjectArray<T>
) : AbstractJsonConfigElement<JsonConfig.ConfigObjectArray<T>>(name, qualifiedName, element) {
    private val defaultsArray: Array<JsonConfig.ConfigObjectArray.Entry<T>> =
        element.createDefaultEntries().toTypedArray()

    override fun isProperty(): Boolean = true
    override fun getChildElements(): List<IConfigElement?>? = null
    override fun getType(): ConfigGuiType = ConfigGuiType.CONFIG_CATEGORY

    override fun requiresWorldRestart(): Boolean =
        element.restartRequirement == JsonConfig.RestartRequirement.REQUIRES_MC_RESTART

    override fun requiresMcRestart(): Boolean =
        element.restartRequirement == JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART

    override fun isDefault(): Boolean = false // rather annoying to check

    override fun getDefault(): Any = if (defaultsArray.isEmpty()) "[]" else "[${defaultsArray.size}...]"

    override fun getDefaults(): Array<out Any?> = defaultsArray

    override fun isList(): Boolean = true

    override fun isListLengthFixed(): Boolean = element.sizeLimit is JsonConfig.ListSizeLimit.FixedSize

    override fun getMaxListLength(): Int = element.sizeLimit?.limit ?: -1

    override fun get(): Any = element.entries

    override fun getList(): Array<out Any?> = element.entries.toTypedArray()

    override fun set(value: Any?) {}

    override fun set(aVal: Array<out Any>) {
        if (aVal.isEmpty()) {
            element.entries = emptyList()
        } else if (aVal[0] !is JsonConfig.ConfigObjectArray.Entry<*>) {
            Prelude.logger.error(
                "JSON config array $qualifiedName got invalid data from the Forge config GUI! There may be data loss!"
            )
        } else {
            @Suppress("UNCHECKED_CAST") // we'll just have to hope that the caller got the type right
            element.entries = aVal.toList() as List<JsonConfig.ConfigObjectArray.Entry<T>>
        }
    }

    override fun getConfigEntryClass(): Class<out GuiConfigEntries.IConfigEntry> = ArrayEntry::class.java

    override fun getArrayEntryClass(): Class<out GuiEditArrayEntries.IArrayEntry> = EditArrayEntry::class.java

    class ArrayEntry(
        owningScreen: GuiConfig,
        owningEntryList: GuiConfigEntries,
        configElement: IConfigElement,
    ) : GuiConfigEntries.ArrayEntry(owningScreen, owningEntryList, configElement) {
        override fun updateValueButtonText() {
            btnValue.displayString = I18n.format(
                PreludeStrings.GUI_CONFIG_ENTRIES,
                (configElement as JsonConfigObjectArrayElement<*>).element.entries.size
            )
        }
    }

    class EditArrayEntry<T>(
        owningScreen: GuiEditArray,
        owningEntryList: GuiEditArrayEntries,
        configElement: IConfigElement,
        value: Any
    ) : GuiEditArrayEntries.BaseEntry(owningScreen, owningEntryList, configElement) {
        @Suppress("UNCHECKED_CAST") // think of T as binding configElement's type argument
        private val guiElement: JsonConfigObjectArrayElement<T> = configElement as JsonConfigObjectArrayElement<T>
        @Suppress("UNCHECKED_CAST") // can't guarantee the caller passed the right type of value, but alas
        private val entry: JsonConfig.ConfigObjectArray.Entry<T> =
            value as? JsonConfig.ConfigObjectArray.Entry<T> ?: guiElement.element.createNewEntry()
        private val editButton: GuiButtonExt =
            GuiButtonExt(0, 0, 0, owningEntryList.controlWidth, 18, entry.configObj.getJsonValue().toString())
        private val childScreen: GuiScreen by lazy {
            val index = owningEntryList.listEntries.indexOf(this)
            return@lazy GuiConfig(
                owningScreen,
                entry.configObj.entries.map { (key, elem) ->
                    show(key, "${guiElement.qualifiedName}${Configuration.CATEGORY_SPLITTER}$key", elem)
                },
                findConfigScreenModId((owningScreen as GuiEditArrayAccessor).`prelude$parentScreen`()),
                false, // prooooobably fine
                false,
                owningScreen.`prelude$title`(),
                owningScreen.`prelude$titleLine2`()?.let { line2 ->
                    owningScreen.`prelude$titleLine3`()?.let { line3 ->
                        "$line2 > $line3 > $index"
                    } ?: "$line2 > $index"
                } ?: index.toString()
            )
        }

        init {
            editButton.enabled = (owningScreen as GuiEditArrayAccessor).`prelude$enabled`()
        }

        override fun getValue(): Any = entry

        override fun drawEntry(
            slotIndex: Int, x: Int, y: Int, listWidth: Int, slotHeight: Int, mouseX: Int, mouseY: Int,
            isSelected: Boolean, partial: Float
        ) {
            super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial)
            editButton.x = listWidth / 4
            editButton.y = y
            editButton.drawButton(owningEntryList.mc, mouseX, mouseY, partial)
        }

        override fun mousePressed(
            index: Int, x: Int, y: Int, mouseEvent: Int, relativeX: Int, relativeY: Int
        ): Boolean {
            if (editButton.mousePressed(owningEntryList.mc, x, y)) {
                editButton.playPressSound(owningEntryList.mc.soundHandler)
                owningEntryList.mc.displayGuiScreen(childScreen)
                return true
            }
            return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY)
        }

        override fun mouseReleased(index: Int, x: Int, y: Int, mouseEvent: Int, relativeX: Int, relativeY: Int) {
            editButton.mouseReleased(x, y)
            super.mouseReleased(index, x, y, mouseEvent, relativeX, relativeY)
        }
    }
}

@ClientSide.Physical
class JsonConfigStringArrayElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigStringArray
) : AbstractJsonConfigLeafArrayElement<List<String>, JsonConfig.ConfigStringArray>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.STRING
    override fun getValidationPattern(): Pattern? = element.validPattern?.toPattern()

    @Suppress("UNCHECKED_CAST")
    override fun set(aVal: Array<out Any>) {
        if (aVal.isEmpty()) {
            element.currentValue = emptyList()
        } else if (aVal[0] is String) {
            // technically not sound because it could be anything above string, but it's probably homogeneous...?
            element.currentValue = (aVal as Array<String>).toList()
        } else {
            element.currentValue = aVal.map { it.toString() }
        }
    }
}

@ClientSide.Physical
class JsonConfigIntArrayElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigIntArray
) : AbstractJsonConfigLeafArrayElement<List<Int>, JsonConfig.ConfigIntArray>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.INTEGER
    override fun getMinimum(): Any? = element.minValue
    override fun getMaximum(): Any? = element.maxValue

    @Suppress("UNCHECKED_CAST")
    override fun set(aVal: Array<out Any>) {
        if (aVal.isEmpty()) {
            element.currentValue = emptyList()
        } else if (aVal[0] is Int) {
            element.currentValue = (aVal as Array<Int>).toList() // this api is really annoying
        } else {
            element.currentValue = aVal.map { it.toString().toInt() }
        }
    }
}

@ClientSide.Physical
class JsonConfigStringElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigString
) : AbstractJsonConfigLeafElement<String, JsonConfig.ConfigString>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.STRING
    override fun getValidationPattern(): Pattern? = element.validPattern?.toPattern()

    override fun set(value: Any) {
        element.currentValue = value as? String ?: value.toString()
    }
}

@ClientSide.Physical
class JsonConfigEnumElement<E : Enum<E>>(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigEnum<E>
) : AbstractJsonConfigLeafElement<E, JsonConfig.ConfigEnum<E>>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.STRING
    override fun getValidValues(): Array<out String> = element.valueStrings.toTypedArray()

    override fun set(value: Any) {
        element.currentValue = element.enumType.getEnumConstant(value as? String ?: value.toString())
    }
}

@ClientSide.Physical
class JsonConfigBoolElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigBool
) : AbstractJsonConfigLeafElement<Boolean, JsonConfig.ConfigBool>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.BOOLEAN

    override fun set(value: Any) {
        element.currentValue = value as? Boolean ?: value.toString().toBooleanStrict()
    }
}

@ClientSide.Physical
class JsonConfigByteElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigByte
) : AbstractJsonConfigLeafElement<Byte, JsonConfig.ConfigByte>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.INTEGER
    override fun getMinimum(): Any? = element.minValue?.toInt()
    override fun getMaximum(): Any? = element.maxValue?.toInt()

    override fun set(value: Any) {
        element.currentValue = when (value) {
            is Byte -> value
            is Int -> value.toByte()
            else -> value.toString().toByte()
        }
    }
}

@ClientSide.Physical
class JsonConfigShortElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigShort
) : AbstractJsonConfigLeafElement<Short, JsonConfig.ConfigShort>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.INTEGER
    override fun getMinimum(): Any? = element.minValue?.toInt()
    override fun getMaximum(): Any? = element.maxValue?.toInt()

    override fun set(value: Any) {
        element.currentValue = when (value) {
            is Short -> value
            is Int -> value.toShort()
            else -> value.toString().toShort()
        }
    }
}

@ClientSide.Physical
class JsonConfigIntElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigInt
) : AbstractJsonConfigLeafElement<Int, JsonConfig.ConfigInt>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.INTEGER
    override fun getMinimum(): Any? = element.minValue
    override fun getMaximum(): Any? = element.maxValue

    override fun set(value: Any) {
        element.currentValue = value as? Int ?: value.toString().toInt()
    }
}

@ClientSide.Physical
class JsonConfigLongElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigLong
) : AbstractJsonConfigLeafElement<Long, JsonConfig.ConfigLong>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.DOUBLE
    override fun getMinimum(): Any? = element.minValue?.toDouble()
    override fun getMaximum(): Any? = element.maxValue?.toDouble()

    override fun set(value: Any) {
        element.currentValue = when (value) {
            is Long -> value
            is Double -> value.toLong()
            else -> value.toString().toLong()
        }
    }
}

@ClientSide.Physical
class JsonConfigFloatElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigFloat
) : AbstractJsonConfigLeafElement<Float, JsonConfig.ConfigFloat>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.DOUBLE
    override fun getMinimum(): Any? = element.minValue?.toDouble()
    override fun getMaximum(): Any? = element.maxValue?.toDouble()

    override fun set(value: Any) {
        element.currentValue = when (value) {
            is Float -> value
            is Double -> value.toFloat()
            else -> value.toString().toFloat()
        }
    }
}

@ClientSide.Physical
class JsonConfigDoubleElement(
    name: String,
    qualifiedName: String,
    element: JsonConfig.ConfigDouble
) : AbstractJsonConfigLeafElement<Double, JsonConfig.ConfigDouble>(name, qualifiedName, element) {
    override fun getType(): ConfigGuiType = ConfigGuiType.DOUBLE
    override fun getMinimum(): Any? = element.minValue
    override fun getMaximum(): Any? = element.maxValue

    override fun set(value: Any) {
        element.currentValue = value as? Double ?: value.toString().toDouble()
    }
}

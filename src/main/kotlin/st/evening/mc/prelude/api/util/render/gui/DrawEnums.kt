package st.evening.mc.prelude.api.util.render.gui

import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.util.collection.sumOf
import st.evening.mc.prelude.api.util.math.Arithmetic
import st.evening.mc.prelude.api.util.math.arithSumOf

enum class DrawAxis {
    X, Y;

    companion object {
        val serializer: EnumSerializer<DrawAxis> = EnumSerializer()
    }
}

enum class DrawOffsetReference {
    FROM_START {
        override fun <T> computeOffset(arith: Arithmetic<T>, offset: T, elementSize: T, parentSize: T): T = offset
    },
    FROM_END {
        override fun <T> computeOffset(arith: Arithmetic<T>, offset: T, elementSize: T, parentSize: T): T =
            arith.run { parentSize - offset - elementSize }
    };

    abstract fun <T> computeOffset(arith: Arithmetic<T>, offset: T, elementSize: T, parentSize: T): T

    companion object {
        val serializer: EnumSerializer<DrawOffsetReference> = EnumSerializer()
    }
}

enum class DrawOrientation(val axis: DrawAxis, val offsetRef: DrawOffsetReference) {
    LEFT_TO_RIGHT(DrawAxis.X, DrawOffsetReference.FROM_START),
    RIGHT_TO_LEFT(DrawAxis.X, DrawOffsetReference.FROM_END),
    TOP_TO_BOTTOM(DrawAxis.Y, DrawOffsetReference.FROM_START),
    BOTTOM_TO_TOP(DrawAxis.Y, DrawOffsetReference.FROM_END);

    companion object {
        val serializer: EnumSerializer<DrawOrientation> = EnumSerializer()
    }
}

enum class DrawAlignment {
    START {
        override fun <T> computeOffset(arith: Arithmetic<T>, elementSize: T, parentSize: T): T = arith.zero
    },
    CENTER {
        override fun <T> computeOffset(arith: Arithmetic<T>, elementSize: T, parentSize: T): T =
            arith.run { (parentSize - elementSize) / fromInt(2) }
    },
    END {
        override fun <T> computeOffset(arith: Arithmetic<T>, elementSize: T, parentSize: T): T =
            arith.run { parentSize - elementSize }
    };

    abstract fun <T> computeOffset(arith: Arithmetic<T>, elementSize: T, parentSize: T): T

    companion object {
        val serializer: EnumSerializer<DrawAlignment> = EnumSerializer()
    }
}

data class FlexChild<T>(val size: T, val flex: Float)

data class FlexCell<T>(val offset: T, val size: T)

enum class DrawJustification(val alignment: DrawAlignment) {
    START(DrawAlignment.START),
    CENTER(DrawAlignment.CENTER),
    END(DrawAlignment.END),
    SPACE_BETWEEN(DrawAlignment.CENTER) {
        override fun <T> justify(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            parentSize: T,
            childrenSize: T,
            minGap: T,
            space: T
        ): List<FlexCell<T>> =
            layOutNoFlex(arith, children, arith.zero, arith.run { space / fromInt(children.size - 1) })
    },
    SPACE_AROUND(DrawAlignment.CENTER) {
        override fun <T> justify(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            parentSize: T,
            childrenSize: T,
            minGap: T,
            space: T
        ): List<FlexCell<T>> {
            val pad = arith.run { space / fromInt(children.size * 2) }
            return layOutNoFlex(arith, children, pad, arith.run { pad + pad })
        }
    },
    SPACE_EVENLY(DrawAlignment.CENTER) {
        override fun <T> justify(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            parentSize: T,
            childrenSize: T,
            minGap: T,
            space: T
        ): List<FlexCell<T>> {
            val pad = arith.run { space / fromInt(children.size + 1) }
            return layOutNoFlex(arith, children, pad, pad)
        }
    };

    fun <T> computeCells(
        arith: Arithmetic<T>,
        children: List<FlexChild<T>>,
        parentSize: T?,
        minGap: T
    ): List<FlexCell<T>> {
        if (children.isEmpty()) return emptyList()
        if (parentSize == null) { // parent is infinitely large; just place children at start
            return layOutNoFlex(arith, children, arith.zero, minGap)
        }
        val childCount = children.size
        if (childCount == 1) { // only one child; just align it in the parent
            val size = children[0].size
            return listOf(FlexCell(alignment.computeOffset(arith, size, parentSize), size))
        }
        val childrenSize = arith.run { children.arithSumOf { it.size } + fromInt(childCount - 1) * minGap }
        val flexSpace = arith.run { parentSize - childrenSize }
        if (arith.run { flexSpace <= zero }) { // parent is too small; lay out elements as compact as possible
            return layOutNoFlex(arith, children, alignment.computeOffset(arith, childrenSize, parentSize), minGap)
        }
        val totalFlex = children.sumOf { it.flex }
        if (totalFlex <= 0) { // no flexible elements; distribute space around elements
            return justify(arith, children, parentSize, childrenSize, minGap, flexSpace)
        }
        return layOutWithFlex(arith, children, flexSpace, totalFlex, arith.zero, minGap)
    }

    open fun <T> justify(
        arith: Arithmetic<T>,
        children: List<FlexChild<T>>,
        parentSize: T,
        childrenSize: T,
        minGap: T,
        space: T
    ): List<FlexCell<T>> =
        layOutNoFlex(arith, children, alignment.computeOffset(arith, childrenSize, parentSize), minGap)

    companion object {
        val serializer: EnumSerializer<DrawJustification> = EnumSerializer()

        private fun <T> layOutNoFlex(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            initialPos: T,
            gap: T
        ): List<FlexCell<T>> = layOut(arith, children, initialPos, gap) { it.size }

        private fun <T> layOutWithFlex(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            flexSpace: T,
            totalFlex: Float,
            initialPos: T,
            gap: T
        ): List<FlexCell<T>> = layOut(arith, children, initialPos, gap) {
            arith.run { it.size + flexSpace.scale(it.flex / totalFlex) }
        }

        private inline fun <T> layOut(
            arith: Arithmetic<T>,
            children: List<FlexChild<T>>,
            initialPos: T,
            gap: T,
            getChildSize: (FlexChild<T>) -> T
        ): List<FlexCell<T>> {
            var pos = initialPos
            return children.map {
                val size = getChildSize(it)
                val cell = FlexCell(pos, getChildSize(it))
                pos = arith.run { pos + size + gap }
                return@map cell
            }
        }
    }
}

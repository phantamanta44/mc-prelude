package st.evening.mc.prelude.api.gui.engine.prefab

import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.util.collection.asOrToMutableList
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawJustification
import st.evening.mc.prelude.api.util.render.gui.FlexCell
import st.evening.mc.prelude.api.util.render.gui.FlexChild
import kotlin.math.max

@ClientSide.Physical
class AbsoluteLayout(fixedWidth: Int? = null, fixedHeight: Int? = null) : AbstractGuiLayout(fixedWidth, fixedHeight) {
    private val children: MutableList<Child> = mutableListOf()

    fun addChild(x: Int, y: Int, element: GuiElement) {
        children += Child(x, y, element)
        invalidateContentDimensions()
    }

    override fun computeContentWidth(): Int = children.maxOfOrNull { it.x + it.element.contentWidth } ?: 0

    override fun computeContentHeight(): Int = children.maxOfOrNull { it.y + it.element.contentHeight } ?: 0

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart =
        GuiLayoutPart(posX, posY, width, height, children.map {
            it.element.bake(posX + it.x, posY + it.y, width - it.x, height - it.y)
        })

    private class Child(val x: Int, val y: Int, val element: GuiElement)
}

@ClientSide.Physical
class StackLayout(fixedWidth: Int? = null, fixedHeight: Int? = null) : AbstractGuiLayout(fixedWidth, fixedHeight) {
    private val children: MutableList<Child> = mutableListOf()

    fun addChild(
        element: GuiElement,
        alignX: DrawAlignment = DrawAlignment.CENTER,
        alignY: DrawAlignment = DrawAlignment.CENTER
    ) {
        children += Child(element, alignX, alignY)
        invalidateContentDimensions()
    }

    override fun computeContentWidth(): Int = children.maxOfOrNull { it.element.contentWidth } ?: 0

    override fun computeContentHeight(): Int = children.maxOfOrNull { it.element.contentHeight } ?: 0

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart =
        GuiLayoutPart(posX, posY, width, height, children.map {
            it.element.bake(posX, posY, width, height, it.alignX, it.alignY)
        })

    private class Child(val element: GuiElement, val alignX: DrawAlignment, val alignY: DrawAlignment)
}

@ClientSide.Physical
interface FlexLayout : GuiElement {
    fun addChild(element: GuiElement, flex: Float = 0F, align: DrawAlignment? = null)
}

@ClientSide.Physical
private class FlexLayoutChild(val element: GuiElement, val flex: Float, val align: DrawAlignment?)

@ClientSide.Physical
class RowLayout(
    fixedWidth: Int? = null,
    fixedHeight: Int? = null,
    private val justifyX: DrawJustification = DrawJustification.START,
    private val alignY: DrawAlignment = DrawAlignment.START,
    private val minGap: Int = 0
) : AbstractGuiLayout(fixedWidth, fixedHeight), FlexLayout {
    private val children: MutableList<FlexLayoutChild> = mutableListOf()

    override fun addChild(element: GuiElement, flex: Float, align: DrawAlignment?) {
        children += FlexLayoutChild(element, flex, align)
        invalidateContentDimensions()
    }

    override fun computeContentWidth(): Int =
        if (children.isEmpty()) 0 else children.sumOf { it.element.contentWidth } + (children.size - 1) * minGap

    override fun computeContentHeight(): Int = children.maxOfOrNull { it.element.contentHeight } ?: 0

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart {
        val sizes = children.map { FlexChild(it.element.contentWidth, it.flex) }
        val cells = justifyX.computeCells(IntArithmetic, sizes, width, minGap)
        return GuiLayoutPart(posX, posY, width, height, children.zip(cells) { child, cell ->
            child.element.bake(posX + cell.offset, posY, cell.size, height, justifyX.alignment, child.align ?: alignY)
        })
    }
}

@ClientSide.Physical
class ColumnLayout(
    fixedWidth: Int? = null,
    fixedHeight: Int? = null,
    private val justifyY: DrawJustification = DrawJustification.START,
    private val alignX: DrawAlignment = DrawAlignment.START,
    private val minGap: Int = 0
) : AbstractGuiLayout(fixedWidth, fixedHeight), FlexLayout {
    private val children: MutableList<FlexLayoutChild> = mutableListOf()

    override fun addChild(element: GuiElement, flex: Float, align: DrawAlignment?) {
        children += FlexLayoutChild(element, flex, align)
        invalidateContentDimensions()
    }

    override fun computeContentWidth(): Int = children.maxOfOrNull { it.element.contentWidth } ?: 0

    override fun computeContentHeight(): Int =
        if (children.isEmpty()) 0 else children.sumOf { it.element.contentHeight } + (children.size - 1) * minGap

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart {
        val sizes = children.map { FlexChild(it.element.contentHeight, it.flex) }
        val cells = justifyY.computeCells(IntArithmetic, sizes, height, minGap)
        return GuiLayoutPart(posX, posY, width, height, children.zip(cells) { child, cell ->
            child.element.bake(posX, posY + cell.offset, width, cell.size, child.align ?: alignX, justifyY.alignment)
        })
    }
}

@ClientSide.Physical
class TableLayout(
    rowCount: Int,
    columnCount: Int,
    fixedWidth: Int? = null,
    fixedHeight: Int? = null,
    private val rowSep: Int = 0,
    private val columnSep: Int = 0,
    private val justifyX: DrawJustification = DrawJustification.SPACE_BETWEEN,
    private val justifyY: DrawJustification = DrawJustification.SPACE_BETWEEN
) : AbstractGuiLayout(fixedWidth, fixedHeight) {
    private val rowConfigs: Array<RowColConfig?> = arrayOfNulls(rowCount)
    private val columnConfigs: Array<RowColConfig?> = arrayOfNulls(columnCount)
    private val children: Array<Array<GuiElement?>> = Array(rowCount) { arrayOfNulls(columnCount) }
    private val cachedRowContentHeights: IntArray = IntArray(rowCount) { -1 }
    private val cachedColumnContentWidths: IntArray = IntArray(columnCount) { -1 }

    fun configureRow(
        rowIndex: Int,
        height: Int? = null,
        flex: Float = 0F,
        startPadding: Int = 0,
        endPadding: Int = 0,
        alignment: DrawAlignment = DrawAlignment.CENTER
    ) {
        rowConfigs[rowIndex] = RowColConfig(height, flex, startPadding, endPadding, alignment)
        if (height != null) {
            cachedRowContentHeights[rowIndex] = height
            invalidateContentHeight()
        }
    }

    fun configureColumn(
        columnIndex: Int,
        width: Int? = null,
        flex: Float = 0F,
        startPadding: Int = 0,
        endPadding: Int = 0,
        alignment: DrawAlignment = DrawAlignment.CENTER
    ) {
        columnConfigs[columnIndex] = RowColConfig(width, flex, startPadding, endPadding, alignment)
        if (width != null) {
            cachedColumnContentWidths[columnIndex] = width
            invalidateContentWidth()
        }
    }

    fun setChild(rowIndex: Int, columnIndex: Int, element: GuiElement) {
        children[rowIndex][columnIndex] = element
        if (rowConfigs[rowIndex]?.size == null) {
            cachedRowContentHeights[rowIndex] = -1
            invalidateContentHeight()
        }
        if (columnConfigs[columnIndex]?.size == null) {
            cachedColumnContentWidths[columnIndex] = -1
            invalidateContentWidth()
        }
    }

    private fun getRowContentHeight(rowIndex: Int, config: RowColConfig?): Int {
        val cached = cachedRowContentHeights[rowIndex]
        if (cached >= 0) return cached
        config?.size?.let {
            cachedRowContentHeights[rowIndex] = it
            return it
        }
        var maxHeight = 0
        children[rowIndex].forEach {
            if (it != null) {
                maxHeight = max(maxHeight, it.contentHeight)
            }
        }
        cachedRowContentHeights[rowIndex] = maxHeight
        return maxHeight
    }

    private fun getColumnContentWidth(columnIndex: Int, config: RowColConfig?): Int {
        val cached = cachedColumnContentWidths[columnIndex]
        if (cached >= 0) return cached
        config?.size?.let {
            cachedColumnContentWidths[columnIndex] = it
            return it
        }
        var maxWidth = 0
        children.forEach { row ->
            row[columnIndex]?.let {
                maxWidth = max(maxWidth, it.contentWidth)
            }
        }
        cachedColumnContentWidths[columnIndex] = maxWidth
        return maxWidth
    }

    override fun computeContentWidth(): Int {
        val colCount = columnConfigs.size
        if (colCount == 0) return 0
        var width = columnSep * (colCount - 1)
        columnConfigs.forEachIndexed { index, config ->
            width += getColumnContentWidth(index, config)
            config?.let { width += it.totalPadding }
        }
        return width
    }

    override fun computeContentHeight(): Int {
        val rowCount = rowConfigs.size
        if (rowCount == 0) return 0
        var height = rowSep * (rowCount - 1)
        rowConfigs.forEachIndexed { index, config ->
            height += getRowContentHeight(index, config)
            config?.let { height += it.totalPadding }
        }
        return height
    }

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart {
        val rowSizes = rowConfigs.mapIndexed { index, config ->
            FlexChild(getRowContentHeight(index, config), config?.flex ?: 0F)
        }
        val rowCells = justifyY.computeCells(IntArithmetic, rowSizes, height, rowSep).asOrToMutableList()
        rowConfigs.forEachIndexed { index, config ->
            if (config != null) {
                val cell = rowCells[index]
                rowCells[index] = FlexCell(cell.offset + config.startPadding, cell.size - config.totalPadding)
            }
        }

        val colSizes = columnConfigs.mapIndexed { index, config ->
            FlexChild(getColumnContentWidth(index, config), config?.flex ?: 0F)
        }
        val colCells = justifyX.computeCells(IntArithmetic, colSizes, width, columnSep).asOrToMutableList()
        columnConfigs.forEachIndexed { index, config ->
            if (config != null) {
                val cell = colCells[index]
                colCells[index] = FlexCell(posY + cell.offset + config.startPadding, cell.size - config.totalPadding)
            }
        }

        val parts = mutableListOf<GuiPart>()
        rowConfigs.forEachIndexed { rowIndex, rowConfig ->
            val rowCell = rowCells[rowIndex]
            val rowAlign = rowConfig?.alignment ?: DrawAlignment.CENTER
            val row = children[rowIndex]
            columnConfigs.forEachIndexed { colIndex, colConfig ->
                row[colIndex]?.let {
                    val colCell = colCells[colIndex]
                    parts += it.bake(
                        posX + colCell.offset,
                        posY + rowCell.offset,
                        colCell.size,
                        rowCell.size,
                        colConfig?.alignment ?: DrawAlignment.CENTER,
                        rowAlign
                    )
                }
            }
        }
        return GuiLayoutPart(posX, posY, width, height, parts)
    }

    private class RowColConfig(
        val size: Int?,
        val flex: Float,
        val startPadding: Int,
        endPadding: Int,
        val alignment: DrawAlignment
    ) {
        val totalPadding: Int = startPadding + endPadding
    }
}

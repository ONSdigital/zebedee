package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.TableModifications;
import com.github.onsdigital.zebedee.exceptions.TableBuilderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.apache.poi.hssf.converter.ExcelToHtmlUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hwpf.converter.HtmlDocumentFacade;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.text.MessageFormat.format;

/**
 * Helper class to convert an xls file to a html table.
 */
public class XlsToHtmlConverter extends ExcelToHtmlConverter {

    private static final String CSS_ROW_PREFIX = "r-{0}-";
    private static final String ROW = "row";
    private static final String COL = "col";
    private static final String SCOPE_ATTR = "scope";
    protected final HtmlDocumentFacade htmlDocument;
    protected final Document document;
    protected Optional<TableModifications> tableModifications;

    private XlsToHtmlConverter(Document document, HtmlDocument htmlDocument) {
        super(htmlDocument);
        this.htmlDocument = htmlDocument;
        this.document = document;
        this.tableModifications = Optional.empty();
        this.setCssClassPrefixRow(format(CSS_ROW_PREFIX, UUID.randomUUID().toString()));
    }

    /**
     * Convert the xls file at the given path
     *
     * @param xlsFileIn
     * @throws IOException
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public static Node convertToTable(File xlsFileIn) throws ParserConfigurationException, IOException {
        final HSSFWorkbook workbook = ExcelToHtmlUtils.loadXls(xlsFileIn);
        return createTable(workbook);
    }

    /**
     * Convert the given xls file as InputStream.
     *
     * @param xlsInputStream
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static Node convertToTable(InputStream xlsInputStream) throws ParserConfigurationException, IOException {
        try {
            final HSSFWorkbook workbook = new HSSFWorkbook(xlsInputStream);
            return createTable(workbook);
        } finally {
            IOUtils.closeQuietly(xlsInputStream);
        }
    }

    private static Node createTable(HSSFWorkbook workbook) throws ParserConfigurationException {
        XlsToHtmlConverter converter = createConverter();
        Element table = converter.createTableFromWorkbook(workbook);
        return table;
    }

    /**
     * Convert the xls file at the given path
     *
     * @param xlsFileIn
     * @throws IOException
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public static Node convertToHtmlPage(File xlsFileIn) throws ParserConfigurationException, IOException {
        final HSSFWorkbook workbook = ExcelToHtmlUtils.loadXls(xlsFileIn);
        XlsToHtmlConverter converter = createConverter();

        converter.processWorkbook(workbook);
        Document document = converter.getDocument();
        return document;
    }

    public static Node convertToHtmlPageWithModifications(InputStream stream, TableModifications modifications)
            throws IOException, ParserConfigurationException, TableBuilderException {
        try (HSSFWorkbook workbook = new HSSFWorkbook(stream)) {
            XlsToHtmlConverter converter = createConverter();
            converter.addTableModifications(modifications);
            converter.processWorkbook(workbook);
            return converter.getDocument();
        }
    }

    /**
     * Serialise and save the given document instance to the given Path.
     *
     * @param document
     * @param fileOut
     * @throws IOException
     * @throws TransformerException
     */
    public static void save(Node document, Path fileOut) throws IOException, TransformerException {
        WriteHtmlToFile(document, fileOut);
    }

    /**
     * Serialise the given Document and save to the given Path.
     *
     * @param doc
     * @param htmlFileOut
     * @throws IOException
     * @throws TransformerException
     */
    private static void WriteHtmlToFile(Node doc, Path htmlFileOut) throws IOException, TransformerException {
        FileWriter out = new FileWriter(htmlFileOut.toFile());
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(out);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();

        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        serializer.transform(domSource, streamResult);
        out.close();
    }

    public static String docToString(Node doc) throws IOException, TransformerException {
        StringWriter out = new StringWriter();
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(out);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();

        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        serializer.transform(domSource, streamResult);
        out.close();

        return out.toString();
    }

    /**
     * Factory method to create a new instance of XlsToHtmlConverter.
     *
     * @return
     * @throws ParserConfigurationException
     */
    private static XlsToHtmlConverter createConverter() throws ParserConfigurationException {

        Document document = createDocument();
        HtmlDocument htmlDocument = new HtmlDocument(document);
        XlsToHtmlConverter converter = new XlsToHtmlConverter(document, htmlDocument);
        converter.setOutputColumnHeaders(false);
        converter.setOutputRowNumbers(false);
        converter.setUseDivsToSpan(false);

        return converter;
    }

    /**
     * Create a new empty Document instance.
     *
     * @return
     * @throws ParserConfigurationException
     */
    private static Document createDocument() throws ParserConfigurationException {
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .newDocument();
    }

    public Element createTableFromWorkbook(HSSFWorkbook workbook) {
        final SummaryInformation summaryInformation = workbook
                .getSummaryInformation();
        if (summaryInformation != null) {
            processDocumentInformation(summaryInformation);
        }

        // take only the first sheet
        HSSFSheet sheet = workbook.getSheetAt(0);
        Element table = createTable(sheet);
        return table;
        //htmlDocument.updateStylesheet();
    }

    public void addTableModifications(TableModifications modifications) {
        this.tableModifications = Optional.ofNullable(modifications);
    }

    @Override
    protected void processSheetHeader(Element htmlBody, HSSFSheet sheet) {

        // dont do anything with headers.

//        Element h2 = htmlDocumentFacade.createHeader2();
//        h2.appendChild(htmlDocumentFacade.createText(sheet.getSheetName()));
//        htmlBody.appendChild( h2 );
    }


    @Override
    protected void processColumnWidths(HSSFSheet sheet, int maxSheetColumns, Element table) {
        // draw COLS after we know max column number
        Element columnGroup = htmlDocument.createTableColumnGroup();
        if (isOutputRowNumbers()) {
            columnGroup.appendChild(htmlDocument.createTableColumn());
        }
        for (int c = 0; c < maxSheetColumns; c++) {
            if (!isOutputHiddenColumns() && sheet.isColumnHidden(c))
                continue;

            Element col = htmlDocument.createTableColumn();
//            col.setAttribute( "width",
//                    String.valueOf( getColumnWidth( sheet, c ) ) );
            columnGroup.appendChild(col);
        }
        table.appendChild(columnGroup);
    }

    protected String buildStyle(HSSFWorkbook workbook, HSSFCellStyle cellStyle) {
        StringBuilder style = new StringBuilder();

        style.append("white-space:pre-wrap;");
        ExcelToHtmlUtils.appendAlign(style, cellStyle.getAlignment());

        if (cellStyle.getFillPattern() == 0) {
            // no fill
        } else if (cellStyle.getFillPattern() == 1) {
//            final HSSFColor foregroundColor = cellStyle
//                    .getFillForegroundColorColor();
//            if (foregroundColor != null)
//                style.append("background-color:"
//                        + ExcelToHtmlUtils.getColor(foregroundColor) + ";");
        } else {

            // ignore background colors

//            final HSSFColor backgroundColor = cellStyle
//                    .getFillBackgroundColorColor();
//            if (backgroundColor != null)
//                style.append("background-color:"
//                        + ExcelToHtmlUtils.getColor(backgroundColor) + ";");
        }

        buildStyle_border(workbook, style, "top", cellStyle.getBorderTop(),
                cellStyle.getTopBorderColor());
//        buildStyle_border( workbook, style, "right",
//                cellStyle.getBorderRight(), cellStyle.getRightBorderColor() );
        buildStyle_border(workbook, style, "bottom",
                cellStyle.getBorderBottom(), cellStyle.getBottomBorderColor());
//        buildStyle_border( workbook, style, "left", cellStyle.getBorderLeft(),
//                cellStyle.getLeftBorderColor() );

        HSSFFont font = cellStyle.getFont(workbook);
        buildStyle_font(workbook, style, font);

        return style.toString();
    }

    private void buildStyle_border(
            HSSFWorkbook workbook, StringBuilder style,
            String type, short xlsBorder, short borderColor
    ) {
        if (xlsBorder == HSSFCellStyle.BORDER_NONE)
            return;

        StringBuilder borderStyle = new StringBuilder();
        borderStyle.append(ExcelToHtmlUtils.getBorderWidth(xlsBorder));
        borderStyle.append(' ');
        borderStyle.append(ExcelToHtmlUtils.getBorderStyle(xlsBorder));

        final HSSFColor color = workbook.getCustomPalette().getColor(
                borderColor);
        if (color != null) {
            borderStyle.append(' ');
            borderStyle.append(ExcelToHtmlUtils.getColor(color));
        }

        style.append("border-" + type + ":" + borderStyle + ";");
    }

    void buildStyle_font(
            HSSFWorkbook workbook, StringBuilder style,
            HSSFFont font
    ) {
        switch (font.getBoldweight()) {
            case HSSFFont.BOLDWEIGHT_BOLD:
                style.append("font-weight:bold;");
                break;
            case HSSFFont.BOLDWEIGHT_NORMAL:
                // by default, not not increase HTML size
                // style.append( "font-weight: normal; " );
                break;
        }

        final HSSFColor fontColor = workbook.getCustomPalette().getColor(
                font.getColor());
        if (fontColor != null)
            style.append("color: " + ExcelToHtmlUtils.getColor(fontColor)
                    + "; ");

        if (font.getFontHeightInPoints() != 0)
            // ignore font size
            //style.append("font-size:" + font.getFontHeightInPoints() + "pt;");

            if (font.getItalic()) {
                style.append("font-style:italic;");
            }
    }

    public void addStyleClass(Element element, String style) {
        String existing = element.getAttribute("style");
        String newStyleValue = StringUtils.isEmpty(existing) ? style
                : (existing + " " + style);
        element.setAttribute("style", newStyleValue);
    }

    protected Element createTable(HSSFSheet sheet) {
        processSheetHeader(htmlDocument.getBody(), sheet);

        Element table = htmlDocument.createTable();

        final int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
        if (physicalNumberOfRows <= 0)
            return table;

        addStyleClass(table, "border-collapse:collapse;border-spacing:0;");

        Element tableBody = htmlDocument.createTableBody();

        final CellRangeAddress[][] mergedRanges = ExcelToHtmlUtils
                .buildMergedRangesMap(sheet);

        final List<Element> emptyRowElements = new ArrayList<Element>(
                physicalNumberOfRows);
        int maxSheetColumns = 1;
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            HSSFRow row = sheet.getRow(r);

            if (row == null)
                continue;

            if (!isOutputHiddenRows() && row.getZeroHeight())
                continue;

            Element tableRowElement = htmlDocument.createTableRow();
            addStyleClass(tableRowElement, "height:" + (row.getHeight() / 20f) + "pt;");

            int maxRowColumnNumber = processRow(mergedRanges, row,
                    tableRowElement);

            if (maxRowColumnNumber == 0) {
                emptyRowElements.add(tableRowElement);
            } else {
                if (!emptyRowElements.isEmpty()) {
                    for (Element emptyRowElement : emptyRowElements) {
                        tableBody.appendChild(emptyRowElement);
                    }
                    emptyRowElements.clear();
                }

                tableBody.appendChild(tableRowElement);
            }
            maxSheetColumns = Math.max(maxSheetColumns, maxRowColumnNumber);
        }

        processColumnWidths(sheet, maxSheetColumns, table);

        if (isOutputColumnHeaders()) {
            processColumnHeaders(sheet, maxSheetColumns, table);
        }

        table.appendChild(tableBody);

        htmlDocument.getBody().appendChild(table);

        return table;
    }

    protected boolean processCellContent(
            HSSFCell cell, Element tableCellElement,
            int normalWidthPx, int maxSpannedWidthPx, float normalHeightPt
    ) {
        final HSSFCellStyle cellStyle = cell.getCellStyle();

        String value;
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                // XXX: enrich
                value = cell.getRichStringCellValue().getString();
                break;
            case HSSFCell.CELL_TYPE_FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case HSSFCell.CELL_TYPE_STRING:
                        HSSFRichTextString str = cell.getRichStringCellValue();
                        if (str != null && str.length() > 0) {
                            value = (str.toString());
                        } else {
                            value = "";
                        }
                        break;
                    case HSSFCell.CELL_TYPE_NUMERIC:
                        HSSFCellStyle style = cellStyle;
                        if (style == null) {
                            value = String.valueOf(cell.getNumericCellValue());
                        } else {
                            value = (_formatter.formatRawCellContents(
                                    cell.getNumericCellValue(), style.getDataFormat(),
                                    style.getDataFormatString()));
                        }
                        break;
                    case HSSFCell.CELL_TYPE_BOOLEAN:
                        value = String.valueOf(cell.getBooleanCellValue());
                        break;
                    case HSSFCell.CELL_TYPE_ERROR:
                        value = ErrorEval.getText(cell.getErrorCellValue());
                        break;
                    default:
                        value = "";
                        break;
                }
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                value = "";
                break;
            case HSSFCell.CELL_TYPE_NUMERIC:
                value = _formatter.formatCellValue(cell);
                break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case HSSFCell.CELL_TYPE_ERROR:
                value = ErrorEval.getText(cell.getErrorCellValue());
                break;
            default:
                return true;
        }

        final boolean noText = StringUtils.isEmpty(value);

        final short cellStyleIndex = cellStyle.getIndex();
        if (cellStyleIndex != 0) {
            HSSFWorkbook workbook = cell.getRow().getSheet().getWorkbook();
            //String mainCssClass = getStyleClassName(workbook, cellStyle);

            String cssStyle = buildStyle(workbook, cellStyle);
            addStyleClass(tableCellElement, cssStyle);
//            String cssClass = htmlDocumentFacade.getOrCreateCssClass(
//                    cssClassPrefixCell, cssStyle);
//            .setAttribute("class", mainCssClass);

            if (noText) {
                /*
                 * if cell style is defined (like borders, etc.) but cell text
                 * is empty, add "&nbsp;" to output, so browser won't collapse
                 * and ignore cell
                 */
                value = "\u00A0";
            }
        }

        if (isOutputLeadingSpacesAsNonBreaking() && value.startsWith(" ")) {
            StringBuilder builder = new StringBuilder();
            for (int c = 0; c < value.length(); c++) {
                if (value.charAt(c) != ' ')
                    break;
                builder.append('\u00a0');
            }

            if (value.length() != builder.length())
                builder.append(value.substring(builder.length()));

            value = builder.toString();
        }

        tableCellElement.setTextContent(value);
        return StringUtils.isEmpty(value) && cellStyleIndex == 0;
    }

    protected boolean processCell(
            HSSFCell cell, Element tableCellElement,
            int normalWidthPx, int maxSpannedWidthPx, float normalHeightPt
    ) {
        boolean result = processCellContent(cell, tableCellElement, normalWidthPx, maxSpannedWidthPx, normalHeightPt);

        HSSFWorkbook workbook = cell.getRow().getSheet().getWorkbook();
        int formattingRunIndex = 0;
        int toIndex = 0;
        int fromIndex = 0;

        if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {

            HSSFRichTextString rts = cell.getRichStringCellValue();
            HSSFCellStyle style = cell.getCellStyle();
            HSSFFont font = style.getFont(workbook);

            StringBuilder builder = new StringBuilder();
            String content = rts.toString();

            if (rts.numFormattingRuns() > 0) {

                // remove any existing elements as we want to replace them
                while (tableCellElement.hasChildNodes())
                    tableCellElement.removeChild(tableCellElement.getFirstChild());

                for (formattingRunIndex = 0; formattingRunIndex < rts.numFormattingRuns(); formattingRunIndex++) {

                    toIndex = rts.getIndexOfFormattingRun(formattingRunIndex);
                    String subString = content.substring(fromIndex, toIndex);

                    if (font.getTypeOffset() == HSSFFont.SS_SUPER || font.getTypeOffset() == HSSFFont.SS_SUB) {

                        if (font.getTypeOffset() == HSSFFont.SS_SUPER) {
                            builder.append("<sup>" + subString + "</sup>");
                            Element element = document.createElement("sup");
                            element.appendChild(htmlDocument.createText(subString));
                            tableCellElement.appendChild(element);
                        } else {
                            builder.append("<sub>" + subString + "</sub>");
                            Element element = document.createElement("sub");
                            element.appendChild(htmlDocument.createText(subString));
                            tableCellElement.appendChild(element);
                        }
                    } else {
                        builder.append(subString);
                        tableCellElement.appendChild(htmlDocument.createText(subString));
                    }
                    font = workbook.getFontAt(rts.getFontOfFormattingRun(formattingRunIndex));
                    fromIndex = toIndex;
                }
                toIndex = rts.length();

                String subString = content.substring(fromIndex, toIndex);
                if (font.getTypeOffset() == HSSFFont.SS_SUPER || font.getTypeOffset() == HSSFFont.SS_SUB) {


                    if (font.getTypeOffset() == HSSFFont.SS_SUPER) {
                        builder.append("<sup>" + subString + "</sup>");
                        Element element = document.createElement("sup");
                        element.appendChild(htmlDocument.createText(subString));
                        tableCellElement.appendChild(element);
                    } else {
                        builder.append("<sub>" + subString + "</sub>");
                        Element element = document.createElement("sub");
                        element.appendChild(htmlDocument.createText(subString));
                        tableCellElement.appendChild(element);
                    }
                } else {
                    builder.append(subString);
                    tableCellElement.appendChild(htmlDocument.createText(subString));
                }
            } else {
                if (font.getTypeOffset() == HSSFFont.SS_SUPER) {
                } else {
                }
            }
        } else {
        }


        return result;
    }

    @Override
    protected int processRow(CellRangeAddress[][] mergedRanges, HSSFRow row,
                             Element tableRowElement) {
        if (tableModifications.isPresent() && tableModifications.get().getRowsExcluded().contains(row.getRowNum())) {
            // if this row is listed in the row exclusions skip completely.
            return 0;
        }

        final HSSFSheet sheet = row.getSheet();
        final short maxColIx = row.getLastCellNum();
        if (maxColIx <= 0)
            return 0;

        final List<Element> emptyCells = new ArrayList<Element>(maxColIx);

        if (isOutputRowNumbers()) {
            Element tableRowNumberCellElement = htmlDocument
                    .createTableHeaderCell();
            processRowNumber(row, tableRowNumberCellElement);
            emptyCells.add(tableRowNumberCellElement);
        }

        int maxRenderedColumn = 0;
        for (int columnIndex = 0; columnIndex < maxColIx; columnIndex++) {
            if (!isOutputHiddenColumns() && sheet.isColumnHidden(columnIndex))
                continue;

            CellRangeAddress range = ExcelToHtmlUtils.getMergedRange(
                    mergedRanges, row.getRowNum(), columnIndex);

            if (range != null
                    && (range.getFirstColumn() != columnIndex || range.getFirstRow() != row
                    .getRowNum()))
                continue;

            HSSFCell cell = row.getCell(columnIndex);

            int divWidthPx = 0;
            if (isUseDivsToSpan()) {
                divWidthPx = getColumnWidth(sheet, columnIndex);

                boolean hasBreaks = false;
                for (int nextColumnIndex = columnIndex + 1; nextColumnIndex < maxColIx; nextColumnIndex++) {
                    if (!isOutputHiddenColumns()
                            && sheet.isColumnHidden(nextColumnIndex))
                        continue;

                    if (row.getCell(nextColumnIndex) != null
                            && !isTextEmpty(row.getCell(nextColumnIndex))) {
                        hasBreaks = true;
                        break;
                    }

                    divWidthPx += getColumnWidth(sheet, nextColumnIndex);
                }

                if (!hasBreaks)
                    divWidthPx = Integer.MAX_VALUE;
            }

            Element tableElement = getTableCell(row, cell, columnIndex);

            if (range != null) {
                if (range.getFirstColumn() != range.getLastColumn())
                    tableElement.setAttribute(
                            "colspan",
                            String.valueOf(range.getLastColumn()
                                    - range.getFirstColumn() + 1));
                if (range.getFirstRow() != range.getLastRow())
                    tableElement.setAttribute(
                            "rowspan",
                            String.valueOf(range.getLastRow()
                                    - range.getFirstRow() + 1));
            }

            boolean emptyCell;
            if (cell != null) {
                emptyCell = processCell(cell, tableElement,
                        getColumnWidth(sheet, columnIndex), divWidthPx,
                        row.getHeight() / 20f);
            } else {
                emptyCell = true;
            }

            if (emptyCell) {
                emptyCells.add(tableElement);
            } else {
                for (Element emptyCellElement : emptyCells) {
                    tableRowElement.appendChild(emptyCellElement);
                }
                emptyCells.clear();

                tableRowElement.appendChild(tableElement);
                maxRenderedColumn = columnIndex;
            }
        }

        return maxRenderedColumn + 1;
    }

    /**
     * Determines the table element type of this cell.
     * <p>
     * If {@link TableModifications#headerRows} contains the row index of this cell and the cell has a value then returns 'th'.
     * If {@link TableModifications#headerColumns} contains the cell index of this cell and the cell has a value then returns 'th'.
     * 'td' is returned in all other cases.
     */
    private Element getTableCell(Row row, HSSFCell cell, int columnIndex) {
        boolean isHeader = false;
        String scope = null;
        Element tableElement;

        if (tableModifications.isPresent()) {
            if (tableModifications.get().getHeaderRows().contains(row.getRowNum()) && !isEmptyCell(cell)) {
                isHeader = true;
                // If row is defined as header and this column is also a header then col scope takes priority.
                scope = tableModifications.get().getHeaderColumns().contains(columnIndex) ? COL : ROW;

            } else if (tableModifications.get().getHeaderColumns().contains(columnIndex) && !isEmptyCell(cell)) {
                isHeader = true;
                scope = COL;
            }
        }

        if (isHeader) {
            tableElement = this.htmlDocument.createTableHeaderCell();
            tableElement.setAttribute(SCOPE_ATTR, scope);
        } else {
            tableElement = this.htmlDocument.createTableCell();
        }
        return tableElement;
    }

    private static boolean isEmptyCell(HSSFCell cell) {
        switch (cell.getCellType()) {
            case (HSSFCell.CELL_TYPE_STRING):
                return StringUtils.isEmpty(cell.getStringCellValue());
            case (HSSFCell.CELL_TYPE_BLANK):
                return true;
            default:
                return false;
        }
    }

    static class HtmlDocument extends HtmlDocumentFacade {
        HtmlDocument(Document document) {
            super(document);
        }
    }
}

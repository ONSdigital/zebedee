package com.github.onsdigital.zebedee.util;

import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.apache.poi.hssf.converter.ExcelToHtmlUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hwpf.converter.HtmlDocumentFacade;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * Helper class to convert an xls file to a html table.
 */
public class XlsToHtmlConverter extends ExcelToHtmlConverter {

    protected final HtmlDocumentFacade htmlDocument;

    private XlsToHtmlConverter(HtmlDocument htmlDocument) {
        super(htmlDocument);
        this.htmlDocument = htmlDocument;
    }

    /**
     * Convert the xls file at the given path
     *
     * @param xlsFileIn
     * @throws IOException
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public static Document convert(File xlsFileIn) throws ParserConfigurationException, IOException {
        final HSSFWorkbook workbook = ExcelToHtmlUtils.loadXls(xlsFileIn);
        XlsToHtmlConverter converter = createConverter();

        converter.processWorkbook(workbook);
        Document document = converter.getDocument();
        return document;
    }

    /**
     * Serialise and save the given document instance to the given Path.
     *
     * @param document
     * @param fileOut
     * @throws IOException
     * @throws TransformerException
     */
    public static void save(Document document, Path fileOut) throws IOException, TransformerException {
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
    private static void WriteHtmlToFile(Document doc, Path htmlFileOut) throws IOException, TransformerException {
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

    public static String docToString(Document doc) throws IOException, TransformerException {
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
        XlsToHtmlConverter converter = new XlsToHtmlConverter(new HtmlDocument(createDocument()));
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

    private void buildStyle_border(HSSFWorkbook workbook, StringBuilder style,
                                   String type, short xlsBorder, short borderColor) {
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

    void buildStyle_font(HSSFWorkbook workbook, StringBuilder style,
                         HSSFFont font) {
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

//    @Override
//    protected boolean processCell( HSSFCell cell, Element tableCellElement,
//                                   int normalWidthPx, int maxSpannedWidthPx, float normalHeightPt )
//    {
//        boolean result = super.processCell(cell, tableCellElement, normalWidthPx, maxSpannedWidthPx, normalHeightPt);
//
//        HSSFWorkbook workbook = cell.getRow().getSheet().getWorkbook();
//        int formattingRunIndex = 0;
//        int toIndex = 0;
//        int fromIndex = 0;
//
//        if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
//
//            HSSFRichTextString rts = cell.getRichStringCellValue();
//            System.out.println("The cell contains [" +
//                    rts.toString() +
//                    "]");
//
//            HSSFCellStyle style = cell.getCellStyle();
//            HSSFFont font = style.getFont(workbook);
//
//            if (rts.numFormattingRuns() > 0) {
//
//                for (formattingRunIndex = 0; formattingRunIndex < rts.numFormattingRuns(); formattingRunIndex++) {
//                    toIndex = rts.getIndexOfFormattingRun(formattingRunIndex);
//                    System.out.println("\tSubstring [" +
//                            rts.toString().substring(fromIndex, toIndex) +
//                            "]");
//                    if(font.getTypeOffset() == HSSFFont.SS_SUPER || font.getTypeOffset() == HSSFFont.SS_SUB) {
//                        System.out.println("\t\tSuperscripted");
//                    }
//                    else {
//                        System.out.println("\t\tNOT Superscripted");
//                    }
//                    font = workbook.getFontAt(rts.getFontOfFormattingRun(formattingRunIndex));
//                    fromIndex = toIndex;
//                }
//                toIndex = rts.length();
//                System.out.println("\tSubstring [" +
//                        rts.toString().substring(fromIndex, toIndex) +
//                        "]");
//                if(font.getTypeOffset() == HSSFFont.SS_SUPER || font.getTypeOffset() == HSSFFont.SS_SUB) {
//
//                    String content = rts.toString();
//                    String toSuperscript = content.substring(fromIndex, toIndex);
//                    String updated;
//
//                    if(font.getTypeOffset() == HSSFFont.SS_SUPER) {
//                        updated = content.replace(toSuperscript, "<span>" + toSuperscript + "</span><p>hello</p><strong>there</strong>");
//                    } else {
//                        updated = content.replace(toSuperscript, "<span>" + toSuperscript + "</span><p>hello</p><strong>there</strong>");
//                    }
//                    // todo - replace all occurances of superscript
//
//                    tableCellElement.setTextContent(updated);
//
//                    System.out.println("\t\tSuperscripted");
//                }
//                else {
//                    System.out.println("\t\tNOT Superscripted");
//                }
//            }
//            else {
//
//                System.out.print("The String [" + rts.toString());
//                if (font.getTypeOffset() == HSSFFont.SS_SUPER) {
//                    System.out.print("] is ");
//                }
//                else {
//                    System.out.print("] is not ");
//                }
//                System.out.println("superscripted.");
//            }
//        }
//        else {
//            System.out.println("The cell at row number " +
//                    cell.getRowIndex() +
//                    " and column number " +
//                    cell.getColumnIndex() +
//                    " does not contain a String.");
//        }
//
//
//        return result;
//    }

    static class HtmlDocument extends HtmlDocumentFacade {

        HtmlDocument(Document document) {
            super(document);
        }
    }
}

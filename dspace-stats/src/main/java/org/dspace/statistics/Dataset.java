/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Ostermiller.util.ExcelCSVPrinter;
import org.apache.commons.lang.ArrayUtils;

/**
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 21-jan-2009
 * Time: 13:44:48
 * 
 */
public class Dataset {

    private int nbRows;
    private int nbCols;
    /* The labels shown in our columns */
    private List<String> colLabels;
    /* The labels shown in our rows */
    private List<String> rowLabels;
    private String colTitle;
    private String rowTitle;
    /* The attributes for the colls */
    private List<Map<String, String>> colLabelsAttrs;
    /* The attributes for the rows */
    private List<Map<String, String>> rowLabelsAttrs;
    /* The data in a matrix */
    private float[][]matrix;
    /* The format in which we format our floats */
    private String format = "0";



    public Dataset(int rows, int cols){
        matrix = new float[rows][cols];
        nbRows = rows;
        nbCols = cols;
        initColumnLabels(cols);
        initRowLabels(rows);
    }

    public Dataset(float[][] matrix){
        this.matrix = (float[][]) ArrayUtils.clone(matrix);
        nbRows = matrix.length;
        if(0 < matrix.length && 0 < matrix[0].length)
        {
            nbCols = matrix[0].length;
        }
        initColumnLabels(nbCols);
        initRowLabels(nbRows);
    }

    private void initRowLabels(int rows) {
        rowLabels = new ArrayList<String>(rows);
        rowLabelsAttrs = new ArrayList<Map<String, String>>();
        for (int i = 0; i < rows; i++) {
            rowLabels.add("Row " + (i+1));
            rowLabelsAttrs.add(new HashMap<String, String>());
        }
    }

    private void initColumnLabels(int nbCols) {
        colLabels = new ArrayList<String>(nbCols);
        colLabelsAttrs = new ArrayList<Map<String, String>>();
        for (int i = 0; i < nbCols; i++) {
            colLabels.add("Column " + (i+1));
            colLabelsAttrs.add(new HashMap<String, String>());
        }
    }

    public void setColLabel(int n, String label){
        colLabels.set(n, label);
    }

    public void setRowLabel(int n, String label){
        rowLabels.set(n, label);
    }

    public String getRowTitle() {
        return rowTitle;
    }

    public String getColTitle() {
        return colTitle;
    }

    public void setColTitle(String colTitle) {
        this.colTitle = colTitle;
    }


    public void setRowTitle(String rowTitle) {
        this.rowTitle = rowTitle;
    }

    public void setRowLabelAttr(int pos, String attrName, String attr){
        Map<String, String> attrs = rowLabelsAttrs.get(pos);
        attrs.put(attrName, attr);
        rowLabelsAttrs.set(pos, attrs);
    }

    public void setRowLabelAttr(int pos, Map<String, String> attrMap){
        rowLabelsAttrs.set(pos, attrMap);
    }

    public void setColLabelAttr(int pos, String attrName, String attr){
        Map<String, String> attrs = colLabelsAttrs.get(pos);
        attrs.put(attrName, attr);
        colLabelsAttrs.set(pos, attrs);
    }

    public void setColLabelAttr(int pos, Map<String, String> attrMap) {
        colLabelsAttrs.set(pos, attrMap);
    }


    public List<Map<String, String>> getColLabelsAttrs() {
        return colLabelsAttrs;
    }

    public List<Map<String, String>> getRowLabelsAttrs() {
        return rowLabelsAttrs;
    }

    public List<String> getColLabels() {
        return colLabels;
    }

    public List<String> getRowLabels() {
        return rowLabels;
    }

    public float[][] getMatrix() {
        return (float[][]) ArrayUtils.clone(matrix);
    }

    public int getNbRows() {
        return nbRows;
    }

    public int getNbCols() {
        return nbCols;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String[][] getMatrixFormatted(){
        DecimalFormat decimalFormat = new DecimalFormat(format);
        if (matrix.length == 0) {
            return new String[0][0];
        } else {
            String[][] strMatrix = new String[matrix.length][matrix[0].length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    strMatrix[i][j] = decimalFormat.format(matrix[i][j]);
                }
            }
            return strMatrix;
        }
    }

    public void addValueToMatrix(int row, int coll, float value) {
        matrix[row][coll] = value;
    }


    public void addValueToMatrix(int row, int coll, String value) throws ParseException {
        DecimalFormat decimalFormat = new DecimalFormat(format);
        Number number = decimalFormat.parse(value);
        matrix[row][coll] = number.floatValue();
    }

    /**
     * Returns false if this dataset only contains zero's. 
     */
    public boolean containsNonZeroValues(){
        if (matrix != null) {
            for (float[] vector : matrix) {
                for (float v : vector) {
                    if (v != 0)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    public void flipRowCols(){
        //Lets make sure we at least have something to flip
        if(0 < matrix.length && 0 < matrix[0].length){
            //Flip the data first
            float[][] newMatrix = new float[matrix[0].length][matrix.length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    newMatrix[j][i] = matrix[i][j];
                }
            }
            //Flip the rows & column labels
            List<String> backup = colLabels;
            colLabels = rowLabels;
            rowLabels = backup;

            //Also flip the links
            List<Map<String, String>> backList = colLabelsAttrs;
            colLabelsAttrs = rowLabelsAttrs;
            rowLabelsAttrs = backList;

            matrix = newMatrix;
        }
        //Also flip these sizes
        int backUp = nbRows;
        nbRows = nbCols;
        nbCols = backUp;
        //Also flip the title's
        String backup = rowTitle;
        rowTitle = colTitle;
        colTitle = backup;

    }

    public ByteArrayOutputStream exportAsCSV() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExcelCSVPrinter ecsvp = new ExcelCSVPrinter(baos);
        ecsvp.changeDelimiter(';');
        ecsvp.setAlwaysQuote(true);
        //Generate the item row
        List<String> colLabels = getColLabels();
        ecsvp.write("");
        for (String colLabel : colLabels) {
            ecsvp.write(colLabel);
        }
        ecsvp.writeln();
        List<String> rowLabels = getRowLabels();

        String[][] matrix = getMatrixFormatted();
        for (int i = 0; i < rowLabels.size(); i++) {
            String rowLabel = rowLabels.get(i);
            ecsvp.write(rowLabel);
            for (int j = 0; j < matrix[i].length; j++) {
                ecsvp.write(matrix[i][j]);
            }
            ecsvp.writeln();
        }
        ecsvp.flush();
        ecsvp.close();
        return baos;

    }

}

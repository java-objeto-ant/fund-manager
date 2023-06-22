/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.fund.manager.base;

import com.sun.rowset.CachedRowSetImpl;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.UserRight;

/**
 *
 * @author User
 */
public class MCImage {
    private final GRider p_oApp;
    private final boolean p_bWithParent;
    
    private final String MASTER_TABLE = "MC_Model_Images";
    
    private String p_sBranchCd;
    private int p_nEditMode;
    private int p_nTranStat;
    private String p_sMessage;
    private boolean p_bWithUI = true;
    private LMasDetTrans p_oListener;
    private CachedRowSet p_oImages;
    private JSONArray jsonImg;
    
    
    
    
    public MCImage(GRider foApp, String fsBranchCd, boolean fbWithParent){        
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;        
                
        if (p_sBranchCd.isEmpty()) p_sBranchCd = p_oApp.getBranchCode();
        p_nTranStat = 0;
        jsonImg = new JSONArray();
        p_nEditMode = EditMode.UNKNOWN;
    }
    
    public void setTranStat(int fnValue){
        p_nTranStat = fnValue;
    }
   
    public void setListener(LMasDetTrans foValue){
        p_oListener = foValue;
    }
    
    public void setWithUI(boolean fbValue){
        p_bWithUI = fbValue;
    }
    
    public int getEditMode(){
        return p_nEditMode;
    }
    
    public String getMessage(){
        return p_sMessage;
    }
    public boolean NewTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        p_sMessage = "";
        
        createImages();
        p_nEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean UpdateTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        
        if (p_nEditMode != EditMode.READY){
            p_sMessage = "Invalid edit mode.";
            return false;
        }
        
        
        p_nEditMode = EditMode.UPDATE;
        return true;
    }
    
    
    public int getImageCount() throws SQLException{
        if(p_oImages == null){
            return 0;
        }
        p_oImages.last();
        return p_oImages.getRow();
    }
    public int getItemCount() throws SQLException{
        if(jsonImg == null){
            return 0;
        }
        return jsonImg.size();
    }
    public Object getDetail(int fnRow, int fnIndex) throws SQLException{
        if (getImageCount()  == 0) return null;
        
        if (getImageCount() == 0 || fnRow > getImageCount()) return null;   
        p_oImages.absolute(fnRow);
        return p_oImages.getObject(fnIndex);
    }
    
    
    public Object getDetail(int fnRow, String fsIndex) throws SQLException{
        return getDetail(fnRow, getColumnIndex(p_oImages, fsIndex));
    }
    
    public void setDetail(int fnRow, int fnIndex, Object foValue) throws SQLException{
        if (getImageCount()== 0 || fnRow > getImageCount()) return;
        
        p_oImages.absolute(fnRow);
        switch (fnIndex){
            case 3://sImageURL
            case 4://cPrimaryx
            case 8://sBrandIDx
            case 9://sBrandNme
            case 10://sModelIDx
            case 11://sModelCde
            case 12://sModelNme
            case 13://sColorIDx
            case 14://sColorNme
                p_oImages.updateString(fnIndex, (String) foValue);
                p_oImages.updateRow();
                if (p_oListener != null) p_oListener.DetailRetreive(fnRow,fnIndex,getDetail(fnRow, fnIndex));
                break;
        }
        
    }
    
    public void setDetail(int fnRow, String fsIndex, Object foValue) throws SQLException{
        setDetail(fnRow, getColumnIndex(p_oImages, fsIndex), foValue);
    }
    public void setPrimary(int fnRow) throws SQLException{
        int lnCtr = 1;
        p_oImages.beforeFirst();
        while(p_oImages.next()){
            p_oImages.updateObject("cPrimaryx", 0);
            if(lnCtr == fnRow){
                p_oImages.updateObject("cPrimaryx", 1);
            }
            lnCtr++;
        }
    }
    private void createImages() throws SQLException{
        
        RowSetMetaData meta = new RowSetMetaDataImpl();

        meta.setColumnCount(14);

        meta.setColumnName(1, "sMCInvIDx");
        meta.setColumnLabel(1, "sMCInvIDx");
        meta.setColumnType(1, Types.VARCHAR);
        meta.setColumnDisplaySize(1, 9);

        meta.setColumnName(2, "nEntryNox");
        meta.setColumnLabel(2, "nEntryNox");
        meta.setColumnType(2, Types.INTEGER);
        meta.setColumnDisplaySize(2, 4);

        meta.setColumnName(3, "sImageURL");
        meta.setColumnLabel(3, "sImageURL");
        meta.setColumnType(3, Types.VARCHAR);
        meta.setColumnDisplaySize(3, 128);
        
        meta.setColumnName(4, "cPrimaryx");
        meta.setColumnLabel(4, "cPrimaryx");
        meta.setColumnType(4, Types.CHAR);
        meta.setColumnDisplaySize(4, 1);
        
        meta.setColumnName(5, "cRecdStat");
        meta.setColumnLabel(5, "cRecdStat");
        meta.setColumnType(5, Types.CHAR);
        meta.setColumnDisplaySize(5, 1);
        
        meta.setColumnName(6, "sModified");
        meta.setColumnLabel(6, "sModified");
        meta.setColumnType(6, Types.VARCHAR);
        meta.setColumnDisplaySize(6, 12);
        
        meta.setColumnName(7, "dModified");
        meta.setColumnLabel(7, "dModified");
        meta.setColumnType(7, Types.DATE);
        
        meta.setColumnName(8, "sBrandIDx");
        meta.setColumnLabel(8, "sBrandIDx");
        meta.setColumnType(8, Types.VARCHAR);
        meta.setColumnDisplaySize(8, 7);
        
        meta.setColumnName(9, "sBrandNme");
        meta.setColumnLabel(9, "sBrandNme");
        meta.setColumnType(9, Types.VARCHAR);
        meta.setColumnDisplaySize(9, 25);
        
        meta.setColumnName(10, "sModelIDx");
        meta.setColumnLabel(10, "sModelIDx");
        meta.setColumnType(10, Types.VARCHAR);
        meta.setColumnDisplaySize(10, 9);
        
        meta.setColumnName(11, "sModelCde");
        meta.setColumnLabel(11, "sModelCde");
        meta.setColumnType(11, Types.VARCHAR);
        meta.setColumnDisplaySize(11, 25);
        
        meta.setColumnName(12, "sModelNme");
        meta.setColumnLabel(12, "sModelNme");
        meta.setColumnType(12, Types.VARCHAR);
        meta.setColumnDisplaySize(12, 7);
        
        meta.setColumnName(13, "sColorIDx");
        meta.setColumnLabel(13, "sColorIDx");
        meta.setColumnType(13, Types.VARCHAR);
        meta.setColumnDisplaySize(13, 25);
        
        meta.setColumnName(14, "sColorNme");
        meta.setColumnLabel(14, "sColorNme");
        meta.setColumnType(14, Types.VARCHAR);
        meta.setColumnDisplaySize(14, 25);
        
        p_oImages = new CachedRowSetImpl();
        p_oImages.setMetaData(meta);
        
        p_oImages.last();
        p_oImages.moveToInsertRow();
        MiscUtil.initRowSet(p_oImages); 
        p_oImages.updateObject("nEntryNox",  1);
        p_oImages.updateObject("cPrimaryx", 1);
        p_oImages.updateObject("cRecdStat", 1);
        p_oImages.updateObject("sImageURL", "");
        p_oImages.updateObject("sMCInvIDx", "");
        p_oImages.updateObject("cPrimaryx", 1);
        p_oImages.updateObject("sModified", "");
        p_oImages.updateObject("dModified", "");
        p_oImages.updateObject("sBrandNme", "");
        p_oImages.updateObject("sModelNme", "");
        p_oImages.updateObject("sColorNme", "");
        p_oImages.insertRow();
        p_oImages.moveToCurrentRow();
        
    }
    
     public boolean addImage() throws SQLException{
        int lnRox = getImageCount();
        
        if(getDetail(lnRox, "sMCInvIDx").toString().isEmpty() || 
            getDetail(lnRox, "sImageURL").toString().isEmpty()) return false;                                                                                                                                                                                                                                                                                                                                                                    
        
        System.out.println("getDetails = " + getDetail(1, "sMCInvIDx"));
        String sMCInvIDx = (String) getDetail(1, "sMCInvIDx");
        String sBrandNme = (String) getDetail(1, "sBrandNme");
        String sModelNme = (String) getDetail(1, "sModelNme");
        String sColorNme = (String) getDetail(1, "sColorNme");
        String sModelCde = (String) getDetail(1, "sModelCde");
        String sModelIDx = (String) getDetail(1, "sModelIDx");
        String sBrandIDx = (String) getDetail(1, "sBrandIDx");
        String sColorIDx = (String) getDetail(1, "sColorIDx");
        System.out.println("lnRox = " + lnRox);
        
        p_oImages.last();
        p_oImages.moveToInsertRow();
        MiscUtil.initRowSet(p_oImages); 
        p_oImages.updateObject("nEntryNox", lnRox + 1);
        p_oImages.updateObject("cPrimaryx", (lnRox > 0)? 0: 1);
        p_oImages.updateObject("cRecdStat", 1);
        p_oImages.updateObject("sImageURL", "");
        p_oImages.updateObject("sMCInvIDx", sMCInvIDx);
        p_oImages.updateObject("sModified", "");
        p_oImages.updateObject("dModified", "");
        p_oImages.updateObject("sBrandNme", sBrandNme);
        p_oImages.updateObject("sModelNme", sModelNme);
        p_oImages.updateObject("sModelCde", sModelCde);
        p_oImages.updateObject("sModelIDx", sModelIDx);
        p_oImages.updateObject("sBrandIDx", sBrandIDx);
        p_oImages.updateObject("sColorIDx", sColorIDx);
        p_oImages.insertRow();
        p_oImages.moveToCurrentRow();

        System.out.println();
        return true;
    }
     
    public boolean removeImage(int fnRow) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }
        
        if (fnRow > getImageCount()) return false;
        
        p_oImages.absolute(fnRow);
        p_oImages.deleteRow();
        
        return true;
    }
    public boolean SearchTransaction(String fsValue, boolean fbByCode) throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        String lsSQL = getSQ_Images();
        String lsCondition = "";
        
       
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sMCInvIDx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sMCInvIDx LIKE " + SQLUtil.toSQL(fsValue + "%")); 
        }
        if (!lsCondition.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        System.out.println(lsSQL);
        lsSQL = lsSQL + " GROUP BY a.sMCInvIDx";
        if (p_bWithUI){
            JSONObject loJSON = showFXDialog.jsonSearch(
                                p_oApp, 
                                lsSQL, 
                                fsValue, 
                                "MC Inv. ID»Brand»Model»Color", 
                                "a.sMCInvIDx»d.sBrandNme»c.sModelNme»e.sColorNme",
                                "a.sMCInvIDx»d.sBrandNme»c.sModelNme»e.sColorNme",
                                fbByCode ? 0 : 1);
            
            if (loJSON != null) 
                return OpenTransaction((String) loJSON.get("sMCInvIDx"));
            else {
                p_sMessage = "No record selected.";
                return false;
            }
        }
        
        if (fbByCode)
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sMCInvIDx = " + SQLUtil.toSQL(fsValue));   
        else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sMCInvIDx LIKE " + SQLUtil.toSQL(fsValue + "%")); 
            lsSQL += " LIMIT 1";
        }
        
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        
        if (!loRS.next()){
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }
        
        lsSQL = loRS.getString("sMCInvIDx");
        MiscUtil.close(loRS);
        
        return OpenTransaction(lsSQL);
    }
    
    public boolean OpenTransaction(String fsTransNox) throws SQLException{
        p_nEditMode = EditMode.UNKNOWN;
        
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();
        
        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Images(), "a.sMCInvIDx = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oImages = factory.createCachedRowSet();
        p_oImages.populate(loRS);
        MiscUtil.close(loRS);
        
        p_nEditMode = EditMode.READY;
        
        return true;
    }
    public boolean searchItem(int fnRow, String fsValue, boolean fbByCode, boolean fbSearch) throws SQLException{
        if (p_nEditMode != EditMode.ADDNEW) {
            p_sMessage = "This feature was for New Tranactions Only.";
            return false;
        }
                
        String lsCondition = "";
        
        if (fbSearch)
            lsCondition = "b.sModelCde LIKE " + SQLUtil.toSQL(fsValue + "%");
        else
            lsCondition = "b.sModelNme LIKE " + SQLUtil.toSQL(fsValue + "%");
        
        String lsSQL = "SELECT" +
                    "  a.sMCInvIDx " +
                    ", b.sModelCde " +
                    ", b.sModelIDx " +
                    ", c.sBrandIDx " +
                    ", d.sColorIDx " +
                    ", c.sBrandNme " +
                    ", b.sModelNme " +
                    ", d.sColorNme " +
                    ", a.nSelPrice " +
                " FROM MC_Inventory a" +
                    " LEFT JOIN Color d" +
                       " ON a.sColorIDx = d.sColorIDx" +
                    ", MC_Model b" +
                    ", Brand c" +
                " WHERE b.sBrandIDx = c.sBrandIDx" +
                    " AND a.sModelIDx = b.sModelIDx" +
                    " AND a.sBranchCd =  " + SQLUtil.toSQL(p_oApp.getBranchCode());
        
        if (!lsCondition.isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        ResultSet loRS;
        
        p_oImages.absolute(fnRow);
        if (!p_bWithUI){
            lsSQL += " LIMIT 1";
            
            loRS = p_oApp.executeQuery(lsSQL);
            
            if (loRS.next()){
                if(!loRS.getString("sMCInvIDx").equalsIgnoreCase((String) getDetail(1, "sMCInvIDx"))){
                   p_sMessage = "MC Model  can only be searched once.  Please Create New Transaction for Other model!";
                   return false;
                }
                p_oImages.updateObject("sMCInvIDx", loRS.getString("sMCInvIDx"));
                p_oImages.updateObject("sModelCde", loRS.getString("sModelCde"));
                p_oImages.updateObject("sModelIDx", loRS.getString("sModelIDx"));
                p_oImages.updateObject("sBrandIDx", loRS.getString("sBrandIDx"));
                p_oImages.updateObject("sColorIDx", loRS.getString("sColorIDx"));
                p_oImages.updateObject("sBrandNme", loRS.getString("sBrandNme"));
                p_oImages.updateObject("sModelNme", loRS.getString("sModelNme"));
                p_oImages.updateObject("sColorNme", loRS.getString("sColorNme"));
                p_oImages.updateRow();

                if (p_oListener != null){
                    p_oListener.DetailRetreive(fnRow,1, getDetail(fnRow,"sMCInvIDx"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelCde"), getDetail(fnRow,"sModelCde"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelIDx"), getDetail(fnRow,"sModelIDx"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sBrandIDx"), getDetail(fnRow,"sBrandIDx"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sColorIDx"), getDetail(fnRow,"sColorIDx"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sBrandNme"), getDetail(fnRow,"sBrandNme"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelNme"), getDetail(fnRow,"sModelNme"));
                    p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sColorNme"), getDetail(fnRow,"sColorNme"));
                }

                
                return true;
            } else return false;
        }
        
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        
        JSONObject loJSON;
        
        loJSON = showFXDialog.jsonBrowse(
                    p_oApp, 
                    loRS, 
                    "Brand»Model»Color»SRP", 
                    "sBrandNme»sModelNme»sColorNme»nSelPrice");

        if (loJSON != null){
                System.out.println("loRS = " + (String) loJSON.get("sMCInvIDx"));
                System.out.println("getDetail = " + getDetail(1, "sMCInvIDx"));
                
            if(!getDetail(1, "sMCInvIDx").toString().isEmpty()){
                if(!loRS.getString("sMCInvIDx").equalsIgnoreCase((String) getDetail(1, "sMCInvIDx"))){
                    p_sMessage = "MC Model  can only be searched once.  Please Create New Transaction for Other model!";
                    return false;
                }
            }
            p_oImages.updateObject("sMCInvIDx", (String) loJSON.get("sMCInvIDx"));
            p_oImages.updateObject("sModelCde", (String) loJSON.get("sModelCde"));
            p_oImages.updateObject("sModelIDx", (String) loJSON.get("sModelIDx"));
            p_oImages.updateObject("sBrandIDx", (String) loJSON.get("sBrandIDx"));
            p_oImages.updateObject("sColorIDx", (String) loJSON.get("sColorIDx"));
            p_oImages.updateObject("sBrandNme", (String) loJSON.get("sBrandNme"));
            p_oImages.updateObject("sModelNme", (String) loJSON.get("sModelNme"));
            p_oImages.updateObject("sColorNme", (String) loJSON.get("sColorNme"));
//            p_oImages.updateObject("nUnitPrce", Double.valueOf((String) loJSON.get("nSelPrice")));
            p_oImages.updateRow();

//            if (p_oListener != null){
//                p_oListener.DetailRetreive(fnRow, 1, getDetail(fnRow,"sMCInvIDx"));
//                p_oListener.DetailRetreive(fnRow, 11, getDetail(fnRow,"sModelCde"));
//                p_oListener.DetailRetreive(fnRow, 10, getDetail(fnRow,"sModelIDx"));
//                p_oListener.DetailRetreive(fnRow, 8, getDetail(fnRow,"sBrandIDx"));
//                p_oListener.DetailRetreive(fnRow, 13, getDetail(fnRow,"sColorIDx"));
//                p_oListener.DetailRetreive(fnRow, 9, getDetail(fnRow,"sBrandNme"));
//                p_oListener.DetailRetreive(fnRow,12, getDetail(fnRow,"sModelNme"));
//                p_oListener.DetailRetreive(fnRow, 14, getDetail(fnRow,"sColorNme"));
//            }

            if (p_oListener != null){
                p_oListener.DetailRetreive(fnRow,1, getDetail(fnRow,"sMCInvIDx"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelCde"), getDetail(fnRow,"sModelCde"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelIDx"), getDetail(fnRow,"sModelIDx"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sBrandIDx"), getDetail(fnRow,"sBrandIDx"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sColorIDx"), getDetail(fnRow,"sColorIDx"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sBrandNme"), getDetail(fnRow,"sBrandNme"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sModelNme"), getDetail(fnRow,"sModelNme"));
                p_oListener.DetailRetreive(fnRow, getColumnIndex(p_oImages, "sColorNme"), getDetail(fnRow,"sColorNme"));
            }
            return true;
        }
        return false;
    }
     public boolean SaveTransaction() throws SQLException{
        if (p_oApp == null){
            p_sMessage = "Application driver is not set.";
            return false;
        }
        
        p_sMessage = "";
        
        if (p_nEditMode != EditMode.ADDNEW &&
            p_nEditMode != EditMode.UPDATE){
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }
        
        
        int lnCtr;
        int lnRow;
        String lsSQL;
        
        if (p_nEditMode == EditMode.ADDNEW){    
            if (!p_bWithParent) p_oApp.beginTrans();
            
            lnCtr = 1;
            p_oImages.beforeFirst();
            while (p_oImages.next()){
                
                if (!isEntryOK()) return false;
                String lsInvIDx =(String) getDetail(lnCtr, "sMCInvIDx");
                
            //set transaction number on records
                p_oImages.updateObject("sModified", p_oApp.getUserID());
                p_oImages.updateObject("dModified", p_oApp.getServerDate());
                p_oImages.updateObject("nEntryNox", lnCtr);
                p_oImages.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oImages, MASTER_TABLE, "sBrandIDx;sModelCde;sModelIDx;sColorIDx;sBrandNme;sModelNme;sColorNme");
                               
                if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd,  lsInvIDx.substring(0, 4)) <= 0){
                    if (!p_bWithParent) p_oApp.rollbackTrans();
                    p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                    return false;
                }
                
                lnCtr++;
            }
            
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        } else {    
            if (!p_bWithParent) p_oApp.beginTrans(); 
            lnCtr = 1;
             p_oImages.beforeFirst();
            while (p_oImages.next()){
                String lsInvIDx = p_oImages.getString("sMCInvIDx");
                
                p_oImages.updateObject("sModified", p_oApp.getUserID());
                p_oImages.updateObject("dModified", p_oApp.getServerDate());
                p_oImages.updateObject("nEntryNox", lnCtr);
                p_oImages.updateRow();
                
                lsSQL = MiscUtil.rowset2SQL(p_oImages, 
                                            MASTER_TABLE, 
                                            "sBrandIDx;sModelCde;sModelIDx;sColorIDx;sBrandNme;sModelNme;sColorNme",
                                            "sMCInvIDx = " + SQLUtil.toSQL(lsInvIDx) +
                                            " AND nEntryNox = " + SQLUtil.toSQL(lnCtr));
                
                if (!lsSQL.isEmpty()){
                    if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsInvIDx.substring(0, 4)) <= 0){
                        if (!p_bWithParent) p_oApp.rollbackTrans();
                        p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                        return false;
                    }
                }
                lnCtr++;
            }
            
            
            if (!p_bWithParent) p_oApp.commitTrans();
            
            p_nEditMode = EditMode.UNKNOWN;
            return true;
        }
    }
    
    private boolean isEntryOK() throws SQLException{           
        //validate master        
        //validate detail
        if (getImageCount() == 0){
            p_sMessage = "No employee detected.";
            return false;
        }
        
        p_oImages.beforeFirst();
        while (p_oImages.next()){            
            if (p_oImages.getString("sImageUrl").isEmpty()){
                p_sMessage = p_oImages.getString("sImageUrl") + " has empty value.";
                return false;
            }    
        }
        return true;
    }
    
    public String getSQ_Images(){
        String lsSQL = "";
        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);
        
        if (lsStat.length() > 1){
            for (int lnCtr = 0; lnCtr <= lsStat.length()-1; lnCtr++){
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            
            lsCondition = "a.cRecdStat IN (" + lsSQL.substring(2) + ")";
        } else 
            lsCondition = "a.cRecdStat = " + SQLUtil.toSQL(lsStat);
                
        lsSQL = "SELECT " +
                    " a.sMCInvIDx " +
                    ", a.nEntryNox " +
                    ", a.sImageURL " +
                    ", a.cPrimaryx " +
                    ", a.cRecdStat " +
                    ", a.sModified " + 
                    ", a.dModified " +
                    ", d.sBrandIDx " +
                    ", d.sBrandNme " +
                    ", c.sModelIDx " +
                    ", c.sModelCde " +
                    ", c.sModelNme " +
                    ", e.sColorIDx " +
                    ", e.sColorNme " +
                " FROM MC_Model_Images a" +
                "  LEFT JOIN MC_Inventory b" +
                       " ON a.sMCInvIDx = b.sMCInvIDx" +
                    " LEFT JOIN Color e" +
                       " ON b.sColorIDx = e.sColorIDx" +
                    ", MC_Model c" +
                    ", Brand d" +
                " WHERE c.sBrandIDx = d.sBrandIDx" +
                    " AND b.sModelIDx = c.sModelIDx" +
                    " AND b.sBranchCd =  " + SQLUtil.toSQL(p_oApp.getBranchCode()) +
                    " AND " + lsCondition;
        
        return lsSQL;
    }
    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException{
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();
        
        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))){
                lnIndex = lnCtr;
                break;
            }
        }
        
        return lnIndex;
    }
    

}
